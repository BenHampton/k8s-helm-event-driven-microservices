package com.k8sdemo.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderResponseDto {

    private Long id;
    private String customerEmail;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;
}
