package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.SysFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysFileRepository extends JpaRepository<SysFile, Long> {
}