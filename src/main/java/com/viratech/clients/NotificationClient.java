package com.viratech.clients;

import com.viratech.dtos.NotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationClient {

    private final RestClient restClient;

    public NotificationClient(
            @Value("${notification.simulator.base-url}")
            String baseUrl
    ){
        var factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(2000);
                factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public void send(Long taskId, NotificationRequest request){
        restClient.post()
                .uri("/api/v1/notifications")
                .header("Idempotency-Key", taskId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

}
