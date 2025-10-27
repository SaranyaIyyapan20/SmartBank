package com.smartbank.queue;

import com.smartbank.entity.NotificationEntity;
import com.smartbank.worker.NotificationWorker;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;

@Component
public class NotificationQueue {

    private final ExecutorService executor;
    private final BlockingQueue<NotificationEntity> queue;
    private final NotificationWorker worker;

    public NotificationQueue(NotificationWorker worker) {
        this.queue = new LinkedBlockingQueue<>(10000);
        this.executor = Executors.newFixedThreadPool(3);
        this.worker = worker;

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        NotificationEntity item = queue.take();
                        worker.process(item);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    public void publish(NotificationEntity entity) {
        if (!queue.offer(entity)) {
            throw new RuntimeException("Notification queue is full");
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}

