package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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
    
    private final Client client;
    private final ObjectMapper objectMapper;
    private String cachedApiToken;
    
    public WhatsAppNotificationStrategy() {
        this.client = ClientBuilder.newClient();
        this.objectMapper = new ObjectMapper();
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
            
            Response response = client.target(whatsappApiUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .post(Entity.json(payload));
            
            if (response.getStatus() == 200) {
                LOG.infof("WhatsApp message sent successfully to %s", phoneNumber);
            } else {
                LOG.errorf("WhatsApp API error: %d - %s", response.getStatus(), response.readEntity(String.class));
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