[![Build Status](https://travis-ci.com/sitmun/sitmun-backend-core.svg?branch=master)](https://travis-ci.com/sitmun/sitmun-backend-core)
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

The development profile (`unsafe`) may be used for UI development.
It enables an anonymous user to work with admin privileges.

```bash
SPRING_PROFILES_ACTIVE=dev,unsafe ./gradlew bootRun
```

When the application runs in local the API Documentation is available at 
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config.