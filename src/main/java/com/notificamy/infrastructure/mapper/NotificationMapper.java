package com.notificamy.infrastructure.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRecord;
import com.notificamy.domain.model.NotificationStatus;
import com.notificamy.infrastructure.persistence.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface NotificationMapper {
    
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "channelsAttempted", source = "channelsAttempted", qualifiedByName = "stringToChannelSet")
    @Mapping(target = "channelsSuccessful", source = "channelsSuccessful", qualifiedByName = "stringToChannelSet")
    NotificationRecord toDomain(NotificationEntity entity);
    
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "channelsAttempted", source = "channelsAttempted", qualifiedByName = "channelSetToString")
    @Mapping(target = "channelsSuccessful", source = "channelsSuccessful", qualifiedByName = "channelSetToString")
    NotificationEntity toEntity(NotificationRecord domain);
    
    @Named("stringToStatus")
    default NotificationStatus stringToStatus(String status) {
        if (status == null || status.isEmpty()) {
            return NotificationStatus.PENDING;
        }
        try {
            return NotificationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return NotificationStatus.ERROR;
        }
    }
    
    @Named("statusToString")
    default String statusToString(NotificationStatus status) {
        return status != null ? status.name() : NotificationStatus.PENDING.name();
    }
    
    @Named("stringToChannelSet")
    default Set<NotificationChannel> stringToChannelSet(String channelsJson) {
        if (channelsJson == null || channelsJson.isEmpty()) {
            return Set.of();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> channelNames = mapper.readValue(channelsJson, new TypeReference<List<String>>() {});
            return channelNames.stream()
                    .map(NotificationChannel::valueOf)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of();
        }
    }
    
    @Named("channelSetToString")
    default String channelSetToString(Set<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return "[]";
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> channelNames = channels.stream()
                    .map(NotificationChannel::name)
                    .collect(Collectors.toList());
            return mapper.writeValueAsString(channelNames);
        } catch (Exception e) {
            return "[]";
        }
    }
}