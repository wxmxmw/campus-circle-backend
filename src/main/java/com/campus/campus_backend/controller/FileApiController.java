package com.campus.campus_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/uploads")
public class FileApiController {

    @Value("${upload.path:/uploads}")
    private String uploadPath;

    @GetMapping("/**")
    public ResponseEntity<Resource> getFile(HttpServletRequest request) {
        try {
            // 从请求路径中提取文件路径
            String requestPath = request.getRequestURI();
            String prefix = "/api/uploads/";
            if (!requestPath.startsWith(prefix)) {
                return ResponseEntity.notFound().build();
            }
            String filePath = requestPath.substring(prefix.length());

            // 构建完整的文件路径
            Path root = Paths.get(uploadPath).toRealPath();
            Path file = root.resolve(filePath).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file) ||
                    !file.toRealPath().startsWith(root)) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(file);
            if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) &&
                    !MediaType.IMAGE_PNG_VALUE.equals(contentType) &&
                    !MediaType.IMAGE_GIF_VALUE.equals(contentType)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header("X-Content-Type-Options", "nosniff")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
