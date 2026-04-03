package com.notificationhub.delivery.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.in.GetDeliveryLogUseCase;
import com.notificationhub.delivery.presentation.dto.DeliveryLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryLogController {

    private final GetDeliveryLogUseCase getDeliveryLogUseCase;

    public DeliveryLogController(GetDeliveryLogUseCase getDeliveryLogUseCase) {
        this.getDeliveryLogUseCase = getDeliveryLogUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryLogResponse>> getById(@PathVariable("id") String id) {
        DeliveryLog log = getDeliveryLogUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(DeliveryLogResponse.from(log)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryLogResponse>>> getByTenant(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        List<DeliveryLogResponse> logs = getDeliveryLogUseCase.getByTenantId(tenantId)
                .stream().map(DeliveryLogResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
