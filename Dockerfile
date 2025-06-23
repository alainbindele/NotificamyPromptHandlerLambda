# Use the official AWS Lambda Java 21 base image
FROM public.ecr.aws/lambda/java:21

# Set working directory
WORKDIR ${LAMBDA_TASK_ROOT}

# Copy the uber JAR directly to the Lambda task root
COPY target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]