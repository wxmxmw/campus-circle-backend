package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    Page<PointTransaction> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    java.util.List<PointTransaction> findByUserUsernameAndTypeOrderByCreatedAtDesc(String username, Short type);
}
