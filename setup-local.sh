#!/bin/bash
# setup-local.sh - Complete local development setup script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

print_step "Setting up local development environment..."

# Step 1: Build the web service image
print_step "1. Building badhtaxrefundweb Docker image..."
cd badhtaxrefundweb
if [ -f "build-and-push.sh" ]; then
    chmod +x build-and-push.sh
    # Use the enhanced build script to build locally (without pushing)
    print_status "Building Docker image locally with auto-version detection..."
    # Extract just the build part from the script (without push)
    docker build -t jbadhree/badhtaxrefundweb:$(docker images --format "table {{.Tag}}" | grep -E "^(v)?1\.[0-9]+\.[0-9]+$" | sed 's/^v//' | sort -V | tail -1 | awk -F. '{print "v"$1"."$2"."($3+1)}') .
    print_status "Web service image built successfully!"
else
    print_error "build-and-push.sh not found in badhtaxrefundweb directory"
    exit 1
fi

# Step 2: Start all services with docker-compose
print_step "2. Starting all services with docker-compose..."
cd ../local

# Stop any existing containers
print_status "Stopping existing containers..."
docker-compose down

# Start all services
print_status "Starting services..."
docker-compose up -d

# Wait for services to be ready
print_status "Waiting for services to be ready..."
sleep 10

# Check if services are running
print_step "3. Checking service status..."

# Check PostgreSQL
if docker-compose ps postgres | grep -q "Up"; then
    print_status "✓ PostgreSQL is running"
else
    print_error "✗ PostgreSQL is not running"
fi

# Check badhtaxfileserv
if docker-compose ps badhtaxfileserv | grep -q "Up"; then
    print_status "✓ badhtaxfileserv is running on http://localhost:4000"
else
    print_error "✗ badhtaxfileserv is not running"
fi

# Check badhtaxrefundweb
if docker-compose ps badhtaxrefundweb | grep -q "Up"; then
    print_status "✓ badhtaxrefundweb is running on http://localhost:3000"
else
    print_error "✗ badhtaxrefundweb is not running"
fi

# Check pgAdmin
if docker-compose ps pgadmin | grep -q "Up"; then
    print_status "✓ pgAdmin is running on http://localhost:8080"
else
    print_warning "⚠ pgAdmin is not running (optional service)"
fi

print_step "4. Running integration tests..."

# Run the integration test script
cd ../badhtaxrefundweb
if [ -f "test-integration.sh" ]; then
    chmod +x test-integration.sh
    ./test-integration.sh
else
    print_warning "test-integration.sh not found, skipping automated tests"
fi

print_step "Setup completed!"
echo ""
print_status "Services are now running:"
print_status "  • Web Application: http://localhost:3000"
print_status "  • Tax File Service: http://localhost:4000"
print_status "  • Database: localhost:5432"
print_status "  • pgAdmin: http://localhost:8080 (optional)"
echo ""
print_status "To stop all services, run: cd local && docker-compose down"
print_status "To view logs, run: cd local && docker-compose logs -f"
