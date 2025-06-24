package com.notificamy.infrastructure.adapter.ai;

import com.notificamy.domain.port.AiServicePort;
import com.notificamy.infrastructure.external.dto.ChatGptResponse;
import com.notificamy.infrastructure.external.dto.OpenAiRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class ChatGptAdapter implements AiServicePort {
    
    private static final Logger LOG = Logger.getLogger(ChatGptAdapter.class);
    
    @Inject
    SecretsManagerClient secretsManagerClient;
    
    @ConfigProperty(name = "app.aws.secrets.api-keys")
    String apiKeysSecretName;
    
    @ConfigProperty(name = "app.openai.api-url")
    String apiUrl;
    
    @ConfigProperty(name = "app.openai.policy")
    String chatGptPolicy;
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private String cachedApiKey;
    
    public ChatGptAdapter() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public String processPrompt(String prompt) {
        try {
            String apiKey = getOpenAiApiKey();
            
            LOG.infof("OpenAI API Key retrieved: %s", apiKey != null ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) : "null");
            
            if (apiKey == null || apiKey.isEmpty()) {
                LOG.error("OpenAI API key not found in secrets");
                return "Sorry, the AI service is currently unavailable.";
            }
            
            String policy = buildPolicy();
            policy = "Fai parte di un servizio di notifica online. Il prompt che ricevi " +
                    "è un prompt riguardante qualcosa di cui voler essere notificato" +
                    "ES. i risultati di una partita di calcio" +
                    "ES. le ultime notizie internazionali" +
                    "Devi leggere il prompt, effettuare una ricerca sul web (se serve)" +
                    "e restituire esclusivamente un file HTML+CSS con un contenuto che possa " +
                    "essere presentato per email oppure discord, oppure whatsapp oppure slack" +
                    "quindi deve essere chiaro e ben paragrafato se necessario, può contenere qualche commento" +
                    "dentro l'html che possa essere utile all'utente." +
                    "Può contenere link web delle risorse da cui sono prese le informazioni " +
                    "(se presenti aggiungi infondo anche una bibliografia)";
            OpenAiRequest request = new OpenAiRequest(policy, prompt);
            
            LOG.infof("Sending request to ChatGPT for prompt: %s", prompt);
            LOG.infof("Using policy: %s", policy.length() > 100 ? policy.substring(0, 100) + "..." : policy);
            
            String requestBody = objectMapper.writeValueAsString(request);
            LOG.debugf("Request body: %s", requestBody);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            LOG.infof("ChatGPT API response status: %d", response.statusCode());
            LOG.debugf("ChatGPT API response body: %s", response.body());
            
            if (response.statusCode() == 200) {
                ChatGptResponse chatGptResponse = objectMapper.readValue(response.body(), ChatGptResponse.class);
                
                if (chatGptResponse.getChoices() != null && !chatGptResponse.getChoices().isEmpty()) {
                    ChatGptResponse.Message message = chatGptResponse.getChoices().get(0).getMessage();
                    
                    // Controlla se c'è un refusal (rifiuto da parte di OpenAI)
                    if (message.getRefusal() != null && !message.getRefusal().isEmpty()) {
                        LOG.warnf("OpenAI refused the request: %s", message.getRefusal());
                        return "I'm sorry, but I cannot process this request due to content policy restrictions.";
                    }
                    
                    String content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        LOG.infof("ChatGPT response received successfully");
                        return content;
                    } else {
                        LOG.error("Empty content in ChatGPT response");
                        return "Sorry, I couldn't generate a proper response at this time.";
                    }
                } else {
                    LOG.error("Empty choices in ChatGPT response");
                    return "Sorry, I couldn't process your request at this time.";
                }
            } else {
                LOG.errorf("ChatGPT API error: %d - %s", response.statusCode(), response.body());
                return "Sorry, there was an error processing your request.";
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Error calling ChatGPT API");
            return "Sorry, there was an error processing your request.";
        }
    }
    
    private String getOpenAiApiKey() {
        if (cachedApiKey != null) {
            return cachedApiKey;
        }
        
        try {
            GetSecretValueRequest secretRequest = GetSecretValueRequest.builder()
                    .secretId(apiKeysSecretName)
                    .build();
            
            GetSecretValueResponse secretResponse = secretsManagerClient.getSecretValue(secretRequest);
            String secretString = secretResponse.secretString();
            
            JsonNode secretJson = objectMapper.readTree(secretString);
            cachedApiKey = secretJson.get("OPENAI_API_KEY").asText();
            
            LOG.info("OpenAI API key retrieved from AWS Secrets Manager");
            return cachedApiKey;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve OpenAI API key from AWS Secrets Manager");
            return null;
        }
    }
    
    private String buildPolicy() {
        // Se la policy è configurata e non è il placeholder, usala
        if (chatGptPolicy != null && !chatGptPolicy.equals("POLICY TEXT") && !chatGptPolicy.trim().isEmpty()) {
            LOG.infof("Using custom ChatGPT policy from configuration");
            return chatGptPolicy;
        }
        
        // Altrimenti usa la policy di default
        LOG.infof("Using default ChatGPT policy");
        return getDefaultPolicy();
    }
    
    private String getDefaultPolicy() {
        return """
                You are an AI assistant for Notificamy, a smart notification service. 
                Your role is to help users create intelligent notification rules based on their requests.
                
                Rules:
                1. Always respond in a helpful and professional manner
                2. Focus on notification-related tasks (email, WhatsApp, Slack, Discord)
                3. Help users define when, how, and what they want to be notified about
                4. Suggest appropriate notification schedules (periodic, specific dates/times)
                5. If the request is not notification-related, politely redirect to notification use cases
                6. Keep responses concise and actionable
                7. Always prioritize user privacy and security
                8. Format your response as if it's content for a notification across multiple channels
                
                Format your response as a structured notification plan when possible.
                """;
    }
}