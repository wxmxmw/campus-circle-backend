package com.campus.campus_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRespVO {
    private String token;
    private long expiresAt;
    private UserBriefVO user;
}

