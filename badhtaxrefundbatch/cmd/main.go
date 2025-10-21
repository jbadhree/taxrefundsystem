package main

import (
	"context"
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"

	"badhtaxrefundbatch/internal/config"
	"badhtaxrefundbatch/internal/database"
	"badhtaxrefundbatch/internal/seeder"
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
		"seed_data":              cfg.SeedData,
		"csv_file_path":          cfg.CSVFilePath,
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
	refundProcessor := services.NewRefundProcessor(refundRepo, irsService, cfg.MaxConcurrentWorkers, cfg.BatchSize)

	// Seed data if requested
	if cfg.SeedData {
		if err := seedData(refundRepo, cfg.CSVFilePath); err != nil {
			logrus.WithError(err).Fatal("Failed to seed data")
		}
	}

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
	if err := processRefunds(ctx, refundProcessor, cfg.ProcessingInterval); err != nil {
		logrus.WithError(err).Error("Failed to process refunds")
		os.Exit(1)
	}

	logrus.Info("BadhTaxRefundBatch completed successfully")
}

// processRefunds processes refunds either once or continuously based on configuration
func processRefunds(ctx context.Context, processor *services.RefundProcessor, interval time.Duration) error {
	// If interval is 0, process once and exit
	if interval == 0 {
		logrus.Info("Processing refunds once")
		return processor.ProcessPendingRefunds(ctx)
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

			if err := processor.ProcessPendingRefunds(ctx); err != nil {
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

// seedData seeds the database with sample data
func seedData(refundRepo *database.RefundRepository, csvFilePath string) error {
	logrus.Info("Starting data seeding")

	// Check if CSV file exists
	if _, err := os.Stat(csvFilePath); os.IsNotExist(err) {
		logrus.WithField("file_path", csvFilePath).Info("CSV file does not exist, creating sample data")

		// Create sample CSV file
		seeder := seeder.NewCSVSeeder(refundRepo)
		if err := seeder.CreateSampleCSV(csvFilePath, 100); err != nil {
			return fmt.Errorf("failed to create sample CSV: %w", err)
		}
	}

	// Seed from CSV
	seeder := seeder.NewCSVSeeder(refundRepo)
	if err := seeder.SeedRefundsFromCSV(csvFilePath); err != nil {
		return fmt.Errorf("failed to seed from CSV: %w", err)
	}

	logrus.Info("Data seeding completed successfully")
	return nil
}



