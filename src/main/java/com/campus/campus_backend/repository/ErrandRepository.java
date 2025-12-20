package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Errand;

import java.util.Optional;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ErrandRepository extends JpaRepository<Errand, Long> {
    // 新增：查询用户接受的跑腿
    Page<Errand> findByRunnerIdOrderByCreatedAtDesc(Long runnerId, Pageable pageable);

    Page<Errand> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);

    Page<Errand> findByPublisherIdOrderByCreatedAtDesc(Long publisherId, Pageable pageable);

    Page<Errand> findByStatusOrderByBountyDesc(Integer status, Pageable pageable);

    Optional<Errand> findById(Long id);

    // 添加JOIN FETCH查询解决LAZY加载问题
    @Query("SELECT e FROM Errand e JOIN FETCH e.publisher WHERE e.id = :id")
    Optional<Errand> findByIdWithPublisher(@Param("id") Long id);
}