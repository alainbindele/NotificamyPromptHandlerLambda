package com.notificamy.application.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MINIMAL AWS Lambda handler - NO CDI, NO Quarkus dependencies
 * This ensures AWS Lambda can find and instantiate the class
 */
public class NotificamyLambdaHandler implements RequestHandler<SQSEvent, String> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Default constructor required by AWS Lambda runtime
     */
    public NotificamyLambdaHandler() {
        System.out.println("🚀 NotificamyLambdaHandler instantiated");
    }
    
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        System.out.println("🚀 Lambda handler started - Processing SQS event");
        
        // Handle null event gracefully
        if (event == null || event.getRecords() == null) {
            String result = "⚠️ No SQS records to process";
            System.out.println(result);
            return result;
        }
        
        int processedCount = 0;
        int errorCount = 0;
        
        for (SQSEvent.SQSMessage sqsMessage : event.getRecords()) {
            try {
                System.out.println("📨 Processing message: " + sqsMessage.getBody());
                processMessage(sqsMessage);
                processedCount++;
                System.out.println("✅ Message processed successfully");
            } catch (Exception e) {
                System.err.println("❌ Error processing SQS message: " + e.getMessage());
                e.printStackTrace();
                errorCount++;
            }
        }
        
        String result = String.format("✅ Lambda execution completed - Processed: %d, Errors: %d", 
                processedCount, errorCount);
        System.out.println(result);
        return result;
    }
    
    private void processMessage(SQSEvent.SQSMessage sqsMessage) throws Exception {
        System.out.println("🔍 Parsing SQS message body...");
        
        // Parse SQS message to extract basic info
        try {
            // Simple JSON parsing without complex dependencies
            String body = sqsMessage.getBody();
            System.out.println("📋 Message body: " + body);
            
            // For now, just simulate processing
            System.out.println("🤖 Simulating AI processing...");
            Thread.sleep(100); // Simulate processing time
            
            System.out.println("📤 Simulating notification sending...");
            Thread.sleep(100); // Simulate notification time
            
            System.out.println("🎉 Message processed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error in processMessage: " + e.getMessage());
            throw e;
        }
    }
}