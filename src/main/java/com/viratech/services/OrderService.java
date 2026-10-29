package com.viratech.services;

import com.viratech.dtos.CreateOrderRequest;
import com.viratech.dtos.OrderCreationResult;
import com.viratech.dtos.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderCreationService orderCreationService;
    private final NotificationService notificationService;

    public OrderResponse createOrder(CreateOrderRequest request){
        OrderCreationResult result = orderCreationService.createOrder(request);

        notificationService.process(result.notificationTaskId());

        return result.order();
    }
}
