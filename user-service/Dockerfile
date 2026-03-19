# Use official Eclipse Temurin JDK 21 base image
#FROM eclipse-temurin:21-jdk AS build
FROM eclipse-temurin:21-jdk-jammy AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for dependency caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw -q -B dependency:go-offline

# Copy source code. Improve layer caching
COPY src src

# Build the application
RUN ./mvnw -q -B clean package -DskipTests

# ----------------------------
# Runtime image
# ----------------------------
#FROM eclipse-temurin:21-jre
#FROM eclipse-temurin:21-jre-ubi10-minimal
FROM gcr.io/distroless/java21:nonroot

# Set working directory
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/user-service-*.jar user-service.jar

# Expose application port (adjust if not 8080)
EXPOSE 8080

# Run the application
# Memory tuning Java should be container-aware. Add JVM flags: For performance testing this matters
# Observability recommendation for observability with OpenTelemetry : "-XX:+UseContainerSupport"
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "user-service.jar"]
