package com.notificamy.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ApiKeysConfigService {
    
    private static final Logger LOG = Logger.getLogger(ApiKeysConfigService.class);
    
    @Inject
    SecretsManagerService secretsManager;
    
    @ConfigProperty(name = "app.aws.secrets.api-keys-secret-name")
    String apiKeysSecretName;
    
    private String cachedOpenAiApiKey;
    private String cachedWhatsAppApiToken;
    
    public String getOpenAiApiKey() {
        if (cachedOpenAiApiKey == null) {
            cachedOpenAiApiKey = secretsManager.getSecretValue(apiKeysSecretName, "OPENAI_API_KEY");
            LOG.infof("OpenAI API key retrieved from secrets manager");
        }
        return cachedOpenAiApiKey;
    }
    
    public String getWhatsAppApiToken() {
        if (cachedWhatsAppApiToken == null) {
            cachedWhatsAppApiToken = secretsManager.getSecretValue(apiKeysSecretName, "WHATSAPP_API_TOKEN");
            LOG.infof("WhatsApp API token retrieved from secrets manager");
        }
        return cachedWhatsAppApiToken;
    }
}