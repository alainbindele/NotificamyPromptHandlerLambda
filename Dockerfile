# Minimal Dockerfile for AWS Lambda
FROM public.ecr.aws/lambda/java:21

# Copy the shaded JAR
COPY target/lambda-processor-1.0.0-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler
CMD ["com.notificamy.application.lambda.NotificamyLambdaHandler::handleRequest"]