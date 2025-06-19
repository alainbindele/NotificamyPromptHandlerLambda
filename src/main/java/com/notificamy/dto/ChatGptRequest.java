package com.notificamy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatGptRequest {
    
    @JsonProperty("policy")
    private String policy;
    
    @JsonProperty("client_prompt")
    private String clientPrompt;

    public ChatGptRequest() {}

    public ChatGptRequest(String policy, String clientPrompt) {
        this.policy = policy;
        this.clientPrompt = clientPrompt;
    }

    public String getPolicy() { return policy; }
    public void setPolicy(String policy) { this.policy = policy; }

    public String getClientPrompt() { return clientPrompt; }
    public void setClientPrompt(String clientPrompt) { this.clientPrompt = clientPrompt; }
}