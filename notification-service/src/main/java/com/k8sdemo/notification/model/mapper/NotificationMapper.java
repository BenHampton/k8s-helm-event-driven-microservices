package com.k8sdemo.notification.model.mapper;

import com.k8sdemo.notification.entity.NotificationEntity;
import com.k8sdemo.notification.model.NotificationResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "status", expression = "java(notificationEntity.getStatus().name())")
    NotificationResponseDto entityToDto(NotificationEntity notificationEntity);
}
