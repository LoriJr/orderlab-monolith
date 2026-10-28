package com.viratech.repositories;

import com.viratech.models.NotificationTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {
}
