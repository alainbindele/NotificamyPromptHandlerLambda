package com.notificamy.infrastructure.mapper;

import com.notificamy.infrastructure.external.dto.SqsMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface SqsMessageMapper {
    
    // Simple mapping - no transformation needed for this case
    // but keeping the mapper for consistency and future extensions
    default Long extractQueryId(SqsMessage sqsMessage) {
        return sqsMessage.getQueryId();
    }
    
    default String extractPrompt(SqsMessage sqsMessage) {
        return sqsMessage.getPrompt();
    }
}