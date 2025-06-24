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
                    .map(NotificationChannel::valueOf)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of(NotificationChannel.EMAIL); // Fallback to email
        }
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