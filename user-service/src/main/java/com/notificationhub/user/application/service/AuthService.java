package com.notificationhub.user.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.domain.model.User;
import com.notificationhub.user.domain.model.AuditLog;
import com.notificationhub.user.domain.port.in.AuthenticateUseCase;
import com.notificationhub.user.domain.port.out.PasswordEncoder;
import com.notificationhub.user.domain.port.out.TenantRepository;
import com.notificationhub.user.domain.port.out.TokenProvider;
import com.notificationhub.user.domain.port.out.UserRepository;
import com.notificationhub.user.domain.port.out.AuditLogRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements AuthenticateUseCase {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuditLogRepository auditLogRepository;

    public AuthService(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder,
                       TokenProvider tokenProvider, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public Result authenticate(Command command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(command.rawPassword(), user.getEncodedPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Tenant not found"));

        if (!tenant.isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tenant is inactive");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getTenantId(), user.getRole(), tenant.getPlan().name());
        auditLogRepository.save(AuditLog.create(user.getTenantId(), user.getId(), "USER_LOGIN", user.getId()));
        return new Result(token, user.getTenantId(), user.getId());
    }
}
