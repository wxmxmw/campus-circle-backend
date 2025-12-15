package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    Page<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);
    Page<Post> findByCategoryIdOrderByCreatedAtDesc(Long categoryId, Pageable pageable);
    Page<Post> findByCategoryIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(Long categoryId, String keyword, Pageable pageable);
}
