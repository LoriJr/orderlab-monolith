package com.viratech.services;

import com.viratech.dtos.NotificationRequest;
import com.viratech.enums.NotificationStatus;
import com.viratech.models.NotificationTask;
import com.viratech.repositories.NotificationTaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationTaskService {

    private final NotificationTaskRepository repository;

    @Transactional
    public NotificationRequest start(Long taskId){
        NotificationTask task = findTask(taskId);

        if(task.getStatus() != NotificationStatus.PENDING){
            throw new RuntimeException("Task is not pending");
        }

        task.setStatus(NotificationStatus.PROCESSING);
        task.setAttempts(task.getAttempts() + 1);
        task.setStartedAt(Instant.now());
        task.setLastError(null);

        return new NotificationRequest(
                task.getConsumerEmail(),
                task.getContent()
        );
    }

    @Transactional
    public void complete(Long taskId) {
        NotificationTask task = findTask(taskId);

        task.setStatus(NotificationStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setLastError(null);
    }

    @Transactional
    public void fail(Long taskId, String error) {
        NotificationTask task = findTask(taskId);

        task.setStatus(NotificationStatus.FAILED);
        task.setLastError(error);
    }


    private NotificationTask findTask(Long taskId) {
        return repository.findById(taskId)
                .orElseThrow(
                        ()-> new RuntimeException(
                                "Task not found: " + taskId
                        )
                ); // TODO ADICIONAR EXCEÇÃO
    }
}
