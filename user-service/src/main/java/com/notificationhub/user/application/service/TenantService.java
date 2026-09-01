package com.notificationhub.user.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.user.domain.model.SubscriptionPlan;
import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.domain.model.User;
import com.notificationhub.user.domain.model.AuditLog;
import com.notificationhub.user.domain.port.in.RegisterTenantUseCase;
import com.notificationhub.user.domain.port.out.PasswordEncoder;
import com.notificationhub.user.domain.port.out.TenantRepository;
import com.notificationhub.user.domain.port.out.UserRepository;
import com.notificationhub.user.domain.port.out.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService implements RegisterTenantUseCase {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    public TenantService(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder,
                         AuditLogRepository auditLogRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public Result register(Command command) {
        if (tenantRepository.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Tenant tenant = Tenant.create(command.name(), command.email(), SubscriptionPlan.FREE);
        Tenant savedTenant = tenantRepository.save(tenant);

        String encodedPw = passwordEncoder.encode(command.rawPassword());
        User adminUser = User.create(savedTenant.getId(), command.email(), encodedPw);
        User savedUser = userRepository.save(adminUser);
        auditLogRepository.save(AuditLog.create(savedTenant.getId(), "system", "TENANT_REGISTERED", savedTenant.getId()));

        return new Result(savedTenant.getId(), savedUser.getId());
    }
}
