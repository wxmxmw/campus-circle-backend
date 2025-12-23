package com.campus.campus_backend.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrandListItemVO {
    private Long id;
    private String content;
    private String pickupAddr;
    private String deliveryAddr;
    private BigDecimal bounty;
    private Short currency;
    private LocalDateTime deadline;
    private Short status;
    private LocalDateTime createdAt;
    private Long publisherId;
    private String publisherNickname;
    private String publisherAvatar;
}