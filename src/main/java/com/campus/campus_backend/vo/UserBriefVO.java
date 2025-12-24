package com.campus.campus_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserBriefVO {
    private Long id;
    private String nickname;
    private Integer points;
    private String role;
}
