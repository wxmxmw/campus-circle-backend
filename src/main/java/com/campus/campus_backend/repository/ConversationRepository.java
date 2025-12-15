package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}

