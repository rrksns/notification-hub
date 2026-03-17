package com.notificationhub.analytics.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import com.notificationhub.analytics.domain.port.in.GetDailyStatsUseCase;
import com.notificationhub.analytics.domain.port.in.GetRealtimeStatsUseCase;
import com.notificationhub.analytics.presentation.dto.DailyStatsResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final GetDailyStatsUseCase getDailyStatsUseCase;
    private final GetRealtimeStatsUseCase getRealtimeStatsUseCase;

    public AnalyticsController(GetDailyStatsUseCase getDailyStatsUseCase,
                                GetRealtimeStatsUseCase getRealtimeStatsUseCase) {
        this.getDailyStatsUseCase = getDailyStatsUseCase;
        this.getRealtimeStatsUseCase = getRealtimeStatsUseCase;
    }

    @GetMapping("/daily")
    public ApiResponse<DailyStatsResponse> getDaily(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(DailyStatsResponse.from(getDailyStatsUseCase.getByTenantAndDate(tenantId, date)));
    }

    @GetMapping("/realtime")
    public ApiResponse<GetRealtimeStatsUseCase.RealtimeStats> getRealtime(
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        return ApiResponse.ok(getRealtimeStatsUseCase.getByTenant(tenantId));
    }
}
