package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DiscordNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(DiscordNotificationStrategy.class);
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    public DiscordNotificationStrategy() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
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
            String requestBody = objectMapper.writeValueAsString(payload);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 204) {
                LOG.infof("Discord message sent successfully to user %s", request.getUser().getEmail());
            } else {
                LOG.errorf("Discord webhook error: %d - %s", response.statusCode(), response.body());
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