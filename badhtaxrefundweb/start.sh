#!/bin/sh
set -e

# Set default port if not provided
export PORT=${PORT:-8080}

# Start the Next.js server
exec node server.js
