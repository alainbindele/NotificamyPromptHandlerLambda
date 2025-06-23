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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            
            LOG.infof("Sending Slack message to user %s for query %d", request.getUser().getEmail(), request.getQueryId());
            
            Response response = client.target(webhookUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .post(Entity.json(payload));
            
            if (response.getStatus() == 200) {
                LOG.infof("Slack message sent successfully to user %s for query %d", request.getUser().getEmail(), request.getQueryId());
            } else {
                LOG.errorf("Slack webhook error for query %d: %d - %s", 
                        request.getQueryId(), response.getStatus(), response.readEntity(String.class));
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Slack message to user %s for query %d", request.getUser().getEmail(), request.getQueryId());
            throw new RuntimeException("Failed to send Slack message", e);
        }
    }
    
    private Map<String, Object> buildSlackPayload(NotificationRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        
        return Map.of(
            "text", "Notificamy - Nuova risposta AI",
            "blocks", List.of(
                Map.of(
                    "type", "header",
                    "text", Map.of(
                        "type", "plain_text",
                        "text", "🔔 Notificamy - Risposta AI"
                    )
                ),
                Map.of(
                    "type", "section",
                    "text", Map.of(
                        "type", "mrkdwn",
                        "text", String.format("Ciao *%s*! 👋\n\nLa tua richiesta è stata elaborata dall'AI.",
                                request.getUser().getName() != null ? request.getUser().getName() : "Utente")
                    )
                ),
                Map.of(
                    "type", "divider"
                ),
                Map.of(
                    "type", "section",
                    "fields", List.of(
                        Map.of(
                            "type", "mrkdwn",
                            "text", String.format("*📝 La tua richiesta:*\n\"%s\"", request.getPrompt())
                        )
                    )
                ),
                Map.of(
                    "type", "section",
                    "text", Map.of(
                        "type", "mrkdwn",
                        "text", String.format("*🤖 Risposta dell'AI:*\n%s", request.getAiResponse())
                    )
                ),
                Map.of(
                    "type", "context",
                    "elements", List.of(
                        Map.of(
                            "type", "mrkdwn",
                            "text", String.format("⏰ Ricevuto: %s | Grazie per aver usato Notificamy! 🚀", timestamp)
                        )
                    )
                )
            )
        );
    }
}