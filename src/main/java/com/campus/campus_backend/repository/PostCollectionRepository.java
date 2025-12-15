package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.PostCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostCollectionRepository extends JpaRepository<PostCollection, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    java.util.Optional<PostCollection> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    Page<PostCollection> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
