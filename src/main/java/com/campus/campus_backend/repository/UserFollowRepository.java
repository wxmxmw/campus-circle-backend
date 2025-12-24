package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    // ✅ 修复点：加上 countQuery
    @Query(value = "SELECT f FROM UserFollow f JOIN FETCH f.following WHERE f.follower.id = :followerId",
            countQuery = "SELECT count(f) FROM UserFollow f WHERE f.follower.id = :followerId")
    Page<UserFollow> findByFollowerIdWithFollowingOrderByCreatedAtDesc(@Param("followerId") Long followerId, Pageable pageable);

    // ✅ 修复点：加上 countQuery
    @Query(value = "SELECT f FROM UserFollow f JOIN FETCH f.follower WHERE f.following.id = :followingId",
            countQuery = "SELECT count(f) FROM UserFollow f WHERE f.following.id = :followingId")
    Page<UserFollow> findByFollowingIdWithFollowerOrderByCreatedAtDesc(@Param("followingId") Long followingId, Pageable pageable);

    long countByFollowerId(Long followerId);
    long countByFollowingId(Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}