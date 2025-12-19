package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.ChatMessage;
import com.campus.campus_backend.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = true WHERE cm.conversation.id = :conversationId AND cm.receiver.id = :userId AND cm.isRead = false")
    int markMessagesAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversation = :conversation ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByConversationOrderByCreatedAtDesc(@Param("conversation") Conversation conversation, Pageable pageable);
}