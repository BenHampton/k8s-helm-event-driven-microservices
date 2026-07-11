package com.k8sdemo.order.model.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreated(
        String eventId,
        Long orderId,
        String customerEmail,
        BigDecimal amount,
        Instant occurredAt
) { }
