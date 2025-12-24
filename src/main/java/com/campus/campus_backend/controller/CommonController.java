package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.SysFile;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.SysFileRepository;
import com.campus.campus_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    // 允许上传的图片类型
    private static final Set<String> DEFAULT_ALLOW_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    ));

    private final UserRepository userRepository;
    private final SysFileRepository sysFileRepository;

    @Value("${upload.path:/uploads}")
    private String uploadPath;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    // 构造函数注入 Repository
    public CommonController(UserRepository userRepository, SysFileRepository sysFileRepository) {
        this.userRepository = userRepository;
        this.sysFileRepository = sysFileRepository;
    }

    /**
     * 1. 头像单文件上传接口
     * URL: /api/common/upload
     * 功能: 上传单张图片，直接更新当前登录用户的头像字段
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) {

        log.info("=== 接收到头像上传请求 ===");

        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "文件不能为空");
        }

        try {
            // 校验文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "只支持图片文件");
            }

            // 1. 准备路径: uploads/avatars
            Path uploadDir = Paths.get(uploadPath, "avatars");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 2. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.lastIndexOf(".") != -1)
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String filename = UUID.randomUUID().toString() + extension;

            // 3. 保存文件到磁盘
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            log.info("头像文件已保存: {}", filePath);

            // 4. 生成URL
            String fileUrl = serverUrl + "/uploads/avatars/" + filename;

            // 5. 更新用户数据库信息
            String username = principal.getUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

            user.setAvatarUrl(fileUrl);
            userRepository.save(user);

            // 返回结果
            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            return Result.ok(data);

        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            log.error("头像上传IO错误", e);
            throw new BizException(500, "文件写入失败");
        } catch (Exception e) {
            log.error("头像上传未知错误", e);
            e.printStackTrace();
            throw new BizException(500, "服务器内部错误");
        }
    }

    /**
     * 2. 帖子图片批量上传接口
     * URL: /api/common/upload/batch
     * 功能: 支持多图上传，按日期存储，并记录到 sys_file 表
     */
    @PostMapping("/upload/batch")
    public Result<Map<String, Object>> uploadBatch(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("files[]") MultipartFile[] files,
            @RequestParam(value = "scene", defaultValue = "post_image") String scene,
            @RequestParam(value = "allowTypes", required = false) String allowTypes,
            @RequestParam(value = "maxWidth", required = false) Integer maxWidth,
            @RequestParam(value = "maxHeight", required = false) Integer maxHeight
    ) {
        // 1. 权限校验
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 获取当前用户实体（用于记录上传者ID）
        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 2. 参数基础校验
        if (files == null || files.length == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "请选择要上传的文件");
        }
        if (files.length > 9) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "最多同时上传9张图片");
        }

        // 处理允许的文件类型
        Set<String> allowedMimeTypes = DEFAULT_ALLOW_TYPES;
        if (allowTypes != null && !allowTypes.isEmpty()) {
            allowedMimeTypes = new HashSet<>(Arrays.asList(allowTypes.split(",")));
        }

        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            // 3. 构建按日期分类的存储路径 (例如: D:/campus_uploads/post_image/2025/12/19)
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path sceneDir = Paths.get(uploadPath, scene, datePath);

            if (!Files.exists(sceneDir)) {
                Files.createDirectories(sceneDir);
            }

            // 4. 遍历处理每个文件
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                // 大小校验 (10MB)
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "单个文件大小不能超过10MB: " + file.getOriginalFilename());
                }

                // 类型校验
                if (!allowedMimeTypes.contains(file.getContentType())) {
                    throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "不支持的文件类型: " + file.getOriginalFilename());
                }

                // 读取图片宽高
                BufferedImage image = ImageIO.read(file.getInputStream());
                int width = 0;
                int height = 0;
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }

                // 生成文件名
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = UUID.randomUUID().toString() + extension;

                // 保存文件到磁盘
                Path targetPath = sceneDir.resolve(filename);
                Files.copy(file.getInputStream(), targetPath);

                // 生成访问 URL
                String fileUrl = serverUrl + "/uploads/" + scene + "/" + datePath + "/" + filename;

                // 生成简单 Hash
                String fileHash = "sha256:" + UUID.nameUUIDFromBytes(file.getBytes()).toString();

                // === 5. 保存记录到数据库 (SysFile) ===
                SysFile sysFile = new SysFile();
                sysFile.setOriginalName(originalFilename);
                sysFile.setFileName(filename);
                sysFile.setFilePath(targetPath.toString());
                sysFile.setFileUrl(fileUrl);
                sysFile.setFileType(file.getContentType());
                sysFile.setFileSize(String.valueOf(file.getSize()));
                sysFile.setUploaderId(currentUser.getId());
                sysFile.setCreateTime(LocalDateTime.now());

                // 新增：设置业务关联
                sysFile.setBusinessType(scene.equals("post_image") ? "post" : null);
                // businessId 暂时为 null，等帖子创建后再更新

                sysFileRepository.save(sysFile);

                // 6. 构建返回给前端的数据
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", sysFile.getId());
                fileInfo.put("url", fileUrl);
                fileInfo.put("originalName", originalFilename);
                fileInfo.put("size", file.getSize());
                fileInfo.put("mimeType", file.getContentType());
                fileInfo.put("width", width);
                fileInfo.put("height", height);
                fileInfo.put("hash", fileHash);

                resultList.add(fileInfo);
            }

            // 7. 返回总结果
            Map<String, Object> data = new HashMap<>();
            data.put("list", resultList);
            data.put("total", resultList.size());

            return Result.ok(data);

        } catch (IOException e) {
            log.error("批量上传IO异常", e);
            throw new BizException(500, "文件保存失败");
        } catch (Exception e) {
            log.error("批量上传系统错误", e);
            e.printStackTrace();
            throw new BizException(500, "系统内部错误: " + e.getMessage());
        }
    }
}