package com.notificamy.repository;

import com.notificamy.entity.Query;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QueryRepository implements PanacheRepository<Query> {
    
    public Query findByIdWithUser(Long id) {
        return find("SELECT q FROM Query q JOIN FETCH q.user WHERE q.id = ?1", id)
                .firstResult();
    }
}