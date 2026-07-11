package com.k8sdemo.notification.service;

import com.k8sdemo.notification.config.RabbitConfig;
import com.k8sdemo.notification.entity.NotificationEntity;
import com.k8sdemo.notification.model.event.NotificationSent;
import com.k8sdemo.notification.model.event.OrderCreated;
import com.k8sdemo.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class OrderCreatedListener {

    private final NotificationRepository notificationRepository;

    private final RabbitTemplate rabbitTemplate;

    public OrderCreatedListener(NotificationRepository notificationRepository, RabbitTemplate rabbitTemplate) {
        this.notificationRepository = notificationRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreated event) {
        // Idempotency: already notified this order? ack and move on.
        if (notificationRepository.existsByOrderId(event.getOrderId())) {
            log.debug("Order {} already notified, skipping", event.getOrderId());
            return;
        }

        // "Send" the notification. In reality: call an email/SMS provider here.
        log.info("Sending notification for order {} to {}",
                event.getOrderId(), event.getCustomerEmail());
        notificationRepository.save(NotificationEntity.builder()
                .orderId(event.getOrderId())
                .customerEmail(event.getCustomerEmail())
                .build());

        // Publish NotificationSent so order-service can close the loop.
        rabbitTemplate.convertAndSend(RabbitConfig.RK_NOTIFICATION_SENT,
                NotificationSent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(event.getOrderId())
                        .channel("EMAIL")
                        .occurredAt(Instant.now())
                        .build());
        // Normal return -> ack. Exception -> retry -> DLQ.
    }
}
