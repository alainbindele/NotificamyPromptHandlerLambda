package com.notificamy.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@RegisterForReflection
@Slf4j
public class NotificamyHandler extends QuarkusStreamHandler implements RequestHandler<SQSEvent, String> {

	@Inject
	ObjectMapper objectMapper;

	@Override
	@Transactional
	public String handleRequest(SQSEvent sqsEvent, Context context) {

		if (Objects.nonNull(sqsEvent) && !sqsEvent.getRecords().isEmpty()) {
			for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
				processMessage(msg, context);
			}
		} else {
			log.info("No messages to process");
		}
		log.info("Processing complete.");
		return "{\"statusCode\": \"OK\"}";
	}

	private void processMessage(SQSEvent.SQSMessage msg, Context context) {
		try {
			log.info("Lambda Id: {}", context.getAwsRequestId());
			log.info("Processing message: {}", msg.getBody());

			log.info("Message processed successfully ");

		} catch (Exception e) {
			log.error("An error occurred while processing the message", e);
			throw new RuntimeException("Message processing failed", e);
		}
	}
}
