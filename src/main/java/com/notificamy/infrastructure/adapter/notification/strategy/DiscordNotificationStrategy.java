package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DiscordNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(DiscordNotificationStrategy.class);
    
    private final Client client;
    
    public DiscordNotificationStrategy() {
        this.client = ClientBuilder.newClient();
    }
    
    @Override
    public void sendNotification(NotificationRequest request) {
        String webhookUrl = request.getUser().getChannelConfiguration(NotificationChannel.DISCORD);
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOG.warnf("Discord webhook URL not configured for user %s", request.getUser().getEmail());
            return;
        }
        
        try {
            Map<String, Object> payload = buildDiscordPayload(request);
            
            Response response = client.target(webhookUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .post(Entity.json(payload));
            
            if (response.getStatus() == 204) {
                LOG.infof("Discord message sent successfully to user %s", request.getUser().getEmail());
            } else {
                LOG.errorf("Discord webhook error: %d - %s", response.getStatus(), response.readEntity(String.class));
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Discord message to user %s", request.getUser().getEmail());
            throw new RuntimeException("Failed to send Discord message", e);
        }
    }
    
    private Map<String, Object> buildDiscordPayload(NotificationRequest request) {
        return Map.of(
            "content", String.format("🔔 **Notificamy Notification** for %s", 
                    request.getUser().getName() != null ? request.getUser().getName() : "User"),
            "embeds", List.of(
                Map.of(
                    "title", "Your AI-Generated Notification",
                    "color", 6750207, // Purple color
                    "fields", List.of(
                        Map.of(
                            "name", "📝 Your Request",
                            "value", String.format("\"%s\"", request.getPrompt()),
                            "inline", false
                        ),
                        Map.of(
                            "name", "🤖 AI Response",
                            "value", request.getAiResponse(),
                            "inline", false
                        )
                    ),
                    "footer", Map.of(
                        "text", "Thank you for using Notificamy! 🚀"
                    ),
                    "timestamp", java.time.Instant.now().toString()
                )
            )
        );
    }
}