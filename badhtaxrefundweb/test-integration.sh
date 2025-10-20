#!/bin/bash

# Test script to verify badhtaxrefundweb integration with badhtaxfileserv
# This script tests the API endpoints to ensure they work with the docker-compose setup

echo "Testing badhtaxrefundweb integration with badhtaxfileserv..."
echo "Make sure both services are running via docker-compose"
echo "Web service: http://localhost:3000"
echo "File service: http://localhost:4000"
echo ""

# Test 1: Login endpoint
echo "1. Testing login endpoint..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:3000/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"bruce@badhtaxrefund.com","password":"Chang3m3!"}')

echo "Login response: $LOGIN_RESPONSE"
echo ""

# Extract userId from login response
USER_ID=$(echo $LOGIN_RESPONSE | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
echo "Extracted userId: $USER_ID"
echo ""

# Test 2: User details endpoint
echo "2. Testing user details endpoint..."
USER_DETAILS_RESPONSE=$(curl -s "http://localhost:3000/api/user-details?userId=$USER_ID")
echo "User details response: $USER_DETAILS_RESPONSE"
echo ""

# Test 3: Create tax file endpoint
echo "3. Testing create tax file endpoint..."
CREATE_TAX_FILE_RESPONSE=$(curl -s -X POST http://localhost:3000/api/tax-file \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"year\":2024}")

echo "Create tax file response: $CREATE_TAX_FILE_RESPONSE"
echo ""

# Test 4: Get tax file endpoint
echo "4. Testing get tax file endpoint..."
GET_TAX_FILE_RESPONSE=$(curl -s "http://localhost:3000/api/tax-file?userId=$USER_ID&year=2024")
echo "Get tax file response: $GET_TAX_FILE_RESPONSE"
echo ""

# Test 5: Get refund endpoint
echo "5. Testing get refund endpoint..."
GET_REFUND_RESPONSE=$(curl -s "http://localhost:3000/api/refund?userId=$USER_ID&year=2024")
echo "Get refund response: $GET_REFUND_RESPONSE"
echo ""

echo "Integration test completed!"
echo "Check the responses above to verify the integration is working correctly."
