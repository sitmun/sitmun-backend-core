[![CI](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml/badge.svg)](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sitmun%3Asitmun-backend-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sitmun%3Asitmun-backend-core)

# SITMUN backend core

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

## Code modules

The SITMUN backend core is comprised of two fundamental modules, namely:

- The `common` module, which is responsible for the primary business logic and underlying infrastructure of the SITMUN
  backend software.
- The `legacy` module, which contains code that was previously part of the `common` module, and is dependent on its
  functionality.

## Deployment modules

The **SITMUN backend core** can be deployed using two distinct configurations defined in their respective modules, which
are:

- `deploy\heroku-dev-full`: This configuration includes all necessary configuration files and data required to deploy a
  development instance of the **SITMUN backend core** on the Heroku platform using GitHub Actions. This deployment uses
  both the `common` and `legacy` modules, and uses a **Heroku Data postgres** as database. It can be accessed
  at https://sitmun-backend-core.herokuapp.com/. The API definitions for this deployment can be found
  at https://sitmun-backend-core.herokuapp.com/swagger-ui/index.html.
- `app`: This configuration is intended for running a production instance of the **SITMUN backend core**  using only
  the `common` module. It includes all necessary configuration files and data for running the system.

It is possible to run the `deploy\heroku-dev-full` configuration locally by executing the following command:

```bash
./gradlew herokuDevLocal
```

This task expects an `.env` file with at least the following properties:

- `JDBC_DATABASE_URL=jdbc:postgres://{host}/{database}`
- `JDBC_DATABASE_USERNAME={username}`
- `JDBC_DATABASE_PASSWORD={password}`
- `SPRING_PROFILES_ACTIVE=heroku,openapi-provided`
- `spring.datasource.hikari.auto-commit=false`

With `host`, `database`, `username` and `password` replaced with your values.

These properties are essential for the proper functioning of the system as they provide the necessary configuration for
connecting to the target database and ensure the appropriate Spring profiles are active.
Additionally, the `spring.datasource.hikari.auto-commit` property is set to `false` to prevent automatic commits to the
database and maintain better control over transactions.

## Docker for local testing

o facilitate local testing of the SITMUN software system, a Docker configuration file named `heroku-dev-full.yml`
has been created.
This file is located in the folder `docker`.
This file provides a test environment that includes the latest version of `deploy/heroku-dev-full` as well as
the latest version of the SITMUN administration application,
which can be found at https://github.com/sitmun/sitmun-admin-app.

To run the local testing environment, it is necessary to set the `GITHUB_TOKEN` environment variable in an `.env` file
that should be located next to the `heroku-dev-full.yml` file.

After setting the `GITHUB_TOKEN` environment variable, the following steps can be executed in a terminal:

```shell
./gradlew clean :deploy:heroku-dev-full:build :deploy:heroku-dev-full:jibDockerBuild -x test
docker-compose -f docker/heroku-dev-full.yml up -d --build
```

These commands will first build the `deploy/heroku-dev-full` configuration files using Gradle, and then
create its Docker image using the `jibDockerBuild` command.
The second command will then start the Docker container using the `docker-compose` tool and make it available
for testing locally.

The SITMUN administration application can be accessed at port 8080, while the Backend Core of the software system can
be accessed at port 8081. A description of the APIs for the Backend Core can be found
at http://localhost:8081/swagger-ui/index.html.

## Tooling modules

The `cli` module of the **SITMUN backend core** is designed to provide a command line interface for extracting
the SQL schema from SITMUN code. This module is tailored to facilitate the extraction process for different
types of database drivers (Oracle, Postgres, H2).

## Developer documentation

Additional information is available at https://sitmun.github.io/arquitectura/Arq_SITMUN.html
