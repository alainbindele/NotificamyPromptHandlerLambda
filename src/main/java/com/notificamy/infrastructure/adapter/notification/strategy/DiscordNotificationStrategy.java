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
        LOG.infof("=== DISCORD STRATEGY - STARTING ===");
        LOG.infof("Query ID: %d", request.getQueryId());
        LOG.infof("User ID: %d, Email: %s", request.getUser().getId(), request.getUser().getEmail());
        
        String webhookUrl = request.getUser().getChannelConfiguration(NotificationChannel.DISCORD);
        LOG.infof("Discord webhook URL from user config: %s", 
                webhookUrl != null ? (webhookUrl.length() > 50 ? webhookUrl.substring(0, 50) + "..." : webhookUrl) : "null");
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOG.errorf("❌ DISCORD STRATEGY FAILED - Webhook URL not configured for user ID %d (%s)", 
                    request.getUser().getId(), request.getUser().getEmail());
            throw new RuntimeException("Discord webhook URL not configured for user");
        }
        
        try {
            LOG.infof("🎮 Preparing Discord notification to user %s (ID: %d) for query %d", 
                    request.getUser().getEmail(), request.getUser().getId(), request.getQueryId());
            
            Map<String, Object> payload = buildDiscordPayload(request);
            LOG.infof("Discord payload created with %d top-level keys", payload.size());
            
            String requestBody = objectMapper.writeValueAsString(payload);
            LOG.infof("Discord request body length: %d characters", requestBody.length());
            LOG.infof("Discord request body preview: %s", 
                    requestBody.length() > 300 ? requestBody.substring(0, 300) + "..." : requestBody);
            
            LOG.infof("🎮 Sending HTTP POST to Discord webhook...");
            long startTime = System.currentTimeMillis();
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            long endTime = System.currentTimeMillis();
            LOG.infof("🎮 Discord HTTP response received:");
            LOG.infof("  Status Code: %d", response.statusCode());
            LOG.infof("  Response Body: %s", response.body());
            LOG.infof("  Duration: %dms", (endTime - startTime));
            
            if (response.statusCode() == 204) {
                LOG.infof("✅ DISCORD MESSAGE SENT SUCCESSFULLY");
                LOG.infof("  User: %s (ID: %d)", request.getUser().getEmail(), request.getUser().getId());
                LOG.infof("  Query ID: %d", request.getQueryId());
            } else {
                LOG.errorf("❌ DISCORD WEBHOOK ERROR");
                LOG.errorf("  User: %s", request.getUser().getEmail());
                LOG.errorf("  Status Code: %d", response.statusCode());
                LOG.errorf("  Response Body: %s", response.body());
                throw new RuntimeException("Discord webhook returned error: " + response.statusCode());
            }
            
        } catch (Exception e) {
            LOG.errorf("❌ DISCORD SENDING FAILED");
            LOG.errorf("  User: %s (ID: %d)", request.getUser().getEmail(), request.getUser().getId());
            LOG.errorf("  Query ID: %d", request.getQueryId());
            LOG.errorf("  Error: %s", e.getMessage());
            LOG.errorf("  Exception type: %s", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                LOG.errorf("  Caused by: %s - %s", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to send Discord message", e);
        }
    }
    
    private Map<String, Object> buildDiscordPayload(NotificationRequest request) {
        LOG.infof("🎮 Building Discord payload for user: %s", request.getUser().getName());
        
        Map<String, Object> payload = Map.of(
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
        
        LOG.infof("🎮 Discord payload built successfully");
        return payload;
    }
}