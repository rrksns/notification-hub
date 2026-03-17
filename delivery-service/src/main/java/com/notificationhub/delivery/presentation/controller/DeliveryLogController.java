package com.notificationhub.delivery.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import com.notificationhub.delivery.presentation.dto.DeliveryLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryLogController {

    private final DeliveryLogRepository deliveryLogRepository;

    public DeliveryLogController(DeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryLogResponse>> getById(@PathVariable String id) {
        return deliveryLogRepository.findById(id)
                .map(log -> ResponseEntity.ok(ApiResponse.ok(DeliveryLogResponse.from(log))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryLogResponse>>> getByTenant(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        List<DeliveryLogResponse> logs = deliveryLogRepository.findByTenantId(tenantId)
                .stream().map(DeliveryLogResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
