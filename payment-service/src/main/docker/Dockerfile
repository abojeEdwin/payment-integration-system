# Multi-stage build
FROM maven:3.8.6-openjdk-17-slim AS build

WORKDIR /build

# Copy POM and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# Final stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/target/payment-service-0.0.1-SNAPSHOT.jar app.jar

# Create non-root user
RUN useradd -m -u 1000 appuser && chown appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]