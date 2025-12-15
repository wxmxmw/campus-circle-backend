package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}

