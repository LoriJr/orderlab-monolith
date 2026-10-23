package com.viratech.models;

import com.viratech.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public class NotificationTask {

    private UUID id;
    private UUID orderId;
    private String consumerEmail;
    private String content;
    private NotificationStatus status;
    private Integer attemps;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String lastError;

}
