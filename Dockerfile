# ============================================
# Parabola Backend — Multi-stage Docker Build
# ============================================
# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create uploads directory for image storage
RUN mkdir -p /app/uploads

COPY --from=build /app/target/*.jar app.jar

# Expose port (Render sets PORT dynamically)
EXPOSE 8080

# Start the application with render profile
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms128m", "-Xmx256m", "-XX:MaxRAM=400m", "-Dspring.profiles.active=render", "-jar", "app.jar"]
