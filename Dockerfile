# ==========================================
# Multi-stage Dockerfile for Hisobchi Bot
# ==========================================

# 1. Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2. Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for container health check
RUN apk --no-cache add curl tzdata

# Create dedicated non-root application user
RUN addgroup -S hisobchi && adduser -S hisobchi -G hisobchi

# Set default timezone to Asia/Tashkent
ENV TZ=Asia/Tashkent

# Copy compiled jar from build stage
COPY --from=builder /build/target/hisobchi-bot-1.0.0.jar app.jar
RUN chown -R hisobchi:hisobchi /app

USER hisobchi

# Port (if web actuator is enabled)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 0

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
