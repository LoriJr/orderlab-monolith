package com.viratech.dtos;

public record OrderCreationResult(
        OrderResponse order,
        Long notificationTaskId
) {
}
