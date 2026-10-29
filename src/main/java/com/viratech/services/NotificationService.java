package com.viratech.services;

import com.viratech.clients.NotificationClient;
import com.viratech.dtos.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTaskService taskService;
    private final NotificationClient notificationClient;

    public void process(Long taskId){
        NotificationRequest request = taskService.start(taskId);

        try{
            notificationClient.send(taskId, request);
        }catch (RestClientResponseException ex) {
            log.warn("Notification HTTP failure: taskId={}, status={}", taskId, ex.getStatusCode().value(), ex);

            taskService.fail(taskId,"SIMULATOR_HTTP_" + ex.getStatusCode().value()
            );
            return;
        }catch (ResourceAccessException ex){
            log.warn("Notification communication failure: taskId={}", taskId, ex);

            taskService.fail(taskId, "SIMULATOR_COMMUNICATION_ERROR");
            return;

        }catch (RestClientException ex){
            log.warn("Notification attempt failed: taskId={}", taskId, ex);

            taskService.fail(taskId, "SIMULATOR_CALL_FAILED");
            return;
        }

        taskService.complete(taskId);
    }
}
