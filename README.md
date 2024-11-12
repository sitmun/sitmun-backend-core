[![CI](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml/badge.svg)](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sitmun%3Asitmun-backend-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sitmun%3Asitmun-backend-core)

# SITMUN Backend Core

The SITMUN backend core is a key component of the SITMUN software system.
It is built using Java 11 and Spring Boot.
It provides a REST API that allows users to access the backend business logic and interact with the SITMUN database.
The backend core is designed to provide the foundation for the SITMUN system.

The SITMUN backend core is structured as a Gradle multimodule project,
which allows for the separation of the code into multiple smaller modules
that can be more easily managed and maintained.
This structure also allows for greater flexibility in terms of deploying and
testing different parts of the software system independently,
making it easier to identify and fix issues as they arise.

## Prerequisites

Before you begin, ensure you have met the following requirements:

- You have a `Windows/Linux/Mac` machine.
- You have installed the latest version of [Docker CE](https://docs.docker.com/engine/install/) and [Docker Compose](https://docs.docker.com/compose/install/), or [Docker Desktop](https://www.docker.com/products/docker-desktop/).
  Docker CE is fully open-source, while Docker Desktop is a commercial product.
- You have installed [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) on your machine.
- You have a basic understanding of Docker, Docker Compose, and Git.
- You have internet access on your machine to pull Docker images and Git repositories.

## Installing SITMUN Backend Core

To install the SITMUN Proxy Middleware, follow these steps:

1. Clone the repository:

    ```bash
    git clone https://github.com/sitmun/sitmun-backend-core.git
    ```

2. Change to the directory of the repository:

    ```bash
    cd sitmun-backend-core
    ```

3. Edit the file named `.env` inside the directory.

4. Start the SITMUN Backend Core:

    ```bash
    docker compose up
    ```

   This command will build and start all the services defined in the `docker-compose.yml` file.

5. Access the SITMUN Middleware Proxy at [http://localhost:9001/api/dashboard/health](http://localhost:9002/actuator/health) and expect:

    ```json
    {"status":"UP"}
    ```

See [SITMUN Application Stack](https://github.com/sitmun/sitmun-application-stack) as an example of how to deploy and run the proxy as parte of the SITMUN stack.

## Deployment modules

The **SITMUN backend core** can be deployed using two distinct configurations defined in their respective modules, which
are:

- `env\heroku`: This configuration includes all necessary configuration files and data required to deploy a
  development instance of the **SITMUN backend core** on the Heroku platform using GitHub Actions. This deployment uses a **Heroku Data postgres** as database. It can be accessed
  at <https://sitmun-backend-core.herokuapp.com/>. The API definitions for this deployment can be found
  at <https://sitmun-backend-core.herokuapp.com/swagger-ui/index.html>.
- `env\preprod`: This configuration is intended for running a pre production instance of the **SITMUN backend core** . It includes all necessary configuration files and data for testing the system in preproduction.

It is possible to run the `env\heroku` configuration locally by executing the following commands:

```bash
./gradlew stage
heroku local
```

This task expects an `.env` file with at least the following properties:

- `JDBC_DATABASE_URL=jdbc:postgresql://{host}/{database}`
- `JDBC_DATABASE_USERNAME={username}`
- `JDBC_DATABASE_PASSWORD={password}`
- `SPRING_PROFILES_ACTIVE=heroku,openapi-provided`
- `spring.datasource.hikari.auto-commit=false`

With `host`, `database`, `username` and `password` replaced with your values.

These properties are essential for the proper functioning of the system as they provide the necessary configuration for connecting to the target database and ensure the appropriate Spring profiles are active.
Additionally, the `spring.datasource.hikari.auto-commit` property is set to `false` to prevent automatic commits to the database and maintain better control over transactions.

## Uninstalling SITMUN Backend Core

To stop and remove all services, volumes, and networks defined in the `docker-compose.yml` file, use:

```bash
docker compose down -v
```

## Contributing to SITMUN Application Stack

To contribute to SITMUN Application Stack, follow these steps:

1. **Fork this repository** on GitHub.
2. **Clone your forked repository** to your local machine.
3. **Create a new branch** for your changes.
4. **Make your changes** and commit them.
5. **Push your changes** to your forked repository.
6. **Create the pull request** from your branch on GitHub.

Alternatively, see the GitHub documentation on [creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).

## License

This project uses the following license: [European Union Public Licence V. 1.2](LICENSE).
