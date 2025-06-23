package com.notificamy.application.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

/**
 * MINIMAL AWS Lambda handler
 * NO dependencies, NO frameworks - just pure Java
 */
public class NotificamyLambdaHandler implements RequestHandler<SQSEvent, String> {
    
    public NotificamyLambdaHandler() {
        System.out.println("🚀 NotificamyLambdaHandler constructor called");
    }
    
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        System.out.println("🚀 handleRequest called - Lambda is working!");
        
        if (event == null) {
            System.out.println("⚠️ Event is null");
            return "Event is null";
        }
        
        if (event.getRecords() == null) {
            System.out.println("⚠️ No records in event");
            return "No records";
        }
        
        int recordCount = event.getRecords().size();
        System.out.println("📨 Processing " + recordCount + " SQS records");
        
        for (SQSEvent.SQSMessage record : event.getRecords()) {
            System.out.println("📋 Message body: " + record.getBody());
        }
        
        String result = "✅ Processed " + recordCount + " records successfully";
        System.out.println(result);
        return result;
    }
}