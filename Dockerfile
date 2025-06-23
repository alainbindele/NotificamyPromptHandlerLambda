# Use official AWS Lambda Java 21 base image directly
FROM public.ecr.aws/lambda/java:21

# Set working directory
WORKDIR ${LAMBDA_TASK_ROOT}

# Install Maven for building
RUN yum update -y && \
    yum install -y maven git && \
    yum clean all

# Copy Maven configuration
COPY pom.xml settings.xml ./

# Copy source code
COPY src ./src

# Build the application directly in the Lambda container
RUN mvn clean package -DskipTests -Dquarkus.package.type=uber-jar -q

# Copy the built JAR to the Lambda task root
RUN cp target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]