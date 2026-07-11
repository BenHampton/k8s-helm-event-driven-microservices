package com.k8sdemo.order.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateOrderRequestDto {

    @Email
    private String customerEmail;

    @NotNull @Positive
    private BigDecimal amount;
}
