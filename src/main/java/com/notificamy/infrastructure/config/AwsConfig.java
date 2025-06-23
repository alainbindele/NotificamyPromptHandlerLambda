package com.notificamy.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class AwsConfig {
    
    @ConfigProperty(name = "app.aws.region")
    String awsRegion;
    
    @Produces
    @ApplicationScoped
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
    
    @Produces
    @ApplicationScoped
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
    
    @Produces
    @ApplicationScoped
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}