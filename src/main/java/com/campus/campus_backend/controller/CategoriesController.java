package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Category;
import com.campus.campus_backend.repository.CategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/categories")
public class CategoriesController {
    private final CategoryRepository categoryRepository;

    public CategoriesController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public Result<Object> list() {
        List<Category> all = categoryRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Category c : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("icon", c.getIcon());
            item.put("sortOrder", c.getSortOrder());
            item.put("isActive", c.getIsActive());
            list.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.ok(data);
    }
}
