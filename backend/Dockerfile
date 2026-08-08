# Stage 1: Build the application
FROM amazoncorretto:17 AS builder

WORKDIR /app

# Copy gradle wrapper and files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src src

# Build the application (excluding tests as we can run them outside docker during CI/CD)
RUN ./gradlew bootJar -x test --no-daemon --refresh-dependencies

# Stage 2: Create the runtime image
FROM amazoncorretto:17-alpine

WORKDIR /app

# Install postgresql-client to provide pg_dump and pg_restore
# This is required by PostgresProvider and BackupJob
RUN apk add --no-cache postgresql15-client || apk add --no-cache postgresql-client

# Create heimdall-data directory so we can mount it
RUN mkdir -p /app/heimdall-data

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
