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
TAG="${1:-latest}"
FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"
PORT=8080
MEMORY="512Mi"
CPU="1000m"
MIN_INSTANCES=0
MAX_INSTANCES=10
TIMEOUT=300

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

# Main deployment logic
main() {
    print_info "Starting deployment of $SERVICE_NAME"
    print_info "Image: $FULL_IMAGE_NAME"
    print_info "Region: $REGION"
    echo
    
    # Pre-flight checks
    check_gcloud
    get_project
    
    # Check if image exists locally (optional check)
    if ! docker image inspect $FULL_IMAGE_NAME &> /dev/null; then
        print_warning "Image $FULL_IMAGE_NAME not found locally. Make sure it's available in the registry."
    fi
    
    # Deploy or update service
    if check_service_exists; then
        print_info "Service $SERVICE_NAME already exists. Updating..."
        update_service
    else
        print_info "Service $SERVICE_NAME does not exist. Creating new service..."
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
    echo "Usage: $0 [TAG]"
    echo
    echo "Deploy badhtaxrefundweb to Google Cloud Run"
    echo
    echo "Arguments:"
    echo "  TAG        Docker image tag to deploy (default: latest)"
    echo
    echo "Examples:"
    echo "  $0                    # Deploy latest tag"
    echo "  $0 v1.0.9            # Deploy specific version"
    echo "  $0 v1.0.9 --help     # Show this help"
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

# Run main function
main
