# Multi-stage build for Flink Training Exercises
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app
COPY . .

RUN ./gradlew :ride-cleansing:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/ride-cleansing/build/libs/ride-cleansing-*-all.jar /app/ride-cleansing.jar

ENTRYPOINT ["java", "-cp", "/app/ride-cleansing.jar", "org.apache.flink.training.exercises.ridecleansing.RideCleansingSolution"]
