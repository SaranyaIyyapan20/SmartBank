package com.smartbank.worker;

import com.smartbank.entity.NotificationEntity;
import com.smartbank.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NotificationWorker {

    private final NotificationRepository repository;

    public NotificationWorker(NotificationRepository repository) {
        this.repository = repository;
    }

    public void process(NotificationEntity entity) {
        try {
            // Simulated external service call
            System.out.printf("Sending %s notification to user=%s with message='%s'%n",
                    entity.getChannel(), entity.getUserId(), entity.getMessage());

            // Simulate success
            entity.setStatus("SUCCESS");
            entity.setSentAt(LocalDateTime.now());
            repository.save(entity);

        } catch (Exception ex) {
            entity.setStatus("FAILED");
            repository.save(entity);
        }
    }
}

