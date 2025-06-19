package com.notificamy.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificamy.dto.SqsMessage;
import com.notificamy.entity.Query;
import com.notificamy.repository.QueryRepository;
import com.notificamy.service.ChatGptService;
import com.notificamy.service.EmailService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

@Named("notificamyLambda")
public class NotificamyLambdaHandler implements RequestHandler<SQSEvent, String> {
    
    private static final Logger LOG = Logger.getLogger(NotificamyLambdaHandler.class);
    
    @Inject
    QueryRepository queryRepository;
    
    @Inject
    ChatGptService chatGptService;
    
    @Inject
    EmailService emailService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        LOG.infof("Processing SQS event with %d records", event.getRecords().size());
        
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
        
        // Parse SQS message
        SqsMessage message = objectMapper.readValue(sqsMessage.getBody(), SqsMessage.class);
        LOG.infof("Parsed message: %s", message);
        
        // Fetch query with user information
        Query query = queryRepository.findByIdWithUser(message.getQueryId());
        if (query == null) {
            LOG.errorf("Query not found with ID: %d", message.getQueryId());
            throw new RuntimeException("Query not found: " + message.getQueryId());
        }
        
        if (query.getUser() == null) {
            LOG.errorf("User not found for query ID: %d", message.getQueryId());
            throw new RuntimeException("User not found for query: " + message.getQueryId());
        }
        
        LOG.infof("Processing query for user: %s", query.getUser().getEmail());
        
        // Process prompt with ChatGPT
        String aiResponse = chatGptService.processPrompt(message.getPrompt());
        LOG.infof("AI response generated for query %d", message.getQueryId());
        
        // Send email notification
        emailService.sendNotificationEmail(
                query.getUser().getEmail(),
                query.getUser().getName(),
                message.getPrompt(),
                aiResponse
        );
        
        LOG.infof("Notification email sent to %s for query %d", 
                query.getUser().getEmail(), message.getQueryId());
    }
}