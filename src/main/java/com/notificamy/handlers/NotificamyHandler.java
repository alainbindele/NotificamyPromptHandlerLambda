package com.notificamy.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificamy.domain.service.NotificationService;
import com.notificamy.infrastructure.external.dto.SqsMessage;
import com.notificamy.infrastructure.mapper.SqsMessageMapper;
import io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger;

@RegisterForReflection
@Slf4j
public class NotificamyHandler extends QuarkusStreamHandler implements RequestHandler<SQSEvent, String> {

	private static final Logger LOG = Logger.getLogger(NotificamyHandler.class);

	@Inject
	NotificationService notificationService;

	@Inject
	SqsMessageMapper sqsMessageMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String handleRequest(SQSEvent event, Context context) {
		LOG.infof("Processing SQS event with %d records", event.getRecords().size());
		return "OK";
/*
		int processedCount = 0;
		int errorCount = 0;

		for (SQSEvent.SQSMessage sqsMessage : event.getRecords()) {
			try {
				processMessage(sqsMessage);
				processedCount++;
			} catch (Exception e) {
				LOG.errorf(e, "Error processing SQS message: %s", sqsMessage.getBody());
				errorCount++;
			}
		}

		String result = String.format("Processed: %d, Errors: %d", processedCount, errorCount);
		LOG.infof("Lambda execution completed: %s", result);
		return result;
	}

	private void processMessage(SQSEvent.SQSMessage sqsMessage) throws Exception {
		LOG.infof("Processing message: %s", sqsMessage.getBody());

		// Parse SQS message using external DTO
		SqsMessage message = objectMapper.readValue(sqsMessage.getBody(), SqsMessage.class);
		LOG.infof("Parsed message: %s", message);

		// Extract domain values using mapper
		Long queryId = sqsMessageMapper.extractQueryId(message);
		String prompt = sqsMessageMapper.extractPrompt(message);

		// Delegate to domain service
		notificationService.processNotificationRequest(queryId, prompt);

		LOG.infof("Message processed successfully for query ID: %d", queryId);*/
	}
}
