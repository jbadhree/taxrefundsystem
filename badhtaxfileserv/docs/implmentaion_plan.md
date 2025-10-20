## badhtaxfileserv - Implementation Plan

### 1) Goals and Scope
- Build a new Java Spring Boot (Gradle) microservice `badhtaxfileserv`.
- Persist data in local Postgres at URL `postgresql://taxrefund_user:taxrefund_password@localhost:5432/taxrefund?schema=taxfileservdb`.
  - Note for JDBC: we will use `jdbc:postgresql://localhost:5432/taxrefund?currentSchema=taxfileservdb` and set schema in Spring/Hibernate/Flyway configs.
- Expose HTTP on port 4000.
- Implement endpoints:
  1. POST `/taxFile` — create/update a tax file for `(userId, year)` with fields Income, Expense, TaxRate, Deducted, Refund.
  2. GET `/taxFile` — query by `userId`, `year`; return the full record.
  3. GET `/refund` — query by `userId`, `year`, `fileId` (optional); return `refundStatus`, `errors`, and `eta` if status is `IN_PROGRESS`.
  4. POST `/processRefundEvent` — placeholder that will later receive a refund CloudEvent.

### 2) Non-Goals (for now)
- Authentication/Authorization.
- Real queue integration or actual refund processing workflow.
- Cross-service user validation.

### 3) Architecture and Tech Stack
- Spring Boot 3.x, Java 21, Gradle (Kotlin or Groovy DSL; choose Groovy for simplicity).
- Spring Web (REST), Spring Validation, Spring Data JPA, Flyway, PostgreSQL Driver, Jackson.
- Testing: JUnit 5, Mockito, Testcontainers, Spring Boot Test, WireMock.
- API Documentation: SpringDoc OpenAPI 3, Swagger UI.
- Packaging: Dockerfile exposing 4000.

### 4) Data Model (Schema: `taxfileservdb`)
Two tables to separate tax filing from refund processing.

Table: `tax_file`
- `id` UUID PRIMARY KEY
- `user_id` VARCHAR(100) NOT NULL
- `tax_year` INT NOT NULL
- `income` NUMERIC(14,2) NOT NULL CHECK (income >= 0)
- `expense` NUMERIC(14,2) NOT NULL CHECK (expense >= 0)
- `tax_rate_percent` NUMERIC(5,2) NOT NULL CHECK (tax_rate_percent >= 0 AND tax_rate_percent <= 100)
- `deducted` NUMERIC(14,2) NOT NULL CHECK (deducted >= 0)
- `refund_amount` NUMERIC(14,2) NOT NULL CHECK (refund_amount >= 0)
- `tax_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING'
  - Allowed values: `PENDING`, `COMPLETED`
  - `COMPLETED` when `refund_amount = 0` (no refund applicable)
- `created_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- `updated_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- Unique constraint on (`user_id`, `tax_year`) to ensure a single active tax file per user per year.

Table: `refund`
- `id` UUID PRIMARY KEY
- `tax_file_id` UUID NOT NULL REFERENCES `tax_file(id)` ON DELETE CASCADE
- `refund_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING'
  - Allowed values: `PENDING`, `IN_PROGRESS`, `APPROVED`, `REJECTED`, `ERROR`
- `refund_errors` JSONB NULL -- array of error objects like [{ code, message }]
- `refund_eta` TIMESTAMP WITH TIME ZONE NULL
- `created_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- `updated_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- Unique constraint on (`tax_file_id`) to ensure one refund record per tax file.

Table: `refund_events` (append-only for ML training)
- `id` UUID PRIMARY KEY
- `refund_id` UUID NOT NULL REFERENCES `refund(id)` ON DELETE CASCADE
- `event_type` VARCHAR(32) NOT NULL
  - Allowed values: `refund.inprogress`, `refund.approved`, `refund.rejected`, `refund.error`
- `event_date` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- `error_reasons` JSONB NULL -- array of error objects like [{ code, message }] (only for refund.error)
- `created_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()

Notes:
- Exactly one file per `user_id` + `tax_year`. Financial fields are immutable after creation.
- Refund table only created when `refund_amount > 0` (refund applicable).
- Tax status `COMPLETED` when no refund needed (`refund_amount = 0`).
- `refund_events` table is append-only for ML training data - never updates, only inserts.
- Refund status/eta/errors are in separate table for better separation of concerns.

### 5) API Specification

Base URL: `http://localhost:4000`
Content-Type: `application/json`

Validation Rules (common):
- `userId`: non-empty string (max 100)
- `year`: integer between 1900 and 2100
- `income`, `expense`, `deducted`, `refund`: >= 0
- `taxRate`: percentage 0..100 (decimal). Stored as `tax_rate_percent`.

