package com.notificamy.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@ApplicationScoped
public class DatabaseConfig {
    
    private static final Logger LOG = Logger.getLogger(DatabaseConfig.class);
    
    @Inject
    SecretsManagerClient secretsManagerClient;
    
    @ConfigProperty(name = "app.aws.secrets.database")
    String databaseSecretName;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private DatabaseCredentials cachedCredentials;
    
    @Produces
    @ApplicationScoped
    public DataSource dataSource() {
        try {
            DatabaseCredentials credentials = getDatabaseCredentials();
            
            // Parse DB_URL to extract host, port, and database name
            // Format: jdbc:mysql://host:port/database?params
            String dbUrl = credentials.dbUrl;
            String host = extractHostFromUrl(dbUrl);
            int port = extractPortFromUrl(dbUrl);
            String dbname = extractDatabaseFromUrl(dbUrl);
            
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=true&requireSSL=false&serverTimezone=UTC",
                    host, port, dbname);
            
            LOG.infof("Connecting to database: %s:%d/%s", host, port, dbname);
            
            // Set system properties for Quarkus datasource
            System.setProperty("quarkus.datasource.jdbc.url", jdbcUrl);
            System.setProperty("quarkus.datasource.username", credentials.username);
            System.setProperty("quarkus.datasource.password", credentials.password);
            
            return new CustomDataSource(jdbcUrl, credentials.username, credentials.password);
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to configure database connection");
            throw new RuntimeException("Database configuration failed", e);
        }
    }
    
    private DatabaseCredentials getDatabaseCredentials() {
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
    
    private static class DatabaseCredentials {
        final String dbUrl;
        final String username;
        final String password;
        
        DatabaseCredentials(String dbUrl, String username, String password) {
            this.dbUrl = dbUrl;
            this.username = username;
            this.password = password;
        }
    }
    
    private static class CustomDataSource implements DataSource {
        private final String url;
        private final String username;
        private final String password;
        
        CustomDataSource(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }
        
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }
        
        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }
        
        // Other DataSource methods with default implementations
        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException { return null; }
        
        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {}
        
        @Override
        public void setLoginTimeout(int seconds) throws SQLException {}
        
        @Override
        public int getLoginTimeout() throws SQLException { return 0; }
        
        @Override
        public java.util.logging.Logger getParentLogger() { return null; }
        
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
    }
}