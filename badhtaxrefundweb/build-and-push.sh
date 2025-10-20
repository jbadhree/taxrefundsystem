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

# Check if buildx is available
if ! docker buildx version > /dev/null 2>&1; then
    print_error "Docker buildx is not available. Please install or enable buildx."
    exit 1
fi

# Create and use buildx builder if it doesn't exist
BUILDER_NAME="badhtaxrefundweb-builder"
if ! docker buildx inspect $BUILDER_NAME > /dev/null 2>&1; then
    print_status "Creating buildx builder: $BUILDER_NAME"
    docker buildx create --name $BUILDER_NAME --use
else
    print_status "Using existing buildx builder: $BUILDER_NAME"
    docker buildx use $BUILDER_NAME
fi

# Build the Docker image with buildx for linux/amd64 platform
print_status "Building Docker image with buildx for linux/amd64 platform..."
docker buildx build --platform linux/amd64 -t jbadhree/badhtaxrefundweb:latest --load .

# Tag with version (if provided)
if [ ! -z "$1" ]; then
    docker tag jbadhree/badhtaxrefundweb:latest jbadhree/badhtaxrefundweb:$1
    print_status "Tagged as jbadhree/badhtaxrefundweb:$1"
fi

# Check if user is logged in to Docker Hub
if ! docker info | grep -q "Username:"; then
    print_warning "Not logged in to Docker Hub. Attempting to login..."
    docker login
fi

# Push to Docker Hub using buildx
print_status "Pushing to Docker Hub using buildx..."
docker buildx build --platform linux/amd64 -t jbadhree/badhtaxrefundweb:latest --push .

if [ ! -z "$1" ]; then
    docker buildx build --platform linux/amd64 -t jbadhree/badhtaxrefundweb:$1 --push .
    print_status "Pushed jbadhree/badhtaxrefundweb:$1"
fi

print_status "Build and push completed successfully!"
print_status "Images available:"
docker images | grep jbadhree/badhtaxrefundweb
