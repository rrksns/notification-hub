// DLQ 이벤트가 운영자가 지정한 검색 조건과 일치하는지 판단하는 필터
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.util.Objects;

class DlqEventFilter {

    private final String tenantId;
    private final String notificationId;
    private final String channel;

    DlqEventFilter(String tenantId, String notificationId, String channel) {
        this.tenantId = tenantId;
        this.notificationId = notificationId;
        this.channel = channel;
    }

    static DlqEventFilter from(DlqOptions options) {
        return new DlqEventFilter(options.tenantId(), options.notificationId(), options.channel());
    }

    boolean matches(NotificationEvent event) {
        if (event == null) {
            return false;
        }

        return matches(tenantId, event.tenantId())
                && matches(notificationId, event.notificationId())
                && matches(channel, event.channel());
    }

    private boolean matches(String expected, String actual) {
        return expected == null || Objects.equals(expected, actual);
    }
}
