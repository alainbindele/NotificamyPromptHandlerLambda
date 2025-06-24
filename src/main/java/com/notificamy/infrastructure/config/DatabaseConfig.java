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
            
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=true&requireSSL=false&serverTimezone=UTC",
                    credentials.host, credentials.port, credentials.dbname);
            
            LOG.infof("Connecting to database: %s", credentials.host);
            
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
                    secretJson.get("host").asText(),
                    secretJson.get("port").asInt(),
                    secretJson.get("dbname").asText(),
                    secretJson.get("username").asText(),
                    secretJson.get("password").asText()
            );
            
            LOG.info("Database credentials retrieved from AWS Secrets Manager");
            return cachedCredentials;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve database credentials from AWS Secrets Manager");
            throw new RuntimeException("Failed to get database credentials", e);
        }
    }
    
    private static class DatabaseCredentials {
        final String host;
        final int port;
        final String dbname;
        final String username;
        final String password;
        
        DatabaseCredentials(String host, int port, String dbname, String username, String password) {
            this.host = host;
            this.port = port;
            this.dbname = dbname;
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