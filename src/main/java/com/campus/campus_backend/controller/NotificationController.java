package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.ChatMessage;
import com.campus.campus_backend.domain.Conversation;
import com.campus.campus_backend.domain.Notification;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.ChatMessageRepository;
import com.campus.campus_backend.repository.ConversationRepository;
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
import com.campus.campus_backend.service.UserOnlineService;

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
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserOnlineService userOnlineService;
    private final DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final ZoneId zone = ZoneId.of("Asia/Shanghai");

    public NotificationController(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            ChatMessageRepository chatMessageRepository,
            ConversationRepository conversationRepository,
            UserOnlineService userOnlineService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.userOnlineService = userOnlineService;
    }

    // 6.2 标记私信已读
    @PutMapping("/chats/{chatId}/read")
    @Transactional
    public Result<Object> markChatAsRead(
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 标记该会话中当前用户作为接收者的所有未读消息为已读
        chatMessageRepository.markMessagesAsRead(chatId, currentUser.getId());

        // 更新 Conversation 表中的未读计数
        Conversation conversation = conversationRepository.findById(chatId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        if (conversation.getUser1().getId().equals(currentUser.getId())) {
            conversation.setUser1Unread(0);
        } else if (conversation.getUser2().getId().equals(currentUser.getId())) {
            conversation.setUser2Unread(0);
        }
        conversationRepository.save(conversation);

        return Result.ok("标记成功");
    }

    // 6.3 标记通知已读
    @PutMapping("/notifications/{notifyId}/read")
    @Transactional
    public Result<Object> markNotificationAsRead(
            @PathVariable Long notifyId,
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Notification notification = notificationRepository.findById(notifyId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 验证通知属于当前用户
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        return Result.ok("标记成功");
    }

    // 6.4 获取私聊列表
    @GetMapping("/chats")
    @Transactional(readOnly = true)
    public Result<Object> listChats(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        List<Conversation> conversations = conversationRepository.findByUser1OrUser2(currentUser, currentUser);

        List<Map<String, Object>> list = new ArrayList<>();
        int totalUnreadCount = 0;

        for (Conversation conv : conversations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", conv.getId());

            // 确定对方用户
            User otherUser = conv.getUser1().getId().equals(currentUser.getId()) ? conv.getUser2() : conv.getUser1();
            item.put("userId", otherUser.getId());
            item.put("username", otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
            item.put("avatar", otherUser.getAvatarUrl());

            // 未读计数
            int unreadCount = conv.getUser1().getId().equals(currentUser.getId()) ?
                    conv.getUser1Unread() : conv.getUser2Unread();
            item.put("unread", unreadCount);
            totalUnreadCount += unreadCount;

            // 最后一条消息
            if (conv.getLastMessageId() != null) {
                ChatMessage lastMessage = chatMessageRepository.findById(conv.getLastMessageId()).orElse(null);
                if (lastMessage != null) {
                    item.put("lastMessage", lastMessage.getContent());
                    item.put("time", formatTime(lastMessage.getCreatedAt()));
                }
            }

            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("unreadCount", totalUnreadCount);

        return Result.ok(data);
    }

    // 6.5 获取聊天记录
    @GetMapping("/chats/{userId}/messages")
    @Transactional(readOnly = true)
    public Result<Object> getChatMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        User otherUser = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 直接查询双方消息，无需查找 conversation
        List<ChatMessage> allMessages = chatMessageRepository.findByUsers(currentUser.getId(), otherUser.getId());

        // 手动分页处理
        int total = allMessages.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<ChatMessage> pageMessages = fromIndex < total ?
                allMessages.subList(fromIndex, toIndex) : new ArrayList<>();

        List<Map<String, Object>> list = new ArrayList<>();
        for (ChatMessage message : pageMessages) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", message.getId());
            item.put("senderId", message.getSender().getId());
            item.put("receiverId", message.getReceiver().getId());
            item.put("type", "text");
            item.put("content", message.getContent());
            item.put("time", message.getCreatedAt().atZone(zone).format(dtf));
            item.put("status", message.getStatus());
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("hasMore", toIndex < total);

        return Result.ok(data);
    }

    // 6.6 获取聊天用户信息
    @GetMapping("/users/{userId}")
    @Transactional(readOnly = true)
    public Result<Object> getUserInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getNickname() != null ? user.getNickname() : user.getUsername());
        data.put("avatar", user.getAvatarUrl());
        data.put("isOnline", userOnlineService.isUserOnline(userId)); // 使用Redis查询在线状态

        return Result.ok(data);
    }

    // 6.7 发送私聊消息
    @PostMapping("/chats/{userId}/send")
    @Transactional
    public Result<Object> sendMessage(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        String content = request.get("content").toString();

        // 查找或创建会话
        Conversation conversation = conversationRepository.findByUser1AndUser2(currentUser, receiver)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setUser1(currentUser);
                    newConv.setUser2(receiver);
                    newConv.setUser1Unread(0);
                    newConv.setUser2Unread(0);
                    return conversationRepository.save(newConv);
                });

        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setIsRead(false);
        message = chatMessageRepository.save(message);

        // 更新未读计数
        if (receiver.getId().equals(conversation.getUser1().getId())) {
            conversation.setUser1Unread(conversation.getUser1Unread() + 1);
        } else {
            conversation.setUser2Unread(conversation.getUser2Unread() + 1);
        }

        // 更新最后一条消息信息
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversationRepository.save(conversation);

        Map<String, Object> data = new HashMap<>();
        data.put("id", message.getId());
        data.put("senderId", currentUser.getId());
        data.put("receiverId", receiver.getId());
        data.put("type", "text");
        data.put("content", message.getContent());
        data.put("time", message.getCreatedAt().atZone(zone).format(dtf));
        data.put("status", message.getStatus());

        return Result.ok("发送成功", data);
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