# Multi-stage build
FROM maven:3.8.6-openjdk-17-slim AS build

WORKDIR /build

# Copy POM and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Final stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/target/auth-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]