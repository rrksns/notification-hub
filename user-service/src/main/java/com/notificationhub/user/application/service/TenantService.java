package com.notificationhub.user.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.user.domain.model.SubscriptionPlan;
import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.domain.model.User;
import com.notificationhub.user.domain.port.in.RegisterTenantUseCase;
import com.notificationhub.user.domain.port.out.PasswordEncoder;
import com.notificationhub.user.domain.port.out.TenantRepository;
import com.notificationhub.user.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantService implements RegisterTenantUseCase {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Result register(Command command) {
        if (tenantRepository.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Tenant tenant = Tenant.create(command.name(), command.email(), SubscriptionPlan.FREE);
        Tenant savedTenant = tenantRepository.save(tenant);

        String encodedPw = passwordEncoder.encode(command.rawPassword());
        User adminUser = User.create(savedTenant.getId(), command.email(), encodedPw);
        User savedUser = userRepository.save(adminUser);

        return new Result(savedTenant.getId(), savedUser.getId());
    }
}
