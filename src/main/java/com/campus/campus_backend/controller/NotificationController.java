package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Notification;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.NotificationRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.vo.NotificationVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final ZoneId zone = ZoneId.of("Asia/Shanghai");

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    public Result<Object> listNotifications(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> notificationsPage = notificationRepository.findByUserIdWithRelations(
                currentUser.getId(), pageable);

        List<NotificationVO> list = new ArrayList<>();
        for (Notification notification : notificationsPage.getContent()) {
            NotificationVO vo = new NotificationVO();
            vo.setId(notification.getId());
            vo.setType(notificationTypeToString(notification.getType()));
            vo.setTypeText(getTypeText(notification.getType()));
            vo.setUserId(notification.getSender() != null ? notification.getSender().getId() : null);
            vo.setUsername(
                    notification.getSender() != null
                            ? (notification.getSender().getNickname() != null ? notification.getSender().getNickname()
                                    : notification.getSender().getUsername())
                            : null);
            vo.setAvatar(notification.getSender() != null ? notification.getSender().getAvatarUrl() : null);
            vo.setTime(formatTime(notification.getCreatedAt()));
            vo.setPostId(notification.getPost() != null ? notification.getPost().getId() : null);
            vo.setCommentId(notification.getComment() != null ? notification.getComment().getId() : null);
            vo.setReplyId(notification.getReplyId());
            vo.setCommentContent(notification.getComment() != null ? notification.getComment().getContent() : null);
            vo.setQuote(notification.getQuote());
            vo.setQuoteLabel(notification.getQuoteLabel());
            vo.setIsRead(notification.getIsRead());
            list.add(vo);
        }

        Long unreadCount = notificationRepository.countUnreadByUserId(currentUser.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("unreadCount", unreadCount);

        return Result.ok(data);
    }

    private String notificationTypeToString(short type) {
        return switch (type) {
            case 1 -> "like";
            case 2 -> "comment";
            case 3 -> "reply";
            case 4 -> "follow";
            default -> "unknown";
        };
    }

    private String getTypeText(short type) {
        return switch (type) {
            case 1 -> "赞了你的帖子";
            case 2 -> "评论了你的帖子";
            case 3 -> "回复了你的评论";
            case 4 -> "关注了你";
            default -> "未知通知";
        };
    }

    private String formatTime(LocalDateTime time) {
        if (time == null)
            return "";
        return time.atZone(zone).format(dtf);
    }
}