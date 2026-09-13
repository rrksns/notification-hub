// 알림 개인정보를 보존 기간에 따라 삭제하는 스케줄 서비스
package com.notificationhub.notification.application.service;

import com.notificationhub.notification.domain.port.out.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class NotificationRetentionService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetentionService.class);

    private final NotificationRepository notificationRepository;
    private final int retentionDays;

    public NotificationRetentionService(NotificationRepository notificationRepository,
                                        @Value("${notification.retention-days:90}") int retentionDays) {
        this.notificationRepository = notificationRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${notification.retention-cron:0 0 3 * * *}", zone = "UTC")
    public int purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        int deleted = notificationRepository.deleteCreatedBefore(cutoff);
        log.info("Notification retention purge completed: cutoff={}, deleted={}", cutoff, deleted);
        return deleted;
    }
}
