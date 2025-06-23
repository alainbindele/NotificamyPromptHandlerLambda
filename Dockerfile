FROM public.ecr.aws/lambda/java:17

# Copy the Quarkus Lambda runner JAR and dependencies
COPY target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ${LAMBDA_TASK_ROOT}/
COPY target/dependency/* ${LAMBDA_TASK_ROOT}/lib/

# Set the Lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]