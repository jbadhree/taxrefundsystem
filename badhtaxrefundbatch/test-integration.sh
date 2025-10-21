#!/bin/bash

# Integration test script for BadhTaxRefundBatch
# This script tests the complete flow from database seeding to refund processing

set -e

echo "Starting integration test for BadhTaxRefundBatch..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install it and try again."
    exit 1
fi

print_status "Starting PostgreSQL database..."
docker-compose up -d postgres

# Wait for database to be ready
print_status "Waiting for database to be ready..."
sleep 10

# Check if database is accessible
print_status "Testing database connection..."
docker-compose exec postgres psql -U taxrefund_user -d taxrefund -c "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_status "Database connection successful"
else
    print_error "Database connection failed"
    exit 1
fi

print_status "Building BadhTaxRefundBatch..."
docker-compose build badhtaxrefundbatch

print_status "Running BadhTaxRefundBatch with seeding..."
# Run the batch job with seeding enabled
docker-compose run --rm badhtaxrefundbatch

print_status "Checking results in database..."
# Query the database to see the results
docker-compose exec postgres psql -U taxrefund_user -d taxrefund -c "
SELECT 
    status,
    COUNT(*) as count
FROM taxrefundbatchdb.refunds 
GROUP BY status 
ORDER BY status;
"

print_status "Integration test completed successfully!"

# Cleanup
print_status "Cleaning up..."
docker-compose down

print_status "Integration test finished!"



