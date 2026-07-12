package com.k8sdemo.order.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreated {

    private String eventId;

    private Long orderId;

    private String customerEmail;

    private BigDecimal amount;

    private Instant occurredAt;
}
