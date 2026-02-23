# Multi-stage build for webhook-service
# Stage 1: Build with Maven
FROM maven:3.8.6-openjdk-17-slim AS build

WORKDIR /build

# Copy POM and download dependencies (cache-friendly)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# Stage 2: Runtime with minimal JRE
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/target/webhook-service-1.0.0-SNAPSHOT.jar app.jar

# Create non-root user for security
RUN useradd -m -u 1000 appuser && chown appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8083

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]