package database

import (
	"fmt"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// ConnectDB establishes a connection to the PostgreSQL database
func ConnectDB(databaseURL string) (*gorm.DB, error) {
	// Configure GORM
	config := &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
		NowFunc: func() time.Time {
			return time.Now().UTC()
		},
	}

	// Connect to database
	db, err := gorm.Open(postgres.Open(databaseURL), config)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to database: %w", err)
	}

	// Set the search path to use the taxrefundbatchdb schema
	if err := db.Exec("SET search_path TO taxrefundbatchdb, public").Error; err != nil {
		return nil, fmt.Errorf("failed to set search path: %w", err)
	}

	// Get underlying sql.DB for connection pool configuration
	sqlDB, err := db.DB()
	if err != nil {
		return nil, fmt.Errorf("failed to get underlying sql.DB: %w", err)
	}

	// Configure connection pool
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetMaxOpenConns(100)
	sqlDB.SetConnMaxLifetime(time.Hour)

	// Test the connection
	if err := sqlDB.Ping(); err != nil {
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	return db, nil
}

// MigrateDB runs database migrations
func MigrateDB(db *gorm.DB) error {
	// Create the refunds table in the taxrefundbatchdb schema using raw SQL
	createTableSQL := `
	CREATE TABLE IF NOT EXISTS taxrefundbatchdb.refunds (
		id SERIAL PRIMARY KEY,
		file_id VARCHAR(255) NOT NULL UNIQUE,
		status VARCHAR(50) NOT NULL DEFAULT 'pending',
		error_message TEXT,
		user_id VARCHAR(255),
		year INTEGER,
		refund_amount VARCHAR(50),
		eta VARCHAR(100),
		created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
		updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
		processed_at TIMESTAMP WITH TIME ZONE
	)`

	if err := db.Exec(createTableSQL).Error; err != nil {
		return fmt.Errorf("failed to create refunds table: %w", err)
	}

	// Create indexes if they don't exist
	if err := createIndexes(db); err != nil {
		return fmt.Errorf("failed to create indexes: %w", err)
	}

	return nil
}

// createIndexes creates necessary database indexes
func createIndexes(db *gorm.DB) error {
	// Create index on status column
	if err := db.Exec("CREATE INDEX IF NOT EXISTS idx_refunds_status ON taxrefundbatchdb.refunds(status)").Error; err != nil {
		return err
	}

	// Create index on file_id column
	if err := db.Exec("CREATE INDEX IF NOT EXISTS idx_refunds_file_id ON taxrefundbatchdb.refunds(file_id)").Error; err != nil {
		return err
	}

	// Create index on created_at column for time-based queries
	if err := db.Exec("CREATE INDEX IF NOT EXISTS idx_refunds_created_at ON taxrefundbatchdb.refunds(created_at)").Error; err != nil {
		return err
	}

	return nil
}

// CloseDB closes the database connection
func CloseDB(db *gorm.DB) error {
	sqlDB, err := db.DB()
	if err != nil {
		return err
	}
	return sqlDB.Close()
}
