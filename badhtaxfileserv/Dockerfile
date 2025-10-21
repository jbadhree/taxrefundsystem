# Multi-stage build for badhtaxfileserv
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper files first for better caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies and plugins (this layer will be cached if pom.xml doesn't change)
# Use dependency:resolve-sources to also download source jars for better IDE support
RUN ./mvnw dependency:go-offline -B && \
    ./mvnw dependency:resolve-sources -B

# Copy source code
COPY src/ src/

# Build the application with optimizations
# Use -T 1C to use one thread per CPU core for faster builds
# Use -Dmaven.test.skip=true to skip tests completely
RUN ./mvnw clean package -Dmaven.test.skip=true -T 1C -Dmaven.compiler.source=17 -Dmaven.compiler.target=17

# Runtime stage - use slim image for smaller size
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Install curl for health check (before switching to non-root user)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash appuser

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 4000

# Set environment variables
ENV SERVER_PORT=4000
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:4000/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
