package seeder

import (
	"encoding/csv"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"time"

	"badhtaxrefundbatch/internal/database"

	"github.com/sirupsen/logrus"
)

// CSVSeeder handles seeding data from CSV files
type CSVSeeder struct {
	refundRepo *database.RefundRepository
}

// NewCSVSeeder creates a new CSV seeder
func NewCSVSeeder(refundRepo *database.RefundRepository) *CSVSeeder {
	return &CSVSeeder{
		refundRepo: refundRepo,
	}
}

// SeedRefundsFromCSV seeds refunds from a CSV file
func (s *CSVSeeder) SeedRefundsFromCSV(filePath string) error {
	logrus.WithField("file_path", filePath).Info("Starting to seed refunds from CSV")

	// Open CSV file
	file, err := os.Open(filePath)
	if err != nil {
		return fmt.Errorf("failed to open CSV file: %w", err)
	}
	defer file.Close()

	// Create CSV reader
	reader := csv.NewReader(file)
	reader.FieldsPerRecord = -1 // Allow variable number of fields

	// Read header row
	header, err := reader.Read()
	if err != nil {
		return fmt.Errorf("failed to read CSV header: %w", err)
	}

	logrus.WithField("header", header).Debug("CSV header read")

	// Process each row
	var refunds []database.Refund
	lineNumber := 1 // Start from 1 since we already read the header

	for {
		record, err := reader.Read()
		if err == io.EOF {
			break
		}
		if err != nil {
			logrus.WithFields(logrus.Fields{
				"line_number": lineNumber,
				"error":       err,
			}).Error("Failed to read CSV line")
			return fmt.Errorf("failed to read CSV line %d: %w", lineNumber, err)
		}

		lineNumber++

		// Parse refund from CSV record
		refund, err := s.parseRefundFromCSV(record, lineNumber)
		if err != nil {
			logrus.WithFields(logrus.Fields{
				"line_number": lineNumber,
				"record":      record,
				"error":       err,
			}).Error("Failed to parse refund from CSV")
			continue // Skip invalid records
		}

		refunds = append(refunds, *refund)
	}

	if len(refunds) == 0 {
		logrus.Warn("No valid refunds found in CSV file")
		return nil
	}

	// Batch insert refunds
	if err := s.batchInsertRefunds(refunds); err != nil {
		return fmt.Errorf("failed to batch insert refunds: %w", err)
	}

	logrus.WithField("count", len(refunds)).Info("Successfully seeded refunds from CSV")
	return nil
}

// parseRefundFromCSV parses a refund from a CSV record
func (s *CSVSeeder) parseRefundFromCSV(record []string, lineNumber int) (*database.Refund, error) {
	// Expected CSV format: file_id,status,error_message
	if len(record) < 1 {
		return nil, fmt.Errorf("record must have at least 1 field (file_id)")
	}

	refund := &database.Refund{
		FileID:    record[0],
		Status:    "pending", // Default status
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	// Parse optional status if provided
	if len(record) > 1 && record[1] != "" {
		refund.Status = record[1]
	}

	// Parse optional error message if provided
	if len(record) > 2 && record[2] != "" {
		refund.ErrorMessage = &record[2]
	}

	// Validate refund
	if err := s.validateRefund(refund); err != nil {
		return nil, fmt.Errorf("validation failed: %w", err)
	}

	return refund, nil
}

// validateRefund validates a refund record
func (s *CSVSeeder) validateRefund(refund *database.Refund) error {
	if refund.FileID == "" {
		return fmt.Errorf("file_id cannot be empty")
	}

	validStatuses := map[string]bool{
		"pending":     true,
		"processed":   true,
		"error":       true,
		"in_progress": true,
	}

	if !validStatuses[refund.Status] {
		return fmt.Errorf("invalid status: %s", refund.Status)
	}

	return nil
}

// batchInsertRefunds inserts refunds in batches
func (s *CSVSeeder) batchInsertRefunds(refunds []database.Refund) error {
	batchSize := 100
	totalRefunds := len(refunds)

	for i := 0; i < totalRefunds; i += batchSize {
		end := i + batchSize
		if end > totalRefunds {
			end = totalRefunds
		}

		batch := refunds[i:end]

		// Insert batch
		for _, refund := range batch {
			if err := s.refundRepo.CreateRefund(&refund); err != nil {
				logrus.WithFields(logrus.Fields{
					"file_id": refund.FileID,
					"error":   err,
				}).Error("Failed to create refund")
				// Continue with other refunds even if one fails
			}
		}

		logrus.WithFields(logrus.Fields{
			"batch_start": i + 1,
			"batch_end":   end,
			"total":       totalRefunds,
		}).Debug("Inserted batch of refunds")
	}

	return nil
}

// CreateSampleCSV creates a sample CSV file with test data
func (s *CSVSeeder) CreateSampleCSV(filePath string, recordCount int) error {
	logrus.WithFields(logrus.Fields{
		"file_path":    filePath,
		"record_count": recordCount,
	}).Info("Creating sample CSV file")

	// Create directory if it doesn't exist
	dir := filepath.Dir(filePath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return fmt.Errorf("failed to create directory: %w", err)
	}

	// Create CSV file
	file, err := os.Create(filePath)
	if err != nil {
		return fmt.Errorf("failed to create CSV file: %w", err)
	}
	defer file.Close()

	// Create CSV writer
	writer := csv.NewWriter(file)
	defer writer.Flush()

	// Write header
	header := []string{"file_id", "status", "error_message"}
	if err := writer.Write(header); err != nil {
		return fmt.Errorf("failed to write CSV header: %w", err)
	}

	// Generate sample data
	for i := 1; i <= recordCount; i++ {
		fileID := fmt.Sprintf("TAX2024-%06d", i)
		status := "pending"
		errorMessage := ""

		// Add some variety to the data
		if i%10 == 0 {
			status = "error"
			errorMessage = "Sample error message"
		} else if i%5 == 0 {
			status = "processed"
		}

		record := []string{fileID, status, errorMessage}
		if err := writer.Write(record); err != nil {
			return fmt.Errorf("failed to write CSV record: %w", err)
		}
	}

	logrus.WithField("file_path", filePath).Info("Sample CSV file created successfully")
	return nil
}
