# Build stage — compile Flink SQL job fat JAR
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/

# Copy sql-dashboard sources (and common if needed)
COPY sql-dashboard/ sql-dashboard/
COPY common/ common/

# Build the shadow (fat) JAR
RUN chmod +x gradlew && ./gradlew :sql-dashboard:shadowJar --no-daemon -x test

# Runtime stage — Flink base image
FROM apache/flink:2.0.0-scala_2.12-java17

# Copy the fat JAR to the Flink user-library directory
COPY --from=builder \
    /build/sql-dashboard/build/libs/sql-dashboard-*-production.jar \
    /opt/flink/usrlib/sql-dashboard-production.jar

# Flink entrypoint is inherited from the base image
