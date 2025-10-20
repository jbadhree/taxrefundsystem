# BadhTaxRefundSystem - Infrastructure

This directory contains Pulumi infrastructure code to deploy the BadhTaxRefundSystem to Google Cloud Run using a Docker Hub image. Currently includes the `badhtaxrefundweb` component. The configuration is set up for production deployment.

## System Architecture

**BadhTaxRefundSystem** is the overall system name with the following components:

- **badhtaxrefundweb**: NextJS web application (currently deployed)
- *Additional components can be added as needed (e.g., API service, database, etc.)*

### Adding New Components

To add a new service component, you would:

1. Add new config variables (e.g., `apiServiceName`, `apiImageName`)
2. Create new Cloud Run service resources in `__main__.py`
3. Export the new service outputs

Example for future API service:
```yaml
# Pulumi.prod.yaml
badhtaxrefundsystem:apiServiceName: badhtaxrefundapi
badhtaxrefundsystem:apiImageName: jbadhree/badhtaxrefundapi:v1.0.0
```

## Prerequisites

1. **Google Cloud SDK**: Install and authenticate with `gcloud auth login`
2. **Pulumi CLI**: Install from [pulumi.com](https://www.pulumi.com/docs/get-started/install/)
3. **Python 3.8+**: Required for Pulumi Python runtime

## Setup

### 1. Install Dependencies

```bash
cd infra
pip install -r requirements.txt
```

### 2. Configure Pulumi

```bash
# Set your Google Cloud project (gcloud will be used automatically)
gcloud config set project YOUR_PROJECT_ID

# Set required configuration
pulumi config set webServiceName badhtaxrefundweb
pulumi config set webImageName jbadhree/badhtaxrefundweb:v1.0.0

# Optional: Set region (defaults to us-central1)
pulumi config set region us-central1
```

**Note**: If you need to create a new stack, run `pulumi stack init prod` first.

### 3. Deploy Infrastructure

```bash
# Preview the deployment
pulumi preview

# Deploy the infrastructure
pulumi up
```

## What Gets Created

### Google Cloud Resources

1. **Cloud Run Service**: `tax-refund-web`
   - Runs the NextJS application from Docker Hub
   - Uses image: `jbadhree/badhtaxrefundweb:v1.0.0`
   - Configured with appropriate resource limits
   - Publicly accessible (no authentication required)

2. **IAM Policy**: Allows unauthenticated access to the Cloud Run service

### Configuration

- **CPU**: 500m request, 1000m limit
- **Memory**: 256Mi request, 512Mi limit
- **Concurrency**: 80 requests per container
- **Timeout**: 300 seconds
- **Auto-scaling**: 0-10 instances

## Environment Variables

The Cloud Run service is configured with:
- `NODE_ENV=production`
- `PORT=3000`

## Deployment Process

1. **Build & Push**: Docker image is built and pushed to Docker Hub
2. **Deploy**: Cloud Run service uses the Docker Hub image
3. **Scale**: Service automatically scales based on traffic

### Updating the Docker Image

To update the Docker image version, use Pulumi config:

```bash
# Update the web image version
pulumi config set webImageName jbadhree/badhtaxrefundweb:v1.1.0

# Deploy the changes
pulumi up
```

### Updating the Web Service Name

To change the web service name:

```bash
# Update the web service name
pulumi config set webServiceName my-new-web-service

# Deploy the changes
pulumi up
```

## Monitoring

After deployment, you can monitor your service:

- **Cloud Run Console**: View service metrics and logs
- **Docker Hub**: View your published image at https://hub.docker.com/r/jbadhree/badhtaxrefundweb

## Cleanup

To destroy all resources:

```bash
pulumi destroy
```

## Customization

### Environment-Specific Configuration

Create different stacks for different environments:

```bash
# Development
pulumi stack init dev
pulumi config set region us-central1

# Production
pulumi stack init prod
pulumi config set region us-east1
```

### Resource Limits

Modify resource limits in `__main__.py`:

```python
resources=gcp.cloudrun.ServiceTemplateSpecContainerResourcesArgs(
    limits={
        "cpu": "2000m",      # Increase CPU limit
        "memory": "1Gi"      # Increase memory limit
    }
)
```

### Custom Domain

To add a custom domain, add the following to your Pulumi code:

```python
# Add domain mapping
domain_mapping = gcp.cloudrun.DomainMapping(
    "custom-domain",
    name="your-domain.com",
    location=region,
    metadata=gcp.cloudrun.DomainMappingMetadataArgs(
        namespace=project_id
    ),
    spec=gcp.cloudrun.DomainMappingSpecArgs(
        route_name=cloud_run_service.name
    )
)
```

## Configuration Management

### Required Configuration

- `webServiceName`: Name of the web Cloud Run service
- `webImageName`: Docker image name and tag for web service (e.g., `jbadhree/badhtaxrefundweb:v1.0.0`)

### Optional Configuration

- `region`: Google Cloud region (defaults to `us-central1`)

### Project ID

The project ID is automatically obtained from your gcloud configuration:
```bash
gcloud config set project YOUR_PROJECT_ID
```

## Useful Commands

```bash
# View current configuration
pulumi config

# Set configuration values
pulumi config set webServiceName my-web-service
pulumi config set webImageName my-web-image:v2.0.0
pulumi config set region us-east1

# View stack outputs
pulumi stack output

# View resource details
pulumi state show <resource-name>

# Refresh state
pulumi refresh

# Preview changes
pulumi preview

# Deploy changes
pulumi up

# Destroy resources
pulumi destroy
```

## Security Considerations

- The service is configured for public access
- Consider adding authentication for production use
- Monitor resource usage and costs
- Regularly update base images for security patches
