package com.k8sdemo.notification.config;

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

    public static final String EXCHANGE = "orders.exchange";

    public static final String ORDER_CREATED_QUEUE = "notification.order-created";
    public static final String ORDER_CREATED_DLQ   = "notification.order-created.dlq";

    public static final String RK_ORDER_CREATED     = "order.created";
    public static final String RK_NOTIFICATION_SENT = "notification.sent";

    @Bean
    TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue orderCreatedDlq() {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(ordersExchange()).with(RK_ORDER_CREATED);
    }

    @Bean
    MessageConverter jsonConverter() { return new JacksonJsonMessageConverter(); }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(mc);
        t.setExchange(EXCHANGE);
        return t;
    }
}
