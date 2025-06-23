
FROM public.ecr.aws/lambda/java:17

ADD target/NotificamyNotifierLambda-1.0-SNAPSHOT-runner.jar /var/task/lib/NotificamyNotifierLambda-1.0-SNAPSHOT-runner.jar
ADD target/lib/  /var/task/lib/

CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]