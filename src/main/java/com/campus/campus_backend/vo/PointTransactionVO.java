package com.campus.campus_backend.vo;

import lombok.Data;

@Data
public class PointTransactionVO {
    private String type;
    private Integer amount;
    private String description;
    private String createdTime;
}