#### 5.1 POST /taxFile
- Purpose: Create a new tax file for `(userId, year)`. Create-only — no updates allowed once filed.
- Request Body:
```json
{
  "userId": "user-123",
  "year": 2024,
  "income": 120000.00,
  "expense": 20000.00,
  "taxRate": 30.0,
  "deducted": 25000.00,
  "refund": 500.00
}
```
- Behavior:
  - If a record for `(userId, year)` already exists, reject with 409 Conflict. Financial fields cannot be modified once created.
  - If none exists, create tax_file record.
  - If `refund > 0`, also create refund record with `refundStatus = PENDING` and `refundEta` populated using `ETAPredictor`.
  - If `refund = 0`, set `taxStatus = COMPLETED` (no refund needed).
- Responses:
  - 201 Created (on create):
```json
{
  "fileId": "e7c8a5e5-2c8c-4a42-9d0e-7d7a6ebf9291",
  "userId": "user-123",
  "year": 2024,
  "income": 120000.00,
  "expense": 20000.00,
  "taxRate": 30.0,
  "deducted": 25000.00,
  "refund": 500.00,
  "taxStatus": "PENDING",
  "refundStatus": "PENDING",
  "refundErrors": [],
  "refundEta": "2025-11-15T09:00:00Z",
  "createdAt": "2025-10-20T09:00:00Z",
  "updatedAt": "2025-10-20T09:00:00Z"
}
```
  - 400 Bad Request (validation errors)
  - 409 Conflict if `(userId, year)` already exists

#### 5.2 GET /taxFile
- Query Params: `userId` (required), `year` (required)
- Response 200 OK:
```json
{
  "fileId": "e7c8a5e5-2c8c-4a42-9d0e-7d7a6ebf9291",
  "userId": "user-123",
  "year": 2024,
  "income": 120000.00,
  "expense": 20000.00,
  "taxRate": 30.0,
  "deducted": 25000.00,
  "refund": 500.00,
  "taxStatus": "PENDING",
  "refundStatus": "IN_PROGRESS",
  "refundErrors": [],
  "refundEta": "2025-10-28T09:00:00Z",
  "createdAt": "2025-10-20T09:00:00Z",
  "updatedAt": "2025-10-20T09:00:00Z"
}
```
- 404 Not Found if no record exists for `(userId, year)`.

#### 5.3 GET /refund
- Query Params: `userId` (optional if `fileId` provided), `year` (optional if `fileId` provided), `fileId` (optional). At least one of (`fileId`) or (`userId` + `year`) must be supplied.
- Behavior: resolve the target record; return refund fields. Only applicable if refund record exists (refund_amount > 0).
- Response 200 OK:
```json
{
  "fileId": "e7c8a5e5-2c8c-4a42-9d0e-7d7a6ebf9291",
  "userId": "user-123",
  "year": 2024,
  "refundStatus": "IN_PROGRESS",
  "errors": [],
  "eta": "2025-10-28T09:00:00Z"
}
```
- 404 Not Found if not resolvable or no refund record exists (refund_amount = 0).

Status/ETA Rules:
- `eta` is populated when refund record is created (using `ETAPredictor`) and returned for all statuses except `APPROVED`, `REJECTED`, `ERROR`.
- ETA is generated by `ETAPredictor` helper class (random 10-60 days from current date).
- Cloud events will update `refundStatus` from `PENDING` → `IN_PROGRESS` → `APPROVED`/`REJECTED`/`ERROR`.
- If no refund record exists (refund_amount = 0), GET `/refund` returns 404.

#### 5.4 POST /processRefundEvent
- Purpose: processes cloud events to update refund status and append event data for ML training.
- Request Body (example):
```json
{
  "eventId": "1b2c3d",
  "fileId": "e7c8a5e5-2c8c-4a42-9d0e-7d7a6ebf9291",
  "type": "refund.inprogress",
  "data": {
    "eventDate": "2025-10-25T14:30:00Z"
  }
}
```
- Event Types and Behavior:
  - `refund.inprogress`: If status is `PENDING`, update to `IN_PROGRESS` and append event. If already `IN_PROGRESS`, do nothing.
  - `refund.approved`: Update status to `APPROVED` and append event with approval date.
  - `refund.rejected`: Update status to `REJECTED` and append event with rejection date.
  - `refund.error`: Update status to `ERROR` and append event with error date and error reasons.
