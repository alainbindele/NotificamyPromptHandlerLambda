package com.notificamy.infrastructure.persistence.repository;

import com.notificamy.infrastructure.persistence.entity.QueryEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QueryEntityRepository implements PanacheRepository<QueryEntity> {
    
    public QueryEntity findByIdWithUser(Long id) {
        return find("SELECT q FROM QueryEntity q JOIN FETCH q.user WHERE q.id = ?1", id)
                .firstResult();
    }
}