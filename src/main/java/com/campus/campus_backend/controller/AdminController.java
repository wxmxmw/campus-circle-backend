package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Report;
import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final NotificationRepository notificationRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCollectionRepository postCollectionRepository;
    private final ObjectMapper mapper = new ObjectMapper();



    public AdminController(ReportRepository reportRepository,
                           UserRepository userRepository,
                           PostRepository postRepository,
                           NotificationRepository notificationRepository,
                           CommentRepository commentRepository,
                           CommentLikeRepository commentLikeRepository,
                           PostLikeRepository postLikeRepository,
                           PostCollectionRepository postCollectionRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.notificationRepository = notificationRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCollectionRepository = postCollectionRepository;
    }

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public Result<Object> getPendingReports(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") Short status) {

        // 权限验证
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

        // 构建返回数据
        List<Map<String, Object>> list = new ArrayList<>();

        for (Report report : reports) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", report.getId());
            item.put("reason", report.getReason());

            // 添加 description 字段（如果 Report 实体有这个字段）
            item.put("description", report.getReason()); // 或者使用 report.getDescription() 如果存在

            // 将 targetType 转换为字符串
            String targetTypeStr = report.getTargetType() != null && report.getTargetType() == 1 ? "POST" : "OTHER";
            item.put("targetType", targetTypeStr);
            item.put("targetId", report.getTargetId());

            // 构建 targetSnapshot
            if (report.getTargetType() != null && report.getTargetType() == 1 && report.getTargetId() != null) {
                try {
                    Optional<Post> postOpt = postRepository.findById(report.getTargetId());
                    if (postOpt.isPresent()) {
                        Post post = postOpt.get();
                        Map<String, Object> snapshot = new LinkedHashMap<>();

                        // 作者信息
                        Map<String, Object> author = new LinkedHashMap<>();
                        User postUser = post.getUser();
                        if (postUser != null) {
                            author.put("nickname", postUser.getNickname() != null ? postUser.getNickname() : postUser.getUsername());
                            author.put("id", postUser.getId());
                        }
                        snapshot.put("author", author);

                        // 帖子内容
                        snapshot.put("content", post.getContent());

                        // 处理图片
                        try {
                            List<String> images = new ArrayList<>();
                            if (post.getImages() != null && !post.getImages().isEmpty()) {
                                images = mapper.readValue(post.getImages(), new TypeReference<List<String>>() {});
                            }
                            snapshot.put("images", images);
                        } catch (Exception ignore) {
                            snapshot.put("images", List.of());
                        }

                        item.put("targetSnapshot", snapshot);
                    } else {
                        item.put("targetSnapshot", null);
                    }
                } catch (Exception e) {
                    item.put("targetSnapshot", null);
                }
            } else {
                item.put("targetSnapshot", null);
            }

            // 时间戳格式
            item.put("createTime", report.getCreatedAt() != null
                    ? report.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
                    : null);
            item.put("status", report.getStatus());

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
                // 删除帖子 - 按顺序清理所有相关记录
                if (report.getTargetType() != null && report.getTargetType() == 1) {
                    Post post = postRepository.findById(report.getTargetId())
                            .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

                    Long postId = post.getId();

                    // 1. 删除通知记录
                    notificationRepository.deleteByPostId(postId);

                    // 2. 删除评论点赞记录（如果有）
                    commentLikeRepository.deleteByPostId(postId);

                    // 3. 删除评论记录
                    commentRepository.deleteByPostId(postId);

                    // 4. 删除点赞记录
                    postLikeRepository.deleteByPostId(postId);

                    // 5. 删除收藏记录
                    postCollectionRepository.deleteByPostId(postId);

                    // 6. 最后删除帖子
                    postRepository.delete(post);
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