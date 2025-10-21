package services

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestIRSService_GetRefundStatus(t *testing.T) {
	service := NewIRSService()

	// Test with valid UUID file ID
	status, err := service.GetRefundStatus("550e8400-e29b-41d4-a716-446655440000")
	assert.NoError(t, err)
	assert.NotNil(t, status)
	assert.Contains(t, []string{"processed", "in_progress", "error"}, status.Status)

	// Test with empty file ID (should still work as it's just a mock)
	status, err = service.GetRefundStatus("")
	assert.NoError(t, err)
	assert.NotNil(t, status)
}

func TestIRSService_ValidateFileID(t *testing.T) {
	service := NewIRSService()

	// Test valid UUID file ID
	err := service.ValidateFileID("550e8400-e29b-41d4-a716-446655440000")
	assert.NoError(t, err)

	// Test empty file ID
	err = service.ValidateFileID("")
	assert.Error(t, err)

	// Test invalid UUID format
	err = service.ValidateFileID("invalid-uuid")
	assert.Error(t, err)

	// Test another valid UUID
	err = service.ValidateFileID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
	assert.NoError(t, err)
}

func TestIRSService_GetServiceHealth(t *testing.T) {
	service := NewIRSService()

	// Test health check
	err := service.GetServiceHealth()
	assert.NoError(t, err)
}

func TestIRSService_GetRandomErrorMessage(t *testing.T) {
	service := NewIRSService()

	// Test that error messages are not empty
	errorMsg := service.getRandomErrorMessage()
	assert.NotEmpty(t, errorMsg)
	assert.Greater(t, len(errorMsg), 0)
}

func TestIRSService_ConcurrentCalls(t *testing.T) {
	service := NewIRSService()

	// Test concurrent calls
	results := make(chan error, 10)

	for i := 0; i < 10; i++ {
		go func() {
			status, err := service.GetRefundStatus("550e8400-e29b-41d4-a716-446655440000")
			if err != nil {
				results <- err
				return
			}
			if status.Status == "" {
				results <- assert.AnError
				return
			}
			results <- nil
		}()
	}

	// Wait for all goroutines to complete
	for i := 0; i < 10; i++ {
		select {
		case err := <-results:
			assert.NoError(t, err)
		case <-time.After(5 * time.Second):
			t.Fatal("Timeout waiting for concurrent calls")
		}
	}
}

func TestIRSService_ErrorMessagesLoading(t *testing.T) {
	service := NewIRSService()

	// Test that error messages are loaded
	assert.NotEmpty(t, service.errorMessages)
	assert.Greater(t, len(service.errorMessages), 0)

	// Test that getRandomErrorMessage returns a valid message
	errorMsg := service.getRandomErrorMessage()
	assert.NotEmpty(t, errorMsg)
	assert.Contains(t, service.errorMessages, errorMsg)
}

func TestIRSService_ProbabilityDistribution(t *testing.T) {
	service := NewIRSService()

	// Test probability distribution over fewer calls for faster testing
	statusCounts := make(map[string]int)
	totalCalls := 50 // Further reduced for faster testing

	for i := 0; i < totalCalls; i++ {
		// Use a test-specific method that doesn't have delays
		status := service.getRefundStatusForTest("550e8400-e29b-41d4-a716-446655440000")
		statusCounts[status.Status]++
	}

	// Check that we have all three statuses
	assert.Contains(t, statusCounts, "processed")
	assert.Contains(t, statusCounts, "in_progress")
	assert.Contains(t, statusCounts, "error")

	// Check approximate distribution (allowing for more variance with fewer samples)
	processedRatio := float64(statusCounts["processed"]) / float64(totalCalls)
	inProgressRatio := float64(statusCounts["in_progress"]) / float64(totalCalls)
	errorRatio := float64(statusCounts["error"]) / float64(totalCalls)

	// Allow 15% variance from expected distribution (more lenient for smaller sample)
	assert.InDelta(t, 0.10, processedRatio, 0.15, "Processed ratio should be around 10%")
	assert.InDelta(t, 0.70, inProgressRatio, 0.15, "In progress ratio should be around 70%")
	assert.InDelta(t, 0.20, errorRatio, 0.15, "Error ratio should be around 20%")
}
