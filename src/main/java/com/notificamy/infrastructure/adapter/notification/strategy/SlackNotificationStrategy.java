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
        String webhookUrl = request.getUser().getChannelConfiguration(NotificationChannel.SLACK);
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOG.warnf("Slack webhook URL not configured for user ID %d (%s), skipping Slack notification", 
                    request.getUser().getId(), request.getUser().getEmail());
            throw new RuntimeException("Slack webhook URL not configured for user");
        }
        
        try {
            LOG.infof("Sending Slack notification to user %s (ID: %d) for query %d", 
                    request.getUser().getEmail(), request.getUser().getId(), request.getQueryId());
            
            Map<String, Object> payload = buildSlackPayload(request);
            String requestBody = objectMapper.writeValueAsString(payload);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOG.infof("Slack message sent successfully to user %s (ID: %d)", 
                        request.getUser().getEmail(), request.getUser().getId());
            } else {
                LOG.errorf("Slack webhook error for user %s: %d - %s", 
                        request.getUser().getEmail(), response.statusCode(), response.body());
                throw new RuntimeException("Slack webhook returned error: " + response.statusCode());
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Slack message to user %s (ID: %d)", 
                    request.getUser().getEmail(), request.getUser().getId());
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