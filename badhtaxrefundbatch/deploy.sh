#!/bin/bash

# Deploy script for BadhTaxRefundBatch to Google Cloud Run
# This script deploys the service to Cloud Run as a batch job

set -e

# Configuration
PROJECT_ID="your-gcp-project-id"
SERVICE_NAME="badhtaxrefundbatch"
REGION="us-central1"
REGISTRY="gcr.io"

# Get version from argument or use latest
VERSION=${1:-"latest"}

# Image details
IMAGE_NAME="$REGISTRY/$PROJECT_ID/$SERVICE_NAME:$VERSION"

echo "Deploying $SERVICE_NAME to Cloud Run"
echo "Version: $VERSION"
echo "Image: $IMAGE_NAME"
echo "Region: $REGION"

# Check if gcloud is authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    echo "Error: Not authenticated with gcloud"
    echo "Please run: gcloud auth login"
    exit 1
fi

# Set the project
gcloud config set project "$PROJECT_ID"

# Deploy to Cloud Run as a batch job
echo "Deploying to Cloud Run..."

gcloud run jobs create "$SERVICE_NAME" \
    --image="$IMAGE_NAME" \
    --region="$REGION" \
    --memory="1Gi" \
    --cpu="1" \
    --max-retries="3" \
    --parallelism="1" \
    --task-count="1" \
    --task-timeout="3600" \
    --set-env-vars="DATABASE_URL=postgresql://taxrefund_user:taxrefund_password@localhost:5432/taxrefund?schema=taxrefundbatchdb" \
    --set-env-vars="MAX_CONCURRENT_WORKERS=10" \
    --set-env-vars="BATCH_SIZE=100" \
    --set-env-vars="PROCESSING_INTERVAL=60" \
    --set-env-vars="LOG_LEVEL=info" \
    --set-env-vars="SEED_DATA=true" \
    --set-env-vars="CSV_FILE_PATH=./data/refunds_seed.csv" \
    --replace

echo "Deployment completed successfully!"
echo "Job name: $SERVICE_NAME"
echo "Region: $REGION"

# Optional: Execute the job
if [[ "$2" == "--execute" ]]; then
    echo "Executing the job..."
    gcloud run jobs execute "$SERVICE_NAME" --region="$REGION" --wait
    echo "Job execution completed!"
fi

echo "Deploy script completed successfully!"



