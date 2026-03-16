package com.notificationhub.user.domain.port.out;

import com.notificationhub.user.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
}
