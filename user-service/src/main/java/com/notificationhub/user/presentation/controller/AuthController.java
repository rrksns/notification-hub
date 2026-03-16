package com.notificationhub.user.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import com.notificationhub.user.domain.port.in.AuthenticateUseCase;
import com.notificationhub.user.presentation.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;

    public AuthController(AuthenticateUseCase authenticateUseCase) {
        this.authenticateUseCase = authenticateUseCase;
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticateUseCase.Result> login(@Valid @RequestBody LoginRequest request) {
        AuthenticateUseCase.Command cmd = new AuthenticateUseCase.Command(request.email(), request.password());
        return ApiResponse.ok(authenticateUseCase.authenticate(cmd));
    }
}
