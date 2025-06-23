package com.notificamy.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SecretsManagerService {
    
    private static final Logger LOG = Logger.getLogger(SecretsManagerService.class);
    
    @Inject
    SecretsManagerClient secretsClient;
    
    @ConfigProperty(name = "app.aws.region")
    String awsRegion;
    
    private final ObjectMapper objectMapper;
    private final Map<String, JsonNode> secretsCache;
    
    public SecretsManagerService() {
        this.objectMapper = new ObjectMapper();
        this.secretsCache = new HashMap<>();
    }
    
    public String getSecretValue(String secretName, String key) {
        try {
            JsonNode secretJson = getSecretAsJson(secretName);
            if (secretJson.has(key)) {
                return secretJson.get(key).asText();
            } else {
                LOG.warnf("Key '%s' not found in secret '%s'", key, secretName);
                return null;
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve secret value for key '%s' from secret '%s'", key, secretName);
            return null;
        }
    }
    
    public JsonNode getSecretAsJson(String secretName) {
        // Check cache first
        if (secretsCache.containsKey(secretName)) {
            return secretsCache.get(secretName);
        }
        
        try {
            LOG.infof("Fetching secret: %s", secretName);
            
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse getSecretValueResponse = secretsClient.getSecretValue(getSecretValueRequest);
            String secretString = getSecretValueResponse.secretString();
            
            JsonNode secretJson = objectMapper.readTree(secretString);
            
            // Cache the result
            secretsCache.put(secretName, secretJson);
            
            LOG.infof("Successfully retrieved secret: %s", secretName);
            return secretJson;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve secret: %s", secretName);
            throw new RuntimeException("Failed to retrieve secret: " + secretName, e);
        }
    }
    
    public void clearCache() {
        secretsCache.clear();
        LOG.info("Secrets cache cleared");
    }
}