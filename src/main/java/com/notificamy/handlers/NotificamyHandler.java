package com.notificamy.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificamy.domain.service.NotificationService;
import com.notificamy.infrastructure.external.dto.SqsMessage;
import com.notificamy.infrastructure.mapper.SqsMessageMapper;
import io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;

@RegisterForReflection
@Slf4j
public class NotificamyHandler extends QuarkusStreamHandler implements RequestHandler<Map<String, Object>, String> {

	private static final Logger LOG = Logger.getLogger(NotificamyHandler.class);

	@Inject
	NotificationService notificationService;

	@Inject
	SqsMessageMapper sqsMessageMapper;
	
	@Inject
	SqsClient sqsClient;
	
	@ConfigProperty(name = "app.aws.sqs.queue-url")
	String queueUrl;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String handleRequest(Map<String, Object> input, Context context) {
		LOG.infof("Lambda invoked - polling last 10 messages from SQS queue: %s", queueUrl);
		
		if (queueUrl == null || queueUrl.isEmpty()) {
			LOG.error("SQS Queue URL not configured");
			return "Error: SQS Queue URL not configured";
		}
		
		int processedCount = 0;
		int errorCount = 0;

		try {
			// Recupera gli ultimi 10 messaggi dalla coda SQS
			List<Message> messages = receiveMessagesFromQueue();
			
			LOG.infof("Retrieved %d messages from SQS queue", messages.size());
			
			for (Message message : messages) {
				try {
					processMessage(message);
					
					// Elimina il messaggio dalla coda dopo averlo processato con successo
					deleteMessageFromQueue(message);
					
					processedCount++;
				} catch (Exception e) {
					LOG.errorf(e, "Error processing SQS message: %s", message.body());
					errorCount++;
					
					// Non eliminiamo il messaggio se il processing fallisce
					// Così può essere ritentato o finire nella DLQ
				}
			}
			
		} catch (Exception e) {
			LOG.errorf(e, "Error polling messages from SQS queue");
			errorCount++;
		}

		String result = String.format("Processed: %d, Errors: %d", processedCount, errorCount);
		LOG.infof("Lambda execution completed: %s", result);
		return result;
	}
	
	private List<Message> receiveMessagesFromQueue() {
		try {
			ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
					.queueUrl(queueUrl)
					.maxNumberOfMessages(10) // Massimo 10 messaggi per chiamata
					.waitTimeSeconds(5) // Long polling per 5 secondi
					.visibilityTimeout(300) // 5 minuti di timeout per processare (CORRETTO)
					.messageAttributeNames("All")
					.build();
			
			ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
			
			LOG.infof("SQS receiveMessage response: %d messages received", response.messages().size());
			
			return response.messages();
			
		} catch (Exception e) {
			LOG.errorf(e, "Failed to receive messages from SQS queue: %s", queueUrl);
			throw new RuntimeException("Failed to receive messages from SQS", e);
		}
	}
	
	private void deleteMessageFromQueue(Message message) {
		try {
			DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
					.queueUrl(queueUrl)
					.receiptHandle(message.receiptHandle())
					.build();
			
			sqsClient.deleteMessage(deleteRequest);
			
			LOG.debugf("Successfully deleted message from queue: %s", message.messageId());
			
		} catch (Exception e) {
			LOG.errorf(e, "Failed to delete message from queue: %s", message.messageId());
			// Non lanciamo l'eccezione perché il messaggio è già stato processato
		}
	}

	private void processMessage(Message sqsMessage) throws Exception {
		LOG.infof("Processing message: %s", sqsMessage.body());

		// Parse SQS message using external DTO
		SqsMessage message = objectMapper.readValue(sqsMessage.body(), SqsMessage.class);
		LOG.infof("Parsed message: %s", message);

		// Extract domain values using mapper
		Long queryId = sqsMessageMapper.extractQueryId(message);
		String prompt = sqsMessageMapper.extractPrompt(message);

		// Validate required fields
		if (queryId == null) {
			throw new IllegalArgumentException("Query ID is required but was null");
		}
		
		if (prompt == null || prompt.trim().isEmpty()) {
			throw new IllegalArgumentException("Prompt is required but was null or empty");
		}

		// Delegate to domain service
		notificationService.processNotificationRequest(queryId, prompt);

		LOG.infof("Message processed successfully for query ID: %d", queryId);
	}
}