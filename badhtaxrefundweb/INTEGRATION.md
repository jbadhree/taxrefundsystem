# BadhTaxRefundWeb Integration with BadhTaxFileServ

This document describes the integration of the Next.js web application with the Spring Boot tax file service.

## Changes Made

### 1. Environment Variable Configuration
- Added `BADHTAXFILESERV_BASEURL` environment variable support
- Default value: `http://localhost:4000` for local development
- Production value: Automatically set to the deployed file service URL via Pulumi

### 2. API Integration
- Created `src/config/api.ts` - Configuration and endpoint helpers
- Created `src/types/api.ts` - TypeScript interfaces matching Java DTOs
- Created `src/services/taxFileService.ts` - Service class for API communication
- Updated API routes to use actual badhtaxfileserv endpoints instead of mock data

### 3. Updated API Routes
- `/api/login` - Updated to use new user structure
- `/api/user-details` - Now fetches actual tax file data from badhtaxfileserv
- `/api/tax-file` - New route for tax file operations (GET/POST)
- `/api/refund` - New route for refund information

### 4. Infrastructure Updates
- Updated Pulumi configuration to pass file service URL to web service
- Updated local docker environment with `BADHTAXFILESERV_BASEURL=http://localhost:4000`

## API Endpoints

### Tax File Service Endpoints
- `POST /taxFile` - Create a new tax file
- `GET /taxFile?userId={userId}&year={year}` - Get tax file by user and year

### Refund Service Endpoints
- `GET /refund?userId={userId}&year={year}` - Get refund by user and year
- `GET /refund?fileId={fileId}` - Get refund by file ID

### Refund Event Service Endpoints
- `POST /processRefundEvent` - Process refund events

## Testing the Integration

### Option 1: Complete Docker Setup (Recommended)
Use the automated setup script that builds the web service image and starts all services:

```bash
# From the project root directory
./setup-local.sh
```

This script will:
1. Build the badhtaxrefundweb Docker image
2. Start all services (PostgreSQL, badhtaxfileserv, badhtaxrefundweb, pgAdmin)
3. Run integration tests
4. Show service status

### Option 2: Manual Docker Setup
1. Build the web service image:
   ```bash
   cd badhtaxrefundweb
   docker build -t jbadhree/badhtaxrefundweb:latest .
   ```

2. Start all services:
   ```bash
   cd local
   docker-compose up -d
   ```

### Option 3: Development Mode
1. Start the database and badhtaxfileserv:
   ```bash
   cd local
   docker-compose up -d postgres badhtaxfileserv
   ```

2. Start the Next.js development server:
   ```bash
   cd badhtaxrefundweb
   npm run dev
   ```

### Running the Test Script
```bash
cd badhtaxrefundweb
./test-integration.sh
```

This script will test all the API endpoints and show the responses.

### Manual Testing
1. **Login**: POST to `/api/login` with credentials:
   ```json
   {
     "email": "bruce@badhtaxrefund.com",
     "password": "Chang3m3!"
   }
   ```

2. **Get User Details**: GET `/api/user-details?userId={userId}`
   - This will fetch actual tax file data from badhtaxfileserv

3. **Create Tax File**: POST to `/api/tax-file`:
   ```json
   {
     "userId": "user-1",
     "year": 2024
   }
   ```

4. **Get Tax File**: GET `/api/tax-file?userId={userId}&year={year}`

5. **Get Refund**: GET `/api/refund?userId={userId}&year={year}`

## Environment Variables

### Local Development
```bash
BADHTAXFILESERV_BASEURL=http://localhost:4000
```

### Production
The environment variable is automatically set by Pulumi to the deployed file service URL.

## Error Handling

The integration includes proper error handling:
- API communication errors are caught and logged
- Fallback responses are provided when the file service is unavailable
- Proper HTTP status codes are returned based on the file service responses

## Notes

- The web application still uses mock user data for authentication (this would typically come from a separate user service)
- Tax file and refund data now comes from the actual badhtaxfileserv API
- The integration gracefully handles cases where the file service is not available