- Event Data Structure:
  - `refund.inprogress`: `{ "eventDate": "ISO_TIMESTAMP" }`
  - `refund.approved`: `{ "eventDate": "ISO_TIMESTAMP" }`
  - `refund.rejected`: `{ "eventDate": "ISO_TIMESTAMP" }`
  - `refund.error`: `{ "eventDate": "ISO_TIMESTAMP", "errorReasons": [{ "code": "ERR001", "message": "Invalid data" }] }`
- Responses:
  - 202 Accepted.

### 6) Database and Migrations
- Use Flyway for schema management.
- Configure Flyway schemas to `taxfileservdb` and baseline/create if not exists.
- Initial migration `V1__init.sql` will:
  - `CREATE SCHEMA IF NOT EXISTS taxfileservdb;`
  - Create `tax_file` table with columns and constraints above.
  - Create `refund` table with foreign key to `tax_file`.
  - Create `refund_events` table with foreign key to `refund` (append-only).
  - Add triggers to auto-update `updated_at` on row updates for both main tables.

### 7) Application Configuration

#### 7.1 Environment-Specific Database Configuration
- Application listens on port `4000` via `server.port=4000`.
- Database configuration supports both **local development** and **cloud deployment** via environment variables.

#### 7.2 Local Development (Default)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taxrefund?currentSchema=taxfileservdb
    username: taxrefund_user
    password: taxrefund_password
  jpa:
    properties:
      hibernate:
        default_schema: taxfileservdb
  flyway:
    schemas: taxfileservdb
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true
    hibernate:
      ddl-auto: validate
```

#### 7.3 Cloud Run + Cloud SQL PostgreSQL
```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/taxrefund?currentSchema=taxfileservdb}
    username: ${DATABASE_USERNAME:taxrefund_user}
    password: ${DATABASE_PASSWORD:taxrefund_password}
  jpa:
    properties:
      hibernate:
        default_schema: ${FLYWAY_SCHEMAS:taxfileservdb}
  flyway:
    schemas: ${FLYWAY_SCHEMAS:taxfileservdb}
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true
    hibernate:
      ddl-auto: validate
```

#### 7.4 Environment Variables for Cloud Deployment
- **DATABASE_URL**: Full JDBC connection string for Cloud SQL
  - Format: `jdbc:postgresql:///taxrefund?host=/cloudsql/PROJECT_ID:REGION:INSTANCE_NAME&currentSchema=taxfileservdb`
- **DATABASE_USERNAME**: Cloud SQL username
- **DATABASE_PASSWORD**: Cloud SQL password (stored in Secret Manager)
- **FLYWAY_SCHEMAS**: Schema name (defaults to `taxfileservdb`)

#### 7.5 Database Swapping Strategy
- **Local Development**: Uses local PostgreSQL with hardcoded values
- **Cloud Run**: Uses Cloud SQL PostgreSQL via environment variables
- **Same Schema**: Both environments use identical `taxfileservdb` schema
- **Flyway Migrations**: Work identically in both environments
- **Zero Code Changes**: Application automatically adapts to environment

### 8) Service/Controller/DTO Design
- Layering:
  - Controller: request/response mapping and validation.
  - Service: business logic (create-only tax file, conditional refund creation, lookups).
  - Repository: Spring Data JPA for `TaxFile` and `Refund` entities.
  - Helper: `ETAPredictor` utility class for random ETA generation.
- DTOs:
  - `CreateTaxFileRequest`
  - `TaxFileResponse`
  - `RefundResponse`
  - `ProcessRefundEventRequest`
  - `RefundEventEntity` (for refund_events table)
- Mapping: MapStruct (optional) or manual mapping for simplicity.

#### 8.1 ETAPredictor Helper Class
```java
@Component
public class ETAPredictor {
    private static final Random RANDOM = new Random();
    private static final int MIN_DAYS = 10;
    private static final int MAX_DAYS = 60;
    
    public LocalDateTime predictETA() {
        int daysToAdd = RANDOM.nextInt(MAX_DAYS - MIN_DAYS + 1) + MIN_DAYS;
        return LocalDateTime.now().plusDays(daysToAdd);
    }
}
```

#### 8.2 Test Configuration
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

#### 8.3 OpenAPI Configuration
```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Tax File Service API",
        version = "1.0.0",
        description = "API for managing tax files and refund processing"
    )
)
public class OpenAPIConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .addServersItem(new Server().url("http://localhost:4000"))
            .components(new Components()
                .addSchemas("ErrorResponse", new Schema<>()
                    .type("object")
                    .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                    .addProperty("path", new Schema<>().type("string"))
                    .addProperty("status", new Schema<>().type("integer"))
                    .addProperty("error", new Schema<>().type("string"))
                    .addProperty("message", new Schema<>().type("string"))
                )
            );
    }
}
```

