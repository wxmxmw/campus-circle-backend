package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost_IdAndParentIdIsNullOrderByCreatedAtAsc(Long postId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
