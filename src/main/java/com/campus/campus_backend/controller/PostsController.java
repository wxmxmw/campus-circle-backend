package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.*;
import com.campus.campus_backend.dto.CreateCommentRequest;
import com.campus.campus_backend.dto.CreatePostRequest;
import com.campus.campus_backend.repository.CategoryRepository;
import com.campus.campus_backend.repository.CommentLikeRepository;
import com.campus.campus_backend.repository.CommentRepository;
import com.campus.campus_backend.repository.NotificationRepository;
import com.campus.campus_backend.repository.PointTransactionRepository;
import com.campus.campus_backend.repository.PostCollectionRepository;
import com.campus.campus_backend.repository.PostLikeRepository;
import com.campus.campus_backend.repository.PostRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.vo.PostPublishRespVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.campus.campus_backend.repository.SysFileRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostsController {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCollectionRepository postCollectionRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final NotificationRepository notificationRepository;
    private final SysFileRepository sysFileRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public PostsController(PostRepository postRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            PostLikeRepository postLikeRepository,
            PostCollectionRepository postCollectionRepository,
            CommentRepository commentRepository,
            PointTransactionRepository pointTransactionRepository,
            CommentLikeRepository commentLikeRepository,
            NotificationRepository notificationRepository,
            SysFileRepository sysFileRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCollectionRepository = postCollectionRepository;
        this.commentRepository = commentRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.notificationRepository = notificationRepository;
        this.sysFileRepository = sysFileRepository;
    }

    @PostMapping
    @Transactional
    public Result<PostPublishRespVO> create(@AuthenticationPrincipal UserDetails principal,
            @Validated @RequestBody CreatePostRequest req) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId()).orElse(null);
        }

        if (category == null) {
            List<Category> all = categoryRepository.findAll();
            if (all.isEmpty()) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "分类未配置");
            }
            category = all.get(0);
        }

        Post p = new Post();
        p.setUser(user);
        p.setCategory(category);
        p.setIsAnonymous(Boolean.TRUE.equals(req.getIsAnonymous()));
        p.setContent(req.getContent());

        try {
            if (req.getImages() != null) {
                p.setImages(mapper.writeValueAsString(req.getImages()));
            }
            if (req.getProduct() != null) {
                p.setProductInfo(mapper.writeValueAsString(req.getProduct()));
            }
        } catch (Exception ignore) {
        }

        p.setViewCount(0);
        p.setLikeCount(0);
        p.setCommentCount(0);
        p = postRepository.save(p);

        // 新增：更新相关文件的业务关联
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            // 从URL中提取文件ID或通过URL查询文件记录
            for (String imageUrl : req.getImages()) {
                SysFile file = sysFileRepository.findByFileUrl(imageUrl);
                if (file != null) {
                    file.setBusinessId(p.getId());
                    file.setBusinessType("post");
                    sysFileRepository.save(file);
                }
            }
        }

        // 发帖积分奖励
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(currentPoints + 20);
        userRepository.save(user);

        PointTransaction pt = new PointTransaction();
        pt.setUser(user);
        pt.setAmount(20);
        pt.setType((short) 2);
        pt.setDescription("发帖奖励");
        pointTransactionRepository.save(pt);

        // 构建返回VO
        PostPublishRespVO vo = new PostPublishRespVO();
        vo.setId(p.getId());
        vo.setUserId(user.getId());
        vo.setUserAvatar(user.getAvatarUrl());
        vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        vo.setUserLevel("v3");
        vo.setTime(p.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        vo.setTopicId(req.getTopicId());
        vo.setTopicName(req.getTopicName());
        vo.setContent(p.getContent());

        try {
            List<String> images = req.getImages();
            vo.setImages(images);
        } catch (Exception ignore) {
        }

        vo.setViews(p.getViewCount());
        vo.setComments(p.getCommentCount());
        vo.setLikes(p.getLikeCount());
        vo.setShares(0);

        return Result.ok(vo);
    }

    @GetMapping("/hot")
    @Transactional
    public Result<List<Map<String, Object>>> hot(@RequestParam(defaultValue = "5") int limit) {
        List<Post> posts = postRepository.findAll();

        // 更新热度分：浏览*1 + 点赞*3 + 评论*5
        for (Post p : posts) {
            int v = p.getViewCount() != null ? p.getViewCount() : 0;
            int l = p.getLikeCount() != null ? p.getLikeCount() : 0;
            int c = p.getCommentCount() != null ? p.getCommentCount() : 0;
            int score = v + l * 3 + c * 5;
            p.setHotScore(score);
        }
        postRepository.saveAll(posts);

        // 排序
        posts.sort((p1, p2) -> {
            int s1 = p1.getHotScore() != null ? p1.getHotScore() : 0;
            int s2 = p2.getHotScore() != null ? p2.getHotScore() : 0;
            return Integer.compare(s2, s1);
        });

        List<Map<String, Object>> list = new ArrayList<>();
        int count = 0;
        for (Post p : posts) {
            if (count >= limit)
                break;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());

            String title = p.getSummary();
            if (title == null || title.isEmpty()) {
                String content = p.getContent();
                if (content != null) {
                    title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                } else {
                    title = "";
                }
            }
            item.put("title", title);
            item.put("content", p.getContent());

            User u = p.getUser();
            item.put("userId", u != null ? u.getId() : null);
            String name = u != null && u.getNickname() != null ? u.getNickname()
                    : (u != null ? u.getUsername() : "匿名用户");
            item.put("userName", name);
            item.put("userAvatar", u != null ? u.getAvatarUrl() : null);

            int views = p.getViewCount() != null ? p.getViewCount() : 0;
            item.put("views", views);
            item.put("viewsText", formatViews(views));

            item.put("likes", p.getLikeCount() != null ? p.getLikeCount() : 0);
            item.put("comments", p.getCommentCount() != null ? p.getCommentCount() : 0);
            item.put("hotRank", count + 1);

            String time = p.getCreatedAt() != null
                    ? p.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : "";
            item.put("time", time);

            list.add(item);
            count++;
        }

        return Result.ok(list);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Result<Object> list(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 参数校验
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1,
                pageSize);
        org.springframework.data.domain.Page<Post> postsPage;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        // 条件查询
        if (hasKeyword && categoryId != null) {
            postsPage = postRepository.findByCategoryIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
                    categoryId, keyword.trim(), pageable);
        } else if (hasKeyword) {
            postsPage = postRepository.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(keyword.trim(), pageable);
        } else if (categoryId != null) {
            postsPage = postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable);
        } else {
            postsPage = postRepository.findAll(pageable);
        }

        // 构建返回数据
        List<Map<String, Object>> list = new ArrayList<>();
        User current = null;
        if (principal != null) {
            current = userRepository.findByUsername(principal.getUsername()).orElse(null);
        }

        for (Post p : postsPage.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());

            User u = p.getUser();
            item.put("userId", u != null ? u.getId() : null);
            item.put("userAvatar", u != null ? u.getAvatarUrl() : null);
            String name = u != null && u.getNickname() != null ? u.getNickname() : (u != null ? u.getUsername() : null);
            item.put("userName", name);
            item.put("userLevel", "LV.3");

            String time = p.getCreatedAt() != null
                    ? p.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : "";
            item.put("time", time);

            String tag = p.getCategory() != null ? p.getCategory().getName() : null;
            item.put("tag", tag);
            item.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
            item.put("content", p.getContent());

            // 处理图片
            try {
                List<String> images = new ArrayList<>();
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    images = mapper.readValue(p.getImages(), new TypeReference<List<String>>() {
                    });
                }
                item.put("images", images);
            } catch (Exception ignore) {
                item.put("images", List.of());
            }

            // 处理商品信息
            try {
                Map<String, Object> product = null;
                if (p.getProductInfo() != null && !p.getProductInfo().isEmpty()) {
                    product = mapper.readValue(p.getProductInfo(), new TypeReference<Map<String, Object>>() {
                    });
                    if (product != null) {
                        Map<String, Object> onlyPrice = new LinkedHashMap<>();
                        if (product.containsKey("price")) {
                            onlyPrice.put("price", product.get("price"));
                        }
                        item.put("product", onlyPrice.isEmpty() ? product : onlyPrice);
                    }
                }
            } catch (Exception ignore) {
            }

            item.put("views", String.valueOf(p.getViewCount()));
            item.put("comments", String.valueOf(p.getCommentCount()));
            item.put("likes", String.valueOf(p.getLikeCount()));
            item.put("isTop", Boolean.TRUE.equals(p.getIsSticky()));

            boolean isLiked = false;
            if (current != null) {
                isLiked = postLikeRepository.existsByPostIdAndUserId(p.getId(), current.getId());
            }
            item.put("isLiked", isLiked);

            list.add(item);
        }

        long total = postsPage.getTotalElements();
        boolean hasMore = postsPage.hasNext();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("hasMore", hasMore);

        return Result.ok(data);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Post p = postRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (!isAdmin && (p.getUser() == null || !p.getUser().getId().equals(user.getId()))) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        postRepository.delete(p);
        return Result.okMessage("删除成功");
    }

    @PostMapping("/{postId}/like")
    @Transactional
    public Result<Map<String, Boolean>> like(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId,
            @RequestParam(required = false) Boolean isLike) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        boolean likeAction = isLike == null ? true : isLike;
        boolean existed = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());

        // 点赞逻辑
        if (likeAction && !existed) {
            com.campus.campus_backend.domain.PostLike pl = new com.campus.campus_backend.domain.PostLike();
            pl.setPost(post);
            pl.setUser(user);
            postLikeRepository.save(pl);

            // 创建点赞通知
            if (!user.getId().equals(post.getUser().getId())) {
                Notification notification = new Notification();
                notification.setUser(post.getUser());
                notification.setSender(user);
                notification.setType((short) 1);
                notification.setPost(post);
                notification.setContent("赞了你的帖子");
                notification.setQuote(post.getContent());
                notification.setQuoteLabel("我的帖子");
                notificationRepository.save(notification);
            }
        } else if (!likeAction && existed) {
            postLikeRepository.findByPostIdAndUserId(post.getId(), user.getId())
                    .ifPresent(postLikeRepository::delete);
        }

        // 更新点赞数
        long count = postLikeRepository.countByPostId(post.getId());
        post.setLikeCount((int) count);
        postRepository.save(post);

        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
        Map<String, Boolean> data = new LinkedHashMap<>();
        data.put("isLiked", isLiked);

        return Result.ok(data);
    }

    @DeleteMapping("/{postId}/like")
    public Result<Map<String, Boolean>> unlike(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        return like(principal, postId, false);
    }

    @PostMapping("/{postId}/collect")
    public Result<Map<String, Boolean>> collect(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        boolean existed = postCollectionRepository.existsByPostIdAndUserId(post.getId(), user.getId());
        if (!existed) {
            com.campus.campus_backend.domain.PostCollection pc = new com.campus.campus_backend.domain.PostCollection();
            pc.setPost(post);
            pc.setUser(user);
            postCollectionRepository.save(pc);
        }

        // 更新收藏数
        long count = postCollectionRepository.countByPostId(post.getId());
        post.setCollectCount((int) count);
        postRepository.save(post);

        Map<String, Boolean> data = new LinkedHashMap<>();
        data.put("isCollected", true);

        return Result.ok(data);
    }

    @DeleteMapping("/{postId}/collect")
    public Result<Map<String, Boolean>> uncollect(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        postCollectionRepository.findByPostIdAndUserId(post.getId(), user.getId())
                .ifPresent(postCollectionRepository::delete);

        // 更新收藏数
        long count = postCollectionRepository.countByPostId(post.getId());
        post.setCollectCount((int) count);
        postRepository.save(post);

        Map<String, Boolean> data = new LinkedHashMap<>();
        data.put("isCollected", false);

        return Result.ok(data);
    }

    @PostMapping("/{postId}/comments")
    @Transactional
    public Result<Map<String, Object>> createComment(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId,
            @Validated @RequestBody CreateCommentRequest req) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 创建评论
        Comment c = new Comment();
        c.setPost(post);
        c.setUser(user);

        if (req.getParentId() != null && req.getParentId() > 0) {
            c.setParentId(req.getParentId());
        } else {
            c.setParentId(null);
        }

        if (req.getReplyToId() != null && req.getReplyToId() > 0) {
            Optional<Comment> replyOpt = commentRepository.findById(req.getReplyToId());
            if (replyOpt.isPresent()) {
                Comment replyComment = replyOpt.get();
                c.setReplyToUser(replyComment.getUser());
            }
        }

        c.setContent(req.getContent());
        c = commentRepository.save(c);

        // 更新帖子评论数
        int currentCount = post.getCommentCount() != null ? post.getCommentCount() : 0;
        post.setCommentCount(currentCount + 1);
        postRepository.save(post);

        // 评论积分奖励
        int uPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(uPoints + 10);
        userRepository.save(user);

        PointTransaction cpt = new PointTransaction();
        cpt.setUser(user);
        cpt.setAmount(10);
        cpt.setType((short) 3);
        cpt.setDescription("评论奖励");
        pointTransactionRepository.save(cpt);

        // 创建评论通知
        if (!user.getId().equals(post.getUser().getId())) {
            Notification notification = new Notification();
            notification.setUser(post.getUser()); // 通知帖子作者
            notification.setSender(user); // 评论者
            notification.setType((short) 2); // 2-评论类型
            notification.setPost(post);
            notification.setContent("评论了你的帖子");
            notification.setQuote(post.getContent());
            notification.setQuoteLabel("我的帖子");
            notificationRepository.save(notification);
        }

        // 构建返回数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", c.getId());
        data.put("postId", post.getId());
        data.put("userId", user.getId());
        data.put("username", user.getNickname() != null ? user.getNickname() : user.getUsername());
        data.put("avatar", user.getAvatarUrl());
        data.put("content", c.getContent());

        String time = c.getCreatedAt() != null
                ? c.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : "";
        data.put("time", time);

        data.put("likes", 0);
        data.put("isLiked", false);

        boolean isAuthor = post.getUser() != null && post.getUser().getId().equals(user.getId());
        data.put("isAuthor", isAuthor);
        data.put("isMine", true);

        String replyTo = c.getReplyToUser() != null
                ? (c.getReplyToUser().getNickname() != null ? c.getReplyToUser().getNickname()
                        : c.getReplyToUser().getUsername())
                : null;
        data.put("replyTo", replyTo);

        return Result.ok(data);
    }

    @GetMapping("/{postId}/comments")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listComments(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        User current = null;
        if (principal != null) {
            current = userRepository.findByUsername(principal.getUsername()).orElse(null);
        }

        // 查询顶级评论
        List<Comment> topLevel = commentRepository.findByPost_IdAndParentIdIsNullOrderByCreatedAtAsc(postId);
        List<Map<String, Object>> list = new ArrayList<>();

        for (Comment c : topLevel) {
            Map<String, Object> item = new LinkedHashMap<>();
            User cu = c.getUser();

            item.put("id", c.getId());
            item.put("userId", cu != null ? cu.getId() : null);
            item.put("avatar", cu != null ? cu.getAvatarUrl() : null);
            item.put("username",
                    cu != null ? (cu.getNickname() != null ? cu.getNickname() : cu.getUsername()) : null);

            boolean isAuthor = post.getUser() != null && cu != null && post.getUser().getId().equals(cu.getId());
            item.put("isAuthor", isAuthor);

            item.put("content", c.getContent());

            String time = c.getCreatedAt() != null
                    ? c.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : "";
            item.put("time", time);

            item.put("likes", c.getLikeCount() != null ? c.getLikeCount() : 0);

            boolean isLiked = false;
            if (current != null) {
                isLiked = commentLikeRepository.existsByCommentIdAndUserId(c.getId(), current.getId());
            }
            item.put("isLiked", isLiked);

            // 处理回复
            List<Map<String, Object>> replies = new ArrayList<>();
            List<Comment> children = commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId());

            for (Comment r : children) {
                Map<String, Object> ri = new LinkedHashMap<>();
                User ru = r.getUser();

                ri.put("id", r.getId());
                ri.put("userId", ru != null ? ru.getId() : null);
                ri.put("avatar", ru != null ? ru.getAvatarUrl() : null);
                ri.put("username",
                        ru != null ? (ru.getNickname() != null ? ru.getNickname() : ru.getUsername()) : null);

                boolean rIsAuthor = post.getUser() != null && ru != null && post.getUser().getId().equals(ru.getId());
                ri.put("isAuthor", rIsAuthor);

                boolean isOP = ru != null && c.getUser() != null && ru.getId().equals(c.getUser().getId());
                ri.put("isOP", isOP);

                String rtime = r.getCreatedAt() != null
                        ? r.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai"))
                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        : "";
                ri.put("time", rtime);

                ri.put("content", r.getContent());

                String replyTo = r.getReplyToUser() != null
                        ? (r.getReplyToUser().getNickname() != null ? r.getReplyToUser().getNickname()
                                : r.getReplyToUser().getUsername())
                        : null;
                ri.put("replyTo", replyTo);

                ri.put("likes", r.getLikeCount() != null ? r.getLikeCount() : 0);

                boolean rIsLiked = false;
                if (current != null) {
                    rIsLiked = commentLikeRepository.existsByCommentIdAndUserId(r.getId(), current.getId());
                }
                ri.put("isLiked", rIsLiked);

                replies.add(ri);
            }

            item.put("replies", replies);
            list.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", list.size());

        return Result.ok(data);
    }

    @PostMapping("/{postId}/view")
    @Transactional
    public Result<Map<String, Object>> addView(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        int current = post.getViewCount() != null ? post.getViewCount() : 0;
        post.setViewCount(current + 1);
        postRepository.save(post);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("postId", post.getId());
        data.put("views", post.getViewCount());

        return Result.ok(data);
    }

    @GetMapping("/{postId}")
    @Transactional
    public Result<Map<String, Object>> detail(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        Post p = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 增加阅读量
        int currentViews = p.getViewCount() != null ? p.getViewCount() : 0;
        p.setViewCount(currentViews + 1);
        postRepository.save(p);

        // 构建返回数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", p.getId());

        User u = p.getUser();
        data.put("userId", u != null ? u.getId() : null);
        data.put("userAvatar", u != null ? u.getAvatarUrl() : null);
        String name = u != null && u.getNickname() != null ? u.getNickname() : (u != null ? u.getUsername() : null);
        data.put("userName", name);

        String time = p.getCreatedAt() != null
                ? p.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : "";
        data.put("time", time);

        data.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
        String tag = p.getCategory() != null ? p.getCategory().getName() : null;
        data.put("tag", tag);
        data.put("content", p.getContent());

        // 处理图片
        try {
            List<String> images = new ArrayList<>();
            if (p.getImages() != null && !p.getImages().isEmpty()) {
                images = mapper.readValue(p.getImages(), new TypeReference<List<String>>() {
                });
            }
            data.put("images", images);
        } catch (Exception ignore) {
            data.put("images", List.of());
        }

        // 处理商品信息
        try {
            Map<String, Object> product = null;
            if (p.getProductInfo() != null && !p.getProductInfo().isEmpty()) {
                product = mapper.readValue(p.getProductInfo(), new TypeReference<Map<String, Object>>() {
                });
                if (product != null) {
                    Map<String, Object> onlyPrice = new LinkedHashMap<>();
                    if (product.containsKey("price")) {
                        onlyPrice.put("price", product.get("price"));
                    }
                    data.put("product", onlyPrice.isEmpty() ? product : onlyPrice);
                }
            }
        } catch (Exception ignore) {
        }

        data.put("views", p.getViewCount());
        data.put("likes", p.getLikeCount());

        boolean isLiked = false;
        boolean isCollected = false;
        if (principal != null) {
            User current = userRepository.findByUsername(principal.getUsername()).orElse(null);
            if (current != null) {
                isLiked = postLikeRepository.existsByPostIdAndUserId(p.getId(), current.getId());
                isCollected = postCollectionRepository.existsByPostIdAndUserId(p.getId(), current.getId());
            }
        }

        data.put("isLiked", isLiked);
        data.put("isCollected", isCollected);
        data.put("collectCount", p.getCollectCount());
        data.put("commentCount", p.getCommentCount());

        return Result.ok(data);
    }

    private String formatViews(int v) {
        if (v >= 10000) {
            double w = v / 10000.0;
            double rounded = Math.round(w * 10.0) / 10.0;
            String s = String.valueOf(rounded);
            if (s.endsWith(".0")) {
                s = s.substring(0, s.length() - 2);
            }
            return s + "万";
        }
        return String.valueOf(v);
    }

    @PostMapping("/{id}/top")
    @Transactional
    public Result<Map<String, Boolean>> top(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Post post = postRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isAuthor = post.getUser() != null && post.getUser().getId().equals(user.getId());
        if (!isAdmin && !isAuthor) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        Boolean isTop = true;
        if (body != null && body.containsKey("isTop")) {
            Object v = body.get("isTop");
            if (v instanceof Boolean b) {
                isTop = b;
            } else if (v instanceof String s) {
                isTop = "true".equalsIgnoreCase(s) || "1".equals(s);
            } else if (v instanceof Number n) {
                isTop = n.intValue() != 0;
            }
        }

        // 非管理员置顶需要消耗积分
        if (Boolean.TRUE.equals(isTop) && !isAdmin) {
            int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            if (currentPoints < 30) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "积分不足，需30积分置顶");
            }

            user.setPoints(currentPoints - 30);
            userRepository.save(user);

            PointTransaction pt = new PointTransaction();
            pt.setUser(user);
            pt.setAmount(-30);
            pt.setType((short) 4);
            pt.setDescription("帖子置顶消耗");
            pointTransactionRepository.save(pt);
        }

        post.setIsSticky(Boolean.TRUE.equals(isTop));
        postRepository.save(post);

        Map<String, Boolean> data = new LinkedHashMap<>();
        data.put("isTop", Boolean.TRUE.equals(isTop));

        return Result.ok(data);
    }

    @DeleteMapping("/{id}/top")
    @Transactional
    public Result<Map<String, Boolean>> untop(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Post post = postRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isAuthor = post.getUser() != null && post.getUser().getId().equals(user.getId());
        if (!isAdmin && !isAuthor) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        post.setIsSticky(false);
        postRepository.save(post);

        Map<String, Boolean> data = new LinkedHashMap<>();
        data.put("isTop", false);

        return Result.ok(data);
    }
}