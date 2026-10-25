package com.viratech.dtos;

import com.viratech.enums.NotificationStatus;

import java.time.Instant;

public record NotificationTaskResponse(

        Long id,
        Long orderId,
        NotificationStatus status,
        Integer attempts,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
) {
}
