package com.notificationhub.user.infrastructure.persistence.mapper;

import com.notificationhub.user.domain.model.User;
import com.notificationhub.user.infrastructure.persistence.entity.UserEntity;

public class UserMapper {

    private UserMapper() {}

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getEncodedPassword(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public static User toDomain(UserEntity entity) {
        return User.reconstruct(
                entity.getId(),
                entity.getTenantId(),
                entity.getEmail(),
                entity.getEncodedPassword(),
                entity.getRole(),
                entity.getCreatedAt()
        );
    }
}
