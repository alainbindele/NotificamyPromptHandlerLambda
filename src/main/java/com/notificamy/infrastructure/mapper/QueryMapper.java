package com.notificamy.infrastructure.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.Query;
import com.notificamy.infrastructure.persistence.entity.QueryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface QueryMapper {
    
    @Mapping(target = "enabledChannels", source = "enabledChannels", qualifiedByName = "stringToChannelSet")
    Query toDomain(QueryEntity entity);
    
    @Mapping(target = "enabledChannels", source = "enabledChannels", qualifiedByName = "channelSetToString")
    @Mapping(target = "user", ignore = true)
    QueryEntity toEntity(Query domain);
    
    @Named("stringToChannelSet")
    default Set<NotificationChannel> stringToChannelSet(String channelsJson) {
        if (channelsJson == null || channelsJson.isEmpty()) {
            return Set.of(NotificationChannel.EMAIL); // Default to email
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> channelNames = mapper.readValue(channelsJson, new TypeReference<List<String>>() {});
            return channelNames.stream()
                    .map(channelName -> {
                        try {
                            // Map channel names to enum values (case-insensitive)
                            return mapChannelName(channelName);
                        } catch (IllegalArgumentException e) {
                            // Log warning for invalid channel names
                            System.err.println("Warning: Invalid channel name '" + channelName + "' in enabled_channels after mapping, skipping");
                            return null;
                        }
                    })
                    .filter(channel -> channel != null)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            System.err.println("Error parsing enabled_channels JSON: " + e.getMessage());
            return Set.of(NotificationChannel.EMAIL); // Fallback to email
        }
    }
    
    @Named("mapChannelName")
    default NotificationChannel mapChannelName(String channelName) {
        if (channelName == null || channelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty");
        }
        
        String normalizedName = channelName.trim().toUpperCase();
        
        // Map common variations to standard enum values
        return switch (normalizedName) {
            case "EMAIL", "MAIL", "E-MAIL" -> NotificationChannel.EMAIL;
            case "WHATSAPP", "WHATS_APP", "WHATS-APP", "WA" -> NotificationChannel.WHATSAPP;
            case "SLACK" -> NotificationChannel.SLACK;
            case "DISCORD" -> NotificationChannel.DISCORD;
            default -> {
                System.err.println("Warning: Unknown channel name '" + channelName + "' (normalized: '" + normalizedName + "'), trying direct enum mapping");
                yield NotificationChannel.valueOf(normalizedName);
            }
        };
    }
    
    @Named("channelSetToString")
    default String channelSetToString(Set<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return "[\"EMAIL\"]";
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> channelNames = channels.stream()
                    .map(NotificationChannel::name)
                    .collect(Collectors.toList());
            return mapper.writeValueAsString(channelNames);
        } catch (Exception e) {
            return "[\"EMAIL\"]"; // Fallback
        }
    }
}