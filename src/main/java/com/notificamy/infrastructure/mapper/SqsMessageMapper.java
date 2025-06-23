package com.notificamy.infrastructure.mapper;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.User;
import com.notificamy.infrastructure.external.dto.SqsMessage;
import org.mapstruct.Mapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mapper(componentModel = "cdi")
public interface SqsMessageMapper {
    
    default Long extractQueryId(SqsMessage sqsMessage) {
        return sqsMessage.getQueryId();
    }
    
    default String extractPrompt(SqsMessage sqsMessage) {
        return sqsMessage.getPrompt();
    }
    
    default User extractUser(SqsMessage sqsMessage) {
        User user = new User();
        user.setEmail(sqsMessage.getUserEmail());
        user.setName(extractNameFromEmail(sqsMessage.getUserEmail()));
        
        // Build channel configurations
        Map<NotificationChannel, String> channelConfigurations = new HashMap<>();
        
        if (sqsMessage.getUserEmail() != null && !sqsMessage.getUserEmail().isEmpty()) {
            channelConfigurations.put(NotificationChannel.EMAIL, sqsMessage.getUserEmail());
        }
        
        if (sqsMessage.getUserPhone() != null && !sqsMessage.getUserPhone().isEmpty()) {
            channelConfigurations.put(NotificationChannel.WHATSAPP, sqsMessage.getUserPhone());
        }
        
        if (sqsMessage.getUserSlackWebhook() != null && !sqsMessage.getUserSlackWebhook().isEmpty()) {
            channelConfigurations.put(NotificationChannel.SLACK, sqsMessage.getUserSlackWebhook());
        }
        
        if (sqsMessage.getUserDiscordWebhook() != null && !sqsMessage.getUserDiscordWebhook().isEmpty()) {
            channelConfigurations.put(NotificationChannel.DISCORD, sqsMessage.getUserDiscordWebhook());
        }
        
        user.setChannelConfigurations(channelConfigurations);
        return user;
    }
    
    default Set<NotificationChannel> extractEnabledChannels(SqsMessage sqsMessage) {
        Set<NotificationChannel> channels = new HashSet<>();
        
        if (sqsMessage.getUserEmail() != null && !sqsMessage.getUserEmail().isEmpty()) {
            channels.add(NotificationChannel.EMAIL);
        }
        
        if (sqsMessage.getUserPhone() != null && !sqsMessage.getUserPhone().isEmpty()) {
            channels.add(NotificationChannel.WHATSAPP);
        }
        
        if (sqsMessage.getUserSlackWebhook() != null && !sqsMessage.getUserSlackWebhook().isEmpty()) {
            channels.add(NotificationChannel.SLACK);
        }
        
        if (sqsMessage.getUserDiscordWebhook() != null && !sqsMessage.getUserDiscordWebhook().isEmpty()) {
            channels.add(NotificationChannel.DISCORD);
        }
        
        return channels;
    }
    
    default String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }
        
        String localPart = email.substring(0, email.indexOf("@"));
        // Convert dots and underscores to spaces and capitalize
        String result = localPart.replace(".", " ").replace("_", " ").toLowerCase();
        
        // Capitalize first letter of each word
        StringBuilder capitalized = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : result.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                capitalized.append(c);
            } else if (capitalizeNext) {
                capitalized.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                capitalized.append(c);
            }
        }
        
        return capitalized.toString();
    }
}