# Use official AWS Lambda Java 21 base image directly
FROM public.ecr.aws.lambda/java:21

# Set working directory
WORKDIR ${LAMBDA_TASK_ROOT}

# Accept build arguments for GitHub credentials
ARG MY_GITHUB_USERNAME
ARG MY_GITHUB_TOKEN

# Install Maven for building
RUN yum update -y && \
    yum install -y maven git && \
    yum clean all

# Copy Maven configuration
COPY pom.xml settings.xml ./

# Set environment variables for Maven build
ENV MY_GITHUB_USERNAME=${MY_GITHUB_USERNAME}
ENV MY_GITHUB_TOKEN=${MY_GITHUB_TOKEN}

# Copy source code
COPY src ./src

# Build the application directly in the Lambda container
RUN mvn clean package -DskipTests -Dquarkus.package.type=uber-jar -q --settings settings.xml

# Copy the built JAR to the Lambda task root
RUN cp target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Clean up build artifacts to reduce image size
RUN rm -rf target/ src/ pom.xml settings.xml ~/.m2/repository

# Set the Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]