# Insurance API

A comprehensive REST API for managing insurance policies, claims, and pricing. Built with Spring Boot 4.1.1 and PostgreSQL, featuring JWT authentication, role-based access control, and complex business logic for multiple insurance types.

## Overview

This API provides a complete insurance management system with support for:
- **Travel Insurance** - Multi-traveler policies with region-based pricing and age group rates
- **Property Insurance** - Area-based pricing with customizable coverage
- **Roadside Assistance** - Vehicle-specific annual policies
- **Claims Management** - Full workflow for claim submission, review, and decision
- **User Management** - Role-based access (CLIENT, AGENT, ADMIN)

## Tech Stack

- **Language:** Java
- **Framework:** Spring Boot 4.1.1
- **Database:** PostgreSQL 16
- **ORM:** JPA / Hibernate
- **Security:** Spring Security with JWT (HS256)
- **Database Migrations:** Flyway
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Build Tool:** Maven

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose (optional, for containerized PostgreSQL)

### Setup & Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Barxh/insurance-api.git
   cd insurance-api
   ```

2. **Start PostgreSQL using Docker Compose:**
   ```bash
   docker compose up -d
   ```
   This starts PostgreSQL at `localhost:5432` with:
   - Database: `fioneer_insurance_dev`
   - Username: `fioneer_admin`
   - Password: `fioneer_password`

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   
   The API will be available at `http://localhost:8080`

4. **Run tests:**
   ```bash
   ./mvnw test
   ```

## Project Structure

```
src/main/java/com/fioneer/insurance/
  api/                     HTTP error responses and centralized exception handling
  auth/                    Authentication & authorization
    controller/            Login, register, refresh, logout endpoints
    service/               Token generation and user authentication logic
    dto/                   Request/response DTOs
    model/                 RefreshToken entity
    repository/            Database access for tokens
  claim/                   Insurance claims management
    controller/            Claim submission and review endpoints
    service/               Claim processing business logic
    dto/                   Claim request/response DTOs
    model/                 Claim, ClaimStatus, ClaimDecision entities
    repository/            Claim data access
  policy/                  Policy management and purchasing
    controller/            Policy purchase and retrieval endpoints
    service/               Complex policy calculation and validation logic
    dto/                   Policy request/response DTOs
    model/                 Policy, PolicyAddon, PolicyCoverage entities
    model/enums/           PolicyStatus, TravelRegion, VehicleType, etc.
    repository/            Policy data access
  pricing/                 Dynamic pricing administration
    controller/            Admin endpoints for rate configuration
    dto/                   Pricing rate request DTOs
    model/                 TravelPricingRate, PropertyPricingRate, RoadsidePricingRate
    repository/            Pricing data access
  insurance_product/       Product catalog
    model/                 InsuranceProduct, InsuranceType
    repository/            Product data access
  insurance_package/       Package definitions
    model/                 InsurancePackage (grouping coverages and addons)
    repository/            Package data access
  insurance_coverage/      Coverage definitions
    model/                 InsuranceCoverage
    repository/            Coverage data access
  insurance_addon/         Add-on options
    model/                 InsuranceAddon
    repository/            Add-on data access
  product_addon/           Product-to-addon mapping
    model/                 ProductAddon
    repository/            Association data access
  user/                    User management
    model/                 User, Role entities
    repository/            User/Role data access
  security/                Security configuration
    SecurityConfig.java    JWT and Spring Security setup
    JwtService.java        Token generation and validation
    CustomUserDetailsService.java  User details provider

src/main/resources/
  application.yaml         Application configuration
  db/migration/            Flyway database migrations (V1-V8)

src/test/java/            Unit and integration tests
```

## API Endpoints

### Authentication

```
POST /api/auth/register          Register new user (public)
POST /api/auth/login             User login (public)
POST /api/auth/refresh           Refresh access token (public)
POST /api/auth/logout            Invalidate refresh token (authenticated)
```

**Example - Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Nikola",
    "lastName": "Nikolic",
    "username": "nikola",
    "password": "SecurePass123!",
    "email": "nikola@example.com",
    "nationalId": "123456789",
    "street": "Main Street",
    "streetNumber": "42",
    "zipCode": "11000",
    "city": "Belgrade"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "AbCdEfGhIjKlMnOpQrStUvWxYz...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshTokenExpiresIn": 2592000
}
```

### Policies

```
GET /api/policies                Get all policies (role-based filtering)
GET /api/policies/{policyId}     Get policy details
POST /api/policies/travel        Purchase travel insurance
POST /api/policies/property      Purchase property insurance
POST /api/policies/roadside      Purchase roadside assistance
```

**Example - Purchase Travel Insurance:**
```bash
curl -X POST http://localhost:8080/api/policies/travel \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "packageId": 1,
    "startDate": "2024-12-20",
    "endDate": "2024-12-27",
    "region": "EUROPE",
    "addonIds": [1, 2],
    "travelers": [
      {
        "firstName": "Ana",
        "lastName": "Anic",
        "jmbg": "1234567890123"
      }
    ]
  }'
```

### Claims

```
GET /api/claims                           Get all claims (role-based filtering)
GET /api/claims/{claimId}                 Get claim details
POST /api/claims                          Submit new claim (CLIENT only)
POST /api/claims/{claimId}/review         Review/approve/reject claim (AGENT only)
```

**Example - Submit Claim:**
```bash
curl -X POST http://localhost:8080/api/claims \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "policyId": 5,
    "incidentDate": "2024-12-25",
    "description": "Flight cancelled due to weather",
    "requestedAmount": 500.00
  }'
