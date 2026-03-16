package com.notificationhub.user.infrastructure.persistence.adapter;

import com.notificationhub.user.domain.model.User;
import com.notificationhub.user.domain.port.out.UserRepository;
import com.notificationhub.user.infrastructure.persistence.entity.UserEntity;
import com.notificationhub.user.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(UserEntity.from(user)).toDomain();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return jpaRepository.findById(id).map(UserEntity::toDomain);
    }
}
