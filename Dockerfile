# syntax=docker/dockerfile:1.7

########################
# 1) Build stage
########################
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Copy Gradle wrapper & build files first for better caching
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle build.gradle* gradle.properties* ./

# Copy OpenAPI spec used by openApiGenerate (required for your build)
COPY OpenAPI.yaml ./OpenAPI.yaml

# Copy source last
COPY src ./src

# Build the bootable jar (skip tests here; CI already runs them)
RUN chmod +x ./gradlew \
 && ./gradlew --no-daemon clean bootJar -x test

########################
# 2) Runtime stage
########################
FROM eclipse-temurin:17-jre-jammy AS runtime

# Create non-root user
RUN useradd -m appuser
USER appuser

WORKDIR /app

# Copy the built jar
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# JVM flags tuned for containers; set default Spring profile
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
