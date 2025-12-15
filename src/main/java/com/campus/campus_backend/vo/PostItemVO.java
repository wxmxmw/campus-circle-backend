package com.campus.campus_backend.vo;

import lombok.Data;

import java.util.List;

@Data
public class PostItemVO {
    private Long id;
    private String content;
    private List<String> images;
    private String createTime;
    private PostStatsVO stats;
    private AuthorVO author;
    private String collectedAt;
}
