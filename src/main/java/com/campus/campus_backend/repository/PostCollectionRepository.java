package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.PostCollection;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostCollectionRepository extends JpaRepository<PostCollection, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    java.util.Optional<PostCollection> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    Page<PostCollection> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    @Modifying
    @Query("DELETE FROM PostCollection pc WHERE pc.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