### 9) Error Handling
- Use `@ControllerAdvice` to return structured errors:
```json
{
  "timestamp": "2025-10-20T09:00:00Z",
  "path": "/taxFile",
  "status": 400,
  "error": "Bad Request",
  "message": "taxRate must be between 0 and 100"
}
```
- 400 for validation errors, 404 for not found, 409 for duplicate `(userId, year)` on create.

### 10) Dockerization

#### 10.1 Dockerfile Configuration
- Multi-stage Dockerfile building the jar via Gradle and running with `SERVER_PORT=4000`.
- Health endpoint: rely on Spring Boot actuator later (optional). For now default 200 on `/` not required.

#### 10.2 Docker Image Build and Push
- **Image Name**: `jbadhree/badhtaxfileserv`
- **Tag Strategy**: Use version tags (e.g., `jbadhree/badhtaxfileserv:1.0.0`, `jbadhree/badhtaxfileserv:latest`)
- **Build Command**: `docker build -t jbadhree/badhtaxfileserv:latest .`
- **Push Command**: `docker push jbadhree/badhtaxfileserv:latest`

#### 10.3 Docker Build Script
```bash
#!/bin/bash
# build-and-push.sh

# Build the Docker image
echo "Building Docker image..."
docker build -t jbadhree/badhtaxfileserv:latest .

# Tag with version (if provided)
if [ ! -z "$1" ]; then
    docker tag jbadhree/badhtaxfileserv:latest jbadhree/badhtaxfileserv:$1
    echo "Tagged as jbadhree/badhtaxfileserv:$1"
fi

# Push to Docker Hub
echo "Pushing to Docker Hub..."
docker push jbadhree/badhtaxfileserv:latest

if [ ! -z "$1" ]; then
    docker push jbadhree/badhtaxfileserv:$1
    echo "Pushed jbadhree/badhtaxfileserv:$1"
fi

echo "Build and push completed!"
```

#### 10.4 Docker Commands
```bash
# Build image
docker build -t jbadhree/badhtaxfileserv:latest .

# Build with version tag
docker build -t jbadhree/badhtaxfileserv:1.0.0 .

# Push to Docker Hub
docker push jbadhree/badhtaxfileserv:latest
docker push jbadhree/badhtaxfileserv:1.0.0

# Run locally
docker run -p 4000:4000 jbadhree/badhtaxfileserv:latest

# Run with environment variables
docker run -p 4000:4000 \
  -e DATABASE_URL="jdbc:postgresql://localhost:5432/taxrefund?currentSchema=taxfileservdb" \
  -e DATABASE_USERNAME="taxrefund_user" \
  -e DATABASE_PASSWORD="taxrefund_password" \
  jbadhree/badhtaxfileserv:latest
```

### 11) Step-by-Step Implementation Plan
1. Scaffold Spring Boot project with Gradle, Java 21, dependencies: web, validation, data-jpa, postgres, flyway, testcontainers, openapi.
2. Add `application.yml` with **environment-specific database configuration** (local + cloud support).
3. Implement Flyway `V1__init.sql` migration (schema + both tables + triggers for `updated_at`).
4. Create JPA entities `TaxFile`, `Refund`, and `RefundEvent` + repositories.
5. Create `ETAPredictor` helper class.
6. Create DTOs and validation annotations.
7. Implement service methods:
   - `createTaxFile(request)` — create-only; throw if `(userId, year)` exists; conditionally create refund record with ETA.
   - `getTaxFile(userId, year)` — join with refund if exists.
   - `getRefund(fileId | userId+year)` — only if refund record exists.
   - `processRefundEvent(event)` — update refund status and append event to refund_events table.
8. Implement controllers for the four endpoints and response mappers.
9. Add global exception handler (map data integrity violation to 409).
10. Create Dockerfile (port 4000) and `.dockerignore`.
11. **Write comprehensive test suite:**
    - Unit tests for services, repositories, and utilities
    - Integration tests with Testcontainers
    - Controller tests with MockMvc
    - API contract tests
12. **Generate OpenAPI specification** with Swagger UI.
13. **Test database swapping** between local and cloud environments.
14. **Build and push Docker image** to `jbadhree/badhtaxfileserv` repository.
15. Manual verification with curl examples.

#### 11.1 Flyway Migration Configuration Details
- Migrations run automatically at application startup
- `baseline-on-migrate=true` - handles existing databases
- `validate-on-migrate=true` - ensures migration integrity
- `clean-disabled=true` - prevents accidental data loss
- `spring.jpa.hibernate.ddl-auto=validate` - lets Flyway manage schema

