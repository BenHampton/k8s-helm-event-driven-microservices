package com.k8sdemo.order.service;

import com.k8sdemo.order.config.RabbitConfig;
import com.k8sdemo.order.entity.OrderEntity;
import com.k8sdemo.order.model.enums.OrderStatus;
import com.k8sdemo.order.model.event.NotificationSent;
import com.k8sdemo.order.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationSentListener {

    private final OrderRepository orders;

    public NotificationSentListener(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    @RabbitListener(queues = RabbitConfig.NOTIFICATION_SENT_QUEUE)
    public void onNotificationSent(NotificationSent event) {
        OrderEntity order = orders.findById(event.orderId()).orElse(null);
        if (order == null) {
            // Unknown order id: don't retry forever — log and drop (ack).
            log.warn("NotificationSent for unknown order {}", event.orderId());
            return;
        }
        // Idempotent: if already NOTIFIED, a duplicate delivery is a no-op.
        if (order.getStatus() == OrderStatus.NOTIFIED) {
            log.debug("Order {} already NOTIFIED, ignoring duplicate", order.getId());
            return;
        }
        order.markNotified();
        orders.save(order);
        log.info("Order {} -> NOTIFIED", order.getId());
        // Normal return = success = message acked. Throwing would trigger
        // retry, then dead-letter after attempts are exhausted.
    }
}
