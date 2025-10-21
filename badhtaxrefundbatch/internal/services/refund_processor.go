package services

import (
	"context"
	"fmt"
	"sync"

	"badhtaxrefundbatch/internal/database"

	"github.com/sirupsen/logrus"
)

// RefundProcessor handles the processing of refunds
type RefundProcessor struct {
	refundRepo *database.RefundRepository
	irsService *IRSService
	maxWorkers int
	batchSize  int
}

// NewRefundProcessor creates a new refund processor
func NewRefundProcessor(refundRepo *database.RefundRepository, irsService *IRSService, maxWorkers, batchSize int) *RefundProcessor {
	return &RefundProcessor{
		refundRepo: refundRepo,
		irsService: irsService,
		maxWorkers: maxWorkers,
		batchSize:  batchSize,
	}
}

// ProcessPendingRefunds processes all pending refunds in batches
func (p *RefundProcessor) ProcessPendingRefunds(ctx context.Context) error {
	logrus.Info("Starting to process pending refunds")

	for {
		select {
		case <-ctx.Done():
			logrus.Info("Context cancelled, stopping refund processing")
			return ctx.Err()
		default:
			// Get pending refunds
			refunds, err := p.refundRepo.GetPendingRefunds(p.batchSize)
			if err != nil {
				logrus.WithError(err).Error("Failed to get pending refunds")
				return err
			}

			if len(refunds) == 0 {
				logrus.Info("No pending refunds found")
				return nil
			}

			logrus.WithField("count", len(refunds)).Info("Processing batch of refunds")

			// Process refunds concurrently
			if err := p.processRefundsConcurrently(ctx, refunds); err != nil {
				logrus.WithError(err).Error("Failed to process refunds")
				return err
			}

			// If we got fewer refunds than batch size, we're done
			if len(refunds) < p.batchSize {
				logrus.Info("Processed all pending refunds")
				return nil
			}
		}
	}
}

// processRefundsConcurrently processes refunds using a worker pool
func (p *RefundProcessor) processRefundsConcurrently(ctx context.Context, refunds []database.Refund) error {
	// Create channels for work distribution
	refundChan := make(chan database.Refund, len(refunds))
	resultChan := make(chan error, len(refunds))

	// Start workers
	var wg sync.WaitGroup
	for i := 0; i < p.maxWorkers; i++ {
		wg.Add(1)
		go p.worker(ctx, i, refundChan, resultChan, &wg)
	}

	// Send refunds to workers
	for _, refund := range refunds {
		refundChan <- refund
	}
	close(refundChan)

	// Wait for all workers to complete
	go func() {
		wg.Wait()
		close(resultChan)
	}()

	// Collect results
	var errors []error
	for err := range resultChan {
		if err != nil {
			errors = append(errors, err)
		}
	}

	if len(errors) > 0 {
		return fmt.Errorf("encountered %d errors during processing", len(errors))
	}

	return nil
}

// worker processes refunds from the channel
func (p *RefundProcessor) worker(ctx context.Context, workerID int, refundChan <-chan database.Refund, resultChan chan<- error, wg *sync.WaitGroup) {
	defer wg.Done()

	for refund := range refundChan {
		select {
		case <-ctx.Done():
			resultChan <- ctx.Err()
			return
		default:
			if err := p.processRefund(ctx, &refund); err != nil {
				logrus.WithFields(logrus.Fields{
					"worker_id": workerID,
					"file_id":   refund.FileID,
					"error":     err,
				}).Error("Failed to process refund")
				resultChan <- err
			} else {
				logrus.WithFields(logrus.Fields{
					"worker_id": workerID,
					"file_id":   refund.FileID,
					"status":    refund.Status,
				}).Info("Successfully processed refund")
				resultChan <- nil
			}
		}
	}
}

// processRefund processes a single refund
func (p *RefundProcessor) processRefund(ctx context.Context, refund *database.Refund) error {
	logrus.WithField("file_id", refund.FileID).Debug("Processing refund")

	// Validate file ID
	if err := p.irsService.ValidateFileID(refund.FileID); err != nil {
		refund.Status = "error"
		errorMsg := fmt.Sprintf("Invalid file ID: %v", err)
		refund.ErrorMessage = &errorMsg
		return p.refundRepo.UpdateRefundStatus(refund)
	}

	// Get refund status from IRS
	status, err := p.irsService.GetRefundStatus(refund.FileID)
	if err != nil {
		refund.Status = "error"
		errorMsg := fmt.Sprintf("IRS service error: %v", err)
		refund.ErrorMessage = &errorMsg
		return p.refundRepo.UpdateRefundStatus(refund)
	}

	// Update refund based on IRS response
	// Only update database for final statuses (processed/error)
	// Leave "in_progress" records as "pending" for next run
	if status.Status == "processed" || status.Status == "error" {
		refund.Status = status.Status
		refund.ErrorMessage = status.ErrorMessage
		refund.ProcessedAt = status.ProcessedAt

		// Save updated refund
		if err := p.refundRepo.UpdateRefundStatus(refund); err != nil {
			return fmt.Errorf("failed to update refund status: %w", err)
		}

		logrus.WithFields(logrus.Fields{
			"file_id": refund.FileID,
			"status":  status.Status,
		}).Info("Updated refund status")
	} else {
		// For "in_progress" status, just log and leave as pending
		logrus.WithFields(logrus.Fields{
			"file_id": refund.FileID,
			"status":  status.Status,
		}).Info("Refund still in progress, will retry in next run")
	}

	return nil
}

// GetProcessingStats returns statistics about the processing
func (p *RefundProcessor) GetProcessingStats() (map[string]int64, error) {
	return p.refundRepo.GetRefundStats()
}

// HealthCheck performs a health check on the refund processor
func (p *RefundProcessor) HealthCheck() error {
	// Check database connection
	stats, err := p.refundRepo.GetRefundStats()
	if err != nil {
		return fmt.Errorf("database health check failed: %w", err)
	}

	// Check IRS service
	if err := p.irsService.GetServiceHealth(); err != nil {
		return fmt.Errorf("IRS service health check failed: %w", err)
	}

	logrus.WithFields(logrus.Fields{
		"total_refunds":     stats["total"],
		"pending_refunds":   stats["pending"],
		"processed_refunds": stats["processed"],
		"error_refunds":     stats["error"],
	}).Debug("Health check completed successfully")

	return nil
}
