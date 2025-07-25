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
public class SlackNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(SlackNotificationStrategy.class);
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    public SlackNotificationStrategy() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public void sendNotification(NotificationRequest request) {
        LOG.infof("=== SLACK STRATEGY - STARTING ===");
        LOG.infof("Query ID: %d", request.getQueryId());
        LOG.infof("User ID: %d, Email: %s", request.getUser().getId(), request.getUser().getEmail());
        
        String webhookUrl = request.getUser().getChannelConfiguration(NotificationChannel.SLACK);
        LOG.infof("Slack webhook URL from user config: %s", 
                webhookUrl != null ? (webhookUrl.length() > 50 ? webhookUrl.substring(0, 50) + "..." : webhookUrl) : "null");
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOG.errorf("❌ SLACK STRATEGY FAILED - Webhook URL not configured for user ID %d (%s)", 
                    request.getUser().getId(), request.getUser().getEmail());
            throw new RuntimeException("Slack webhook URL not configured for user");
        }
        
        try {
            LOG.infof("💬 Preparing Slack notification to user %s (ID: %d) for query %d", 
                    request.getUser().getEmail(), request.getUser().getId(), request.getQueryId());
            
            Map<String, Object> payload = buildSlackPayload(request);
            LOG.infof("Slack payload created with %d top-level keys", payload.size());
            
            String requestBody = objectMapper.writeValueAsString(payload);
            LOG.infof("Slack request body length: %d characters", requestBody.length());
            LOG.infof("Slack request body preview: %s", 
                    requestBody.length() > 300 ? requestBody.substring(0, 300) + "..." : requestBody);
            
            LOG.infof("💬 Sending HTTP POST to Slack webhook...");
            long startTime = System.currentTimeMillis();
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            long endTime = System.currentTimeMillis();
            LOG.infof("💬 Slack HTTP response received:");
            LOG.infof("  Status Code: %d", response.statusCode());
            LOG.infof("  Response Body: %s", response.body());
            LOG.infof("  Duration: %dms", (endTime - startTime));
            
            if (response.statusCode() == 200) {
                LOG.infof("✅ SLACK MESSAGE SENT SUCCESSFULLY");
                LOG.infof("  User: %s (ID: %d)", request.getUser().getEmail(), request.getUser().getId());
                LOG.infof("  Query ID: %d", request.getQueryId());
            } else {
                LOG.errorf("❌ SLACK WEBHOOK ERROR");
                LOG.errorf("  User: %s", request.getUser().getEmail());
                LOG.errorf("  Status Code: %d", response.statusCode());
                LOG.errorf("  Response Body: %s", response.body());
                throw new RuntimeException("Slack webhook returned error: " + response.statusCode());
            }
            
        } catch (Exception e) {
            LOG.errorf("❌ SLACK SENDING FAILED");
            LOG.errorf("  User: %s (ID: %d)", request.getUser().getEmail(), request.getUser().getId());
            LOG.errorf("  Query ID: %d", request.getQueryId());
            LOG.errorf("  Error: %s", e.getMessage());
            LOG.errorf("  Exception type: %s", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                LOG.errorf("  Caused by: %s - %s", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to send Slack message", e);
        }
    }
    
    private Map<String, Object> buildSlackPayload(NotificationRequest request) {
        LOG.infof("💬 Building Slack payload for user: %s", request.getUser().getName());
        
        Map<String, Object> payload = Map.of(
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
        
        LOG.infof("💬 Slack payload built successfully");
        return payload;
    }
}