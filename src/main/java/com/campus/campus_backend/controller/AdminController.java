package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Report;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.ReportRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.repository.PostRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public AdminController(ReportRepository reportRepository,
                           UserRepository userRepository,
                           PostRepository postRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public Result<Object> getPendingReports(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") Short status) {

        // 权限验证 - 参考PostsController的权限检查模式 [5-cite-0](#5-cite-0)
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 查询待审核举报
        List<Report> reports = reportRepository.findByStatusOrderByCreatedAtDesc(status);

        // 构建返回数据 - 参考PostsController的列表返回格式 [5-cite-1](#5-cite-1)
        List<Map<String, Object>> list = new ArrayList<>();

        for (Report report : reports) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", report.getId());
            item.put("targetType", report.getTargetType());
            item.put("targetId", report.getTargetId());
            item.put("reason", report.getReason());
            item.put("status", report.getStatus());
            item.put("reporterId", report.getReporter() != null ? report.getReporter().getId() : null);
            item.put("reporterName", report.getReporter() != null ?
                    (report.getReporter().getNickname() != null ? report.getReporter().getNickname() : report.getReporter().getUsername()) : null);

            String time = report.getCreatedAt() != null
                    ? report.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : "";
            item.put("createdAt", time);

            list.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", reports.size());

        return Result.ok(data);
    }

    @PostMapping("/reports/{id}/process")
    @Transactional
    public Result<String> processReport(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        // 权限验证
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User admin = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        String action = body.get("action");
        String note = body.get("note");

        switch (action) {
            case "DELETE_POST":
                // 删除帖子 - 参考PostsController的删帖逻辑 [5-cite-2](#5-cite-2)
                if (report.getTargetType() == 1) { // 假设1代表帖子
                    postRepository.deleteById(report.getTargetId());
                }
                report.setStatus((short) 1); // 已处理
                break;

            case "BAN_USER":
                // 简单标记用户为BANNED角色
                User targetUser = userRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
                targetUser.setRole("BANNED");
                userRepository.save(targetUser);
                report.setStatus((short) 1);
                break;

            case "REJECT_REPORT":
                report.setStatus((short) 2); // 已拒绝
                break;

            default:
                throw new BizException(ErrorCode.BAD_REQUEST);
        }

        reportRepository.save(report);
        return Result.okMessage("处理完成: " + note);
    }
}
