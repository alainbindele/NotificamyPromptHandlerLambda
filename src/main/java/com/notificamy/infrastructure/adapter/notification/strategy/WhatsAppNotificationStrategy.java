package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.infrastructure.config.ApiKeysConfigService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class WhatsAppNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(WhatsAppNotificationStrategy.class);
    
    @Inject
    ApiKeysConfigService apiKeysConfig;
    
    @ConfigProperty(name = "app.whatsapp.api-url")
    String whatsappApiUrl;
    
    private final Client client;
    
    public WhatsAppNotificationStrategy() {
        this.client = ClientBuilder.newClient();
    }
    
    @Override
    public void sendNotification(NotificationRequest request) {
        String phoneNumber = request.getUser().getChannelConfiguration(NotificationChannel.WHATSAPP);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            LOG.warnf("WhatsApp phone number not configured for user %s", request.getUser().getEmail());
            return;
        }
        
        String apiToken = apiKeysConfig.getWhatsAppApiToken();
        if (apiToken == null) {
            LOG.error("WhatsApp API token not found in secrets manager");
            return;
        }
        
        try {
            String message = buildWhatsAppMessage(request);
            
            Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", phoneNumber,
                "type", "text",
                "text", Map.of("body", message)
            );
            
            Response response = client.target(whatsappApiUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .post(Entity.json(payload));
            
            if (response.getStatus() == 200) {
                LOG.infof("WhatsApp message sent successfully to %s", phoneNumber);
            } else {
                LOG.errorf("WhatsApp API error: %d - %s", response.getStatus(), response.readEntity(String.class));
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send WhatsApp message to %s", phoneNumber);
            throw new RuntimeException("Failed to send WhatsApp message", e);
        }
    }
    
    private String buildWhatsAppMessage(NotificationRequest request) {
        return String.format("""
                🔔 *Notificamy Notification*
                
                Hello %s!
                
                📝 *Your Request:*
                "%s"
                
                🤖 *AI Response:*
                %s
                
                Thank you for using Notificamy! 🚀
                """, 
                request.getUser().getName() != null ? request.getUser().getName() : "User",
                request.getPrompt(),
                request.getAiResponse());
    }
}