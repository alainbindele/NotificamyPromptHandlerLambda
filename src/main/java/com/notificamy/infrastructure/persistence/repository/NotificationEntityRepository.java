package com.notificamy.infrastructure.persistence.repository;

import com.notificamy.infrastructure.persistence.entity.NotificationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class NotificationEntityRepository implements PanacheRepository<NotificationEntity> {
    
    @Transactional
    public List<NotificationEntity> findByUserId(Long userId) {
        return find("userId", userId).list();
    }
    
    @Transactional
    public List<NotificationEntity> findByQueryId(Long queryId) {
        return find("queryId", queryId).list();
    }
    
    @Transactional
    public List<NotificationEntity> findByStatus(String status) {
        return find("status", status).list();
    }
    
    @Transactional
    public List<NotificationEntity> findFailedNotifications() {
        return find("status IN ('FAILED', 'ERROR') AND retryCount < 3").list();
    }
    
    @Transactional
    public List<NotificationEntity> findRecentNotifications(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return find("createdAt >= ?1", since).list();
    }
}