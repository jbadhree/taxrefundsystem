package config

import (
	"os"
	"strconv"
	"time"

	"github.com/joho/godotenv"
	"github.com/sirupsen/logrus"
)

// Config holds all configuration for the application
type Config struct {
	DatabaseURL          string
	MaxConcurrentWorkers int
	BatchSize            int
	ProcessingInterval   time.Duration
	LogLevel             string
	// Pub/Sub configuration
	GoogleCloudProject     string
	PubSubTopicName        string
	PubSubSubscriptionName string
	EnablePubSub           bool
}

// LoadConfig loads configuration from environment variables
func LoadConfig() (*Config, error) {
	// Load .env file if it exists
	if err := godotenv.Load(); err != nil {
		logrus.Warn("No .env file found, using environment variables only")
	}

	config := &Config{
		DatabaseURL:          getEnv("DATABASE_URL", "postgresql://taxrefund_user:taxrefund_password@localhost:5432/taxrefund"),
		MaxConcurrentWorkers: getEnvAsInt("MAX_CONCURRENT_WORKERS", 10),
		BatchSize:            getEnvAsInt("BATCH_SIZE", 100),
		ProcessingInterval:   time.Duration(getEnvAsInt("PROCESSING_INTERVAL", 60)) * time.Second,
		LogLevel:             getEnv("LOG_LEVEL", "info"),
		// Pub/Sub configuration
		GoogleCloudProject:     getEnv("GOOGLE_CLOUD_PROJECT", "build-and-learn"),
		PubSubTopicName:        getEnv("PUBSUB_SEND_REFUND_TOPIC", "send-refund-to-irs"),
		PubSubSubscriptionName: getEnv("PUBSUB_SUBSCRIPTION_NAME", "refund-batch-subscription"),
		EnablePubSub:           getEnvAsBool("ENABLE_PUBSUB", true),
	}

	// Set log level
	level, err := logrus.ParseLevel(config.LogLevel)
	if err != nil {
		level = logrus.InfoLevel
	}
	logrus.SetLevel(level)

	return config, nil
}

// getEnv gets an environment variable with a default value
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

// getEnvAsInt gets an environment variable as integer with a default value
func getEnvAsInt(key string, defaultValue int) int {
	if value := os.Getenv(key); value != "" {
		if intValue, err := strconv.Atoi(value); err == nil {
			return intValue
		}
	}
	return defaultValue
}

// getEnvAsBool gets an environment variable as boolean with a default value
func getEnvAsBool(key string, defaultValue bool) bool {
	if value := os.Getenv(key); value != "" {
		if boolValue, err := strconv.ParseBool(value); err == nil {
			return boolValue
		}
	}
	return defaultValue
}
