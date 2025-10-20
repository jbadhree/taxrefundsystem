# Docker Setup for Tax Refund System

This guide explains how to build and deploy the Tax Refund System using Docker.

## Prerequisites

- Docker installed on your machine
- Docker Hub account (for pushing images)

## Building the Docker Image

### 1. Build the image locally

```bash
# Navigate to the project directory
cd badhtaxrefundweb

# Build the Docker image
docker build -t badhtaxrefundweb .
```

### 2. Test the image locally

```bash
# Run the container
docker run -p 3000:3000 badhtaxrefundweb

# The application will be available at http://localhost:3000
```

## Pushing to Docker Hub

### 1. Tag the image for Docker Hub

```bash
# Replace 'yourusername' with your Docker Hub username
docker tag badhtaxrefundweb yourusername/badhtaxrefundweb:latest
docker tag badhtaxrefundweb yourusername/badhtaxrefundweb:v1.0.0
```

### 2. Login to Docker Hub

```bash
docker login
# Enter your Docker Hub username and password
```

### 3. Push the image

```bash
# Push the latest version
docker push yourusername/badhtaxrefundweb:latest

# Push the specific version
docker push yourusername/badhtaxrefundweb:v1.0.0
```

## Running from Docker Hub

Once pushed, anyone can run your application with:

```bash
# Pull and run the latest version
docker run -p 3000:3000 yourusername/badhtaxrefundweb:latest

# Or pull and run a specific version
docker run -p 3000:3000 yourusername/badhtaxrefundweb:v1.0.0
```

## Docker Compose (Optional)

Create a `docker-compose.yml` file for easier management:

```yaml
version: '3.8'
services:
  badhtaxrefundweb:
    image: yourusername/badhtaxrefundweb:latest
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
    restart: unless-stopped
```

Then run with:
```bash
docker-compose up -d
```

## Image Details

- **Base Image**: Node.js 18 Alpine (lightweight)
- **Multi-stage Build**: Optimized for production
- **Standalone Output**: Self-contained with minimal dependencies
- **Security**: Runs as non-root user (nextjs:nodejs)
- **Port**: Exposes port 3000
- **Size**: Optimized for minimal image size

## Troubleshooting

### Build Issues
- Ensure you're in the correct directory (`badhtaxrefundweb`)
- Check that all files are present (package.json, src/, etc.)
- Verify Docker is running

### Runtime Issues
- Check if port 3000 is available
- Verify the container is running: `docker ps`
- Check logs: `docker logs <container_id>`

### Push Issues
- Ensure you're logged in to Docker Hub
- Check your Docker Hub username and repository name
- Verify you have push permissions to the repository
