# Multi-stage build for better Lambda compatibility
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /build

# Copy Maven configuration
COPY pom.xml settings.xml ./

# Download dependencies first (better caching)
RUN mvn dependency:go-offline -q

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -Dquarkus.package.type=uber-jar -q

# Runtime stage - Use official AWS Lambda Java 21 base image
FROM public.ecr.aws/lambda/java:21

# Copy the uber JAR from build stage
COPY --from=build /build/target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]