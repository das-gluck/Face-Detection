# ── Stage 1: Build jar ───────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache Maven deps — only re-runs when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime with FFmpeg ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

# FFmpeg is required for frame extraction and video reassembly
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Video storage directories (mounted as Docker volume)
RUN mkdir -p /data/uploads /data/processed /data/frames

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
