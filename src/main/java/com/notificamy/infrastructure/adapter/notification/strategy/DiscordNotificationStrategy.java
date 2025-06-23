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
            
            LOG.infof("Sending Discord message to user %s for query %d", request.getUser().getEmail(), request.getQueryId());
            
            Response response = client.target(webhookUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .post(Entity.json(payload));
            
            if (response.getStatus() == 204) {
                LOG.infof("Discord message sent successfully to user %s for query %d", request.getUser().getEmail(), request.getQueryId());
            } else {
                LOG.errorf("Discord webhook error for query %d: %d - %s", 
                        request.getQueryId(), response.getStatus(), response.readEntity(String.class));
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Discord message to user %s for query %d", request.getUser().getEmail(), request.getQueryId());
            throw new RuntimeException("Failed to send Discord message", e);
        }
    }
    
    private Map<String, Object> buildDiscordPayload(NotificationRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        
        return Map.of(
            "content", String.format("🔔 **Notificamy** - Nuova risposta AI per %s", 
                    request.getUser().getName() != null ? request.getUser().getName() : "Utente"),
            "embeds", List.of(
                Map.of(
                    "title", "🤖 Risposta AI Generata",
                    "color", 6750207, // Purple color
                    "fields", List.of(
                        Map.of(
                            "name", "📝 La tua richiesta",
                            "value", String.format("```\n%s\n```", request.getPrompt()),
                            "inline", false
                        ),
                        Map.of(
                            "name", "🤖 Risposta dell'AI",
                            "value", request.getAiResponse().length() > 1000 ? 
                                    request.getAiResponse().substring(0, 1000) + "..." : 
                                    request.getAiResponse(),
                            "inline", false
                        ),
                        Map.of(
                            "name", "⏰ Ricevuto",
                            "value", timestamp,
                            "inline", true
                        ),
                        Map.of(
                            "name", "🆔 Query ID",
                            "value", String.valueOf(request.getQueryId()),
                            "inline", true
                        )
                    ),
                    "footer", Map.of(
                        "text", "Grazie per aver usato Notificamy! 🚀",
                        "icon_url", "https://cdn.discordapp.com/emojis/123456789.png"
                    ),
                    "timestamp", java.time.Instant.now().toString()
                )
            )
        );
    }
}