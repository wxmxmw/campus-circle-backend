package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Errand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrandRepository extends JpaRepository<Errand, Long> {
}

