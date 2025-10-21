# BadhTaxRefundBatch Implementation Plan

## Overview
A Go-based batch job service that processes pending refunds by checking their status with the IRS and updating the database accordingly. The service uses concurrent processing to handle multiple refunds simultaneously.

## Architecture

### Core Components
1. **Main Application** - Entry point and orchestration
2. **Database Layer** - GORM-based models and migrations
3. **Refund Processor** - Core business logic for processing refunds
4. **IRS Service** - Mock service for IRS refund status checking
5. **Configuration** - Environment-based configuration management
6. **CSV Seeder** - Database seeding functionality

### Technology Stack
- **Language**: Go 1.21+
- **ORM**: GORM v2
- **Database**: PostgreSQL
- **Concurrency**: Goroutines with worker pool pattern
- **Configuration**: Environment variables
- **Containerization**: Docker
- **Deployment**: Google Cloud Run (Batch)

## Database Schema

### Refund Table
```sql
CREATE TABLE refunds (
    id SERIAL PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_file_id ON refunds(file_id);
```

## Project Structure
```
badhtaxrefundbatch/
├── cmd/
│   └── main.go
├── internal/
│   ├── config/
│   │   └── config.go
│   ├── database/
│   │   ├── models.go
│   │   ├── migrations.go
│   │   └── connection.go
│   ├── services/
│   │   ├── refund_processor.go
│   │   └── irs_service.go
│   └── seeder/
│       └── csv_seeder.go
├── migrations/
│   └── 001_create_refunds_table.sql
├── data/
│   └── refunds_seed.csv
├── go.mod
├── go.sum
├── Dockerfile
├── build-and-push.sh
├── deploy.sh
└── README.md
```

## Implementation Details

### 1. Configuration Management
- Environment variables for database connection
- Configurable concurrency limits
- Batch processing intervals
- Logging configuration

### 2. Database Operations
- GORM models for type safety
- Automatic migrations on startup
- Connection pooling
- Transaction management

### 3. Concurrent Processing
- Worker pool pattern
- Configurable concurrency limit
- Graceful shutdown handling
- Error handling and retry logic

### 4. IRS Service Mock
- Random status generation
- Simulated API delays
- Error simulation
- Configurable response patterns

### 5. CSV Seeding
- CSV parsing and validation
- Batch insert operations
- Duplicate handling
- Error reporting

### 6. Containerization
- Multi-stage Docker build
- Optimized for Cloud Run
- Health checks
- Graceful shutdown

## Environment Variables

### Required
- `DATABASE_URL` - PostgreSQL connection string
- `MAX_CONCURRENT_WORKERS` - Maximum concurrent goroutines (default: 10)
- `BATCH_SIZE` - Number of refunds to process per batch (default: 100)
- `PROCESSING_INTERVAL` - Seconds between batch runs (default: 60)

### Optional
- `LOG_LEVEL` - Logging level (default: info)
- `SEED_DATA` - Whether to seed data on startup (default: false)
- `CSV_FILE_PATH` - Path to CSV seed file (default: ./data/refunds_seed.csv)

## API Design

### IRS Service Interface
```go
type IRSService interface {
    GetRefundStatus(fileID string) (*RefundStatus, error)
}

type RefundStatus struct {
    Status      string `json:"status"`      // "processed", "in_progress", "error"
    ErrorMessage string `json:"error_message,omitempty"`
    ProcessedAt *time.Time `json:"processed_at,omitempty"`
}
```

### Refund Processor Interface
```go
type RefundProcessor interface {
    ProcessPendingRefunds() error
    ProcessRefund(refund *Refund) error
}
```

## Error Handling Strategy

1. **Database Errors**: Retry with exponential backoff
2. **IRS Service Errors**: Log and mark as error in database
3. **Concurrency Errors**: Use mutex for shared resources
4. **Validation Errors**: Skip invalid records and log

## Monitoring and Logging

- Structured logging with JSON format
- Metrics for processing rates and errors
- Health check endpoint
- Graceful shutdown on SIGTERM

## Testing Strategy

1. **Unit Tests**: Individual component testing
2. **Integration Tests**: Database and service integration
3. **Concurrency Tests**: Worker pool behavior
4. **End-to-End Tests**: Full batch processing flow

## Deployment

### Local Development
```bash
# Run with Docker Compose
docker-compose up badhtaxrefundbatch

# Run locally
go run cmd/main.go
```

### Cloud Run Deployment
- Containerized application
- Environment variable configuration
- Cloud SQL connection
- Cloud Logging integration

## Security Considerations

- Database connection encryption
- Environment variable validation
- Input sanitization
- Rate limiting for external calls

## Performance Considerations

- Connection pooling
- Batch processing
- Memory-efficient CSV parsing
- Optimized database queries

## Future Enhancements

1. **Real IRS Integration**: Replace mock service with actual IRS API
2. **Metrics and Monitoring**: Prometheus metrics, Grafana dashboards
3. **Dead Letter Queue**: Handle failed refunds
4. **Scheduling**: Cron-based scheduling
5. **Notifications**: Email/SMS notifications for status changes
6. **Audit Trail**: Complete processing history
7. **Rate Limiting**: Respect IRS API rate limits
8. **Circuit Breaker**: Handle IRS service failures gracefully

## Dependencies

```go
require (
    github.com/gin-gonic/gin v1.9.1
    gorm.io/gorm v1.25.5
    gorm.io/driver/postgres v1.5.4
    github.com/joho/godotenv v1.4.0
    github.com/sirupsen/logrus v1.9.3
    github.com/stretchr/testify v1.8.4
)
```

## Timeline

- **Phase 1**: Core structure and database setup (Day 1)
- **Phase 2**: Refund processing logic (Day 2)
- **Phase 3**: Concurrency and error handling (Day 3)
- **Phase 4**: Containerization and deployment (Day 4)
- **Phase 5**: Testing and documentation (Day 5)

## Success Criteria

1. ✅ Processes pending refunds from database
2. ✅ Concurrent processing with configurable limits
3. ✅ Updates refund status based on IRS response
4. ✅ Handles errors gracefully
5. ✅ Seeds database from CSV
6. ✅ Runs in Cloud Run environment
7. ✅ Configurable via environment variables
8. ✅ Proper logging and monitoring