#### 11.2 Database Environment Testing
- **Local Testing**: Verify with local PostgreSQL
- **Cloud Testing**: Verify with Cloud SQL PostgreSQL via environment variables
- **Migration Testing**: Ensure Flyway works in both environments
- **Connection Testing**: Validate database connectivity and schema creation

#### 11.3 Docker Image Testing
- **Local Build**: Test Docker image builds successfully
- **Local Run**: Test Docker container runs with local database
- **Docker Hub Push**: Verify image pushes to `jbadhree/badhtaxfileserv`
- **Pull and Run**: Test pulling image from Docker Hub and running

### 12) Comprehensive Test Suite

#### 12.1 Unit Tests
- **ETAPredictorTest**: Verify random ETA generation (10-60 days range)
- **TaxFileServiceTest**: Mock repository tests for create/get operations
- **RefundServiceTest**: Mock repository tests for refund operations
- **RefundEventServiceTest**: Mock repository tests for event processing
- **ValidationTest**: DTO validation annotation tests

#### 12.2 Integration Tests (Testcontainers)
- **TaxFileControllerIntegrationTest**: Full HTTP request/response cycle
- **RefundControllerIntegrationTest**: Full HTTP request/response cycle
- **RefundEventControllerIntegrationTest**: Full HTTP request/response cycle
- **DatabaseIntegrationTest**: JPA entity persistence and queries
- **FlywayMigrationTest**: Verify migrations run correctly

#### 12.3 Test Scenarios (initial)
- POST `/taxFile` with refund > 0 -> 201 (creates both tax_file and refund records); repeat -> 409.
- POST `/taxFile` with refund = 0 -> 201 (creates tax_file with taxStatus=COMPLETED, no refund record).
- GET `/taxFile` for existing -> 200 with correct fields; non-existing -> 404.
- GET `/refund` with fileId -> 200; with userId+year -> 200; non-existing -> 404; if status `IN_PROGRESS`, `eta` present else null.
- GET `/refund` for tax file with refund=0 -> 404 (no refund record).
- POST `/processRefundEvent` with `refund.inprogress` -> 202 (updates status, appends event).
- POST `/processRefundEvent` with `refund.approved` -> 202 (updates status, appends event with date).
- POST `/processRefundEvent` with `refund.error` -> 202 (updates status, appends event with error reasons).

#### 12.4 API Contract Tests
- **OpenAPI Contract Tests**: Verify API matches OpenAPI specification
- **Response Schema Validation**: Ensure responses match defined schemas
- **Error Response Validation**: Verify error responses follow standard format

### 13) OpenAPI Specification
- **Swagger UI**: Available at `http://localhost:4000/swagger-ui.html`
- **OpenAPI JSON**: Available at `http://localhost:4000/v3/api-docs`
- **API Documentation**: Auto-generated from controller annotations
- **Interactive Testing**: Test endpoints directly from Swagger UI

### 14) Curl Examples
- Create:
```bash
curl -s -X POST http://localhost:4000/taxFile \
  -H 'Content-Type: application/json' \
  -d '{
    "userId":"user-123",
    "year":2024,
    "income":120000,
    "expense":20000,
    "taxRate":30.0,
    "deducted":25000,
    "refund":500
  }'
```
- Get tax file:
```bash
curl -s 'http://localhost:4000/taxFile?userId=user-123&year=2024'
```
- Get refund:
```bash
curl -s 'http://localhost:4000/refund?userId=user-123&year=2024'
```
- Process refund event (inprogress):
```bash
curl -s -X POST http://localhost:4000/processRefundEvent \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"evt-1",
    "fileId":"e7c8a5e5-2c8c-4a42-9d0e-7d7a6ebf9291",
    "type":"refund.inprogress",
    "data":{"eventDate":"2025-10-25T14:30:00Z"}
  }'
```
- Process refund event (approved):
```bash
curl -s -X POST http://localhost:4000/processRefundEvent \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"evt-2",
    "fileId":"e7c8a5e5-2c8c-4a42-9d0e-7d7a6ebf9291",
    "type":"refund.approved",
    "data":{"eventDate":"2025-11-15T10:00:00Z"}
  }'
```
- Process refund event (error):
```bash
curl -s -X POST http://localhost:4000/processRefundEvent \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"evt-3",
    "fileId":"e7c8a5e5-2c8c-4a42-9d0e-7d7a6ebf9291",
    "type":"refund.error",
    "data":{
      "eventDate":"2025-10-26T09:15:00Z",
      "errorReasons":[{"code":"ERR001","message":"Invalid bank account"}]
    }
  }'
```
