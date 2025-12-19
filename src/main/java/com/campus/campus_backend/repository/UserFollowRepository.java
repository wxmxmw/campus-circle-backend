package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    // 获取某用户的关注列表
    Page<UserFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId, Pageable pageable);

    // 获取某用户的粉丝列表
    Page<UserFollow> findByFollowingIdOrderByCreatedAtDesc(Long followingId, Pageable pageable);

    // 使用JOIN FETCH避免懒加载问题
    @Query("SELECT f FROM UserFollow f JOIN FETCH f.following WHERE f.follower.id = :followerId")
    Page<UserFollow> findByFollowerIdWithFollowingOrderByCreatedAtDesc(@Param("followerId") Long followerId, Pageable pageable);

    @Query("SELECT f FROM UserFollow f JOIN FETCH f.follower WHERE f.following.id = :followingId")
    Page<UserFollow> findByFollowingIdWithFollowerOrderByCreatedAtDesc(@Param("followingId") Long followingId, Pageable pageable);

    // 检查是否已关注
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // 取消关注
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}