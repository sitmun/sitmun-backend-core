[![CI](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml/badge.svg)](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sitmun%3Asitmun-backend-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sitmun%3Asitmun-backend-core)

# SITMUN Backend Core

The SITMUN backend core is a key component of the SITMUN software system.
It is built using **Java 11** and **Spring Boot 2.7.18**.
It provides a REST API that allows users to access the backend business logic and interact with the SITMUN database.
The backend core is designed to provide the foundation for the SITMUN system.

The SITMUN backend core is structured as a **Gradle project**,
which provides a clean and maintainable codebase structure.
This structure allows for easy development, testing, and deployment
of the software system.

## Technology Stack

- **Java**: 11
- **Spring Boot**: 2.7.18
- **Build Tool**: Gradle
- **Database**: H2 (development), PostgreSQL, Oracle
- **Security**: Spring Security with JWT
- **Documentation**: OpenAPI/Swagger
- **Testing**: JUnit 5, Spring Boot Test

## Prerequisites

Before you begin, ensure you have met the following requirements:

- You have a `Windows/Linux/Mac` machine.
- You have installed [Java 11](https://adoptopenjdk.net/) or later.
- You have installed [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) on your machine.
- You have a basic understanding of Java, Spring Boot, and Git.
- You have internet access on your machine to download dependencies.
- **Minimum 4GB RAM** recommended for development.

## Installing SITMUN Backend Core

To install the SITMUN Backend Core, follow these steps:

1. Clone the repository:
    ```bash
    git clone https://github.com/sitmun/sitmun-backend-core.git
    ```

2. Change to the directory of the repository:
    ```bash
    cd sitmun-backend-core
    ```

3. Build the project:
    ```bash
    ./gradlew build
    ```

4. Run the SITMUN Backend Core:
    ```bash
    ./gradlew bootRun
    ```
   This command will start the application using the default configuration.

5. Access the SITMUN Backend Core at [http://localhost:8080/api/dashboard/health](http://localhost:8080/api/dashboard/health) and expect:
   ```json
   {"status":"UP"}
   ```

See [SITMUN Application Stack](https://github.com/sitmun/sitmun-application-stack) as an example of how to deploy and run the backend core as part of the SITMUN stack.

## Configuration

### Environment Variables

The application supports different profiles and configurations:

#### Development (Default)
- Uses H2 in-memory database
- No additional configuration required

#### Heroku Profile
- `JDBC_DATABASE_URL=jdbc:postgresql://{host}/{database}`
- `JDBC_DATABASE_USERNAME={username}`
- `JDBC_DATABASE_PASSWORD={password}`
- `SPRING_PROFILES_ACTIVE=heroku,openapi-provided`
- `spring.datasource.hikari.auto-commit=false`

#### Production
- Configure database connection
- Set `SPRING_PROFILES_ACTIVE=prod`
- Configure JWT secrets and other security settings

### Database Setup

The application supports multiple databases:
- **H2**: In-memory database for development (default)
- **PostgreSQL**: Production database
- **Oracle**: Enterprise database

Database migrations are handled by Liquibase.

## Deployment Options

The **SITMUN backend core** can be deployed using different configurations:

### Local Development
Run the application locally using:
```bash
./gradlew bootRun
```

### Heroku Deployment
The project includes Heroku deployment configuration. To deploy to Heroku:

1. Create a Heroku app and configure the database
2. Set up the required environment variables:
   - `JDBC_DATABASE_URL=jdbc:postgresql://{host}/{database}`
   - `JDBC_DATABASE_USERNAME={username}`
   - `JDBC_DATABASE_PASSWORD={password}`
   - `SPRING_PROFILES_ACTIVE=heroku,openapi-provided`
   - `spring.datasource.hikari.auto-commit=false`

3. Deploy using:
   ```bash
   ./gradlew deployHeroku
   ```

The application can be accessed at https://sitmun-backend-core.herokuapp.com/ and the API documentation at https://sitmun-backend-core.herokuapp.com/swagger-ui/index.html.

### Local Heroku Testing
To test the Heroku configuration locally:
```bash
./gradlew stage
heroku local
```

This requires an `.env` file with the database configuration properties mentioned above.

## API Documentation

- **OpenAPI/Swagger UI**: Available at `/swagger-ui/index.html` when running (enabled by default)
- **OpenAPI JSON**: Available at `/v3/api-docs` when running
- **Health Check**: `/api/dashboard/health`
- **Authentication**: JWT-based authentication
- **Security**: Spring Security with role-based access control

### Swagger UI Availability

The Swagger UI is available by default when the application is running. The OpenAPI documentation is automatically generated from the Spring Boot controllers and is accessible without any additional configuration.

For enhanced OpenAPI documentation with custom configuration, you can activate the `openapi` or `openapi-provided` profiles:

```bash
./gradlew bootRun --args='--spring.profiles.active=openapi'
```

## Stopping SITMUN Backend Core

To stop the application, use `Ctrl+C` in the terminal where it's running.

## Contributing to SITMUN Backend Core

To contribute to SITMUN Backend Core, follow these steps:

1. **Fork this repository** on GitHub.
2. **Clone your forked repository** to your local machine.
3. **Create a new branch** for your changes.
4. **Make your changes** and commit them.
5. **Push your changes** to your forked repository.
6. **Create the pull request** from your branch on GitHub.

Alternatively, see the GitHub documentation on [creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).

## License

This project uses the following license: [European Union Public Licence V. 1.2](LICENSE).
