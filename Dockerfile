# Lambda Dockerfile
FROM public.ecr.aws/lambda/java:21

# Copy the Quarkus uber JAR
COPY target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler to our implementation
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]