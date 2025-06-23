# Multi-stage build for Quarkus Lambda
FROM eclipse-temurin:17-jdk AS build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven files first for better caching
COPY pom.xml .
COPY settings.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -q

# Copy source code
COPY src ./src

# Build the application and copy dependencies (skip tests for Docker build)
RUN mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DskipTests -q

# Runtime stage
FROM public.ecr.aws/lambda/java:17

# Copy the Quarkus runner JAR
COPY --from=build /app/target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Copy all dependencies
COPY --from=build /app/target/dependency/* ${LAMBDA_TASK_ROOT}/lib/

# Set the Lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]