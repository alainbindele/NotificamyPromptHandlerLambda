# Multi-stage build for better compatibility and smaller final image
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

# Build the application
RUN mvn clean package -DskipTests -Dquarkus.package.type=uber-jar -q --settings settings.xml

# Runtime stage - Use Amazon Linux 2 with Java 21
FROM amazonlinux:2

# Install Java 21 and required packages
RUN yum update -y && \
    yum install -y java-21-amazon-corretto && \
    yum clean all

# Create Lambda runtime interface client directory
RUN mkdir -p /opt/extensions

# Set Lambda environment variables
ENV LAMBDA_TASK_ROOT=/var/task
ENV LAMBDA_RUNTIME_DIR=/var/runtime

# Create task directory
RUN mkdir -p ${LAMBDA_TASK_ROOT}

# Copy the uber JAR from build stage
COPY --from=build /build/target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set working directory
WORKDIR ${LAMBDA_TASK_ROOT}

# Create a simple bootstrap script for Lambda
RUN echo '#!/bin/bash' > /var/task/bootstrap && \
    echo 'cd /var/task' >> /var/task/bootstrap && \
    echo 'java -jar lambda-processor-1.0.0-SNAPSHOT-runner.jar' >> /var/task/bootstrap && \
    chmod +x /var/task/bootstrap

# Set the Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]