package com.campus.campus_backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 50, message = "nickname too long")
    private String nickname;
    @Size(max = 200, message = "bio too long")
    private String bio;
    @Size(max = 255, message = "avatarUrl too long")
    private String avatarUrl;
    @Size(max = 50, message = "school too long")
    private String school;
}
