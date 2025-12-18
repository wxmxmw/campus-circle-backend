package com.campus.campus_backend.vo;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class UserProfileWithStatsVO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String school;
    private String bio;
    private UserStatsVO stats;
    private Boolean isFollowing;

    @Data
    @AllArgsConstructor
    public static class UserStatsVO {
        private Long likes;
        private Long following;
        private Long followers;
    }
}
