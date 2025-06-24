package com.notificamy.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAiRequest {
    
    private String model = "gpt-3.5-turbo";
    private Message[] messages;
    
    @JsonProperty("max_tokens")
    private int maxTokens;
    
    private double temperature;

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
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

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Message[] getMessages() { return messages; }
    public void setMessages(Message[] messages) { this.messages = messages; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}