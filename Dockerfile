# Stage 0, "build-backend-core", based on OpenJDK 11, to build and compile the frontend
FROM openjdk:11 AS build-backend-core
COPY . /usr/src/sitmun-backend-core
WORKDIR /usr/src/sitmun-backend-core
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -i clean build -x test

# Stage 1, based on OpenJDK, to have only the compiled app
FROM openjdk:11-jre-slim-buster
RUN apt-get update && apt-get install -y curl
COPY --from=build-backend-core /usr/src/sitmun-backend-core/build/libs/*.jar /usr/src/sitmun.jar
WORKDIR /usr/src
ENTRYPOINT ["java", "-jar", "sitmun.jar"]
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 CMD curl --fail http://localhost:8080/api/dashboard/health || exit 1

