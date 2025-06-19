package com.notificamy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SqsMessage {
    
    @JsonProperty("query_id")
    private Long queryId;
    
    @JsonProperty("prompt")
    private String prompt;

    // Constructors
    public SqsMessage() {}

    public SqsMessage(Long queryId, String prompt) {
        this.queryId = queryId;
        this.prompt = prompt;
    }

    // Getters and Setters
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    @Override
    public String toString() {
        return "SqsMessage{" +
                "queryId=" + queryId +
                ", prompt='" + prompt + '\'' +
                '}';
    }
}