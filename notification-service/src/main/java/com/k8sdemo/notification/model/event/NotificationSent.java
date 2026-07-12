package com.k8sdemo.notification.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSent {

    private String eventId;

    private Long orderId;

    private String channel;

    private Instant occurredAt;
}
