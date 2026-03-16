package com.notificationhub.user.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import com.notificationhub.user.domain.port.in.CreateApiKeyUseCase;
import com.notificationhub.user.presentation.dto.CreateApiKeyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    private final CreateApiKeyUseCase createApiKeyUseCase;

    public ApiKeyController(CreateApiKeyUseCase createApiKeyUseCase) {
        this.createApiKeyUseCase = createApiKeyUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateApiKeyUseCase.Result> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateApiKeyRequest request
    ) {
        CreateApiKeyUseCase.Command cmd = new CreateApiKeyUseCase.Command(tenantId, request.name(), request.expiresAt());
        return ApiResponse.ok(createApiKeyUseCase.create(cmd));
    }
}
