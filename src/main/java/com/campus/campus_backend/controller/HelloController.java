package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
public class HelloController {
    @GetMapping("/me")
    public Result<String> me(@AuthenticationPrincipal User user) {
        return Result.ok(user.getUsername());
    }
}

