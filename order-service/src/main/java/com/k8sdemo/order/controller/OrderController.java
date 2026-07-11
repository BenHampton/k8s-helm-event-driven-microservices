package com.k8sdemo.order.controller;

import com.k8sdemo.order.entity.OrderEntity;
import com.k8sdemo.order.model.CreateOrderRequestDto;
import com.k8sdemo.order.model.OrderResponseDto;
import com.k8sdemo.order.model.mapper.OrderMapper;
import com.k8sdemo.order.repository.OrderRepository;
import com.k8sdemo.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;
    private final OrderRepository orders;
    private final OrderMapper mapper;

    public OrderController(OrderService service, OrderRepository orders, OrderMapper mapper) {
        this.service = service;
        this.orders = orders;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> place(@Valid @RequestBody CreateOrderRequestDto req) {
        OrderEntity saved = service.placeOrder(req.getCustomerEmail(), req.getAmount());
        return ResponseEntity.ok(mapper.entityToDto(saved));
    }

    @GetMapping
    public List<OrderResponseDto> list() {
        return orders.findAll().stream().map(mapper::entityToDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> get(@PathVariable Long id) {
        return orders.findById(id)
                .map(o -> ResponseEntity.ok(mapper.entityToDto(o)))
                .orElse(ResponseEntity.notFound().build());
    }
}
