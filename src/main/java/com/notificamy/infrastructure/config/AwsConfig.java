package com.notificamy.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;

@ApplicationScoped
public class AwsConfig {
    
    @ConfigProperty(name = "quarkus.aws.region")
    String awsRegion;
    
    @Produces
    @ApplicationScoped
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .socketTimeout(Duration.ofSeconds(60))
                        .connectionTimeout(Duration.ofSeconds(15)))
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
    
    @Produces
    @ApplicationScoped
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .socketTimeout(Duration.ofSeconds(60))
                        .connectionTimeout(Duration.ofSeconds(15)))
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}