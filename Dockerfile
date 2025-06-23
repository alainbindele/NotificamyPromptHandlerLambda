# Use the official AWS Lambda Java 21 runtime as base image
FROM public.ecr.aws/lambda/java:21

# Copy the built JAR file to the Lambda task root
COPY target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set the handler to our custom Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]