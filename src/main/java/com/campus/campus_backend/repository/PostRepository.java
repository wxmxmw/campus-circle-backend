package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Post;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    Page<Post> findByCategoryIdOrderByCreatedAtDesc(Long categoryId, Pageable pageable);

    Page<Post> findByCategoryIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(Long categoryId, String keyword,
            Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Post> findPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    List<Post> findByUserId(Long userId);
}
