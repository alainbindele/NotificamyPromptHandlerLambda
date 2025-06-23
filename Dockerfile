# Multi-stage build for better Lambda compatibility
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /build

# Accept build arguments for GitHub credentials
ARG MY_GITHUB_USERNAME
ARG MY_GITHUB_TOKEN

# Set environment variables for Maven build
ENV MY_GITHUB_USERNAME=${MY_GITHUB_USERNAME}
ENV MY_GITHUB_TOKEN=${MY_GITHUB_TOKEN}

# Copy Maven configuration first for better caching
COPY pom.xml settings.xml ./

# Download dependencies first (better caching)
RUN mvn dependency:go-offline -q --settings settings.xml

# Copy source code
COPY src ./src

# Build the application with uber-jar
RUN mvn clean package -DskipTests -Dquarkus.package.type=uber-jar -q --settings settings.xml

# Verify JAR was created and list its contents
RUN ls -la target/ && \
    if [ -f target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ]; then \
        echo "✅ JAR file created successfully"; \
        jar tf target/lambda-processor-1.0.0-SNAPSHOT-runner.jar | grep -E "(NotificamyLambdaHandler|application)" | head -10; \
    else \
        echo "❌ JAR file not found"; \
        exit 1; \
    fi

# Runtime stage - Use official AWS Lambda Java 21 base image
FROM public.ecr.aws/lambda/java:21

# Copy the uber JAR from build stage
COPY --from=build /build/target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler - Use Quarkus Lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]