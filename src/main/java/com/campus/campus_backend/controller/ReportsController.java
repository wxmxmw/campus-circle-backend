package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Report;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.repository.PostRepository;
import com.campus.campus_backend.repository.ReportRepository;
import com.campus.campus_backend.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public ReportsController(ReportRepository reportRepository, UserRepository userRepository,
            PostRepository postRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @PostMapping("/post")
    @Transactional
    public Result<Void> reportPost(@AuthenticationPrincipal UserDetails principal, @RequestParam Long postId,
            @Validated @RequestBody java.util.Map<String, String> body) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (postId == null || postId <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "postId invalid");
        }
        String reason = body != null ? body.get("reason") : null;
        if (reason == null || reason.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "reason required");
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        Report r = new Report();
        r.setTargetId(postId);
        r.setTargetType((short) 1);
        r.setReporter(user);
        r.setReason(reason.trim());
        reportRepository.save(r);
        Short st = post.getStatus();
        if (st == null || st == 0) {
            post.setStatus((short) 1);
            postRepository.save(post);
        }
        return Result.okMessage("举报成功");
    }
}
