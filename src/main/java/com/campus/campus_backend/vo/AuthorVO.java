package com.campus.campus_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorVO {
    private Long id;
    private String nickname;
    private String avatar;
}
