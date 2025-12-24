package com.campus.campus_backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class UserOnlineService {

    private final StringRedisTemplate redisTemplate;
    private static final String ONLINE_PREFIX = "user:online:";
    private static final long EXPIRE_TIME = 5; // 5分钟过期

    public UserOnlineService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 用户上线
    public void setUserOnline(Long userId) {
        String key = ONLINE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "1", EXPIRE_TIME, TimeUnit.MINUTES);
    }

    // 用户下线
    public void setUserOffline(Long userId) {
        String key = ONLINE_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // 检查用户是否在线
    public boolean isUserOnline(Long userId) {
        String key = ONLINE_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 刷新在线状态（心跳）
    public void refreshOnlineStatus(Long userId) {
        if (isUserOnline(userId)) {
            setUserOnline(userId); // 重新设置过期时间
        }
    }
}
