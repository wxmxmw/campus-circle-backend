package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 根据状态查询举报，按创建时间倒序排列
     * @param status 举报状态 (0=待处理, 1=已处理, 2=已拒绝)
     * @return 举报列表
     */
    List<Report> findByStatusOrderByCreatedAtDesc(Short status);
}