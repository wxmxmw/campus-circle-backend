package com.campus.campus_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "content required")
    private String content;
    private Long parentId;
    private Long replyToId;
}
