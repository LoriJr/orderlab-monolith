package com.viratech.mappers;

import com.viratech.dtos.NotificationTaskResponse;
import com.viratech.models.NotificationTask;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationTaskResponse notificationResponseDto(NotificationTask entity){
        return new NotificationTaskResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }
}
