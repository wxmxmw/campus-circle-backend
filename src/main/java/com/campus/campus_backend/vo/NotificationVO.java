package com.campus.campus_backend.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationVO {
    private Long id;
    private String type;
    private String typeText;
    private Long userId;
    private String username;
    private String avatar;
    private String time;
    private Long postId;
    private Long commentId;
    private Long replyId;
    private String commentContent;
    private String quote;
    private String quoteLabel;
    private Boolean isRead;
}