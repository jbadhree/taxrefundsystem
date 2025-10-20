# Tax Refund System Infrastructure

This Pulumi project deploys the complete Tax Refund System infrastructure on Google Cloud Platform.

## Architecture

The infrastructure includes:

1. **Cloud SQL PostgreSQL Database**
   - Instance: `taxrefund-db` (db-f1-micro tier)
   - Database: `taxrefund`
   - User: `taxrefund_user`
   - Schemas: `taxfileservdb`, `userservdb`, `aimlservdb`, `taxfilebatchdb`, `taxrefundbatchdb`, `irsdb`

2. **Cloud Run Services**
   - **Web Service**: `badhtaxrefundweb` (Next.js frontend)
   - **File Service**: `badhtaxfileserv` (Spring Boot backend)

## Prerequisites

1. Install Pulumi CLI
2. Install Python 3.8+
3. Install required Python packages:
   ```bash
   pip install -r requirements.txt
   ```
4. Configure Google Cloud authentication
5. Set up Pulumi stack

## Configuration

### Required Configuration

Set the following configuration values in your Pulumi stack:

```bash
# Database password (required secret)
pulumi config set --secret dbPassword "your-secure-password"

# Service names and images
pulumi config set webServiceName "badhtaxrefundweb"
pulumi config set webImageName "jbadhree/badhtaxrefundweb:v1.0.8"
pulumi config set fileServiceName "badhtaxfileserv"
pulumi config set fileImageName "jbadhree/badhtaxfileserv:v1.0.0"

# Database configuration (optional - defaults provided)
pulumi config set dbInstanceName "taxrefund-db"
pulumi config set dbName "taxrefund"
pulumi config set dbUser "taxrefund_user"
```

### Optional Configuration

- `region`: GCP region (default: us-central1)
- `dbInstanceName`: Database instance name (default: taxrefund-db)
- `dbName`: Database name (default: taxrefund)
- `dbUser`: Database user (default: taxrefund_user)

## Deployment

1. **Initialize Pulumi** (if not already done):
   ```bash
   pulumi stack init prod
   ```

2. **Set configuration**:
   ```bash
   pulumi config set --secret dbPassword "your-secure-password"
   ```

3. **Deploy infrastructure**:
   ```bash
   pulumi up
   ```

4. **View outputs**:
   ```bash
   pulumi stack output
   ```

## Database Initialization

The database is automatically initialized with the following schemas:
- `taxfileservdb` - Tax file management
- `userservdb` - User management and authentication
- `aimlservdb` - AI/ML model management
- `taxfilebatchdb` - Tax file batch processing
- `taxrefundbatchdb` - Tax refund batch processing
- `irsdb` - IRS data and interactions

## Service Configuration

### File Service Environment Variables

The `badhtaxfileserv` Cloud Run service is configured with:
- Database connection via private IP
- SSL-required connection
- Spring Boot production profile
- JPA/Hibernate auto-update mode
- Proper resource limits (1 CPU, 1GB RAM)

### Web Service Configuration

The `badhtaxrefundweb` Cloud Run service is configured with:
- Node.js production environment
- Resource limits (1 CPU, 512MB RAM)
- Auto-scaling (0-10 instances)

## Security

- Database requires SSL connections
- Cloud Run services are publicly accessible (configure IAM as needed)
- Database password is stored as a Pulumi secret
- Private IP networking for database connections

## Monitoring and Logging

- Database logging enabled for statements > 1 second
- Cloud Run services include structured logging
- Backup configuration enabled for database

## Cleanup

To destroy all resources:
```bash
pulumi destroy
```

**Warning**: This will permanently delete all data in the database and remove all infrastructure.

## Troubleshooting

1. **Database Connection Issues**: Ensure the Cloud SQL instance is running and the private IP is accessible
2. **Service Startup Issues**: Check Cloud Run logs for application-specific errors
3. **Permission Issues**: Verify IAM roles and service account permissions

## Cost Optimization

- Database uses `db-f1-micro` tier (suitable for development)
- Cloud Run services scale to zero when not in use
- Consider upgrading database tier for production workloads