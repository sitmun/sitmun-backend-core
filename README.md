[![CI](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml/badge.svg)](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sitmun%3Asitmun-backend-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sitmun%3Asitmun-backend-core)

# SITMUN Backend Core

The SITMUN backend core is a key component of the SITMUN software system, providing a comprehensive REST API for geospatial application management and user authorization. Built with modern Java technologies, it serves as the foundation for the SITMUN platform.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.12
- **Build Tool**: Gradle
- **Database**: H2 (development), PostgreSQL, Oracle
- **Security**: Spring Security with JWT
- **Documentation**: OpenAPI/Swagger
- **Testing**: JUnit 5, Spring Boot Test
- **Object Mapping**: MapStruct 1.6.3
- **Query Building**: QueryDSL
- **Database Migration**: Liquibase
- **Utilities**: Lombok, Apache Commons, Google Guava

## Architecture Overview

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

## Prerequisites

Before you begin, ensure you have met the following requirements:

- **Java 17** or later (JDK)
- **Git** for version control
- **Gradle** (wrapper included)
- **Minimum 4GB RAM** recommended for development
- Basic understanding of Spring Boot and JPA

## Quick Start

### Option 1: Local Development (H2 Database)

```bash
git clone https://github.com/sitmun/sitmun-backend-core.git
cd sitmun-backend-core
./gradlew bootRun
```

### Option 2: Docker Development (H2, PostgreSQL, Oracle - Recommended)

- **H2 (default, development):**

    ```bash
    docker-compose -f docker/development/docker-compose.yml up
    ```

- **PostgreSQL:**

    ```bash
    docker-compose -f docker/postgres/docker-compose.yml up
    ```

- **Oracle:**

    ```bash
    docker-compose -f docker/oracle/docker-compose.yml up
    ```


To stop:

```bash
docker-compose -f <compose-file> down
```

### Health Check

```bash
curl http://localhost:8080/api/dashboard/health
```

Expected response:

```json
{"status":"UP"}
```

## Configuration

### Profiles

- **dev** (default): H2 in-memory
- **postgres**: PostgreSQL
- **oracle**: Oracle
- **test**: H2 with test data
- **prod**: Production

### Environment Variables (PostgreSQL example)

```
SPRING_PROFILES_ACTIVE=postgres
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sitmun
SPRING_DATASOURCE_USERNAME=sitmun
SPRING_DATASOURCE_PASSWORD=sitmun123
```

### Database Migrations

The application uses Liquibase for database schema management. 
Migrations are applied automatically on application startup.

## API Documentation

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Key Endpoints

- `POST /api/auth/login` — Authenticate
- `POST /api/auth/logout` — Logout
- `GET /api/config/client/application` — User applications
- `GET /api/dashboard/health` — Health check

## Security

- JWT authentication
- Role-based access
- **Application privacy controls** - Applications can be marked as private to restrict public user access
- **Public user support** - Anonymous/public user access with privacy restrictions
- CORS enabled
- Bean validation

### Application Privacy

Applications can be configured as private to restrict access from public users:

- **Private applications**: Only authenticated users can access
- **Public applications**: Available to both authenticated and public users
- **Configuration warnings**: System provides warnings for private applications with public user roles

## Project Structure

```
src/
  main/
    java/org/sitmun/
      domain/           # Entities, repositories
      authorization/    # Security/auth logic
      authentication/   # Auth controllers
      administration/   # Admin
      infrastructure/   # Technical config
      Application.java  # Main class
    resources/
      application.yml   # Config
      static/v3/        # OpenAPI specs
  test/
    java/org/sitmun/   # Tests
  docker/
    development/       # H2 compose
    postgres/          # PostgreSQL compose
    oracle/            # Oracle compose
    Dockerfile         # App container
```

## Building & Testing

```bash
./gradlew build      # Build
./gradlew test       # Run tests (H2 default)
```

To test with PostgreSQL:

```bash
docker-compose -f docker/postgres/test-docker-compose.yml up
./gradlew testPostgres 
```

To test with Oracle:

```bash
docker-compose -f docker/oracle/test-docker-compose.yml up
./gradlew testOracle
```

## Deployment

- Use Docker Compose for local/production deployment.
- Set environment variables for production (see above).
- For Heroku: configure PostgreSQL and use `./gradlew deployHeroku`.

## Contributing

Contributions welcome. Please open issues or pull requests.

## License

See LICENSE file for details.

## Further Documentation

- API: OpenAPI/Swagger UI (`/swagger-ui/index.html`)
- Code: See `src/main/java/org/sitmun/`
- For questions, open an issue on GitHub.
