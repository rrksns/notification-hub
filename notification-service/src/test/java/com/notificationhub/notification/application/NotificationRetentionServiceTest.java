// 알림 개인정보 보존 기간 삭제 서비스를 검증하는 테스트
package com.notificationhub.notification.application;

import com.notificationhub.notification.application.service.NotificationRetentionService;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationRetentionServiceTest {

    @Mock
    NotificationRepository notificationRepository;

    NotificationRetentionService service;

    @BeforeEach
    void setUp() {
        service = new NotificationRetentionService(notificationRepository, 90);
    }

    @Test
    @DisplayName("보존 기간 이전 알림을 UTC cutoff 기준으로 삭제")
    void purgeExpired_deletesBeforeRetentionCutoff() {
        given(notificationRepository.deleteCreatedBefore(argThat(cutoff -> cutoff.isBefore(
                LocalDateTime.now(ZoneOffset.UTC).minusDays(89))))).willReturn(12);

        int deleted = service.purgeExpired();

        assertThat(deleted).isEqualTo(12);
        then(notificationRepository).should().deleteCreatedBefore(argThat(cutoff ->
                cutoff.isAfter(LocalDateTime.now(ZoneOffset.UTC).minusDays(91))
                        && cutoff.isBefore(LocalDateTime.now(ZoneOffset.UTC).minusDays(89))));
    }
}
