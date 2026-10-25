package com.viratech.dtos;

import com.viratech.enums.NotificationStatus;

import java.time.Instant;

public record NotificationTaskResponse(

        Long id,
        Long orderId,
        String consumerEmail,
        String content,
        NotificationStatus status,
        Integer attempts,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String lastError
) {
}
