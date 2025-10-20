#!/bin/bash

# Load environment variables from docker.env
set -a
source ../local/docker.env
set +a

echo "🚀 Starting badhtaxfileserv with PostgreSQL..."
echo "📊 Database URL: $TAXFILESERVDB_URL"
echo "🔧 Schema: taxfileservdb"
echo ""

# Run the application
mvn spring-boot:run
