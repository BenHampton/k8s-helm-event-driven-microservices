package com.k8sdemo.order.service;

import com.k8sdemo.order.config.RabbitConfig;
import com.k8sdemo.order.model.event.OrderCreated;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbit;

    public OrderEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publishOrderCreated(OrderCreated event) {
        // exchange is defaulted on the template; route by event name.
        rabbit.convertAndSend(RabbitConfig.RK_ORDER_CREATED, event);
    }
}
