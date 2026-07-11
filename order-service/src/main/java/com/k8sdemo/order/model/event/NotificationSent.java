package com.k8sdemo.order.model.event;

import java.time.Instant;

// The event we consume. Shape must match what notification-service publishes.
public record NotificationSent(
        String eventId,
        Long orderId,
        String channel,
        Instant occurredAt
) { }
