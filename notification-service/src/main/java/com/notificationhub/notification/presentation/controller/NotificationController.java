package com.notificationhub.notification.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.in.CreateNotificationUseCase;
import com.notificationhub.notification.domain.port.in.GetNotificationUseCase;
import com.notificationhub.notification.presentation.dto.CreateNotificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final CreateNotificationUseCase createNotificationUseCase;
    private final GetNotificationUseCase getNotificationUseCase;

    public NotificationController(CreateNotificationUseCase createNotificationUseCase,
                                  GetNotificationUseCase getNotificationUseCase) {
        this.createNotificationUseCase = createNotificationUseCase;
        this.getNotificationUseCase = getNotificationUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateNotificationUseCase.Result> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        CreateNotificationUseCase.Command cmd = new CreateNotificationUseCase.Command(
                tenantId, request.channel(), request.recipient(), request.content(), request.idempotencyKey()
        );
        return ApiResponse.ok(createNotificationUseCase.create(cmd));
    }

    @GetMapping("/{id}")
    public ApiResponse<Notification> getById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable("id") String id
    ) {
        return ApiResponse.ok(getNotificationUseCase.getById(id, tenantId));
    }
}
