package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Category;
import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.dto.CreatePostRequest;
import com.campus.campus_backend.dto.CreateCommentRequest;
import com.campus.campus_backend.repository.CategoryRepository;
import com.campus.campus_backend.repository.PostRepository;
import com.campus.campus_backend.repository.PostLikeRepository;
import com.campus.campus_backend.repository.PostCollectionRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.repository.CommentRepository;
import com.campus.campus_backend.repository.CommentLikeRepository;
import com.campus.campus_backend.domain.Comment;
import com.campus.campus_backend.domain.PointTransaction;
import com.campus.campus_backend.repository.PointTransactionRepository;
import com.campus.campus_backend.vo.PostPublishRespVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.transaction.annotation.Transactional;

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
    private final ObjectMapper mapper = new ObjectMapper();

    public PostsController(PostRepository postRepository, UserRepository userRepository,
            CategoryRepository categoryRepository, PostLikeRepository postLikeRepository,
            PostCollectionRepository postCollectionRepository, CommentRepository commentRepository,
            PointTransactionRepository pointTransactionRepository, CommentLikeRepository commentLikeRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCollectionRepository = postCollectionRepository;
        this.commentRepository = commentRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.commentLikeRepository = commentLikeRepository;
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
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(currentPoints + 20);
        userRepository.save(user);
        PointTransaction pt = new PointTransaction();
        pt.setUser(user);
        pt.setAmount(20);
        pt.setType((short) 2);
        pt.setDescription("发帖奖励");
        pointTransactionRepository.save(pt);

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

    @GetMapping
    @Transactional(readOnly = true)
    public Result<Object> list(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1)
            page = 1;
        if (pageSize < 1)
            pageSize = 10;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1,
                pageSize);
        org.springframework.data.domain.Page<Post> postsPage;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword && categoryId != null) {
            postsPage = postRepository.findByCategoryIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(categoryId,
                    keyword.trim(), pageable);
        } else if (hasKeyword) {
            postsPage = postRepository.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(keyword.trim(), pageable);
        } else if (categoryId != null) {
            postsPage = postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable);
        } else {
            postsPage = postRepository.findAll(pageable);
        }
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
            try {
                List<String> images = new ArrayList<>();
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    images = mapper.readValue(p.getImages(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                            });
                }
                item.put("images", images);
            } catch (Exception ignore) {
                item.put("images", java.util.List.of());
            }
            try {
                Map<String, Object> product = null;
                if (p.getProductInfo() != null && !p.getProductInfo().isEmpty()) {
                    product = mapper.readValue(p.getProductInfo(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
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
    public Result<java.util.Map<String, Boolean>> like(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId, @RequestParam(required = false) Boolean isLike) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        boolean likeAction = isLike == null ? true : isLike;
        boolean existed = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
        if (likeAction && !existed) {
            com.campus.campus_backend.domain.PostLike pl = new com.campus.campus_backend.domain.PostLike();
            pl.setPost(post);
            pl.setUser(user);
            postLikeRepository.save(pl);
        } else if (!likeAction && existed) {
            postLikeRepository.findByPostIdAndUserId(post.getId(), user.getId())
                    .ifPresent(postLikeRepository::delete);
        }
        long count = postLikeRepository.countByPostId(post.getId());
        post.setLikeCount((int) count);
        postRepository.save(post);
        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
        java.util.Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("isLiked", isLiked);
        return Result.ok(data);
    }

    @DeleteMapping("/{postId}/like")
    public Result<java.util.Map<String, Boolean>> unlike(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        return like(principal, postId, false);
    }

    @PostMapping("/{postId}/collect")
    public Result<java.util.Map<String, Boolean>> collect(@AuthenticationPrincipal UserDetails principal,
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
        long count = postCollectionRepository.countByPostId(post.getId());
        post.setCollectCount((int) count);
        postRepository.save(post);
        java.util.Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("isCollected", true);
        return Result.ok(data);
    }

    @DeleteMapping("/{postId}/collect")
    public Result<java.util.Map<String, Boolean>> uncollect(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        postCollectionRepository.findByPostIdAndUserId(post.getId(), user.getId())
                .ifPresent(postCollectionRepository::delete);
        long count = postCollectionRepository.countByPostId(post.getId());
        post.setCollectCount((int) count);
        postRepository.save(post);
        java.util.Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("isCollected", false);
        return Result.ok(data);
    }

    @PostMapping("/{postId}/comments")
    @Transactional
    public Result<java.util.Map<String, Object>> createComment(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId, @Validated @RequestBody CreateCommentRequest req) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        Comment c = new Comment();
        c.setPost(post);
        c.setUser(user);
        if (req.getParentId() != null && req.getParentId() > 0) {
            c.setParentId(req.getParentId());
        } else {
            c.setParentId(null);
        }
        if (req.getReplyToId() != null && req.getReplyToId() > 0) {
            java.util.Optional<Comment> replyOpt = commentRepository.findById(req.getReplyToId());
            if (replyOpt.isPresent()) {
                Comment replyComment = replyOpt.get();
                c.setReplyToUser(replyComment.getUser());
            }
        }
        c.setContent(req.getContent());
        c = commentRepository.save(c);
        int currentCount = post.getCommentCount() != null ? post.getCommentCount() : 0;
        post.setCommentCount(currentCount + 1);
        postRepository.save(post);
        int uPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(uPoints + 10);
        userRepository.save(user);
        PointTransaction cpt = new PointTransaction();
        cpt.setUser(user);
        cpt.setAmount(10);
        cpt.setType((short) 3);
        cpt.setDescription("评论奖励");
        pointTransactionRepository.save(cpt);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
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
    public Result<java.util.Map<String, Object>> listComments(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        User current = null;
        if (principal != null) {
            current = userRepository.findByUsername(principal.getUsername()).orElse(null);
        }
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
    public Result<java.util.Map<String, Object>> addView(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Post post = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        int current = post.getViewCount() != null ? post.getViewCount() : 0;
        post.setViewCount(current + 1);
        postRepository.save(post);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("postId", post.getId());
        data.put("views", post.getViewCount());
        return Result.ok(data);
    }

    @GetMapping("/{postId}")
    @Transactional
    public Result<java.util.Map<String, Object>> detail(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long postId) {
        Post p = postRepository.findById(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        int currentViews = p.getViewCount() != null ? p.getViewCount() : 0;
        p.setViewCount(currentViews + 1);
        postRepository.save(p);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
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
        try {
            java.util.List<String> images = new java.util.ArrayList<>();
            if (p.getImages() != null && !p.getImages().isEmpty()) {
                images = mapper.readValue(p.getImages(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                        });
            }
            data.put("images", images);
        } catch (Exception ignore) {
            data.put("images", java.util.List.of());
        }
        try {
            java.util.Map<String, Object> product = null;
            if (p.getProductInfo() != null && !p.getProductInfo().isEmpty()) {
                product = mapper.readValue(p.getProductInfo(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                        });
                if (product != null) {
                    java.util.Map<String, Object> onlyPrice = new java.util.LinkedHashMap<>();
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

    @GetMapping("/hot")
    @Transactional
    public Result<java.util.List<java.util.Map<String, Object>>> hot() {
        java.util.List<Post> all = postRepository.findAll();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Post p : all) {
            int v = p.getViewCount() != null ? p.getViewCount() : 0;
            int l = p.getLikeCount() != null ? p.getLikeCount() : 0;
            int c = p.getCommentCount() != null ? p.getCommentCount() : 0;
            int co = p.getCollectCount() != null ? p.getCollectCount() : 0;
            double decay = 1.0;
            if (p.getCreatedAt() != null) {
                long hours = java.time.Duration.between(p.getCreatedAt(), now).toHours();
                double d = 1.0 - Math.min(hours, 168) / 168.0;
                decay = d < 0.3 ? 0.3 : d;
            }
            double score = v + l * 5 + c * 3 + co * 10;
            int finalScore = (int) Math.round(score * decay);
            p.setHotScore(finalScore);
        }
        postRepository.saveAll(all);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "hotScore"));
        org.springframework.data.domain.Page<Post> page = postRepository.findAll(pageable);
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        int rank = 1;
        for (Post p : page.getContent()) {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", p.getId());
            String title = p.getSummary();
            if (title == null || title.isEmpty()) {
                String c = p.getContent() != null ? p.getContent() : "";
                title = c.length() > 30 ? c.substring(0, 30) : c;
            }
            item.put("title", title);
            item.put("content", p.getContent());
            User u = p.getUser();
            boolean anonymous = Boolean.TRUE.equals(p.getIsAnonymous());
            String uname = anonymous ? "匿名用户"
                    : (u != null && u.getNickname() != null ? u.getNickname() : (u != null ? u.getUsername() : null));
            item.put("userId", u != null ? u.getId() : null);
            item.put("userName", uname);
            String avatar = anonymous ? "/static/avatars/default.png" : (u != null ? u.getAvatarUrl() : null);
            item.put("userAvatar", avatar);
            int views = p.getViewCount() != null ? p.getViewCount() : 0;
            item.put("views", views);
            String viewsText = formatViews(views);
            item.put("viewsText", viewsText);
            item.put("likes", p.getLikeCount());
            item.put("comments", p.getCommentCount());
            item.put("hotRank", rank++);
            String time = p.getCreatedAt() != null
                    ? p.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : "";
            item.put("time", time);
            list.add(item);
        }
        return Result.ok(list);
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
    public Result<java.util.Map<String, Boolean>> top(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id, @RequestBody(required = false) java.util.Map<String, Object> body) {
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
        java.util.Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("isTop", Boolean.TRUE.equals(isTop));
        return Result.ok(data);
    }

    @DeleteMapping("/{id}/top")
    @Transactional
    public Result<java.util.Map<String, Boolean>> untop(@AuthenticationPrincipal UserDetails principal,
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
        java.util.Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("isTop", false);
        return Result.ok(data);
    }
}
