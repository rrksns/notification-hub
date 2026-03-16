package com.notificationhub.user.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import com.notificationhub.user.domain.port.in.RegisterTenantUseCase;
import com.notificationhub.user.presentation.dto.RegisterTenantRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class TenantController {

    private final RegisterTenantUseCase registerTenantUseCase;

    public TenantController(RegisterTenantUseCase registerTenantUseCase) {
        this.registerTenantUseCase = registerTenantUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterTenantUseCase.Result> register(@Valid @RequestBody RegisterTenantRequest request) {
        RegisterTenantUseCase.Command cmd = new RegisterTenantUseCase.Command(
                request.name(), request.email(), request.password()
        );
        return ApiResponse.ok(registerTenantUseCase.register(cmd));
    }
}
