package com.k8sdemo.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;

    private Long orderId;

    private String customerEmail;

    private String channel;

    private String status;

    private Instant sentAt;
}
