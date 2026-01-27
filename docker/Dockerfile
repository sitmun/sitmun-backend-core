ARG JDK_VERSION=17
ARG JDK_TAG=0.17
ARG ALPINE_TAG=3.22

# Use OpenJDK 17 as base image
FROM amazoncorretto:${JDK_VERSION}.${JDK_TAG}

# Set working directory
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
COPY sonar.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies (cached until build files change)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

# Copy source code (doesn't invalidate dependency layer)
COPY src src

# Build the application
# Docker builds use JAR packaging only (default)
# For WAR builds, use: ./gradlew build -Ppackaging=war locally and deploy to servlet container
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon

# Create a new stage for the runtime
FROM amazoncorretto:${JDK_VERSION}.${JDK_TAG}-alpine${ALPINE_TAG}
RUN apk --no-cache add curl
WORKDIR /app
COPY --from=0 /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/dashboard/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]