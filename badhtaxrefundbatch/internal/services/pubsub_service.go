package services

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"badhtaxrefundbatch/internal/config"
	"badhtaxrefundbatch/internal/database"

	"cloud.google.com/go/pubsub"
	"github.com/sirupsen/logrus"
)

// PubSubMessage represents the structure of messages from the tax file service
type PubSubMessage struct {
	FileID       string  `json:"file_id"`
	Status       string  `json:"status"`
	ErrorMessage *string `json:"error_message"`
	RefundAmount string  `json:"refund_amount"`
	UserID       string  `json:"user_id"`
	Year         int     `json:"year"`
	ETA          string  `json:"eta"`
	Timestamp    string  `json:"timestamp"`
}

// PubSubService handles Pub/Sub operations
type PubSubService struct {
	client       *pubsub.Client
	subscription *pubsub.Subscription
	refundRepo   *database.RefundRepository
	config       *config.Config
}

// NewPubSubService creates a new Pub/Sub service
func NewPubSubService(cfg *config.Config, refundRepo *database.RefundRepository) (*PubSubService, error) {
	if !cfg.EnablePubSub {
		logrus.Info("Pub/Sub is disabled, returning nil service")
		return nil, nil
	}

	ctx := context.Background()

	// Create Pub/Sub client
	client, err := pubsub.NewClient(ctx, cfg.GoogleCloudProject)
	if err != nil {
		return nil, fmt.Errorf("failed to create Pub/Sub client: %w", err)
	}

	// Get or create subscription
	subscription := client.Subscription(cfg.PubSubSubscriptionName)

	// Check if subscription exists, create if not
	exists, err := subscription.Exists(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to check subscription existence: %w", err)
	}

	if !exists {
		logrus.WithField("subscription", cfg.PubSubSubscriptionName).Info("Creating Pub/Sub subscription")
		topic := client.Topic(cfg.PubSubTopicName)

		// Check if topic exists
		topicExists, err := topic.Exists(ctx)
		if err != nil {
			return nil, fmt.Errorf("failed to check topic existence: %w", err)
		}

		if !topicExists {
			return nil, fmt.Errorf("topic %s does not exist", cfg.PubSubTopicName)
		}

		subscription, err = client.CreateSubscription(ctx, cfg.PubSubSubscriptionName, pubsub.SubscriptionConfig{
			Topic:       topic,
			AckDeadline: 20 * time.Second,
		})
		if err != nil {
			return nil, fmt.Errorf("failed to create subscription: %w", err)
		}
		logrus.WithField("subscription", cfg.PubSubSubscriptionName).Info("Created Pub/Sub subscription")
	}

	return &PubSubService{
		client:       client,
		subscription: subscription,
		refundRepo:   refundRepo,
		config:       cfg,
	}, nil
}

// PullMessages pulls messages from Pub/Sub and loads them into the database
func (p *PubSubService) PullMessages(ctx context.Context) error {
	if p == nil {
		logrus.Info("Pub/Sub service is disabled, skipping message pull")
		return nil
	}

	logrus.Info("Starting to pull messages from Pub/Sub")

	// Set up message handling
	ctx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()

	var messageCount int
	err := p.subscription.Receive(ctx, func(ctx context.Context, msg *pubsub.Message) {
		messageCount++
		logrus.WithFields(logrus.Fields{
			"message_id": msg.ID,
			"count":      messageCount,
		}).Info("Received message from Pub/Sub")

		// Parse the message
		var pubsubMsg PubSubMessage
		if err := json.Unmarshal(msg.Data, &pubsubMsg); err != nil {
			logrus.WithError(err).Error("Failed to parse Pub/Sub message")
			msg.Ack() // Acknowledge to remove from queue
			return
		}

		// Convert to database refund record
		refund := &database.Refund{
			FileID:       pubsubMsg.FileID,
			Status:       "pending", // Always set as pending for batch processing
			ErrorMessage: pubsubMsg.ErrorMessage,
			UserID:       &pubsubMsg.UserID,
			Year:         &pubsubMsg.Year,
			RefundAmount: &pubsubMsg.RefundAmount,
			ETA:          &pubsubMsg.ETA,
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}

		// Check if refund already exists
		existingRefund, err := p.refundRepo.GetRefundByFileID(pubsubMsg.FileID)
		if err == nil && existingRefund != nil {
			logrus.WithField("file_id", pubsubMsg.FileID).Info("Refund already exists, skipping")
			msg.Ack()
			return
		}

		// Create new refund record
		if err := p.refundRepo.CreateRefund(refund); err != nil {
			logrus.WithError(err).WithField("file_id", pubsubMsg.FileID).Error("Failed to create refund from Pub/Sub message")
			msg.Nack() // Negative acknowledge to retry later
			return
		}

		logrus.WithFields(logrus.Fields{
			"file_id": pubsubMsg.FileID,
			"user_id": pubsubMsg.UserID,
			"year":    pubsubMsg.Year,
		}).Info("Successfully loaded Pub/Sub message into database")

		msg.Ack() // Acknowledge successful processing
	})

	if err != nil {
		return fmt.Errorf("failed to receive messages: %w", err)
	}

	logrus.WithField("message_count", messageCount).Info("Finished pulling messages from Pub/Sub")
	return nil
}

// Close closes the Pub/Sub client
func (p *PubSubService) Close() error {
	if p == nil || p.client == nil {
		return nil
	}
	return p.client.Close()
}
