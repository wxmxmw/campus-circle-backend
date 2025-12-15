package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    java.util.Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    long countByCommentId(Long commentId);
}
