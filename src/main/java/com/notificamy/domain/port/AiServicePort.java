package com.notificamy.domain.port;

public interface AiServicePort {
    String processPrompt(String prompt);
    String processPrompt(String prompt, boolean isConditionalQuery);
}