package com.notificamy.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ChatGptResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @NoArgsConstructor
    public static class Choice {
        private int index;
        private Message message;
        
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        
        @JsonProperty("completion_tokens")
        private int completionTokens;
        
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}