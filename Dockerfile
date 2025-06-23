# Use the official AWS Lambda Java 17 runtime as base image
FROM public.ecr.aws/lambda/java:17

# Copy the built JAR file to the Lambda task root
COPY target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/lib/

# Set the Lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]