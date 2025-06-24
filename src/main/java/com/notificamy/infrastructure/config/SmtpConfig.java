package com.notificamy.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@ApplicationScoped
@Startup
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
            
            LOG.infof("SMTP configuration loaded successfully - Host: %s, Port: %d, From: %s", 
                    smtpHost, smtpPort, fromEmail);
            
            // Configura le proprietà di sistema per Quarkus Mailer
            configureQuarkusMailer();
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load SMTP configuration from AWS Secrets Manager");
            throw new RuntimeException("Failed to load SMTP configuration", e);
        }
    }
    
    private void configureQuarkusMailer() {
        // Configura le proprietà di sistema che Quarkus Mailer utilizzerà
        System.setProperty("quarkus.mailer.host", smtpHost);
        System.setProperty("quarkus.mailer.port", String.valueOf(smtpPort));
        System.setProperty("quarkus.mailer.username", smtpUsername);
        System.setProperty("quarkus.mailer.password", smtpPassword);
        System.setProperty("quarkus.mailer.auth-methods", "DIGEST-MD5 CRAM-SHA256 CRAM-SHA1 CRAM-MD5 PLAIN LOGIN");
        System.setProperty("quarkus.mailer.from", fromEmail);
        
        if (smtpAuth) {
            System.setProperty("quarkus.mailer.login", "REQUIRED");
        }
        
        if (smtpStartTls) {
            System.setProperty("quarkus.mailer.start-tls", "REQUIRED");
        }
        
        LOG.info("Quarkus Mailer system properties configured successfully");
    }
    
    // Getters per accesso alle configurazioni
    public String getSmtpHost() { return smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public String getSmtpUsername() { return smtpUsername; }
    public String getSmtpPassword() { return smtpPassword; }
    public String getFromEmail() { return fromEmail; }
    public boolean isSmtpAuth() { return smtpAuth; }
    public boolean isSmtpStartTls() { return smtpStartTls; }
}