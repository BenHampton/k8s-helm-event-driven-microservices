package com.k8sdemo.notification.controller;

import com.k8sdemo.notification.model.NotificationResponseDto;
import com.k8sdemo.notification.model.mapper.NotificationMapper;
import com.k8sdemo.notification.repository.NotificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    public List<NotificationResponseDto> list() {
        return notificationRepository.findAll().stream().map(notificationMapper::entityToDto).toList();
    }
}
