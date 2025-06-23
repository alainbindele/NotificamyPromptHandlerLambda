# MINIMAL Dockerfile for AWS Lambda
FROM public.ecr.aws/lambda/java:21

# Copy the shaded JAR (uber JAR with all dependencies)
COPY target/lambda-processor.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]