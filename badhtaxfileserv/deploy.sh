#!/bin/bash
# deploy.sh - Deploy badhtaxfileserv to Google Cloud Run

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
SERVICE_NAME="badhtaxfileserv"
REGION="us-central1"
IMAGE_NAME="jbadhree/badhtaxfileserv"
TAG="${1:-latest}"
FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"
PORT=4000
MEMORY="1Gi"
CPU="1000m"
MIN_INSTANCES=0
MAX_INSTANCES=10
TIMEOUT=600

# Database configuration (these should be set as environment variables or provided as arguments)
DB_INSTANCE_NAME="${DB_INSTANCE_NAME:-taxrefund-db}"
DB_NAME="${DB_NAME:-taxrefund}"
DB_USER="${DB_USER:-taxrefund_user}"
DB_PASSWORD="${DB_PASSWORD:-}"

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

# Function to get database connection details from Pulumi
get_database_info() {
    print_info "Getting database information from Pulumi infrastructure..."
    
    # Check if Pulumi is available and we're in the infra directory
    if ! command -v pulumi &> /dev/null; then
        print_error "Pulumi CLI not found. Please install Pulumi or run from infra directory:"
        echo "  cd infra && pulumi stack select prod"
        exit 1
    fi
    
    # Try to get database info from Pulumi stack
    if [ -d "../infra" ]; then
        cd ../infra
        if pulumi stack ls --json | grep -q "prod"; then
            print_info "Using Pulumi stack: prod"
            
            # Get database instance name
            DB_INSTANCE_NAME=$(pulumi config get badhtaxrefundsystem:dbInstanceName 2>/dev/null || echo "taxrefund-db")
            DB_NAME=$(pulumi config get badhtaxrefundsystem:dbName 2>/dev/null || echo "taxrefund")
            DB_USER=$(pulumi config get badhtaxrefundsystem:dbUser 2>/dev/null || echo "taxrefund_user")
            
            print_info "Database instance: $DB_INSTANCE_NAME"
            print_info "Database name: $DB_NAME"
            print_info "Database user: $DB_USER"
            
            # Get database instance public IP
            DB_IP=$(gcloud sql instances describe $DB_INSTANCE_NAME --format="value(ipAddresses[0].ipAddress)" 2>/dev/null || echo "")
            if [ -z "$DB_IP" ]; then
                print_error "Could not find database instance '$DB_INSTANCE_NAME'. Please check:"
                echo "  1. Database instance exists"
                echo "  2. You have permissions to access it"
                echo "  3. Instance name is correct: $DB_INSTANCE_NAME"
                exit 1
            fi
            
            DB_URL="jdbc:postgresql://${DB_IP}:5432/${DB_NAME}?sslmode=require"
            print_info "Database URL: jdbc:postgresql://${DB_IP}:5432/${DB_NAME}?sslmode=require"
            
            # Note: We still need the password, but we can get it from Pulumi secrets
            print_warning "Note: Database password is stored in Pulumi secrets."
            print_warning "You may need to set DB_PASSWORD if the service can't access Pulumi secrets."
            print_info "To get the password: pulumi config get --secret badhtaxrefundsystem:dbPassword"
            
            cd - > /dev/null
        else
            print_error "Pulumi stack 'prod' not found. Please run:"
            echo "  cd infra && pulumi stack select prod"
            exit 1
        fi
    else
        print_error "Infra directory not found. Please run from project root or provide DB_PASSWORD manually."
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
        --set-env-vars="SPRING_PROFILES_ACTIVE=production" \
        --set-env-vars="SPRING_DATASOURCE_URL=$DB_URL" \
        --set-env-vars="SPRING_DATASOURCE_USERNAME=$DB_USER" \
        --set-env-vars="SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD" \
        --set-env-vars="SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect" \
        --set-env-vars="SPRING_JPA_HIBERNATE_DDL_AUTO=update" \
        --set-env-vars="SPRING_JPA_SHOW_SQL=false" \
        --set-env-vars="SERVER_PORT=$PORT" \
        --set-env-vars="SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000" \
        --set-env-vars="SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5" \
        --set-env-vars="GOOGLE_CLOUD_PROJECT=$PROJECT_ID" \
        --set-env-vars="PUBSUB_REFUND_UPDATE_TOPIC=refund-update-from-irs" \
        --set-env-vars="PUBSUB_SEND_REFUND_TOPIC=send-refund-to-irs" \
        --set-env-vars="PUBSUB_ENABLED=true" \
        --execution-environment=gen2 \
        --cpu-boost \
        --quiet
}

