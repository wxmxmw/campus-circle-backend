package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n JOIN FETCH n.user JOIN FETCH n.sender WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdWithRelations(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);
}