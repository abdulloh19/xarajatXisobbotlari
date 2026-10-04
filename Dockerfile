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

# Install curl, tzdata, python, flac and ffmpeg for speech recognition
RUN apk --no-cache add curl tzdata python3 py3-pip ffmpeg flac && \
    ln -sf /usr/bin/python3 /usr/bin/python && \
    pip install --no-cache-dir imageio-ffmpeg SpeechRecognition --break-system-packages

# Create dedicated non-root application user
RUN addgroup -S hisobchi && adduser -S hisobchi -G hisobchi

# Set default timezone to Asia/Tashkent
ENV TZ=Asia/Tashkent

# Copy scripts and compiled jar from build stage
COPY scripts ./scripts
COPY --from=builder /build/target/hisobchi-bot-1.0.0.jar app.jar
RUN chown -R hisobchi:hisobchi /app

USER hisobchi

# Port (if web actuator is enabled)
EXPOSE 10000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${PORT:-10000}/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
