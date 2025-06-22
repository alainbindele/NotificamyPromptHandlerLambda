package com.notificamy.infrastructure.persistence.repository;

import com.notificamy.infrastructure.persistence.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserEntityRepository implements PanacheRepository<UserEntity> {
}