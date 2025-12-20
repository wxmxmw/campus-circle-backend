package com.campus.campus_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileWithStatsVO {
    private Long id;
    private String username;  // ✅ 新增：用户名
    private String nickname;
    private String avatarUrl;
    private String school;
    private String bio;
    private Integer points;   // ✅ 新增：积分
    private UserStatsVO stats;
    private Boolean isFollowing;

    // ⚠️ 手动添加这个构造函数，为了兼容 Controller 中旧的 new 写法
    // 这样上一条回答的代码就不用改，直接能跑
    public UserProfileWithStatsVO(Long id, String nickname, String avatarUrl, String school, String bio, UserStatsVO stats, Boolean isFollowing) {
        this.id = id;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.school = school;
        this.bio = bio;
        this.stats = stats;
        this.isFollowing = isFollowing;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserStatsVO {
        private Long likes;
        private Long following;
        private Long followers;
    }
}