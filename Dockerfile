FROM public.ecr.aws/lambda/java:17

# Copy the Quarkus Lambda runner JAR
COPY target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/lib/

# Copy all dependencies
COPY target/lib/ ${LAMBDA_TASK_ROOT}/lib/

# Set the Lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]