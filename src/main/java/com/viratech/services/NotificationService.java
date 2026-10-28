package com.viratech.services;

import com.viratech.clients.NotificationClient;
import com.viratech.dtos.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTaskService taskService;
    private final NotificationClient notificationClient;

    public void process(Long taskId){
        NotificationRequest request = taskService.start(taskId);

        try{
            notificationClient.send(taskId, request);
        }catch (RestClientException ex){
            taskService.fail(taskId, "SIMULATOR_CALL_FAILED");
            return;
        }

        taskService.complete(taskId);
    }
}
