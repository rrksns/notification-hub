// delivery log 조회의 tenant 경계 적용을 검증하는 테스트
package com.notificationhub.delivery.application;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.delivery.application.service.GetDeliveryLogService;
import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.model.DeliveryStatus;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetDeliveryLogServiceTest {

    @Mock
    private DeliveryLogRepository deliveryLogRepository;

    @Test
    @DisplayName("delivery log 단건 조회는 tenant 조건을 함께 사용한다")
    void getById_usesTenantScope() {
        DeliveryLog log = log("delivery-1", "tenant-1");
        given(deliveryLogRepository.findByIdAndTenantId("delivery-1", "tenant-1"))
                .willReturn(Optional.of(log));
        GetDeliveryLogService service = new GetDeliveryLogService(deliveryLogRepository);

        DeliveryLog result = service.getById("delivery-1", "tenant-1");

        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        verify(deliveryLogRepository).findByIdAndTenantId("delivery-1", "tenant-1");
    }

    @Test
    @DisplayName("다른 tenant의 delivery log는 존재하지 않는 리소스로 처리한다")
    void getById_differentTenant_returnsNotFound() {
        given(deliveryLogRepository.findByIdAndTenantId("delivery-1", "tenant-2"))
                .willReturn(Optional.empty());
        GetDeliveryLogService service = new GetDeliveryLogService(deliveryLogRepository);

        assertThatThrownBy(() -> service.getById("delivery-1", "tenant-2"))
                .isInstanceOf(BusinessException.class);
    }

    private DeliveryLog log(String id, String tenantId) {
        return DeliveryLog.reconstruct(
                id, "notification-1", tenantId, ChannelType.EMAIL, "user@example.com",
                DeliveryStatus.SUCCESS, null, 1, LocalDateTime.now()
        );
    }
}
