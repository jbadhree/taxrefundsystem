package services

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"math/rand"
	"time"

	"badhtaxrefundbatch/internal/database"

	"github.com/google/uuid"
)

// ErrorMessages represents the structure of the error messages JSON file
type ErrorMessages struct {
	ErrorMessages []string `json:"error_messages"`
}

// IRSService handles communication with the IRS
type IRSService struct {
	errorMessages []string
	// In a real implementation, this would contain HTTP client, API keys, etc.
}

// NewIRSService creates a new IRS service
func NewIRSService() *IRSService {
	service := &IRSService{}
	service.loadErrorMessages()
	return service
}

// loadErrorMessages loads error messages from JSON file
func (s *IRSService) loadErrorMessages() {
	// Try to find the error messages JSON file
	possiblePaths := []string{
		"./data/error_messages.json",
		"./error_messages.json",
		"data/error_messages.json",
	}

	var jsonData []byte
	var err error

	for _, path := range possiblePaths {
		if jsonData, err = ioutil.ReadFile(path); err == nil {
			break
		}
	}

	if err != nil {
		// Fallback to default error messages if file not found
		s.errorMessages = []string{
			"Invalid file ID format",
			"File not found in IRS system",
			"Processing timeout exceeded",
			"Invalid taxpayer information",
			"Duplicate submission detected",
		}
		return
	}

	var errorData ErrorMessages
	if err := json.Unmarshal(jsonData, &errorData); err != nil {
		// Fallback to default error messages if JSON parsing fails
		s.errorMessages = []string{
			"Invalid file ID format",
			"File not found in IRS system",
			"Processing timeout exceeded",
			"Invalid taxpayer information",
			"Duplicate submission detected",
		}
		return
	}

	s.errorMessages = errorData.ErrorMessages
}

// GetRefundStatus simulates a call to the IRS API to get refund status
// In a real implementation, this would make an HTTP request to the actual IRS API
func (s *IRSService) GetRefundStatus(fileID string) (*database.RefundStatus, error) {
	// Simulate API delay (100ms to 2s)
	delay := time.Duration(rand.Intn(1900)+100) * time.Millisecond
	time.Sleep(delay)

	// Simulate random status response with new probability distribution
	rand.Seed(time.Now().UnixNano())
	statusCode := rand.Intn(100)

	var status database.RefundStatus

	switch {
	case statusCode < 10: // 10% chance of processed
		status.Status = "processed"
		now := time.Now()
		status.ProcessedAt = &now
	case statusCode < 80: // 70% chance of in progress
		status.Status = "in_progress"
	default: // 20% chance of error
		status.Status = "error"
		errorMsg := s.getRandomErrorMessage()
		status.ErrorMessage = &errorMsg
	}

	return &status, nil
}

// getRandomErrorMessage returns a random error message from the loaded JSON file
func (s *IRSService) getRandomErrorMessage() string {
	if len(s.errorMessages) == 0 {
		return "Unknown error occurred"
	}

	rand.Seed(time.Now().UnixNano())
	return s.errorMessages[rand.Intn(len(s.errorMessages))]
}

// ValidateFileID performs UUID validation on file ID
func (s *IRSService) ValidateFileID(fileID string) error {
	if fileID == "" {
		return fmt.Errorf("file ID cannot be empty")
	}

	// Validate UUID format (e.g., "550e8400-e29b-41d4-a716-446655440000")
	if _, err := uuid.Parse(fileID); err != nil {
		return fmt.Errorf("invalid UUID format: %v", err)
	}

	return nil
}

// GetServiceHealth returns the health status of the IRS service
func (s *IRSService) GetServiceHealth() error {
	// In a real implementation, this would check if the IRS API is accessible
	// For now, we'll always return healthy
	return nil
}

// getRefundStatusForTest is a test-specific method without delays for faster testing
func (s *IRSService) getRefundStatusForTest(fileID string) *database.RefundStatus {
	// Simulate random status response with new probability distribution
	rand.Seed(time.Now().UnixNano())
	statusCode := rand.Intn(100)

	var status database.RefundStatus

	switch {
	case statusCode < 10: // 10% chance of processed
		status.Status = "processed"
		now := time.Now()
		status.ProcessedAt = &now
	case statusCode < 80: // 70% chance of in progress
		status.Status = "in_progress"
	default: // 20% chance of error
		status.Status = "error"
		errorMsg := s.getRandomErrorMessage()
		status.ErrorMessage = &errorMsg
	}

	return &status
}
