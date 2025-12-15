package com.campus.campus_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVO {
    private String token;
    private long expiresAt;
}

