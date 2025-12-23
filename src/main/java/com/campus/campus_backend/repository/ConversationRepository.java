package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Conversation;
import com.campus.campus_backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1AndUser2(User user1, User user2);

    List<Conversation> findByUser1OrUser2(User user1, User user2);
}

