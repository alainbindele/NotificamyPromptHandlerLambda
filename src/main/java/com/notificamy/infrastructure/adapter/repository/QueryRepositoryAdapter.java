package com.notificamy.infrastructure.adapter.repository;

import com.notificamy.domain.model.Query;
import com.notificamy.domain.model.User;
import com.notificamy.domain.port.QueryRepositoryPort;
import com.notificamy.infrastructure.mapper.QueryMapper;
import com.notificamy.infrastructure.mapper.UserMapper;
import com.notificamy.infrastructure.persistence.entity.QueryEntity;
import com.notificamy.infrastructure.persistence.entity.UserEntity;
import com.notificamy.infrastructure.persistence.repository.QueryEntityRepository;
import com.notificamy.infrastructure.persistence.repository.UserEntityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QueryRepositoryAdapter implements QueryRepositoryPort {
    
    private static final Logger LOG = Logger.getLogger(QueryRepositoryAdapter.class);
    
    @Inject
    QueryEntityRepository queryEntityRepository;
    
    @Inject
    UserEntityRepository userEntityRepository;
    
    @Inject
    QueryMapper queryMapper;
    
    @Inject
    UserMapper userMapper;
    
    @Override
    @Transactional
    public Query findById(Long id) {
        QueryEntity entity = queryEntityRepository.findByIdWithUser(id);
        return entity != null ? queryMapper.toDomain(entity) : null;
    }
    
    @Override
    @Transactional
    public User findUserById(Long userId) {
        UserEntity entity = userEntityRepository.findById(userId);
        return entity != null ? userMapper.toDomain(entity) : null;
    }
    
    @Override
    @Transactional
    public void updateQueryClosed(Long queryId, boolean closed) {
        try {
            QueryEntity entity = queryEntityRepository.findById(queryId);
            if (entity != null) {
                entity.setClosed(closed);
                queryEntityRepository.persist(entity);
                LOG.infof("Updated query %d closed status to: %s", queryId, closed);
            } else {
                LOG.warnf("Query not found for closing: %d", queryId);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to update query %d closed status", queryId);
        }
    }
}