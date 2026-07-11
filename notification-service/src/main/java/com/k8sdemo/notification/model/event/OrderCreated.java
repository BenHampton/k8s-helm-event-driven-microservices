package com.k8sdemo.notification.model.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderCreated {

    private String eventId;

    private Long orderId;

    private String customerEmail;

    private BigDecimal amount;

    private Instant occurredAt;
}
