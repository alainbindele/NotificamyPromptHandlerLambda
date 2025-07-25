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
    
    @ConfigProperty(name = "app.aws.secrets.api-keys")
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
            LOG.warnf("WhatsApp phone number not configured for user ID %d (%s), skipping WhatsApp notification", 
                    request.getUser().getId(), request.getUser().getEmail());
            throw new RuntimeException("WhatsApp phone number not configured for user");
        }
        
        LOG.infof("WhatsApp notification requested for user %s (ID: %d) for query %d - Implementation not fully active", 
                request.getUser().getEmail(), request.getUser().getId(), request.getQueryId());
        
        // Per ora lanciamo un'eccezione per indicare che WhatsApp non è completamente implementato
        throw new RuntimeException("WhatsApp notifications are not fully implemented yet");
        
        /*
        // Implementazione completa WhatsApp (commentata per ora)
        try {
            String apiToken = getWhatsAppApiToken();
            if (apiToken == null || apiToken.isEmpty()) {
                LOG.error("WhatsApp API token not found in secrets");
                throw new RuntimeException("WhatsApp API token not configured");
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
                LOG.infof("WhatsApp message sent successfully to %s for user %s", phoneNumber, request.getUser().getEmail());
            } else {
                LOG.errorf("WhatsApp API error for user %s: %d - %s", 
                        request.getUser().getEmail(), response.statusCode(), response.body());
                throw new RuntimeException("WhatsApp API returned error: " + response.statusCode());
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send WhatsApp message to %s for user %s", phoneNumber, request.getUser().getEmail());
            throw new RuntimeException("Failed to send WhatsApp message", e);
        }
        */
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