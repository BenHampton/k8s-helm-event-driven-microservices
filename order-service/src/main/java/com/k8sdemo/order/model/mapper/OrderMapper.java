package com.k8sdemo.order.model.mapper;

import com.k8sdemo.order.entity.OrderEntity;
import com.k8sdemo.order.model.OrderResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // entity -> DTO. The status enum becomes its String name.
    @Mapping(target = "status", expression = "java(orderEntity.getStatus().name())")
    OrderResponseDto entityToDto(OrderEntity orderEntity);
}
