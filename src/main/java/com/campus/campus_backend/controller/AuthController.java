package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.dto.LoginRequest;
import com.campus.campus_backend.dto.RegisterRequest;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.service.UserService;
import com.campus.campus_backend.security.JwtUtil;
import com.campus.campus_backend.vo.TokenVO;
import com.campus.campus_backend.vo.LoginRespVO;
import com.campus.campus_backend.vo.UserBriefVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.campus.campus_backend.service.UserOnlineService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserOnlineService userOnlineService;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                          UserRepository userRepository, UserService userService, StringRedisTemplate redisTemplate, UserOnlineService userOnlineService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.userOnlineService = userOnlineService;
    }

    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()) || userRepository.existsByEmail(req.getEmail())) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "用户名或邮箱已存在");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setPasswordHash(req.getPassword());
        u.setRole("USER");
        userService.save(u);
        return Result.okMessage("注册成功");
    }

    @PostMapping("/login")
    public Result<LoginRespVO> login(@Validated @RequestBody LoginRequest req) {
        com.campus.campus_backend.domain.User entity = userRepository.findByUsername(req.getUsername())
                .orElseGet(() -> userRepository.findByEmail(req.getUsername()).orElse(null));
        if (entity == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(req.getPassword(), entity.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String token = jwtUtil.generate(entity.getUsername(), Map.of());
        long expiresAt = Instant.now().toEpochMilli() + jwtUtil.getExpirationMillis();
        // 添加角色参数到 UserBriefVO
        UserBriefVO userBrief = new UserBriefVO(entity.getId(), entity.getNickname(), entity.getPoints(), entity.getRole());
        return Result.ok(new LoginRespVO(token, expiresAt, userBrief));
    }

    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String token = authorization.substring(7);
        if (!jwtUtil.validate(token)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String username = jwtUtil.getSubject(token);
        String newToken = jwtUtil.generate(username, Map.of());
        long expiresAt = Instant.now().toEpochMilli() + jwtUtil.getExpirationMillis();
        return Result.ok(new TokenVO(newToken, expiresAt));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String token = authorization.substring(7);
        if (!jwtUtil.validate(token)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 从 JWT token 中提取用户名并查找用户
        String username = jwtUtil.getSubject(token);
        User user = userRepository.findByUsername(username).orElse(null);

        // 设置用户下线
        if (user != null) {
            userOnlineService.setUserOffline(user.getId());
        }

        long exp = jwtUtil.getExpirationEpochMillis(token);
        long now = Instant.now().toEpochMilli();
        long ttl = Math.max(exp - now, 1000);
        String key = "BLACKLIST:JWT:" + token;
        try {
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
        return Result.okMessage("已退出");
    }
}