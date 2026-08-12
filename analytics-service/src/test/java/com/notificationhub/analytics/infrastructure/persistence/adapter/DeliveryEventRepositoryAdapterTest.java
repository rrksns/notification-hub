// DeliveryEvent repository adapter의 페이징 조회를 검증하는 테스트
package com.notificationhub.analytics.infrastructure.persistence.adapter;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import com.notificationhub.analytics.infrastructure.persistence.document.DeliveryEventDocument;
import com.notificationhub.analytics.infrastructure.persistence.repository.DeliveryEventMongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DeliveryEventRepositoryAdapterTest {

    @Mock
    private DeliveryEventMongoRepository mongoRepository;

    @Test
    @DisplayName("tenant 이벤트 조회는 Pageable을 전달하고 Page 메타데이터를 보존한다")
    void findByTenantId_returnsPagedEvents() {
        DeliveryEventRepositoryAdapter adapter = new DeliveryEventRepositoryAdapter(mongoRepository);
        Pageable pageable = PageRequest.of(1, 2);
        DeliveryEvent event = DeliveryEvent.reconstruct(
                "event-1",
                "log-1",
                "notification-1",
                "tenant-1",
                "EMAIL",
                "SUCCESS",
                null,
                LocalDateTime.of(2026, 8, 12, 10, 0)
        );
        DeliveryEventDocument document = DeliveryEventDocument.from(event);
        given(mongoRepository.findByTenantId("tenant-1", pageable))
                .willReturn(new PageImpl<>(List.of(document), pageable, 5));

        Page<DeliveryEvent> result = adapter.findByTenantId("tenant-1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("event-1");
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        then(mongoRepository).should().findByTenantId("tenant-1", pageable);
    }
}
