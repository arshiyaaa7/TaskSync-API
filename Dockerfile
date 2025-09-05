# Use official OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory inside container
WORKDIR /app

# Copy Maven wrapper and pom.xml first (to cache dependencies)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src src

# Package the application (skip tests for speed)
RUN ./mvnw package -DskipTests

# Expose port 8080
EXPOSE 8080

# Run the built JAR
CMD ["java", "-jar", "target/TaskSync-0.0.1-SNAPSHOT.jar"]
