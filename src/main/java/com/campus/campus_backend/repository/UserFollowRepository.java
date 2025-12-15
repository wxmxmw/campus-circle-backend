package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
}

