#!/bin/bash
# deploy.sh - Deploy badhtaxrefundweb to Google Cloud Run

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

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# Configuration
SERVICE_NAME="badhtaxrefundweb"
REGION="us-central1"
IMAGE_NAME="jbadhree/badhtaxrefundweb"
PORT=8080
MEMORY="512Mi"
CPU="1000m"
MIN_INSTANCES=0
MAX_INSTANCES=10
TIMEOUT=300

# Function to validate version format
validate_version() {
    local version="$1"
    
    # Check if version is provided
    if [ -z "$version" ]; then
        print_error "Version argument is required"
        echo "Usage: $0 <version>"
        echo "Example: $0 v1.0.10"
        exit 1
    fi
    
    # Validate version format (supports v1.0.10 or 1.0.10)
    if [[ ! $version =~ ^v?[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_error "Invalid version format: $version"
        echo "Version must be in format: v1.0.10 or 1.0.10"
        exit 1
    fi
    
    # Ensure version has 'v' prefix
    if [[ ! $version =~ ^v ]]; then
        version="v$version"
    fi
    
    echo "$version"
}

# Function to check if gcloud is installed and authenticated
check_gcloud() {
    if ! command -v gcloud &> /dev/null; then
        print_error "gcloud CLI is not installed. Please install it first:"
        echo "  https://cloud.google.com/sdk/docs/install"
        exit 1
    fi
    
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
        print_error "No active gcloud authentication found. Please run:"
        echo "  gcloud auth login"
        exit 1
    fi
    
    print_status "gcloud CLI is installed and authenticated"
}

# Function to get current project
get_project() {
    PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
    if [ -z "$PROJECT_ID" ]; then
        print_error "No project set. Please run:"
        echo "  gcloud config set project YOUR_PROJECT_ID"
        exit 1
    fi
    print_info "Using project: $PROJECT_ID"
}

# Function to check if image exists in registry
check_image_exists() {
    local image_name="$1"
    print_status "Checking if image $image_name exists in registry..."
    
    # Try to pull the image manifest to check if it exists
    if docker manifest inspect "$image_name" &> /dev/null; then
        print_status "✓ Image $image_name found in registry"
        return 0
    else
        print_error "✗ Image $image_name not found in registry"
        print_error "Please ensure the image has been built and pushed first:"
        echo "  ./build-and-push.sh $TAG"
        exit 1
    fi
}

# Function to check if service exists
check_service_exists() {
    if gcloud run services describe $SERVICE_NAME --region=$REGION --format="value(metadata.name)" 2>/dev/null | grep -q $SERVICE_NAME; then
        return 0
    else
        return 1
    fi
}

# Function to deploy new service
deploy_new_service() {
    print_status "Deploying new Cloud Run service: $SERVICE_NAME"
    
    gcloud run deploy $SERVICE_NAME \
        --image=$FULL_IMAGE_NAME \
        --region=$REGION \
        --platform=managed \
        --allow-unauthenticated \
        --port=$PORT \
        --memory=$MEMORY \
        --cpu=$CPU \
        --min-instances=$MIN_INSTANCES \
        --max-instances=$MAX_INSTANCES \
        --timeout=$TIMEOUT \
        --set-env-vars="NODE_ENV=production" \
        --execution-environment=gen2 \
        --quiet
}

# Function to update existing service
update_service() {
    print_status "Updating existing Cloud Run service: $SERVICE_NAME"
    
    gcloud run services update $SERVICE_NAME \
        --image=$FULL_IMAGE_NAME \
        --region=$REGION \
        --quiet
}

# Function to get service URL
get_service_url() {
    SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region=$REGION --format="value(status.url)")
    echo "$SERVICE_URL"
}

# Function to show service status
show_service_status() {
    print_status "Service Status:"
    gcloud run services describe $SERVICE_NAME --region=$REGION --format="table(metadata.name,status.url,spec.template.spec.containers[0].image,status.conditions[0].status)"
}

# Function to get current deployed version
get_current_version() {
    local current_image=$(gcloud run services describe $SERVICE_NAME --region=$REGION --format="value(spec.template.spec.containers[0].image)" 2>/dev/null)
    if [ ! -z "$current_image" ]; then
        echo "$current_image" | sed 's/.*://'
    else
        echo "unknown"
    fi
}

# Main deployment logic
main() {
    # Validate version argument
    TAG=$(validate_version "$1")
    FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"
    
    print_info "Starting deployment of $SERVICE_NAME"
    print_info "Image: $FULL_IMAGE_NAME"
    print_info "Region: $REGION"
    echo
    
    # Pre-flight checks
    check_gcloud
    get_project
    check_image_exists "$FULL_IMAGE_NAME"
    
    # Show version comparison if service exists
    if check_service_exists; then
        CURRENT_VERSION=$(get_current_version)
        print_info "Current deployed version: $CURRENT_VERSION"
        print_info "Deploying version: $TAG"
        if [ "$CURRENT_VERSION" != "$TAG" ]; then
            print_info "Service $SERVICE_NAME will be updated from $CURRENT_VERSION to $TAG"
        else
            print_warning "Service $SERVICE_NAME is already running version $TAG"
        fi
        update_service
    else
        print_info "Service $SERVICE_NAME does not exist. Creating new service with version $TAG..."
        deploy_new_service
    fi
    
    # Get and display service information
    echo
    print_status "Deployment completed successfully!"
    SERVICE_URL=$(get_service_url)
    print_info "Service URL: $SERVICE_URL"
    echo
    show_service_status
    
    echo
    print_status "You can test the deployment with:"
    echo "  curl $SERVICE_URL"
    echo
    print_status "To view logs:"
    echo "  gcloud run logs read $SERVICE_NAME --region=$REGION"
}

# Help function
show_help() {
    echo "Usage: $0 <VERSION>"
    echo
    echo "Deploy badhtaxrefundweb to Google Cloud Run"
    echo
    echo "Arguments:"
    echo "  VERSION    Docker image version to deploy (required)"
    echo "             Format: v1.0.10 or 1.0.10"
    echo
    echo "Examples:"
    echo "  $0 v1.0.10           # Deploy specific version"
    echo "  $0 1.0.10            # Deploy specific version (v prefix added automatically)"
    echo "  $0 --help            # Show this help"
    echo
    echo "Prerequisites:"
    echo "  - Image must be built and pushed first: ./build-and-push.sh v1.0.10"
    echo "  - gcloud CLI must be installed and authenticated"
    echo "  - Google Cloud project must be set"
    echo
    echo "Environment Variables:"
    echo "  GOOGLE_CLOUD_PROJECT  Override the default project"
    echo
}

# Parse command line arguments
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_help
    exit 0
fi

# Run main function with arguments
main "$@"
