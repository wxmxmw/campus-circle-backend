package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

