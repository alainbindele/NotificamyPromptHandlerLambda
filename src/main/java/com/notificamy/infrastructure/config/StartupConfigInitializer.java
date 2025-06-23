package com.notificamy.infrastructure.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StartupConfigInitializer {
    
    private static final Logger LOG = Logger.getLogger(StartupConfigInitializer.class);
    
    @Inject
    DatabaseConfigService databaseConfig;
    
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Initializing configuration from AWS Secrets Manager...");
        
        try {
            // Initialize database configuration
            databaseConfig.initializeDatabaseConfig();
            LOG.info("Configuration initialization completed successfully");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize configuration from Secrets Manager");
            // Don't fail startup - let the application try to continue
        }
    }
}