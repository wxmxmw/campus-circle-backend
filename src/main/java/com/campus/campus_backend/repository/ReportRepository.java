package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
