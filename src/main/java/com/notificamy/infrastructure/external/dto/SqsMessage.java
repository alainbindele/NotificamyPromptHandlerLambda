package com.notificamy.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SqsMessage {
    
    @JsonProperty("query_id")
    private Long queryId;
    
    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("user_email")
    private String userEmail;

    @JsonProperty("user_discord_webhook")
    private String userDiscordWebhook;

    @JsonProperty("user_slack_webhook")
    private String userSlackWebhook;

    @JsonProperty("user_phone")
    private String userPhone;
}