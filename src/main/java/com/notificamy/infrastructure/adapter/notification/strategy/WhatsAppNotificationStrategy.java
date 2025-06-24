package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@ApplicationScoped
public class WhatsAppNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(WhatsAppNotificationStrategy.class);
    
    @Inject
    SecretsManagerClient secretsManagerClient;
    
    @ConfigProperty(name = "app.whatsapp.api-url")
    String whatsappApiUrl;
    
    @ConfigProperty(name = "app.aws.secrets.whatsapp.api.key")
    String apiKeysSecretName;
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private String cachedApiToken;
    
    public WhatsAppNotificationStrategy() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public void sendNotification(NotificationRequest request) {
        String phoneNumber = request.getUser().getChannelConfiguration(NotificationChannel.WHATSAPP);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            LOG.warnf("WhatsApp phone number not configured for user %s", request.getUser().getEmail());
            return;
        }
        
        try {
            String apiToken = getWhatsAppApiToken();
            if (apiToken == null || apiToken.isEmpty()) {
                LOG.error("WhatsApp API token not found in secrets");
                return;
            }
            
            String message = buildWhatsAppMessage(request);
            
            Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", phoneNumber,
                "type", "text",
                "text", Map.of("body", message)
            );
            
            String requestBody = objectMapper.writeValueAsString(payload);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(whatsappApiUrl))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOG.infof("WhatsApp message sent successfully to %s", phoneNumber);
            } else {
                LOG.errorf("WhatsApp API error: %d - %s", response.statusCode(), response.body());
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send WhatsApp message to %s", phoneNumber);
            throw new RuntimeException("Failed to send WhatsApp message", e);
        }
    }
    
    private String getWhatsAppApiToken() {
        if (cachedApiToken != null) {
            return cachedApiToken;
        }
        
        try {
            GetSecretValueRequest secretRequest = GetSecretValueRequest.builder()
                    .secretId(apiKeysSecretName)
                    .build();
            
            GetSecretValueResponse secretResponse = secretsManagerClient.getSecretValue(secretRequest);
            String secretString = secretResponse.secretString();
            
            JsonNode secretJson = objectMapper.readTree(secretString);
            cachedApiToken = secretJson.get("WHATSAPP_API_TOKEN").asText();
            
            LOG.info("WhatsApp API token retrieved from AWS Secrets Manager");
            return cachedApiToken;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve WhatsApp API token from AWS Secrets Manager");
            return null;
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