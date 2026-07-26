# SITMUN Backend Core

[![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL%20v1.2-blue.svg)](LICENSE)
![Version](https://img.shields.io/badge/version-1.2.8-blue.svg)

Spring Boot service providing REST APIs for geospatial application management, authentication, and configuration in the [SITMUN](https://sitmun.github.io/) platform.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Docker Deployment](#docker-deployment)
  - [Troubleshooting](#troubleshooting)
  - [Packaging Options](#packaging-options)
- [API Reference](#api-reference)
  - [Endpoints](#endpoints)
  - [Usage Examples](#usage-examples)
  - [Request Parameters](#request-parameters)
- [Configuration](#configuration)
  - [Environment Variables](#environment-variables)
  - [Profiles](#profiles)
  - [Configuration Files](#configuration-files)
- [Architecture](#architecture)
  - [Technology Stack](#technology-stack)
  - [System Architecture](#system-architecture)
  - [Key Components](#key-components)
  - [Request Processing Flow](#request-processing-flow)
  - [Domain Model](#domain-model)
- [Development](#development)
  - [Project Structure](#project-structure)
  - [Build System](#build-system)
  - [Code Quality](#code-quality)
  - [Testing](#testing)
  - [Development Workflow](#development-workflow)
- [Advanced Features](#advanced-features)
  - [Security and Authentication](#security-and-authentication)
  - [Monitoring and Observability](#monitoring-and-observability)
- [Integration with SITMUN](#integration-with-sitmun)
  - [Prerequisites](#prerequisites-1)
  - [Configuration Steps](#configuration-steps)
  - [Service Types and Configuration](#service-types-and-configuration)
  - [Troubleshooting Integration](#troubleshooting-integration)
- [Contributing](#contributing)
  - [Development Guidelines](#development-guidelines)
- [Support](#support)
- [License](#license)

## Overview

SITMUN Backend Core provides:

- Multiple authentication methods (Database, LDAP, OIDC/OAuth2)
- User authorization and account management
- Application and territory configuration
- Cartography services (WMS, WFS, WMTS)
- Task management and execution
- Database support (H2, PostgreSQL, Oracle)
- JWT-based security and OpenAPI documentation

Integrates with the [SITMUN Map Viewer](https://github.com/sitmun/sitmun-map-viewer) and [SITMUN Proxy Middleware](https://github.com/sitmun/sitmun-proxy-middleware).

## Quick Start

### Prerequisites

- **Java 17** or later (JDK)
- **Docker CE** or Docker Desktop (optional)
- **Git** for version control
- **Minimum 4GB RAM** recommended for development
- Basic understanding of Spring Boot and JPA

### Local Development

1. **Clone the repository**

   ```bash
   git clone https://github.com/sitmun/sitmun-backend-core.git
   cd sitmun-backend-core
   ```

2. **Build the application**

   ```bash
   ./gradlew build -x test
   ```

3. **Run the application**

   ```bash
   # Run with Java directly (recommended)
   java -jar build/libs/sitmun-backend-core.jar --spring.profiles.active=dev
   
   # Or use Gradle bootRun directly
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

4. **Verify the service is running**

   ```bash
   # Check health status
   curl http://localhost:8080/api/dashboard/health
   
   # Test authentication endpoint
   curl -X POST http://localhost:8080/api/authenticate \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin"}'
   ```

### Docker Deployment

1. **Start with Docker Compose (H2 - Development)**

   ```bash
   cd docker/development
   docker-compose up
   ```

2. **PostgreSQL Database**

   ```bash
   cd docker/postgres
   docker-compose up
   ```

3. **Oracle Database**

   ```bash
   cd docker/oracle
   docker-compose up
   ```

4. **Verify deployment**

   ```bash
   curl http://localhost:8080/api/dashboard/health
   ```

### Troubleshooting

#### Port Already in Use

```bash
# Use different port
./gradlew bootRun --args='--spring.profiles.active=dev --server.port=8081'
```

#### Memory Issues

```bash
# Increase heap size
./gradlew bootRun --args='--spring.profiles.active=dev -Xmx4g -Xms2g'
```

#### Docker Issues

```bash
# Clean up Docker resources
cd docker/development
docker-compose down -v
docker system prune -f
```

#### Database Connection Issues

```bash
# Check database connectivity
docker exec sitmun-backend ping postgres
docker exec sitmun-backend ping oracle

# Verify database configuration
curl http://localhost:8080/api/dashboard/health
```

### Packaging Options

The application supports two packaging formats:

#### JAR (Default)

Standalone executable JAR with embedded Tomcat server.

```bash
# Build JAR (default)
./gradlew build

# Or explicitly
./gradlew build -Ppackaging=jar

# Run JAR
java -jar build/libs/sitmun-backend-core.jar --spring.profiles.active=dev
```

**Use JAR when:**

- Deploying with Docker (only JAR is supported)
- Running as a standalone microservice
- Using Spring Boot's embedded server

#### WAR

Web Application Archive for deployment to external servlet containers.

```bash
# Build WAR
./gradlew build -Ppackaging=war

# Output: build/libs/sitmun-backend-core.war
```

**Use WAR when:**

- Deploying to existing Tomcat, WildFly, or WebSphere servers
- Required by organizational infrastructure policies
- Need to run multiple applications on the same servlet container

**Deployment Example (Tomcat):**

```bash
# Copy WAR to Tomcat
cp build/libs/sitmun-backend-core.war /path/to/tomcat/webapps/

# Tomcat will auto-deploy at:
# http://localhost:8080/sitmun-backend-core/

# Or rename to ROOT.war for root context:
cp build/libs/sitmun-backend-core.war /path/to/tomcat/webapps/ROOT.war
# http://localhost:8080/
```

**Configuring Active Profile for WAR:**

Unlike JAR files, WAR files cannot use command-line arguments. Configure the active profile using one of these methods:

**Method 1: Environment Variable (Recommended):**

Set the environment variable in your servlet container:

```bash
# For Tomcat, add to setenv.sh (or setenv.bat on Windows)
export SPRING_PROFILES_ACTIVE=prod

# For systemd service
[Service]
Environment="SPRING_PROFILES_ACTIVE=prod"
```

**Method 2: System Property:**

Add to your servlet container's startup script:

```bash
# For Tomcat, add to catalina.sh
export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=prod"
```

**Method 3: JNDI (Enterprise Deployments):**

For application servers like WildFly or WebSphere, configure via JNDI or server configuration.

**Method 4: application.properties in WAR:**

You can also include a `WEB-INF/classes/application.properties` file in the WAR with:

```properties
spring.profiles.active=prod
```

**Note:** The `ServletInitializer` class enables WAR deployment by configuring the application for external servlet containers.

## API Reference

### Endpoints

| Endpoint | Method | Description | Access | Controller |
| ---------- | --------- | ------------- | ---------- | ------------ |
| `/api/authenticate` | POST | Viewer cookie authentication (no JSON token) | Public | AuthenticationController |
| `/api/authenticate/mobile` | POST | Mobile edition Bearer JSON token | Public | AuthenticationController |
| `/api/authenticate/proxy` | POST | Generate short-lived proxy token | USER or MOBILE_EDITION | AuthenticationController |
| `/api/account` | GET | User account management | Authenticated | UserController |
| `/api/account/{id}` | GET | Get user by ID | Authenticated | UserController |
| `/api/account/public/{id}` | GET | Get public user info | Public | UserController |
| `/api/account/all` | GET | Get all users | Authenticated | UserController |
| `/api/config/client/application` | GET | Client application configuration | Public or authenticated | ClientConfigurationController |
| `/api/config/proxy` | POST | Proxy configuration | Authenticated | ProxyConfigurationController |
| `/api/dashboard/health` | GET | Health check | Public | Actuator |
| `/api/user-verification/verify-password` | POST | Verify user password | Public | VerificationController |
| `/api/user-verification/verify-email` | POST | Verify email availability | Public | VerificationController |
| `/api/userTokenValid` | GET | Validate user token | Public | UserTokenController |
| `/api/recover-password` | POST | Send password recovery email | Public | RecoverPasswordController |
| `/api/recover-password` | PUT | Reset password with token | Public | RecoverPasswordController |
| `/api/connections/{id}/test` | GET | Test database connection | Admin | DatabaseConnectionController |
| `/api/connections/test` | POST | Test database connection config | Admin | DatabaseConnectionController |
| `/api/helpers/capabilities` | GET | Extract service capabilities | Admin | ServiceCapabilitiesExtractorController |
| `/api/helpers/feature-type` | GET | Extract feature type info | Admin | FeatureTypeExtractorController |
| `/swagger-ui/index.html` | GET | API documentation | Public | OpenAPI |
| `/api/authenticate/logout` | POST | Logout user and clear authentication cookie | Authenticated | AuthenticationController |

### Usage Examples

#### Authentication

```bash
# Viewer login - empty 200 + httpOnly viewer_access_token cookie
curl -X POST http://localhost:8080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Mobile edition login - JSON Bearer token, no cookie (only JWT-returning password endpoint)
curl -X POST http://localhost:8080/api/authenticate/mobile \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

Browser clients use session cookies (`viewer_access_token` / `admin_access_token`). Mobile edition clients send `Authorization: Bearer <access_token>` to the three read-only client-config routes and to `/api/authenticate/proxy`.

#### Proxy Authentication

Generate a short-lived proxy token for the SITMUN Proxy Middleware:

```bash
# Viewer: cookie session
curl -X POST http://localhost:8080/api/authenticate/proxy

# Mobile: Bearer edition access token
curl -X POST http://localhost:8080/api/authenticate/proxy \
  -H "Authorization: Bearer <mobile-access-token>"
```

**Note:** Proxy token lifetime uses `sitmun.proxy-middleware.token-validity-in-milliseconds`. Mobile access token lifetime uses `sitmun.mobile.token-validity-in-milliseconds`. Delegated credentials on `/api/config/proxy` are read from `Authorization: Bearer`, not from the request body.

#### Health Check

```bash
curl http://localhost:8080/api/dashboard/health

# Response
{
  "status": "UP"
}
```

#### User Verification

```bash
# Verify password
curl -X POST http://localhost:8080/api/user-verification/verify-password \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Verify email
curl -X POST http://localhost:8080/api/user-verification/verify-email \
  -H "Content-Type: application/json" \
  -d '"admin@example.com"'
```

#### Password Recovery

```bash
# Request password recovery
curl -X POST http://localhost:8080/api/recover-password \
  -H "Content-Type: application/json" \
  -d '{"login":"admin@example.com"}'

# Reset password
curl -X PUT http://localhost:8080/api/recover-password \
  -H "Content-Type: application/json" \
  -d '{"token":"recovery-token","newPassword":"newpassword"}'
```

#### Database Connection Testing

```bash
# Test existing connection
curl -X GET http://localhost:8080/api/connections/1/test

# Test new connection configuration
curl -X POST http://localhost:8080/api/connections/test \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-connection",
    "driver": "org.postgresql.Driver",
    "url": "jdbc:postgresql://localhost:5432/testdb",
    "username": "user",
    "password": "password"
  }'
```

#### Service Capabilities Extraction

```bash
# Extract WMS capabilities
curl -X GET "http://localhost:8080/api/helpers/capabilities?url=http://example.com/wms"

# Extract feature type information
curl -X GET "http://localhost:8080/api/helpers/feature-type?url=http://example.com/wfs"
```

#### Logout

```bash
# Logout - clears the authentication cookie and invalidates the session
curl -X POST http://localhost:8080/api/authenticate/logout

```

#### Authentication and authorization status

| Condition | Status | Problem type |
| --- | --- | --- |
| Missing, invalid, expired, revoked, or blocked authentication | `401` | `unauthorized` |
| Identified principal lacks access to a resource | `403` | `forbidden` |
| Malformed request | `400` | `bad-request` |
| Authentication identity store unavailable | `503` | `service-unavailable` |
| Unexpected authentication processing failure | `500` | `internal-server-error` |

Security responses use `application/problem+json` with generic details. Invalid credentials clear the cookie. Authentication infrastructure and processing failures stop the request without clearing the cookie or falling through to the public principal. Client-configuration endpoints accept the `access_token` cookie when present and otherwise use the public principal.

### Request Parameters

#### Authentication Request

```json
{
  "username": "string",
  "password": "string"
}
```

#### Proxy Configuration Request

```json
{
  "appId": "integer",
  "terId": "integer", 
  "type": "string",
  "typeId": "integer",
  "token": "string"
}
```

#### Password Recovery Request

```json
{
  "login": "string"
}
```

#### Password Reset Request

```json
{
  "token": "string",
  "newPassword": "string"
}
```

#### Database Connection Test Request

```json
{
  "name": "string",
  "driver": "string",
  "url": "string",
  "username": "string",
  "password": "string"
}
```

## Configuration

### Environment Variables

| Variable                                 | Description | Default                                  | Required |
| ---------------------------------------- | ------------- |------------------------------------------| ---------- |
| `SPRING_PROFILES_ACTIVE`                 | Active Spring profile | `dev`                                    | No |
| `SPRING_DATASOURCE_URL`                  | Database connection URL | H2 in-memory                             | Yes (prod) |
| `SPRING_DATASOURCE_USERNAME`             | Database username | `sa`                                     | Yes (prod) |
| `SPRING_DATASOURCE_PASSWORD`             | Database password | ``                                       | Yes (prod) |
| `SITMUN_USER_SECRET`                     | JWT signing secret (min 32 chars; startup-validated) | -                            | Yes |
| `SITMUN_BOOTSTRAP_ADMIN_PASSWORD`        | Optional plaintext used only to create a missing built-in `admin` or restore an empty admin password at startup. No default. Remove after health is UP and rotate via the admin UI. | - | Only when admin is missing/passwordless |
| `SITMUN_USER_TOKEN_VALIDITY_IN_MILLISECONDS` | JWT token validity in milliseconds | `36000000`                               | No |
| `SITMUN_AUTHENTICATION_HTTP_ONLY_COOKIE` | HttpOnly flag for JWT cookie | `true`                                   | No |
| `SITMUN_AUTHENTICATION_SAME_SITE_COOKIE` | SameSite attribute for JWT cookie | `Strict`                                 | No |
| `SITMUN_PROXY_MIDDLEWARE_SECRET`         | Proxy middleware shared secret (min 32 chars; startup-validated) | -                | Yes |
| `SITMUN_PROXY_MIDDLEWARE_TOKEN_VALIDITY_IN_MILLISECONDS` | Proxy token validity in milliseconds | `900000` (15 min)                        | No |
| `SITMUN_AUTH_OIDC_ENABLED`               | Enable OIDC authentication | `false`                                  | If OIDC enabled |
| `SITMUN_AUTHENTICATION_OIDC_FRONTENDREDIRECTURL` | Default frontend callback URL for OIDC | `http://localhost:9000/viewer/callback`  | If OIDC enabled |
| `SITMUN_AUTHENTICATION_OIDC_FRONTENDREDIRECTURLVIEWER` | Viewer frontend callback URL for OIDC | `http://localhost:9000/viewer/callback`  | If OIDC enabled |
| `SITMUN_AUTHENTICATION_OIDC_FRONTENDREDIRECTURLADMIN` | Admin frontend callback URL for OIDC | `http://localhost:9000/admin/#/callback` | If OIDC enabled |
| `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_*` | Dynamic OIDC provider configuration | -                                        | If OIDC enabled |

**Note:** OIDC providers are configured dynamically under `sitmun.authentication.oidc.providers.{providerId}`. See [OIDC Configuration](#oidcoauth2-configuration) for details.

### Profiles

- **dev** (default): H2 in-memory database, development logging
- **test**: H2 with test data, test-specific configuration
- **postgres**: PostgreSQL database configuration
- **oracle**: Oracle database configuration
- **ldap**: LDAP authentication enabled
- **oidc**: OpenID Connect authentication enabled (requires OAuth2 client configuration)
- **mail**: Email functionality enabled
- **prod**: Production configuration with security optimizations

### Configuration Files

#### Application Configuration (`application.yml`)

```yaml
spring:
  application:
    name: SITMUN
  liquibase:
    change-log: file:./config/db/changelog/db.changelog-master.yaml
  jpa:
    hibernate:
      ddl-auto: none

sitmun:
  module: SITMUN Core
  version: 3.0-SNAPSHOT
  user:
    secret: ${SITMUN_USER_SECRET}
    token-validity-in-milliseconds: 36000000
  authentication:
    http-only-cookie: true
    same-site-cookie: Strict
  proxy-middleware:
    secret: ${SITMUN_PROXY_MIDDLEWARE_SECRET}
    token-validity-in-milliseconds: 900000
    config-response-validity-in-seconds: 3600
```

#### Database Configuration Examples

**PostgreSQL:**

```yaml
spring:
  profiles:
    active: postgres
  datasource:
    url: jdbc:postgresql://postgres:5432/sitmun
    username: sitmun
    password: sitmun123
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

**Oracle:**

```yaml
spring:
  profiles:
    active: oracle
  datasource:
    url: jdbc:oracle:thin:@oracle:1521:XE
    username: sitmun
    password: sitmun123
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.OracleDialect
```

## Architecture

### Technology Stack

- **Java**: 17 (LTS)
- **Spring Boot**: 3.5.4
- **Spring Security**: JWT authentication and authorization
- **Spring Data JPA**: Data persistence layer
- **Spring Data REST**: REST API generation
- **QueryDSL**: Type-safe query building
- **MapStruct**: Object mapping
- **Liquibase**: Database migration
- **H2/PostgreSQL/Oracle**: Database support
- **OpenAPI/Swagger**: API documentation
- **JUnit 5**: Testing framework
- **Gradle**: Build system
- **Docker**: Containerization

### System Architecture

The application follows a layered architecture pattern:

```
Controllers → Services → Repositories → Entities
     ↓           ↓           ↓           ↓
   REST API   Business   Data Access   Domain
              Logic      Layer         Model
```

### Key Components

- **Domain Layer**: JPA entities with custom type converters and validators
- **Repository Layer**: Spring Data JPA with custom queries and REST exposure
- **Service Layer**: Business logic with constructor injection and functional programming
- **Controller Layer**: REST endpoints with security context and pagination
- **Infrastructure**: Security, persistence, and web configurations

### Request Processing Flow

1. **Request Reception**: REST controller receives HTTP request
2. **Authentication**: JWT token validation and user authentication
3. **Authorization**: Role-based access control verification
4. **Request Validation**: Input validation and sanitization
5. **Business Logic**: Service layer processing
6. **Data Access**: Repository layer database operations
7. **Response Generation**: DTO mapping and response formatting
8. **Security Headers**: CORS and security headers added

### Domain Model

The application manages several key domain entities:

- **User**: Authentication and user management
- **Application**: Geospatial application configuration
- **Territory**: Hierarchical territorial organization
- **Cartography**: WMS, WFS, WMTS service configuration
- **Task**: Geospatial task execution
- **Role**: User roles and permissions
- **Background**: Application background configuration
- **Service**: External service integration

## Development

### Project Structure

```
src/
├── main/
│   ├── java/org/sitmun/
│   │   ├── Application.java              # Main application class
│   │   ├── authentication/              # Authentication controllers
│   │   ├── authorization/               # Security and authorization
│   │   ├── administration/              # Admin functionality
│   │   │   ├── controller/             # Admin controllers
│   │   │   └── service/                # Admin services
│   │   ├── domain/                      # Domain entities and repositories
│   │   │   ├── application/            # Application management
│   │   │   ├── cartography/            # Cartography services
│   │   │   ├── territory/              # Territory management
│   │   │   ├── task/                   # Task management
│   │   │   ├── user/                   # User management
│   │   │   └── ...
│   │   ├── infrastructure/             # Technical infrastructure
│   │   │   ├── persistence/           # Database configuration
│   │   │   ├── security/              # Security configuration
│   │   │   └── web/                   # Web configuration
│   │   ├── recover/                   # Password recovery
│   │   └── verification/              # User verification
│   └── resources/
│       ├── application.yml             # Main configuration
│       ├── static/v3/                 # OpenAPI specifications
│       └── templates/                 # Email templates
├── test/                              # Test code
└── docker/                            # Docker configurations
    ├── development/                   # H2 development setup
    ├── postgres/                      # PostgreSQL setup
    └── oracle/                        # Oracle setup
```

### Build System

The project uses Gradle with version catalogs for dependency management:

```toml
# gradle/libs.versions.toml
[versions]
spring-boot = "3.5.4"
project-version = "0.9.0-SNAPSHOT"

[libraries]
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
spring-boot-starter-security = { module = "org.springframework.boot:spring-boot-starter-security" }
# ... additional dependencies
```

### Code Quality

The project enforces high code quality standards:

- **Spotless**: Automated code formatting
- **SonarQube**: Code quality analysis
- **JaCoCo**: Code coverage reporting
- **Conventional Commits**: Structured commit messages
- **Git Hooks**: Pre-commit validation

### Testing

Comprehensive testing strategy:

```bash
# Run all tests
./gradlew test

# Test with PostgreSQL
./gradlew testPostgres

# Test with Oracle
./gradlew testOracle

# Run specific test class
./gradlew test --tests AuthenticationControllerTest
```

**Oracle Test Notes:**

- Oracle tests use Docker Compose and wait for database healthchecks (60-160 seconds startup time is normal)
- Tests automatically start when the database is ready (no fixed delays)
- If Oracle startup fails and containers are retained for troubleshooting, clean them with:

  ```bash
  ./gradlew oracleComposeDownForced
  ```

### Development Workflow

1. **Setup**: Clone repository and run with H2 database
2. **Development**: Use dev profile for local development
3. **Testing**: Write tests for new functionality
4. **Quality**: Run quality checks before committing
5. **Commit**: Use conventional commit format
6. **Deploy**: Use appropriate profile for deployment

## Advanced Features

### Security and Authentication

The application provides comprehensive security features:

- **JWT Authentication**: Secure token-based authentication via HTTP-only cookies
- **Cookie-Based Token Storage**: JWT tokens are stored in HTTP-only cookies for improved security
- **Role-Based Access Control**: Fine-grained permission system
- **Application Privacy**: Applications can be marked as private
- **Public User Support**: Anonymous access with restrictions
- **LDAP Integration**: Enterprise authentication support
- **OIDC/OAuth2 Integration**: External identity provider support (Apereo CAS, Azure AD, etc.)
- **Password Security**: Secure password storage and validation

#### JWT Token Configuration

```yaml
sitmun:
  user:
    secret: ${SITMUN_USER_SECRET}
    token-validity-in-milliseconds: 36000000  # JWT token lifetime in milliseconds (10 hours)
  authentication:
    http-only-cookie: true  # Whether to set HttpOnly flag on JWT cookie
    same-site-cookie: Strict  # SameSite attribute for CSRF protection
```

JWT tokens are stored in an HTTP-only cookie (`access_token`) that is automatically set during authentication. The cookie's `max-age` is automatically derived from `token-validity-in-milliseconds` to keep both values synchronized. The `http-only-cookie` setting controls whether the cookie can be accessed by JavaScript.

#### Proxy Token Configuration

```yaml
sitmun:
  proxy-middleware:
    secret: ${SITMUN_PROXY_MIDDLEWARE_SECRET}
    token-validity-in-milliseconds: 900000  # Proxy token lifetime (15 minutes default)
```

The proxy token is a short-lived JWT returned by `/api/authenticate/proxy` for communication with Proxy Middleware. The viewer stores it in IndexedDB for its service worker, which adds it as a Bearer token only to configured middleware requests. It has a much shorter lifetime than the regular access token.

The main `access_token` cookie defaults to `HttpOnly` and `SameSite=Strict`; its `Secure` attribute follows the request scheme. CSRF is currently disabled, so deployments must preserve the external HTTPS scheme and should not treat `SameSite` as a replacement for CSRF protection.

#### LDAP Configuration

```yaml
spring:
  profiles:
    active: ldap

sitmun:
  authentication:
    ldap:
      url: ldap://ldap.example.com:389
      base-dn: dc=example,dc=com
      user-dn-pattern: uid={0}
      username: cn=admin,dc=example,dc=com
      password: admin_password
```

#### OIDC/OAuth2 Configuration

Enable OIDC authentication with external identity providers:

```yaml
spring:
  profiles:
    active: oidc

sitmun:
  authentication:
    oidc:
      # Redirect URLs for frontend callbacks, will depend on `client_type` parameter
      frontend-redirect-url: "https://frontend.example.com/callback"
      frontend-redirect-url-admin: "https://frontend.example.com/admin/#/callback"
      frontend-redirect-url-viewer: "https://frontend.example.com/viewer/callback"
      # HttpOnly flag for the oidc_token JWT cookie.
      # false (default): cookie readable by frontend JavaScript (required by current clients).
      # true: cookie hidden from JavaScript (more secure, requires planned frontend changes).
      http-only-cookie: false
      providers:
        cas:
          provider-name: "cas"
          display-name: "Corporate Identity Provider"
          image-path: "https://cdn.example.com/images/idp-logo.png"
          # IdP endpoints - your identity provider URLs
          issuer-uri: "https://idp.example.com/oidc"
          authorization-uri: "https://idp.example.com/oidc/authorize"
          token-uri: "https://idp.example.com/oidc/token"
          user-info-uri: "https://idp.example.com/oidc/userinfo"
          jwk-set-uri: "https://idp.example.com/oidc/jwks"
          # OAuth2 client credentials (registered at IdP)
          client-id: "your-client-id"
          client-secret: "your-client-secret"
          # Backend callback URL - where IdP redirects after authentication
          redirect-uri: "https://backend.example.com/login/oauth2/code/cas"
          scope: "openid,profile,email"
```

**Environment Variables:**

| Property | Environment Variable | Description |
|----------|---------------------|-------------|
| `sitmun.authentication.oidc.enabled` | `SITMUN_AUTH_OIDC_ENABLED` | Enables OIDC authentication |
| `sitmun.authentication.oidc.frontend-redirect-url` | `SITMUN_AUTHENTICATION_OIDC_FRONTENDREDIRECTURL` | Default frontend callback URL |
| `sitmun.authentication.oidc.frontend-redirect-url-admin` | `SITMUN_AUTHENTICATION_OIDC_FRONTENDREDIRECTURLADMIN` | Admin frontend callback URL |
| `sitmun.authentication.oidc.frontend-redirect-url-viewer` | `SITMUN_AUTHENTICATION_OIDC_FRONTENDREDIRECTURLVIEWER` | Viewer frontend callback URL |
| `sitmun.authentication.oidc.http-only-cookie` | `SITMUN_AUTHENTICATION_OIDC_HTTPONLYCOOKIE` | HttpOnly flag for the `oidc_token` cookie. Default `false`. |
| `sitmun.authentication.oidc.providers.{id}.provider-name` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_PROVIDERNAME` | Provider identifier |
| `sitmun.authentication.oidc.providers.{id}.display-name` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_DISPLAYNAME` | Display name shown to users |
| `sitmun.authentication.oidc.providers.{id}.image-path` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_IMAGEPATH` | Provider logo URL |
| `sitmun.authentication.oidc.providers.{id}.issuer-uri` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_ISSUERURI` | IdP issuer URI |
| `sitmun.authentication.oidc.providers.{id}.authorization-uri` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_AUTHORIZATIONURI` | IdP authorization endpoint |
| `sitmun.authentication.oidc.providers.{id}.token-uri` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_TOKENURI` | IdP token endpoint |
| `sitmun.authentication.oidc.providers.{id}.user-info-uri` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_USERINFOURI` | IdP user info endpoint |
| `sitmun.authentication.oidc.providers.{id}.jwk-set-uri` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_JWKSETURI` | IdP JWK set endpoint |
| `sitmun.authentication.oidc.providers.{id}.client-id` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_CLIENTID` | OAuth2 client ID |
| `sitmun.authentication.oidc.providers.{id}.client-secret` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_CLIENTSECRET` | OAuth2 client secret |
| `sitmun.authentication.oidc.providers.{id}.redirect-uri` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_REDIRECTURI` | Backend callback URL |
| `sitmun.authentication.oidc.providers.{id}.scope` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_SCOPE` | OAuth2 scopes. Default `openid,profile,email`. |
| `sitmun.authentication.oidc.providers.{id}.user-name-attribute-name` | `SITMUN_AUTHENTICATION_OIDC_PROVIDERS_{ID}_USERNAMEATTRIBUTENAME` | UserInfo claim used as principal name. Default `sub` (OIDC subject). |

**OIDC Features:**

- Multiple provider support (CAS, Azure AD, Google, GitHub, etc.)
- Dynamic provider discovery via `/api/auth/enabled-methods`
- User must exist in local database (no auto-provisioning)
- Configurable cookie security settings
- Automatic redirect to frontend after authentication

**OIDC Flow:**

1. Frontend calls `/api/auth/enabled-methods` to discover available providers
2. User clicks on OIDC provider (e.g., "Login with CAS")
3. Frontend redirects to `/oauth2/authorization/{providerId}`
4. User authenticates at external provider
5. Provider redirects back to `/login/oauth2/code/{providerId}`
6. Backend validates user, generates JWT, sets cookie and redirects to frontend
7. Frontend extracts token from `oidc_token` cookie

**Current limitation and future improvement (`http-only-cookie`):**

The current viewer frontend reads the `oidc_token` cookie with JavaScript. When `HttpOnly` is `true` the browser hides the cookie from JavaScript, so `CookieService.get()` returns an empty string and the OIDC callback silently fails. For this reason, the default is `false`.

### Monitoring and Observability

- **Spring Boot Actuator**: Health checks and metrics
- **Custom Health Indicators**: Application-specific health monitoring
- **Request Logging**: Comprehensive request/response logging
- **Error Tracking**: Detailed error logging and monitoring
- **Performance Metrics**: Request timing and performance monitoring

#### Built-in user startup repair

On every start, `BuiltInUserStartupRepairer` soft-repairs the built-in `admin` and `public` accounts:

- Restores required flags (`admin` must be administrator/unblocked; `public` must not be administrator/blocked).
- Clears stale `public` personal data/password and deletes positions for both built-ins.
- Creates a missing `public` automatically.
- Creates a missing `admin`, or restores an empty admin password, only when `SITMUN_BOOTSTRAP_ADMIN_PASSWORD` is set. The value is BCrypt-encoded; plaintext is never logged or stored.
- Never aborts the JVM. `/api/dashboard/health` stays `DOWN` (HTTP 503) until repair succeeds.
- `/api/dashboard/startup` is a public, read-only diagnostic that returns only `{ "state": "ready|blocked|initializing" }` and, when blocked, a stable `"reason"` such as `admin-missing-bootstrap-password`. It never exposes secrets, hashes, or exception text. Do not enable global Actuator health details for this purpose — that would leak details from every health contributor.
- Under the `dev` profile, property dump logging redacts keys containing `password`, `secret`, `token`, or `credential` (including `SITMUN_BOOTSTRAP_ADMIN_PASSWORD`).

Operator sequence when admin is missing or passwordless:

1. Inject `SITMUN_BOOTSTRAP_ADMIN_PASSWORD` through the deployment secret mechanism (no committed default).
2. Start the backend and wait for `/api/dashboard/health` to become `UP`. Use `GET /api/dashboard/startup` if you need the stable reason.
3. Remove the bootstrap secret from deployment configuration.
4. Rotate the password through the normal admin UI.

#### Actuator Endpoints

| Endpoint | Description | Access |
|----------|-------------|--------|
| `/api/dashboard/health` | Application health / readiness status | Public |
| `/api/dashboard/startup` | Built-in user startup state and stable reason | Public |
| `/api/dashboard/info` | Application information | Public |
| `/api/dashboard/metrics` | Application metrics | Authenticated |

## Integration with SITMUN

This service is designed to provide core backend functionality for the [SITMUN](https://github.com/sitmun/) platform. It integrates with other SITMUN components to provide a complete geospatial solution.

### Prerequisites

Before integrating the Backend Core with SITMUN, ensure you have:

- **Database**: PostgreSQL or Oracle configured
- **Network connectivity**: Between all SITMUN components
- **Shared secret keys**: For secure communication
- **LDAP server**: If using enterprise authentication

### Configuration Steps

#### 1. Security Configuration

Configure JWT and proxy middleware secrets. Both are required with no fallback default, so the values come entirely from the environment:

```yaml
sitmun:
  user:
    secret: ${SITMUN_USER_SECRET}
    token-validity-in-milliseconds: 36000000
  proxy-middleware:
    secret: ${SITMUN_PROXY_MIDDLEWARE_SECRET}
    config-response-validity-in-seconds: 3600
```

Startup is fail-fast: `SecuritySecretValidator` rejects a blank or shorter-than-32-character `sitmun.user.secret` or `sitmun.proxy-middleware.secret` with an `IllegalStateException`, and a missing environment variable fails placeholder resolution before the context starts. `SITMUN_PROXY_MIDDLEWARE_SECRET` must match the proxy's `SITMUN_BACKEND_CONFIG_SECRET`.

#### 2. SITMUN Map Viewer Integration

Configure the SITMUN Map Viewer to use the Backend Core:

```javascript
// Map Viewer configuration
const mapViewerConfig = {
  backend: {
    baseUrl: 'http://localhost:8080/api',
    authentication: {
      endpoint: '/authenticate',
      cookieStorage: 'access_token'
    }
  },
  applications: {
    endpoint: '/config/client/application'
  }
};
```

#### 3. SITMUN Proxy Middleware Integration

Configure the Proxy Middleware to connect to the Backend Core:

```yaml
# Proxy Middleware configuration
sitmun:
  backend:
    config:
      url: http://sitmun-backend:8080/api/config/proxy
      secret: your-shared-secret
```

#### 4. Network Configuration

Ensure proper network connectivity between components:

```yaml
# Docker Compose network configuration
services:
  sitmun-backend:
    # ... backend configuration
    networks:
      - sitmun-network
  sitmun-proxy-middleware:
    # ... proxy configuration
    networks:
      - sitmun-network
    environment:
      - SITMUN_BACKEND_CONFIG_URL=http://sitmun-backend:8080/api/config/proxy
      - SITMUN_BACKEND_CONFIG_SECRET=your-shared-secret

networks:
  sitmun-network:
    driver: bridge
```

### Service Types and Configuration

The Backend Core supports different service types that can be configured:

#### WMS Services

```json
{
  "type": "wms",
  "url": "http://wms-service/wms",
  "layers": ["layer1", "layer2"],
  "authentication": {
    "type": "basic",
    "username": "wms_user",
    "password": "wms_password"
  }
}
```

#### WFS Services

```json
{
  "type": "wfs",
  "url": "http://wfs-service/wfs",
  "featureTypes": ["feature1", "feature2"],
  "authentication": {
    "type": "bearer",
    "token": "your-jwt-token"
  }
}
```

#### JDBC Services

```json
{
  "type": "jdbc",
  "url": "jdbc:postgresql://database:5432/spatial_data",
  "username": "db_user",
  "password": "db_password",
  "query": "SELECT * FROM spatial_data WHERE territory_id = ?"
}
```

### Troubleshooting Integration

#### Common Issues

1. **Database Connection Failures**

   ```bash
   # Check database connectivity
   docker exec sitmun-backend ping postgres
   
   # Verify database configuration
   curl http://localhost:8080/api/dashboard/health
   ```

2. **Authentication Failures**

   ```bash
   # Check the authenticated account using a saved login cookie
   curl -b cookies.txt http://localhost:8080/api/account
   
   # Verify user credentials
   curl -X POST http://localhost:8080/api/authenticate \
     -c cookies.txt \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin"}'
   ```

3. **LDAP Configuration Issues**

   ```bash
   # Test LDAP connection
   curl -X POST http://localhost:8080/api/authenticate \
     -H "Content-Type: application/json" \
     -d '{"username":"ldap_user","password":"ldap_password"}'
   ```

#### Debug Mode

```bash
# Enable debug logging for integration issues
export LOGGING_LEVEL_ORG_SITMUN=DEBUG
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG

# Restart the backend
docker-compose restart sitmun-backend
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes following the conventional commit format
4. Add tests for new functionality
5. Ensure all tests pass and code is formatted
6. Submit a pull request

### Development Guidelines

- Follow the conventional commit format
- Write tests for new functionality
- Ensure code coverage remains high
- Run quality checks before committing
- Update documentation as needed

#### Conventional Commit Format

```
type(scope): description

[optional body]

[optional footer]
```

**Types:**

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test changes
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD changes
- `build`: Build system changes

**Examples:**

```bash
git commit -m "feat(auth): add LDAP authentication support"
git commit -m "fix(api): resolve JWT token validation issue"
git commit -m "docs: update README with deployment instructions"
git commit -m "test: add integration tests for user verification"
git commit -m "style: format code with Google Java Format"
```

#### Managing Git Hooks

```bash
# Install Git hooks (automatic with build)
./gradlew setupGitHooks

# Remove Git hooks
./gradlew removeGitHooks
```

## Support

For questions and support:

- Open an issue on GitHub
- Check the [SITMUN documentation](https://sitmun.github.io/)
- Join the SITMUN community discussions

## License

This project uses the following license: [European Union Public Licence V. 1.2](LICENSE).
