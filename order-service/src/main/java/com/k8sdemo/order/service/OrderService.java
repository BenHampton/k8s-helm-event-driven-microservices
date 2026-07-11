package com.k8sdemo.order.service;

import com.k8sdemo.order.entity.OrderEntity;
import com.k8sdemo.order.model.event.OrderCreated;
import com.k8sdemo.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderEntity placeOrder(String customerEmail, BigDecimal amount) {
        // 1. Persist the order as PENDING.
        OrderEntity saved = orderRepository.save(OrderEntity.builder()
                .customerEmail(customerEmail)
                .amount(amount)
                .build());

        // 2. Publish OrderCreated so notification-service can react.
        orderEventPublisher.publishOrderCreated(new OrderCreated(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getCustomerEmail(),
                saved.getAmount(),
                Instant.now()
        ));
        return saved;
    }
}
