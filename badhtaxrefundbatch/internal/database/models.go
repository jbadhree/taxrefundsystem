package database

import (
	"time"

	"gorm.io/gorm"
)

// Refund represents a refund record in the database
type Refund struct {
	ID           uint       `gorm:"primaryKey" json:"id"`
	FileID       string     `gorm:"uniqueIndex;not null" json:"file_id"`
	Status       string     `gorm:"not null;default:'pending'" json:"status"`
	ErrorMessage *string    `gorm:"type:text" json:"error_message,omitempty"`
	CreatedAt    time.Time  `json:"created_at"`
	UpdatedAt    time.Time  `json:"updated_at"`
	ProcessedAt  *time.Time `json:"processed_at,omitempty"`
}

// TableName returns the table name for the Refund model
func (Refund) TableName() string {
	return "refunds"
}

// RefundStatus represents the status of a refund check
type RefundStatus struct {
	Status       string     `json:"status"` // "processed", "in_progress", "error"
	ErrorMessage *string    `json:"error_message,omitempty"`
	ProcessedAt  *time.Time `json:"processed_at,omitempty"`
}

// RefundRepository handles database operations for refunds
type RefundRepository struct {
	db *gorm.DB
}

// NewRefundRepository creates a new refund repository
func NewRefundRepository(db *gorm.DB) *RefundRepository {
	return &RefundRepository{db: db}
}

// GetPendingRefunds retrieves all refunds with pending status
func (r *RefundRepository) GetPendingRefunds(limit int) ([]Refund, error) {
	var refunds []Refund
	err := r.db.Where("status = ?", "pending").Limit(limit).Find(&refunds).Error
	return refunds, err
}

// UpdateRefundStatus updates the status and related fields of a refund
func (r *RefundRepository) UpdateRefundStatus(refund *Refund) error {
	now := time.Now()
	refund.UpdatedAt = now

	if refund.Status == "processed" {
		refund.ProcessedAt = &now
	}

	return r.db.Save(refund).Error
}

// CreateRefund creates a new refund record
func (r *RefundRepository) CreateRefund(refund *Refund) error {
	return r.db.Create(refund).Error
}

// GetRefundByFileID retrieves a refund by file ID
func (r *RefundRepository) GetRefundByFileID(fileID string) (*Refund, error) {
	var refund Refund
	err := r.db.Where("file_id = ?", fileID).First(&refund).Error
	if err != nil {
		return nil, err
	}
	return &refund, nil
}

// GetRefundStats returns statistics about refunds
func (r *RefundRepository) GetRefundStats() (map[string]int64, error) {
	stats := make(map[string]int64)

	var count int64
	err := r.db.Model(&Refund{}).Count(&count).Error
	if err != nil {
		return nil, err
	}
	stats["total"] = count

	err = r.db.Model(&Refund{}).Where("status = ?", "pending").Count(&count).Error
	if err != nil {
		return nil, err
	}
	stats["pending"] = count

	err = r.db.Model(&Refund{}).Where("status = ?", "processed").Count(&count).Error
	if err != nil {
		return nil, err
	}
	stats["processed"] = count

	err = r.db.Model(&Refund{}).Where("status = ?", "error").Count(&count).Error
	if err != nil {
		return nil, err
	}
	stats["error"] = count

	return stats, nil
}



