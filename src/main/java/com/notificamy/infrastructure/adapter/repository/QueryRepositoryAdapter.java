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

@ApplicationScoped
public class QueryRepositoryAdapter implements QueryRepositoryPort {
    
    @Inject
    QueryEntityRepository queryEntityRepository;
    
    @Inject
    UserEntityRepository userEntityRepository;
    
    @Inject
    QueryMapper queryMapper;
    
    @Inject
    UserMapper userMapper;
    
    @Override
    public Query findById(Long id) {
        QueryEntity entity = queryEntityRepository.findByIdWithUser(id);
        return entity != null ? queryMapper.toDomain(entity) : null;
    }
    
    @Override
    public User findUserById(Long userId) {
        UserEntity entity = userEntityRepository.findById(userId);
        return entity != null ? userMapper.toDomain(entity) : null;
    }
}