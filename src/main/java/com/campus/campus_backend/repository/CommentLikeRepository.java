package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.CommentLike;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    java.util.Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    long countByCommentId(Long commentId);
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
