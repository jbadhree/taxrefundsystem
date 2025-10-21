#!/bin/bash

# Local run script for BadhTaxRefundBatch
# This script runs the batch job locally with proper environment setup

set -e

echo "Starting BadhTaxRefundBatch locally..."

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    cp env.example .env
    echo "Please edit .env file with your database settings before running again."
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Check if DATABASE_URL is set
if [ -z "$DATABASE_URL" ]; then
    echo "Error: DATABASE_URL not set in .env file"
    exit 1
fi

# Build the application
echo "Building application..."
go build -o badhtaxrefundbatch ./cmd/main.go

# Run the application
echo "Running BadhTaxRefundBatch..."
echo "Database URL: $DATABASE_URL"
echo "Max Workers: $MAX_CONCURRENT_WORKERS"
echo "Batch Size: $BATCH_SIZE"
echo "Processing Interval: $PROCESSING_INTERVAL"
echo ""

./badhtaxrefundbatch



