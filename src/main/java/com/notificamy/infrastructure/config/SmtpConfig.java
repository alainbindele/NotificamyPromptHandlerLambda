package com.notificamy.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@ApplicationScoped
@Startup
@Getter
public class SmtpConfig {
    
    private static final Logger LOG = Logger.getLogger(SmtpConfig.class);
    
    @Inject
    SecretsManagerClient secretsManagerClient;
    
    @ConfigProperty(name = "app.aws.secrets.smtp")
    String smtpSecretName;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String fromEmail;
    private boolean smtpAuth;
    private boolean smtpStartTls;
    
    @PostConstruct
    public void loadSmtpConfiguration() {
        try {
            LOG.infof("Loading SMTP configuration from AWS Secrets Manager: %s", smtpSecretName);
            
            GetSecretValueRequest secretRequest = GetSecretValueRequest.builder()
                    .secretId(smtpSecretName)
                    .build();
            
            GetSecretValueResponse secretResponse = secretsManagerClient.getSecretValue(secretRequest);
            String secretString = secretResponse.secretString();
            
            JsonNode secretJson = objectMapper.readTree(secretString);
            
            // Carica le configurazioni SMTP
            this.smtpHost = secretJson.get("SMTP_HOST").asText();
            this.smtpPort = secretJson.get("SMTP_PORT").asInt(587);
            this.smtpUsername = secretJson.get("SMTP_USERNAME").asText();
            this.smtpPassword = secretJson.get("SMTP_PASSWORD").asText();
            this.fromEmail = secretJson.get("FROM_EMAIL").asText();
            this.smtpAuth = secretJson.has("SMTP_AUTH") ? secretJson.get("SMTP_AUTH").asBoolean() : true;
            this.smtpStartTls = secretJson.has("SMTP_START_TLS") ? secretJson.get("SMTP_START_TLS").asBoolean() : true;
            
            LOG.infof("SMTP configuration loaded successfully - Host: %s, Port: %d, From: %s, Auth: %s, StartTLS: %s", 
                    smtpHost, smtpPort, fromEmail, smtpAuth, smtpStartTls);
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load SMTP configuration from AWS Secrets Manager");
            throw new RuntimeException("Failed to load SMTP configuration", e);
        }
    }
}