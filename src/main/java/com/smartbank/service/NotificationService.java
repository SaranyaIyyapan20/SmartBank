package com.smartbank.service;

import com.smartbank.dto.NotificationRequest;
import com.smartbank.entity.NotificationEntity;
import com.smartbank.queue.NotificationQueue;
import com.smartbank.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationQueue queue;
    private final NotificationRepository repository;

    public NotificationService(NotificationQueue queue, NotificationRepository repository) {
        this.queue = queue;
        this.repository = repository;
    }

    public String sendNotification(NotificationRequest request) {
        if (request.getChannel() == null || request.getMessage() == null) {
            throw new IllegalArgumentException("Channel and message must be provided");
        }

        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(request.getUserId());
        entity.setChannel(request.getChannel());
        entity.setMessage(request.getMessage());
        entity.setStatus("PENDING");
        entity.setCreatedAt(LocalDateTime.now());

        repository.save(entity);

        queue.publish(entity);

        return entity.getId();
    }
}

