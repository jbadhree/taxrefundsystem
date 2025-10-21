#!/bin/bash
# build-and-push.sh

set -e

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

# Build the Docker image with buildx for linux/amd64 platform
print_status "Building Docker image for linux/amd64 platform..."
docker buildx build --platform linux/amd64 -t jbadhree/badhtaxfileserv:latest --push .

# Tag with version (if provided) and push
if [ ! -z "$1" ]; then
    print_status "Building and pushing version jbadhree/badhtaxfileserv:$1"
    docker buildx build --platform linux/amd64 -t jbadhree/badhtaxfileserv:$1 --push .
    print_status "Tagged and pushed as jbadhree/badhtaxfileserv:$1"
fi

# Check if user is logged in to Docker Hub
if ! docker info | grep -q "Username:"; then
    print_warning "Not logged in to Docker Hub. Attempting to login..."
    docker login
fi

print_status "Build and push completed successfully!"
print_status "Images pushed to Docker Hub:"
print_status "- jbadhree/badhtaxfileserv:latest"
if [ ! -z "$1" ]; then
    print_status "- jbadhree/badhtaxfileserv:$1"
fi

