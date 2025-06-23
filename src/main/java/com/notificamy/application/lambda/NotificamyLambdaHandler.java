package com.notificamy.application.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.domain.model.User;
import com.notificamy.domain.port.AiServicePort;
import com.notificamy.domain.port.NotificationPort;
import com.notificamy.infrastructure.external.dto.SqsMessage;
import com.notificamy.infrastructure.mapper.SqsMessageMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.util.Set;

@Named("notificamyLambda")
public class NotificamyLambdaHandler implements RequestHandler<SQSEvent, String> {
    
    private static final Logger LOG = Logger.getLogger(NotificamyLambdaHandler.class);
    
    @Inject
    AiServicePort aiService;
    
    @Inject
    NotificationPort notificationPort;
    
    @Inject
    SqsMessageMapper sqsMessageMapper;
    
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
        LOG.infof("Parsed message for query ID: %d", message.getQueryId());
        
        // Extract data using mapper
        Long queryId = sqsMessageMapper.extractQueryId(message);
        String prompt = sqsMessageMapper.extractPrompt(message);
        User user = sqsMessageMapper.extractUser(message);
        Set<NotificationChannel> enabledChannels = sqsMessageMapper.extractEnabledChannels(message);
        
        // Validate that at least one channel is available
        if (enabledChannels.isEmpty()) {
            throw new RuntimeException("No notification channels available for query ID: " + queryId);
        }
        
        LOG.infof("User: %s, Enabled channels: %s", user.getEmail(), enabledChannels);
        
        // Process with AI
        String aiResponse = aiService.processPrompt(prompt);
        LOG.infof("AI response generated for query ID: %d", queryId);
        
        // Create notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                queryId, prompt, user, enabledChannels, aiResponse
        );
        
        // Send notifications through all enabled channels using Decorator pattern
        notificationPort.sendNotification(notificationRequest);
        
        LOG.infof("Notification request processed successfully for query ID: %d through %d channels", 
                queryId, enabledChannels.size());
    }
}