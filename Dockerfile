# Use the official AWS Lambda Java 21 runtime as base image
FROM public.ecr.aws/lambda/java:21

# Copy the built uber-jar file to the Lambda task root
COPY target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/

# Set the Lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]