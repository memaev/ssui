# syntax=docker/dockerfile:1.7

# ---------- Stage 1: build the Spring Boot jar ----------
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
ENV GRADLE_USER_HOME=/gradle-cache

# Resolve dependencies first so this layer is cached until the build files change.
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/gradle-cache \
    gradle --no-daemon dependencies --quiet > /dev/null

COPY src ./src
RUN --mount=type=cache,target=/gradle-cache \
    gradle --no-daemon bootJar --quiet \
 && cp build/libs/*.jar /workspace/app.jar

# ---------- Stage 2: minimal JRE runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar
USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD ["sh", "-c", "curl -fsS http://localhost:8080/actuator/health || exit 1"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
