package com.k8sdemo.order.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// The event we consume. Shape must match what notification-service publishes.
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
