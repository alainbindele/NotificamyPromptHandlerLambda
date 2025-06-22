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
        try {
            // Execute current strategy
            if (strategy != null) {
                strategy.sendNotification(request);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send notification via %s for query %d", 
                    strategy.getClass().getSimpleName(), request.getQueryId());
        }
        
        // Continue with next decorator in chain
        if (next != null) {
            next.sendNotification(request);
        }
    }
}