# Use an official lightweight JDK runtime as a base image
FROM eclipse-temurin:24-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY target/Data-Governance-Service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app runs on (default: 8080)
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
