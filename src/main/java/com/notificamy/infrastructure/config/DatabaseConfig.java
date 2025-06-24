package com.notificamy.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@ApplicationScoped
public class DatabaseConfig {
    
    private static final Logger LOG = Logger.getLogger(DatabaseConfig.class);
    
    @Inject
    SecretsManagerClient secretsManagerClient;
    
    @ConfigProperty(name = "app.aws.secrets.database")
    String databaseSecretName;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private DatabaseCredentials cachedCredentials;
    
    @PostConstruct
    public void init() {
        try {
            DatabaseCredentials credentials = getDatabaseCredentials();
            
            // Parse DB_URL to extract components
            String dbUrl = credentials.dbUrl;
            String host = extractHostFromUrl(dbUrl);
            int port = extractPortFromUrl(dbUrl);
            String dbname = extractDatabaseFromUrl(dbUrl);
            
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=true&requireSSL=false&serverTimezone=UTC",
                    host, port, dbname);
            
            LOG.infof("Database configuration loaded: %s:%d/%s", host, port, dbname);
            
            // Set system properties for Quarkus datasource
            System.setProperty("quarkus.datasource.jdbc.url", jdbcUrl);
            System.setProperty("quarkus.datasource.username", credentials.username);
            System.setProperty("quarkus.datasource.password", credentials.password);
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to configure database connection - using fallback configuration");
            // Don't throw exception to allow Lambda to start
        }
    }
    
    public DatabaseCredentials getDatabaseCredentials() {
        if (cachedCredentials != null) {
            return cachedCredentials;
        }
        
        try {
            GetSecretValueRequest secretRequest = GetSecretValueRequest.builder()
                    .secretId(databaseSecretName)
                    .build();
            
            GetSecretValueResponse secretResponse = secretsManagerClient.getSecretValue(secretRequest);
            String secretString = secretResponse.secretString();
            
            JsonNode secretJson = objectMapper.readTree(secretString);
            
            cachedCredentials = new DatabaseCredentials(
                    secretJson.get("DB_URL").asText(),
                    secretJson.get("DB_USER").asText(),
                    secretJson.get("DB_PASSWORD").asText()
            );
            
            LOG.info("Database credentials retrieved from AWS Secrets Manager");
            return cachedCredentials;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve database credentials from AWS Secrets Manager");
            throw new RuntimeException("Failed to get database credentials", e);
        }
    }
    
    private String extractHostFromUrl(String dbUrl) {
        // Extract host from jdbc:mysql://host:port/database
        String[] parts = dbUrl.split("://")[1].split("/")[0].split(":");
        return parts[0];
    }
    
    private int extractPortFromUrl(String dbUrl) {
        // Extract port from jdbc:mysql://host:port/database
        String[] parts = dbUrl.split("://")[1].split("/")[0].split(":");
        return parts.length > 1 ? Integer.parseInt(parts[1]) : 3306;
    }
    
    private String extractDatabaseFromUrl(String dbUrl) {
        // Extract database name from jdbc:mysql://host:port/database?params
        String afterHost = dbUrl.split("://")[1].split("/", 2)[1];
        return afterHost.split("\\?")[0];
    }
    
    public static class DatabaseCredentials {
        public final String dbUrl;
        public final String username;
        public final String password;
        
        public DatabaseCredentials(String dbUrl, String username, String password) {
            this.dbUrl = dbUrl;
            this.username = username;
            this.password = password;
        }
    }
}