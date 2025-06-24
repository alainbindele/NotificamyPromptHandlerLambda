#FROM public.ecr.aws/lambda/java:17
## Copy function code and runtime dependencies from Maven layout
#COPY target/classes ${LAMBDA_TASK_ROOT}
#COPY target/dependency/* ${LAMBDA_TASK_ROOT}/lib/
#
## Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
##CMD [ "org.besafe.hex.infrastructure.adapter.input.lambda.QueueStartLambda::handleRequest" ]
#CMD [ "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest" ]

FROM public.ecr.aws/lambda/java:17

ADD target/NotificamyNotifierLambda-1.0-SNAPSHOT-runner.jar /var/task/lib/NotificamyNotifierLambda-1.0-SNAPSHOT-runner.jar
ADD target/lib/  /var/task/lib/

CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]