#!/bin/bash
# build-and-push.sh
#
# Enhanced build and push script with automatic version detection
# 
# Usage:
#   ./build-and-push.sh                    # Auto-increment patch version
#   ./build-and-push.sh v1.2.3            # Use specific version
#   ./build-and-push.sh v1.2.3 latest     # Use specific version + tag as latest
#
# Features:
#   - Automatically detects current highest version and increments patch
#   - Supports both v1.0.10 and 1.0.10 format (adds 'v' prefix if missing)
#   - Validates version format
#   - Builds and pushes to Docker Hub

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

# Function to get the next patch version
get_next_patch_version() {
    local image_name="jbadhree/badhtaxrefundweb"
    local latest_version="v0.0.0"
    
    # Get all local images with version tags (including both v1.0.10 and 1.0.10 formats)
    # Filter for versions that start with v1. or 1. to match our project pattern
    local versions=$(docker images --format "table {{.Tag}}" | grep -E "^(v)?1\.[0-9]+\.[0-9]+$" | sed 's/^v//' | sort -V | sed 's/^/v/')
    
    if [ ! -z "$versions" ]; then
        latest_version=$(echo "$versions" | tail -1)
    fi
    
    # Extract major, minor, patch from latest version
    local version_without_v=$(echo "$latest_version" | sed 's/^v//')
    local major=$(echo "$version_without_v" | cut -d. -f1)
    local minor=$(echo "$version_without_v" | cut -d. -f2)
    local patch=$(echo "$version_without_v" | cut -d. -f3)
    
    # Increment patch version
    local next_patch=$((patch + 1))
    local next_version="v${major}.${minor}.${next_patch}"
    
    echo "$next_version"
}

# Check if a specific version was provided
if [ ! -z "$1" ]; then
    # Validate version format
    if [[ $1 =~ ^v?[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        NEXT_VERSION="$1"
        # Ensure it has 'v' prefix
        if [[ ! $NEXT_VERSION =~ ^v ]]; then
            NEXT_VERSION="v$NEXT_VERSION"
        fi
        print_status "Using specified version: $NEXT_VERSION"
    else
        print_error "Invalid version format. Please use format like 'v1.0.10' or '1.0.10'"
        exit 1
    fi
else
    # Auto-detect next version
    NEXT_VERSION=$(get_next_patch_version)
    print_status "Current latest version detected, next version will be: $NEXT_VERSION"
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
docker buildx build --platform linux/amd64 -t jbadhree/badhtaxrefundweb:$NEXT_VERSION --load .

# Tag with additional version (if provided as second argument)
if [ ! -z "$2" ]; then
    docker tag jbadhree/badhtaxrefundweb:$NEXT_VERSION jbadhree/badhtaxrefundweb:$2
    print_status "Tagged as jbadhree/badhtaxrefundweb:$2"
fi

# Check if user is logged in to Docker Hub
if ! docker info | grep -q "Username:"; then
    print_warning "Not logged in to Docker Hub. Attempting to login..."
    docker login
fi

# Push to Docker Hub using buildx
print_status "Pushing to Docker Hub using buildx..."
docker buildx build --platform linux/amd64 -t jbadhree/badhtaxrefundweb:$NEXT_VERSION --push .

if [ ! -z "$2" ]; then
    docker buildx build --platform linux/amd64 -t jbadhree/badhtaxrefundweb:$2 --push .
    print_status "Pushed jbadhree/badhtaxrefundweb:$2"
fi

print_status "Build and push completed successfully!"
print_status "New version created: jbadhree/badhtaxrefundweb:$NEXT_VERSION"
print_status "Images available:"
docker images | grep jbadhree/badhtaxrefundweb
