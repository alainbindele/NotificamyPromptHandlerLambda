package com.notificamy.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OpenAiRequest {
    
    private String model = "gpt-3.5-turbo";
    private Message[] messages;
    
    @JsonProperty("max_tokens")
    private int maxTokens;
    
    private double temperature;

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public OpenAiRequest(String systemPrompt, String userPrompt, int maxTokens, double temperature) {
        this.messages = new Message[]{
            new Message("system", systemPrompt),
            new Message("user", userPrompt)
        };
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    // Costruttore di compatibilità (deprecato)
    @Deprecated
    public OpenAiRequest(String systemPrompt, String userPrompt) {
        this(systemPrompt, userPrompt, 2000, 0.7);
    }
}