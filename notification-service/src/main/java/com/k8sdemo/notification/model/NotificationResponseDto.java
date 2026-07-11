package com.k8sdemo.notification.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponseDto {

    private Long id;

    private Long orderId;

    private String customerEmail;

    private String channel;

    private String status;

    private Instant sentAt;
}
