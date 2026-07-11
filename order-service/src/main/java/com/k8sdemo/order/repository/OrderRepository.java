package com.k8sdemo.order.repository;

import com.k8sdemo.notification.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