```

### Pricing (Admin Only)

```
POST /api/admin/pricing/travel             Configure travel rates
POST /api/admin/pricing/property           Configure property rates
POST /api/admin/pricing/roadside           Configure roadside rates
```

## Authentication & Authorization

### JWT Configuration

Tokens are configured in `application.yaml`:
- **Access Token TTL:** 15 minutes (PT15M)
- **Refresh Token TTL:** 30 days (P30D)
- **Algorithm:** HS256 (HMAC-SHA256)
- **Secret Key:** 64-byte base64-encoded (override with `JWT_SECRET` environment variable)

### Role-Based Access Control

| Role | Permissions |
|------|------------|
| **ROLE_CLIENT** | Purchase policies, submit claims, view own data |
| **ROLE_AGENT** | View all policies & claims, approve/reject claims |
| **ROLE_ADMIN** | Full access including pricing configuration |

**Securing Endpoints:**
```java
@PreAuthorize("hasAuthority('ROLE_CLIENT')")
public PolicyResponse travel(/* ... */) { }
```

## Database Schema

Flyway migrations automatically set up the database:

- **V1:** Users and Roles
- **V2:** Insurance catalog (products, packages, coverages, addons)
- **V3:** Policies and policy details
- **V4:** Default roles (ROLE_CLIENT, ROLE_AGENT, ROLE_ADMIN)
- **V5:** Refresh tokens table
- **V6:** Pricing rates
- **V7:** Split policy type details (travel, property, roadside)
- **V8:** Claims management

## Error Handling

The API uses consistent error responses via `GlobalExceptionHandler`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Policy start date cannot be in the past",
  "path": "/api/policies/travel",
  "fieldErrors": [
    {
      "field": "startDate",
      "message": "must be a future date"
    }
  ]
}
```

## Key Business Logic

### Travel Insurance Pricing

Prices are calculated as:
```
Premium = Base Package Price + Σ(Daily Rate × Travel Days) for each traveler + Add-ons
```

Daily rates vary by:
- **Region** (EUROPE, ASIA, AMERICAS, WORLD)
- **Traveler Age Group** (UNDER_4, AGE_4_TO_18, AGE_19_TO_70, OVER_70)

Traveler age is derived from JMBG (Serbian national ID) with checksum validation.

### Property Insurance Pricing

```
Premium = Base Package Price + (Price per m² × Property Area) + Add-ons
```

Minimum property area: 20 m²

### Roadside Assistance

```
Annual Price = Configured rate by vehicle type + Add-ons
```

Vehicle types: CAR, MOTORCYCLE, TRUCK, BUS

### Claims Workflow

1. **PENDING:** Client submits claim
2. **APPROVED/REJECTED:** Agent reviews and decides
3. **Validation:** 
   - Incident date must fall within policy coverage period
   - Agent cannot review their own claims
   - Only pending claims can be reviewed

## Configuration

### Environment Variables

```bash
JWT_SECRET=<base64-encoded-64-byte-key>  # Override default JWT secret
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
```

### Application Properties

Edit `src/main/resources/application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fioneer_insurance_dev
    username: fioneer_admin
    password: fioneer_password
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
```

## Testing

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test Class
```bash
./mvnw test -Dtest=AuthControllerTest
```

### Test Coverage

- **AuthControllerTest:** Login, register, refresh, logout endpoints
- **AuthServiceTest:** Token generation and validation
- **PolicyControllerTest:** Policy retrieval and authorization
- **PolicyPurchaseServiceTest:** Complex pricing calculations
- **ClaimControllerTest & ClaimServiceTest:** Claim workflow
- **GlobalExceptionHandlerTest:** Error response consistency

Tests use:
- **MockMvc** for endpoint testing
- **Mockito** for mocking dependencies
- **Testcontainers** for PostgreSQL integration

## Development

### Code Style

- Follow Spring Boot conventions
- Use constructor injection (no field injection)
- Keep services focused on business logic
- Validate inputs in DTOs with Bean Validation annotations
- Use records for immutable DTOs

### Adding New Features

1. Create domain package structure
2. Define JPA entity in `model/`
3. Create repository extending `JpaRepository`
4. Implement business logic in service layer
5. Expose via REST controller
6. Add Flyway migration if database changes
7. Write unit and integration tests

## Known Issues & Future Improvements

- [ ] Add pagination to list endpoints (getPolicies, getClaims)
- [ ] Implement caching for insurance catalog
- [ ] Add audit logging for policy purchases and claim decisions
- [ ] Add Swagger/OpenAPI documentation
- [ ] Implement rate limiting
- [ ] Add two-factor authentication
- [ ] Set up CI/CD pipeline (GitHub Actions)
- [ ] Add application metrics (Actuator/Prometheus)
- [ ] Implement transaction-level optimistic locking for concurrent updates

## Production Deployment

Before production:

1. **Security:**
   - Change default JWT secret
   - Use environment variables for sensitive data
   - Enable HTTPS
   - Configure CORS properly

2. **Database:**
   - Use managed PostgreSQL service
   - Configure backups and replication
   - Set up monitoring and alerts

3. **Monitoring:**
   - Enable Spring Actuator endpoints
   - Set up centralized logging
   - Configure error tracking (Sentry, etc.)

4. **Scaling:**
   - Use connection pooling (HikariCP)
   - Consider caching layer (Redis)
   - Implement API rate limiting

## Contributing

1. Create feature branch from `develop`
2. Follow code style guidelines
3. Add tests for new features
4. Submit pull request with description

## License

This project is provided as-is for educational and portfolio purposes.

## Contact

For questions or feedback, reach out to the project maintainer.

---

**Last Updated:** October 2026  
**Version:** 0.0.1-SNAPSHOT
