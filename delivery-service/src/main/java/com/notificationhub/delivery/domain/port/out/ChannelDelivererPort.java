package com.notificationhub.delivery.domain.port.out;

import com.notificationhub.delivery.domain.model.ChannelType;

public interface ChannelDelivererPort {
    void deliver(ChannelType channel, String recipient, String content);
}
