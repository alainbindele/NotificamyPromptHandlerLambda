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
public class SlackNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(SlackNotificationStrategy.class);
    
    private final Client client;
    
    public SlackNotificationStrategy() {
        this.client = ClientBuilder.newClient();
    }
    
    @Override
    public void sendNotification(NotificationRequest request) {
        String webhookUrl = request.getUser().getChannelConfiguration(NotificationChannel.SLACK);
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOG.warnf("Slack webhook URL not configured for user %s", request.getUser().getEmail());
            return;
        }
        
        try {
            Map<String, Object> payload = buildSlackPayload(request);
            
            Response response = client.target(webhookUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .post(Entity.json(payload));
            
            if (response.getStatus() == 200) {
                LOG.infof("Slack message sent successfully to user %s", request.getUser().getEmail());
            } else {
                LOG.errorf("Slack webhook error: %d - %s", response.getStatus(), response.readEntity(String.class));
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Slack message to user %s", request.getUser().getEmail());
            throw new RuntimeException("Failed to send Slack message", e);
        }
    }
    
    private Map<String, Object> buildSlackPayload(NotificationRequest request) {
        return Map.of(
            "text", "Notificamy Notification",
            "blocks", List.of(
                Map.of(
                    "type", "header",
                    "text", Map.of(
                        "type", "plain_text",
                        "text", "🔔 Notificamy Notification"
                    )
                ),
                Map.of(
                    "type", "section",
                    "text", Map.of(
                        "type", "mrkdwn",
                        "text", String.format("Hello *%s*!\n\nYour notification request has been processed by our AI assistant.",
                                request.getUser().getName() != null ? request.getUser().getName() : "User")
                    )
                ),
                Map.of(
                    "type", "section",
                    "fields", List.of(
                        Map.of(
                            "type", "mrkdwn",
                            "text", String.format("*📝 Your Request:*\n\"%s\"", request.getPrompt())
                        ),
                        Map.of(
                            "type", "mrkdwn",
                            "text", String.format("*🤖 AI Response:*\n%s", request.getAiResponse())
                        )
                    )
                ),
                Map.of(
                    "type", "context",
                    "elements", List.of(
                        Map.of(
                            "type", "mrkdwn",
                            "text", "Thank you for using Notificamy! 🚀"
                        )
                    )
                )
            )
        );
    }
}