package com.notificamy.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class AwsConfig {
    
    @ConfigProperty(name = "quarkus.ses.aws.region")
    String region;
    
    @ConfigProperty(name = "quarkus.ses.endpoint-override")
    Optional<String> sesEndpointOverride;
    
    @ConfigProperty(name = "quarkus.sqs.endpoint-override")
    Optional<String> sqsEndpointOverride;
    
    @Produces
    @ApplicationScoped
    public SesClient sesClient() {
        var builder = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
        
        sesEndpointOverride.ifPresent(endpoint -> builder.endpointOverride(URI.create(endpoint)));
        
        return builder.build();
    }
    
    @Produces
    @ApplicationScoped
    public SqsClient sqsClient() {
        var builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
        
        sqsEndpointOverride.ifPresent(endpoint -> builder.endpointOverride(URI.create(endpoint)));
        
        return builder.build();
    }
}