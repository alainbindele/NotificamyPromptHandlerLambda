package com.notificamy.domain.port;

import com.notificamy.domain.model.Query;
import com.notificamy.domain.model.User;

public interface QueryRepositoryPort {
    Query findById(Long id);
    User findUserById(Long userId);
}