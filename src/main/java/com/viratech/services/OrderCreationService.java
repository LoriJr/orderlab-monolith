package com.viratech.services;

import com.viratech.dtos.CreateOrderRequest;
import com.viratech.dtos.OrderCreationResult;
import com.viratech.mappers.OrderMapper;
import com.viratech.models.NotificationTask;
import com.viratech.models.Order;
import com.viratech.repositories.NotificationTaskRepository;
import com.viratech.repositories.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final NotificationTaskRepository notificationTaskRepository;

    private static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("99999999999999999.99");

    @Transactional
    public OrderCreationResult createOrder(CreateOrderRequest request){
        Order order = orderMapper.createOrderRequestEntity(request);

        BigDecimal totalAmount = order.getItems()
                .stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if(totalAmount.compareTo(MAX_TOTAL_AMOUNT) > 0){
            throw new RuntimeException("Order total exceeds the supported limit"); //TODO CRIAR EXCEÇÃO PERSONALIZADA
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        NotificationTask task = NotificationTask.builder()
                .orderId(savedOrder.getId())
                .consumerEmail(savedOrder.getCustomerEmail())
                .content("Order: " + savedOrder.getId())
                .build();

        NotificationTask savedTask = notificationTaskRepository.save(task);

        return new OrderCreationResult(
                orderMapper.createOrderResponseDto(savedOrder),
                savedTask.getId());
    }
}
