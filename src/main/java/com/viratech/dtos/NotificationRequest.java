package com.viratech.dtos;

public record NotificationRequest(
        String consumerEmail,
        String content
) {
}
