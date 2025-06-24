package com.notificamy.infrastructure.mapper;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.User;
import com.notificamy.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "cdi")
public interface UserMapper {
    
    @Mapping(target = "channelConfigurations", source = ".", qualifiedByName = "buildChannelConfigurations")
    User toDomain(UserEntity entity);
    
    @Mapping(target = "whatsappPhone", ignore = true)
    @Mapping(target = "slackWebhook", ignore = true)
    @Mapping(target = "discordWebhook", ignore = true)
    @Mapping(target = "queries", ignore = true)
    UserEntity toEntity(User domain);
    
    @Named("buildChannelConfigurations")
    default Map<NotificationChannel, String> buildChannelConfigurations(UserEntity entity) {
        Map<NotificationChannel, String> configurations = new HashMap<>();
        
        if (entity.getEmail() != null) {
            configurations.put(NotificationChannel.EMAIL, entity.getEmail());
        }
        if (entity.getWhatsappPhone() != null) {
            configurations.put(NotificationChannel.WHATSAPP, entity.getWhatsappPhone());
        }
        if (entity.getSlackWebhook() != null) {
            configurations.put(NotificationChannel.SLACK, entity.getSlackWebhook());
        }
        if (entity.getDiscordWebhook() != null) {
            configurations.put(NotificationChannel.DISCORD, entity.getDiscordWebhook());
        }
        
        return configurations;
    }
}