# Function to update existing service
update_service() {
    print_status "Updating existing Cloud Run service: $SERVICE_NAME"
    print_info "Updating Docker image and Pub/Sub environment variables"
    
    gcloud run services update $SERVICE_NAME \
        --image=$FULL_IMAGE_NAME \
        --region=$REGION \
        --set-env-vars="GOOGLE_CLOUD_PROJECT=$PROJECT_ID" \
        --set-env-vars="PUBSUB_REFUND_UPDATE_TOPIC=refund-update-from-irs" \
        --set-env-vars="PUBSUB_SEND_REFUND_TOPIC=send-refund-to-irs" \
        --set-env-vars="PUBSUB_ENABLED=true" \
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

# Function to show health check
check_health() {
    SERVICE_URL=$(get_service_url)
    print_info "Checking service health..."
    
    # Wait a moment for service to be ready
    sleep 5
    
    if curl -s -f "${SERVICE_URL}/actuator/health" > /dev/null 2>&1; then
        print_status "✅ Service is healthy!"
        print_info "Health check URL: ${SERVICE_URL}/actuator/health"
    else
        print_warning "⚠️  Service health check failed. Service may still be starting up."
        print_info "Health check URL: ${SERVICE_URL}/actuator/health"
    fi
}

# Main deployment logic
main() {
    print_info "Starting deployment of $SERVICE_NAME"
    print_info "Image: $FULL_IMAGE_NAME"
    print_info "Region: $REGION"
    print_info "Port: $PORT"
    print_info "Memory: $MEMORY"
    print_info "CPU: $CPU"
    echo
    
    # Pre-flight checks
    check_gcloud
    get_project
    get_database_info
    
    # Check if image exists locally (optional check)
    if ! docker image inspect $FULL_IMAGE_NAME &> /dev/null; then
        print_warning "Image $FULL_IMAGE_NAME not found locally. Make sure it's available in the registry."
    fi
    
    # Deploy or update service
    if check_service_exists; then
        print_info "Service $SERVICE_NAME already exists. Updating Docker image only..."
        print_info "Database configuration and environment variables will remain unchanged."
        update_service
    else
        print_info "Service $SERVICE_NAME does not exist. Creating new service with full configuration..."
        deploy_new_service
    fi
    
    # Get and display service information
    echo
    print_status "Deployment completed successfully!"
    SERVICE_URL=$(get_service_url)
    print_info "Service URL: $SERVICE_URL"
    print_info "API Documentation: ${SERVICE_URL}/swagger-ui.html"
    print_info "Health Check: ${SERVICE_URL}/actuator/health"
    echo
    show_service_status
    
    # Check service health
    echo
    check_health
    
    echo
    print_status "You can test the deployment with:"
    echo "  curl ${SERVICE_URL}/actuator/health"
    echo "  curl ${SERVICE_URL}/swagger-ui.html"
    echo
    print_status "To view logs:"
    echo "  gcloud run logs read $SERVICE_NAME --region=$REGION"
}

# Help function
show_help() {
    echo "Usage: $0 [TAG]"
    echo
    echo "Deploy badhtaxfileserv to Google Cloud Run"
    echo
    echo "Arguments:"
    echo "  TAG        Docker image tag to deploy (default: latest)"
    echo
    echo "Environment Variables:"
    echo "  GOOGLE_CLOUD_PROJECT  Override the default project"
    echo "  DB_PASSWORD          Database password (optional - will try to get from Pulumi)"
    echo "  DB_INSTANCE_NAME     Database instance name (optional - will get from Pulumi)"
    echo "  DB_NAME              Database name (optional - will get from Pulumi)"
    echo "  DB_USER              Database user (optional - will get from Pulumi)"
    echo
    echo "Examples:"
    echo "  $0                           # Deploy latest tag"
    echo "  $0 v1.0.4                   # Deploy specific version"
    echo "  DB_PASSWORD=secret $0        # Deploy with database password"
    echo "  $0 v1.0.4 --help            # Show this help"
    echo
    echo "Required Setup:"
    echo "  1. Ensure Pulumi infrastructure is deployed (database instance exists)"
    echo "  2. Ensure gcloud is authenticated and project is set"
    echo "  3. Run from project root or have infra directory accessible"
    echo
    echo "Note:"
    echo "  - First deployment: Creates service with full database configuration"
    echo "  - Subsequent deployments: Only updates Docker image, keeps existing config"
    echo
}

# Parse command line arguments
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_help
    exit 0
fi

# Run main function
main
