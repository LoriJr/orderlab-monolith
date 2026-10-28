package com.viratech.mappers;

import com.viratech.dtos.CreateOrderRequest;
import com.viratech.dtos.OrderItemResponse;
import com.viratech.dtos.OrderResponse;
import com.viratech.models.Order;
import com.viratech.models.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse createOrderResponseDto(Order entity){

        List<OrderItemResponse> items = entity.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getDescription(),
                        item.getQuantity(),
                        item.getUnitPrice()
                )).toList();

        return new OrderResponse(
                entity.getId(),
                entity.getCustomerName(),
                entity.getCustomerEmail(),
                entity.getTotalAmount(),
                entity.getCreatedAt(),
                items
        );
    }

    public Order createOrderRequestEntity(CreateOrderRequest request){
        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .build();

        for(var itemRequest : request.items()){
            OrderItem item = OrderItem.builder()
                    .description(itemRequest.description())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .build();

            order.addItem(item);
        }

        return order;

    }

}
