package com.notificamy.infrastructure.adapter.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRecord;
import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.domain.model.NotificationStatus;
import com.notificamy.domain.port.NotificationRecordPort;
import com.notificamy.infrastructure.mapper.NotificationMapper;
import com.notificamy.infrastructure.persistence.entity.NotificationEntity;
import com.notificamy.infrastructure.persistence.repository.NotificationEntityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationRecordAdapter implements NotificationRecordPort {
    
    private static final Logger LOG = Logger.getLogger(NotificationRecordAdapter.class);
    
    @Inject
    NotificationEntityRepository notificationRepository;
    
    @Inject
    NotificationMapper notificationMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public NotificationRecord createNotificationRecord(NotificationRequest request) {
        try {
            // Serialize channels to JSON
            List<String> channelNames = request.getChannels().stream()
                    .map(NotificationChannel::name)
                    .collect(Collectors.toList());
            
            NotificationEntity entity = NotificationEntity.builder()
                    .userId(request.getUser().getId())
                    .queryId(request.getQueryId())
                    .status(NotificationStatus.PENDING.name())
                    .subject("Notificamy: Your AI-Generated Notification")
                    .content(request.getAiResponse())
                    .sentAt(LocalDateTime.now())
                    .channelsAttempted(objectMapper.writeValueAsString(channelNames))
                    .channelsSuccessful("[]") // Initially empty
                    .build();
            
            notificationRepository.persist(entity);
            
            LOG.infof("Created notification record with ID: %d for query: %d", 
                    entity.getId(), request.getQueryId());
            
            return notificationMapper.toDomain(entity);
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to create notification record for query: %d", request.getQueryId());
            throw new RuntimeException("Failed to create notification record", e);
        }
    }
    
    @Override
    @Transactional
    public void updateNotificationStatus(Long notificationId, NotificationStatus status, 
                                       Set<NotificationChannel> successfulChannels, String errorMessage) {
        try {
            NotificationEntity entity = notificationRepository.findById(notificationId);
            if (entity == null) {
                LOG.warnf("Notification record not found: %d", notificationId);
                return;
            }
            
            entity.setStatus(status.name());
            entity.setUpdatedAt(LocalDateTime.now());
            
            if (successfulChannels != null && !successfulChannels.isEmpty()) {
                List<String> channelNames = successfulChannels.stream()
                        .map(NotificationChannel::name)
                        .collect(Collectors.toList());
                entity.setChannelsSuccessful(objectMapper.writeValueAsString(channelNames));
            }
            
            if (errorMessage != null) {
                entity.setErrorMessage(errorMessage);
            }
            
            // Update sent_at timestamp for successful notifications
            if (status == NotificationStatus.SUCCESS || status == NotificationStatus.PARTIAL) {
                entity.setSentAt(LocalDateTime.now());
            }
            
            notificationRepository.persist(entity);
            
            LOG.infof("Updated notification record %d with status: %s", notificationId, status);
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to update notification record: %d", notificationId);
        }
    }
    
    @Override
    @Transactional
    public void incrementRetryCount(Long notificationId) {
        try {
            NotificationEntity entity = notificationRepository.findById(notificationId);
            if (entity != null) {
                entity.setRetryCount(entity.getRetryCount() + 1);
                entity.setUpdatedAt(LocalDateTime.now());
                notificationRepository.persist(entity);
                
                LOG.infof("Incremented retry count for notification %d to %d", 
                        notificationId, entity.getRetryCount());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to increment retry count for notification: %d", notificationId);
        }
    }
}