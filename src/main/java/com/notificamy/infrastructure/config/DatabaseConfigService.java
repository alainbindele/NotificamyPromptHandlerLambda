package com.notificamy.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DatabaseConfigService {
    
    private static final Logger LOG = Logger.getLogger(DatabaseConfigService.class);
    
    @Inject
    SecretsManagerService secretsManager;
    
    @ConfigProperty(name = "app.aws.secrets.database-secret-name")
    String databaseSecretName;
    
    private String cachedJdbcUrl;
    private String cachedUsername;
    private String cachedPassword;
    
    public String getJdbcUrl() {
        if (cachedJdbcUrl == null) {
            cachedJdbcUrl = secretsManager.getSecretValue(databaseSecretName, "DB_URL");
            if (cachedJdbcUrl != null) {
                // Set system property for Quarkus to pick up
                System.setProperty("DB_URL", cachedJdbcUrl);
                LOG.infof("Database URL retrieved from secrets manager");
            }
        }
        return cachedJdbcUrl;
    }
    
    public String getUsername() {
        if (cachedUsername == null) {
            cachedUsername = secretsManager.getSecretValue(databaseSecretName, "DB_USER");
            if (cachedUsername != null) {
                // Set system property for Quarkus to pick up
                System.setProperty("DB_USERNAME", cachedUsername);
                LOG.infof("Database username retrieved from secrets manager");
            }
        }
        return cachedUsername;
    }
    
    public String getPassword() {
        if (cachedPassword == null) {
            cachedPassword = secretsManager.getSecretValue(databaseSecretName, "DB_PASSWORD");
            if (cachedPassword != null) {
                // Set system property for Quarkus to pick up
                System.setProperty("DB_PASSWORD", cachedPassword);
                LOG.infof("Database password retrieved from secrets manager");
            }
        }
        return cachedPassword;
    }
    
    // Initialize database configuration on startup
    public void initializeDatabaseConfig() {
        getJdbcUrl();
        getUsername();
        getPassword();
    }
}