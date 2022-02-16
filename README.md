[![CI](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml/badge.svg)](https://github.com/sitmun/sitmun-backend-core/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sitmun%3Asitmun-backend-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sitmun%3Asitmun-backend-core)

# SITMUN backend core

REST API, back-end business logic and access to DB.

## Requirements

- Java 8

## Dependencies

## Developer documentation

The development profile (`dev`) should be active via the environment variable `SPRING_PROFILES_ACTIVE` before:

- Building locally the application:

```bash
SPRING_PROFILES_ACTIVE=dev build-scripts/build-local.sh
```

- Running locally the application:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Travis-CI runs the tests with the development profile active.

The development profile (`unsafe`) may be used for UI development. It enables an anonymous user to work with admin
privileges.

```bash
SPRING_PROFILES_ACTIVE=dev,unsafe ./gradlew bootRun
```

When the application runs in local the API Documentation is available at
<http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config>.

## Experimental feature

[HAL Explorer](https://github.com/toedter/hal-explorer) is enabled by default.  
With HAL Explorer you can browse and explore SITMUN Hypermedia APIs. It is available
at <http://localhost:8080/api/explorer/index.html#uri=/api>.
