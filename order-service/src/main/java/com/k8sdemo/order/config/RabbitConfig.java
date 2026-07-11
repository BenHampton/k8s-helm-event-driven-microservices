package com.k8sdemo.order.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // One topic exchange carries all domain events.
    public static final String EXCHANGE = "orders.exchange";

    // order-service consumes NotificationSent on this queue.
    public static final String NOTIFICATION_SENT_QUEUE = "order.notification-sent";
    public static final String NOTIFICATION_SENT_DLQ   = "order.notification-sent.dlq";

    // Routing keys = the event names.
    public static final String RK_ORDER_CREATED      = "order.created";
    public static final String RK_NOTIFICATION_SENT  = "notification.sent";

    @Bean
    TopicExchange ordersExchange() {
        // durable survives broker restart.
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue notificationSentDLQ() {
        return QueueBuilder.durable(NOTIFICATION_SENT_DLQ).build();
    }

    @Bean
    Queue notificationSentQueue() {
        return QueueBuilder.durable(NOTIFICATION_SENT_QUEUE)
                // when a message is rejected/expired, route it to the DLQ:
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_SENT_DLQ)
                .build();
    }

    @Bean
    Binding notificationSentBinding() {
        return BindingBuilder.bind(notificationSentQueue())
                .to(ordersExchange()).with(RK_NOTIFICATION_SENT);
    }

    @Bean
    MessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(mc);
        t.setExchange(EXCHANGE);
        return t;
    }
}
