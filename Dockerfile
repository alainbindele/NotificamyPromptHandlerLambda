# Multi-stage build for Quarkus Lambda with Lambda-specific optimizations
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy Maven files first for better caching
COPY pom.xml .
COPY settings.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
)
RUN mvn dependency:go-offline -q

# Copy source code
COPY src ./src

# Build the application with Lambda-specific settings
RUN mvn clean package dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DskipTests \
    -Dquarkus.package.type=uber-jar \
    -q

# Runtime stage - Use AWS Lambda Java 17 base image
FROM public.ecr.aws/lambda/java:17

# Copy the Quarkus uber JAR (contains all dependencies)
COPY --from=build /app/target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/lib/

# Set the Lambda handler - Quarkus Lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]

# Add labels for better image management
LABEL maintainer="Notificamy Team"
LABEL version="1.0.0"
LABEL description="Notificamy Lambda Processor with Quarkus"