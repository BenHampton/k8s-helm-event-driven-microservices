package com.k8sdemo.notification.model.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationSent {

    private String eventId;

    private Long orderId;

    private String channel;

    private Instant occurredAt;
}
