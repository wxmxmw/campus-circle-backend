package com.campus.campus_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class CreateErrandRequest {
    @NotBlank(message = "内容不能为空")
    private String content;

    private String pickupAddr;

    private String deliveryAddr;

    private String hiddenInfo;

    @NotNull(message = "赏金不能为空")
    @Min(value = 1, message = "赏金必须大于0")
    private Integer bounty;

    @NotNull(message = "币种不能为空")
    @Min(value = 1, message = "币种值无效")
    @Max(value = 2, message = "币种值无效")
    private Integer currency; // 1-积分, 2-现金

    @NotBlank(message = "截止时间不能为空")
    private String deadline;
}