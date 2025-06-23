package com.notificamy.infrastructure.adapter.notification.decorator;

import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.infrastructure.adapter.notification.strategy.NotificationStrategy;
import org.jboss.logging.Logger;

public class NotificationDecorator implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(NotificationDecorator.class);
    
    private final NotificationStrategy strategy;
    private final NotificationDecorator next;
    
    public NotificationDecorator(NotificationStrategy strategy, NotificationDecorator next) {
        this.strategy = strategy;
        this.next = next;
    }
    
    @Override
    public void sendNotification(NotificationRequest request) {
        // Execute current strategy first
        if (strategy != null) {
            try {
                LOG.infof("Executing notification strategy: %s for query %d", 
                        strategy.getClass().getSimpleName(), request.getQueryId());
                strategy.sendNotification(request);
                LOG.infof("Successfully executed %s for query %d", 
                        strategy.getClass().getSimpleName(), request.getQueryId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed to send notification via %s for query %d", 
                        strategy.getClass().getSimpleName(), request.getQueryId());
                // Continue with other channels even if one fails
            }
        }
        
        // Continue with next decorator in chain
        if (next != null) {
            next.sendNotification(request);
        }
    }
}