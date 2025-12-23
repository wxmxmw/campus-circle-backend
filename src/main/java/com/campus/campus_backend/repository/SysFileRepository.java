package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.SysFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysFileRepository extends JpaRepository<SysFile, Long> {
    List<SysFile> findByBusinessTypeAndBusinessId(String businessType, Long businessId);

    @Query("SELECT s FROM SysFile s WHERE s.fileUrl = :url")
    SysFile findByFileUrl(@Param("url") String url);
}