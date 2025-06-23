package com.notificamy.application.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

/**
 * Lambda handler che implementa RequestHandler
 * Minimale e funzionale per AWS Lambda
 */
@Named("notificamyLambda")
public class NotificamyLambdaHandler implements RequestHandler<SQSEvent, String> {
    
    private static final Logger LOG = Logger.getLogger(NotificamyLambdaHandler.class);
    
    public NotificamyLambdaHandler() {
        LOG.info("🚀 NotificamyLambdaHandler constructor called - Lambda ready!");
    }
    
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        LOG.info("🚀 handleRequest called - Lambda is working!");
        
        if (event == null) {
            LOG.warn("⚠️ Event is null");
            return "Event is null";
        }
        
        if (event.getRecords() == null) {
            LOG.warn("⚠️ No records in event");
            return "No records";
        }
        
        int recordCount = event.getRecords().size();
        LOG.infof("📨 Processing %d SQS records", recordCount);
        
        for (SQSEvent.SQSMessage record : event.getRecords()) {
            LOG.infof("📋 Message body: %s", record.getBody());
            LOG.infof("📋 Message ID: %s", record.getMessageId());
            LOG.infof("📋 Receipt Handle: %s", record.getReceiptHandle());
        }
        
        String result = String.format("✅ Processed %d records successfully", recordCount);
        LOG.info(result);
        return result;
    }
}