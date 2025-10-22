package main

import (
	"context"
	"os"
	"os/signal"
	"syscall"
	"time"

	"badhtaxrefundbatch/internal/config"
	"badhtaxrefundbatch/internal/database"
	"badhtaxrefundbatch/internal/services"

	"github.com/sirupsen/logrus"
)

func main() {
	// Load configuration
	cfg, err := config.LoadConfig()
	if err != nil {
		logrus.WithError(err).Fatal("Failed to load configuration")
	}

	logrus.WithFields(logrus.Fields{
		"database_url":           cfg.DatabaseURL,
		"max_concurrent_workers": cfg.MaxConcurrentWorkers,
		"batch_size":             cfg.BatchSize,
		"processing_interval":    cfg.ProcessingInterval,
	}).Info("Starting BadhTaxRefundBatch")

	// Connect to database
	db, err := database.ConnectDB(cfg.DatabaseURL)
	if err != nil {
		logrus.WithError(err).Fatal("Failed to connect to database")
	}
	defer func() {
		if err := database.CloseDB(db); err != nil {
			logrus.WithError(err).Error("Failed to close database connection")
		}
	}()

	// Run database migrations
	if err := database.MigrateDB(db); err != nil {
		logrus.WithError(err).Fatal("Failed to migrate database")
	}

	// Create repositories and services
	refundRepo := database.NewRefundRepository(db)
	irsService := services.NewIRSService()

	// Create Pub/Sub service
	pubsubService, err := services.NewPubSubService(cfg, refundRepo)
	if err != nil {
		logrus.WithError(err).Fatal("Failed to create Pub/Sub service")
	}

	// Create refund processor with Pub/Sub service
	refundProcessor := services.NewRefundProcessor(refundRepo, irsService, pubsubService, cfg.MaxConcurrentWorkers, cfg.BatchSize)
	defer func() {
		if pubsubService != nil {
			if err := pubsubService.Close(); err != nil {
				logrus.WithError(err).Error("Failed to close Pub/Sub service")
			}
		}
	}()

	// Note: Seed data functionality removed - batch job now only processes real refunds from Pub/Sub

	// Create context for graceful shutdown
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Handle shutdown signals
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		sig := <-sigChan
		logrus.WithField("signal", sig).Info("Received shutdown signal")
		cancel()
	}()

	// Run health check
	if err := refundProcessor.HealthCheck(); err != nil {
		logrus.WithError(err).Fatal("Health check failed")
	}

	// Process refunds
	if err := processRefunds(ctx, refundProcessor, pubsubService, cfg.ProcessingInterval); err != nil {
		logrus.WithError(err).Error("Failed to process refunds")
		os.Exit(1)
	}

	logrus.Info("BadhTaxRefundBatch completed successfully")
}

// processRefunds processes refunds either once or continuously based on configuration
func processRefunds(ctx context.Context, processor *services.RefundProcessor, pubsubService *services.PubSubService, interval time.Duration) error {
	// If interval is 0, process once and exit
	if interval == 0 {
		logrus.Info("Processing refunds once")
		return processRefundsOnce(ctx, processor, pubsubService)
	}

	// Process continuously with interval
	logrus.WithField("interval", interval).Info("Processing refunds continuously")
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			logrus.Info("Stopping refund processing")
			return ctx.Err()
		case <-ticker.C:
			logrus.Info("Starting scheduled refund processing")

			if err := processRefundsOnce(ctx, processor, pubsubService); err != nil {
				logrus.WithError(err).Error("Failed to process refunds in scheduled run")
				// Continue processing even if one batch fails
			}

			// Log statistics
			stats, err := processor.GetProcessingStats()
			if err != nil {
				logrus.WithError(err).Error("Failed to get processing statistics")
			} else {
				logrus.WithFields(logrus.Fields{
					"total_refunds":     stats["total"],
					"pending_refunds":   stats["pending"],
					"processed_refunds": stats["processed"],
					"error_refunds":     stats["error"],
				}).Info("Processing statistics")
			}
		}
	}
}

// processRefundsOnce pulls messages from Pub/Sub and processes all pending refunds
func processRefundsOnce(ctx context.Context, processor *services.RefundProcessor, pubsubService *services.PubSubService) error {
	// First, pull messages from Pub/Sub and load them into database
	if pubsubService != nil {
		logrus.Info("Pulling messages from Pub/Sub")
		if err := pubsubService.PullMessages(ctx); err != nil {
			logrus.WithError(err).Error("Failed to pull messages from Pub/Sub")
			// Continue processing even if Pub/Sub fails
		}
	}

	// Then process all pending refunds (including those from Pub/Sub)
	logrus.Info("Processing all pending refunds")
	return processor.ProcessPendingRefunds(ctx)
}

// Note: seedData function removed - batch job now only processes real refunds from Pub/Sub
