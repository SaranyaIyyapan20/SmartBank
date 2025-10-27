package com.smartbank.controller;

import com.smartbank.dto.NotificationRequest;
import com.smartbank.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request) {
        String id = notificationService.sendNotification(request);
        return ResponseEntity.ok(Map.of("status", "QUEUED", "notificationId", id));
    }
}

