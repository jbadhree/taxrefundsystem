# BadhTaxRefundBatch

A Go-based batch job service that processes pending tax refunds by checking their status with the IRS and updating the database accordingly. The service uses concurrent processing to handle multiple refunds simultaneously.

## Features

- **Concurrent Processing**: Uses goroutines with configurable worker limits
- **Database Integration**: PostgreSQL with GORM ORM
- **CSV Seeding**: Seed database with test data from CSV files
- **Environment Configuration**: Fully configurable via environment variables
- **JSON Error Messages**: Configurable error messages via JSON file
- **Probability Distribution**: Configurable status probability (70% in progress, 20% error, 10% processed)
- **Containerized**: Ready for Cloud Run deployment
- **Health Checks**: Built-in health monitoring
- **Graceful Shutdown**: Handles SIGTERM/SIGINT signals

## Quick Start

### Prerequisites

- Go 1.21+
- PostgreSQL database
- Docker (optional)

### Local Development

1. **Clone and setup**:
   ```bash
   cd badhtaxrefundbatch
   go mod download
   ```

2. **Configure environment**:
   ```bash
   cp env.example .env
   # Edit .env with your database settings
   ```

3. **Run the application**:
   ```bash
   # Process once and exit
   go run cmd/main.go

   # Or with environment variables
   DATABASE_URL="your_db_url" go run cmd/main.go
   ```

### Docker

1. **Build the image**:
   ```bash
   docker build -t badhtaxrefundbatch .
   ```

2. **Run the container**:
   ```bash
   docker run -e DATABASE_URL="your_db_url" badhtaxrefundbatch
   ```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `postgresql://taxrefund_user:taxrefund_password@localhost:5432/taxrefund?schema=taxrefundbatchdb` | PostgreSQL connection string |
| `MAX_CONCURRENT_WORKERS` | `10` | Maximum concurrent goroutines |
| `BATCH_SIZE` | `100` | Number of refunds to process per batch |
| `PROCESSING_INTERVAL` | `60` | Seconds between batch runs (0 = run once) |
| `LOG_LEVEL` | `info` | Logging level (debug, info, warn, error) |
| `SEED_DATA` | `false` | Whether to seed data on startup |
| `CSV_FILE_PATH` | `./data/refunds_seed.csv` | Path to CSV seed file |

### Database Schema

The service creates a `refunds` table with the following structure:

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
```

## Usage

### Processing Modes

1. **One-time Processing**: Set `PROCESSING_INTERVAL=0` to process all pending refunds once and exit
2. **Continuous Processing**: Set `PROCESSING_INTERVAL>0` to process refunds at regular intervals

### CSV Seeding

The service can seed the database with test data from a CSV file:

```bash
# Enable seeding
SEED_DATA=true go run cmd/main.go

# Or specify a custom CSV file
CSV_FILE_PATH="./my_data.csv" SEED_DATA=true go run cmd/main.go
```

CSV format:
```csv
file_id,status,error_message
TAX2024-000001,pending,
TAX2024-000002,error,Invalid file format
```

### JSON Error Messages Configuration

The service uses a JSON file to configure error messages that are returned when the IRS service simulates an error response. The file is located at `data/error_messages.json`.

Example configuration:
```json
{
  "error_messages": [
    "Invalid file ID format",
    "File not found in IRS system",
    "Processing timeout exceeded",
    "Invalid taxpayer information",
    "Duplicate submission detected",
    "IRS system temporarily unavailable",
    "Invalid refund amount",
    "Missing required documentation",
    "Tax year not eligible for processing",
    "Account verification failed"
  ]
}
```

The service will automatically load error messages from this file on startup. If the file is not found or invalid, it will fall back to default error messages.

### Status Probability Distribution

The IRS service mock uses the following probability distribution:
- **In Progress**: 70% chance
- **Error**: 20% chance  
- **Processed**: 10% chance

This can be easily modified in the `GetRefundStatus` method in `internal/services/irs_service.go`.

## Deployment

### Google Cloud Run

1. **Build and push**:
   ```bash
   ./build-and-push.sh v1.0.1
   ```

2. **Deploy**:
   ```bash
   ./deploy.sh v1.0.1
   ```

3. **Execute job**:
   ```bash
   ./deploy.sh v1.0.1 --execute
   ```

### Docker Compose

Add to your `docker-compose.yml`:

```yaml
services:
  badhtaxrefundbatch:
    build: ./badhtaxrefundbatch
    environment:
      - DATABASE_URL=postgresql://taxrefund_user:taxrefund_password@postgres:5432/taxrefund?schema=taxrefundbatchdb
      - MAX_CONCURRENT_WORKERS=10
      - BATCH_SIZE=100
      - PROCESSING_INTERVAL=60
    depends_on:
      - postgres
```

## Development

### Project Structure

```
badhtaxrefundbatch/
├── cmd/
│   └── main.go                 # Application entry point
├── internal/
│   ├── config/
│   │   └── config.go          # Configuration management
│   ├── database/
│   │   ├── models.go          # Database models
│   │   └── connection.go      # Database connection
│   ├── services/
│   │   ├── refund_processor.go # Core processing logic
│   │   └── irs_service.go     # IRS service mock
│   └── seeder/
│       └── csv_seeder.go      # CSV seeding functionality
├── data/
│   └── refunds_seed.csv       # Sample data
├── Dockerfile
├── build-and-push.sh
├── deploy.sh
└── README.md
```

### Adding New Features

1. **Database Models**: Add new models in `internal/database/models.go`
2. **Services**: Add business logic in `internal/services/`
3. **Configuration**: Add new config options in `internal/config/config.go`

### Testing

```bash
# Run tests
go test ./...

# Run with coverage
go test -cover ./...

# Run specific test
go test ./internal/services
```

## Monitoring

### Health Check

The service includes a health check endpoint that verifies:
- Database connectivity
- IRS service availability
- Processing statistics

### Logging

Structured JSON logging with the following levels:
- `debug`: Detailed processing information
- `info`: General application flow
- `warn`: Non-critical issues
- `error`: Critical errors

### Metrics

The service logs processing statistics including:
- Total refunds processed
- Pending refunds count
- Error rates
- Processing times

## Troubleshooting

### Common Issues

1. **Database Connection Failed**:
   - Check `DATABASE_URL` environment variable
   - Ensure PostgreSQL is running and accessible
   - Verify database schema exists

2. **No Refunds Processed**:
   - Check if there are pending refunds in the database
   - Verify `BATCH_SIZE` configuration
   - Check logs for processing errors

3. **High Memory Usage**:
   - Reduce `MAX_CONCURRENT_WORKERS`
   - Decrease `BATCH_SIZE`
   - Check for memory leaks in processing logic

### Debug Mode

Enable debug logging for detailed information:

```bash
LOG_LEVEL=debug go run cmd/main.go
```

## License

This project is part of the BadhTaxRefundSystem.
