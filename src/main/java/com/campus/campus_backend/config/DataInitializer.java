package com.campus.campus_backend.config;

import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.Category;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.repository.CategoryRepository;
import com.campus.campus_backend.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, UserService userService) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User u = new User();
                u.setUsername("admin");
                u.setEmail("admin@example.com");
                u.setPasswordHash("admin123");
                u.setRole("USER");
                userService.save(u);
            }
        };
    }

    @Bean
    public CommandLineRunner initCategories(CategoryRepository categoryRepository) {
        return args -> {
            List<String> preset = List.of("闲置", "求助", "日常生活", "投票", "吐槽");
            boolean needSeed = categoryRepository.count() == 0;
            if (!needSeed) {
                long existing = categoryRepository.findAll().stream()
                        .map(Category::getName)
                        .filter(preset::contains)
                        .count();
                needSeed = existing < preset.size();
            }
            if (needSeed) {
                int order = 1;
                for (String name : preset) {
                    boolean exists = categoryRepository.findAll().stream().anyMatch(c -> name.equals(c.getName()));
                    if (exists) {
                        continue;
                    }
                    Category c = new Category();
                    c.setName(name);
                    c.setSortOrder(order++);
                    c.setIsActive(true);
                    categoryRepository.save(c);
                }
            }
        };
    }
}
