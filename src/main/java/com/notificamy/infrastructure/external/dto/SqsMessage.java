package com.notificamy.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqsMessage {
    @JsonProperty("query_id")
    private Long queryId;
    
    @JsonProperty("prompt")
    private String prompt;
}