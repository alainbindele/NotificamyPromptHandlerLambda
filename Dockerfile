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

# Verify JAR was created and contains our handler class
RUN ls -la target/ && \
    if [ -f target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ]; then \
        echo "✅ JAR file created successfully"; \
        echo "📋 JAR size: $(du -h target/lambda-processor-1.0.0-SNAPSHOT-runner.jar)"; \
        echo "📋 Checking for handler class..."; \
        jar tf target/lambda-processor-1.0.0-SNAPSHOT-runner.jar | grep -i "NotificamyLambdaHandler" && echo "✅ Handler class found" || echo "⚠️ Handler class not found in JAR"; \
        echo "📋 Checking Quarkus Lambda classes..."; \
        jar tf target/lambda-processor-1.0.0-SNAPSHOT-runner.jar | grep -i "QuarkusStreamHandler" && echo "✅ QuarkusStreamHandler found" || echo "⚠️ QuarkusStreamHandler not found"; \
        echo "📋 Checking CDI beans..."; \
        jar tf target/lambda-processor-1.0.0-SNAPSHOT-runner.jar | grep -E "(beans\.xml|jandex\.idx)" && echo "✅ CDI configuration found" || echo "⚠️ CDI configuration not found"; \
    else \
        echo "❌ JAR file not found"; \
        exit 1; \
    fi

# Runtime stage - Use official AWS Lambda Java 21 base image
FROM public.ecr.aws/lambda/java:21

# Copy the uber JAR from build stage
COPY --from=build /build/target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# CRITICAL: Use QuarkusStreamHandler - this is the correct approach for Quarkus Lambda
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]