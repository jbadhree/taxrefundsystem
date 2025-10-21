#!/bin/bash
# test-env-var.sh - Test if environment variable is being read correctly

echo "Testing environment variable reading..."

# Test the web service environment variable endpoint
curl -s "https://badhtaxrefundweb-797008539263.us-central1.run.app/api/user-details?userId=user-1" | head -c 200
echo ""

# Test backend directly
echo "Testing backend directly:"
curl -s "https://badhtaxfileserv-797008539263.us-central1.run.app/taxFile?userId=user-1&year=2024" | head -c 200
echo ""



