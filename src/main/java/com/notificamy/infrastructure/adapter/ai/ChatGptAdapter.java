package com.notificamy.infrastructure.adapter.ai;

import com.notificamy.domain.port.AiServicePort;
import com.notificamy.infrastructure.config.ApiKeysConfigService;
import com.notificamy.infrastructure.external.dto.ChatGptResponse;
import com.notificamy.infrastructure.external.dto.OpenAiRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChatGptAdapter implements AiServicePort {
    
    private static final Logger LOG = Logger.getLogger(ChatGptAdapter.class);
    
    @Inject
    ApiKeysConfigService apiKeysConfig;
    
    @ConfigProperty(name = "app.openai.api-url")
    String apiUrl;
    
    private final Client client;
    
    public ChatGptAdapter() {
        this.client = ClientBuilder.newClient();
    }
    
    @Override
    public String processPrompt(String prompt) {
        try {
            String apiKey = apiKeysConfig.getOpenAiApiKey();
            if (apiKey == null) {
                LOG.error("OpenAI API key not found in secrets manager");
                return "Sorry, the AI service is currently unavailable.";
            }
            
            String policy = buildPolicy();
            OpenAiRequest request = new OpenAiRequest(policy, prompt);
            
            LOG.infof("Sending request to ChatGPT for prompt: %s", prompt);
            
            Response response = client.target(apiUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(Entity.json(request));
            
            if (response.getStatus() == 200) {
                ChatGptResponse chatGptResponse = response.readEntity(ChatGptResponse.class);
                
                if (chatGptResponse.getChoices() != null && !chatGptResponse.getChoices().isEmpty()) {
                    String content = chatGptResponse.getChoices().get(0).getMessage().getContent();
                    LOG.infof("ChatGPT response received successfully");
                    return content;
                } else {
                    LOG.error("Empty response from ChatGPT");
                    return "Sorry, I couldn't process your request at this time.";
                }
            } else {
                LOG.errorf("ChatGPT API error: %d - %s", response.getStatus(), response.readEntity(String.class));
                return "Sorry, there was an error processing your request.";
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Error calling ChatGPT API");
            return "Sorry, there was an error processing your request.";
        }
    }
    
    private String buildPolicy() {
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