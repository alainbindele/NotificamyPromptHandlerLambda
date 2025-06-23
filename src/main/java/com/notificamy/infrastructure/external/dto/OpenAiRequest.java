package com.notificamy.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpenAiRequest {
    private String model = "gpt-3.5-turbo";
    private Message[] messages;
    
    @JsonProperty("max_tokens")
    private int maxTokens = 500;
    
    private double temperature = 0.7;

    @Data
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public OpenAiRequest(String systemPrompt, String userPrompt) {
        this.messages = new Message[]{
            new Message("system", systemPrompt),
            new Message("user", userPrompt)
        };
    }
}