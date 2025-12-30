package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.UserFollow;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.dto.UpdateProfileRequest;
import com.campus.campus_backend.dto.PointsChangeRequest;
import com.campus.campus_backend.vo.UserProfileVO;
import com.campus.campus_backend.vo.PostItemVO;
import com.campus.campus_backend.vo.PostStatsVO;
import com.campus.campus_backend.vo.UserListItemVO;
import com.campus.campus_backend.vo.UserProfileWithStatsVO;
import com.campus.campus_backend.vo.ErrandListItemVO;
import com.campus.campus_backend.vo.PointTransactionVO;
import com.campus.campus_backend.repository.PostRepository;
import com.campus.campus_backend.repository.UserFollowRepository;
import com.campus.campus_backend.repository.PointTransactionRepository;
import com.campus.campus_backend.repository.PostCollectionRepository;
import com.campus.campus_backend.repository.ErrandRepository;
import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.domain.Errand;
import com.campus.campus_backend.domain.PointTransaction;
import com.campus.campus_backend.domain.PostCollection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PostCollectionRepository postCollectionRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final ZoneId zone = ZoneId.of("Asia/Shanghai");
    private final UserFollowRepository userFollowRepository;
    private final ErrandRepository errandRepository;

    public UsersController(UserRepository userRepository, PostRepository postRepository,
            PointTransactionRepository pointTransactionRepository, PostCollectionRepository postCollectionRepository,
            UserFollowRepository userFollowRepository, ErrandRepository errandRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.postCollectionRepository = postCollectionRepository;
        this.userFollowRepository = userFollowRepository;
        this.errandRepository = errandRepository;
    }

    @GetMapping("/me")
    // 修改返回类型为 UserProfileWithStatsVO，以便携带统计数据
    public Result<UserProfileWithStatsVO> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 1. 获取当前用户实体
        User u = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 2. 实时计算统计数据 (关注数、粉丝数、获赞数)
        // 注意：这里复用了 getUserProfile 里的逻辑，确保数据一致
        Long followingCount = userFollowRepository.countByFollowerId(u.getId());
        Long followerCount = userFollowRepository.countByFollowingId(u.getId());
        Long likeCount = calculateUserLikes(u.getId()); // 调用下方的辅助方法

        // 3. 构建 Stats 对象
        UserProfileWithStatsVO.UserStatsVO stats = new UserProfileWithStatsVO.UserStatsVO(
                likeCount, followingCount, followerCount);

        // 4. 构建完整的返回 VO
        // 参数顺序参考你的 UserProfileWithStatsVO 构造函数:
        // (id, nickname, avatarUrl, school, bio, stats, isFollowing)
        UserProfileWithStatsVO vo = new UserProfileWithStatsVO(
                u.getId(),
                u.getNickname() != null ? u.getNickname() : u.getUsername(), // 确保昵称不为空
                u.getAvatarUrl(),
                u.getSchool(),
                u.getBio(),
                stats,
                false // 查看自己的主页，isFollowing (是否已关注) 默认为 false
        );

        // 5. ⚠️关键：补全基础 VO 里的字段 (如积分、用户名)
        // 因为 UserProfileWithStatsVO 可能继承自 UserProfileVO，或者有 setter
        vo.setUsername(u.getUsername());
        vo.setPoints(u.getPoints() != null ? u.getPoints() : 0);

        return Result.ok(vo);
    }

    @PutMapping("/me")
    @Transactional
    public Result<UserProfileVO> updateMe(@AuthenticationPrincipal UserDetails principal,
            @Validated @RequestBody UpdateProfileRequest req) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User u = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (req.getNickname() != null) {
            u.setNickname(req.getNickname());
        }
        if (req.getBio() != null) {
            u.setBio(req.getBio());
        }
        if (req.getAvatarUrl() != null) {
            u.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getSchool() != null) {
            u.setSchool(req.getSchool());
        }
        userRepository.save(u);
        UserProfileVO vo = new UserProfileVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarUrl());
        vo.setSchool(u.getSchool());
        vo.setPoints(u.getPoints() != null ? u.getPoints() : 0);
        vo.setBio(u.getBio());
        return Result.ok(vo);
    }

    @PostMapping("/me/points")
    @Transactional
    public Result<java.util.Map<String, Integer>> changePoints(@AuthenticationPrincipal UserDetails principal,
            @Validated @RequestBody PointsChangeRequest req) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (req.getAmount() == null || req.getAmount() == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "amount invalid");
        }
        short tp = mapType(req.getType());
        User u = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        int newPoints = u.getPoints() + req.getAmount();
        u.setPoints(newPoints);
        userRepository.save(u);
        PointTransaction pt = new PointTransaction();
        pt.setUser(u);
        pt.setAmount(req.getAmount());
        pt.setType(tp);
        pt.setDescription(req.getDescription());
        pointTransactionRepository.save(pt);
        java.util.Map<String, Integer> data = new java.util.HashMap<>();
        data.put("currentPoints", u.getPoints());
        return Result.ok(data);
    }

    private short mapType(String type) {
        if (type == null)
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "type required");
        String t = type.trim().toLowerCase();
        return switch (t) {
            case "signin" -> 1;
            case "postreward" -> 2;
            case "commentreward" -> 3;
            case "topconsume" -> 4;
            case "errandpay" -> 5;
            case "errandincome" -> 6;
            default -> throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "type invalid");
        };
    }

    @GetMapping("/me/posts")
    public Result<Object> myPosts(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (page < 1)
            page = 1;
        if (size < 1)
            size = 10;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Post> postsPage = postRepository.findByUserUsernameOrderByCreatedAtDesc(principal.getUsername(), pageable);
        List<PostItemVO> list = new ArrayList<>();
        for (Post p : postsPage.getContent()) {
            PostItemVO item = new PostItemVO();
            item.setId(p.getId());
            item.setContent(p.getContent());
            List<String> imgs = new ArrayList<>();
            try {
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    imgs = mapper.readValue(p.getImages(), new TypeReference<List<String>>() {
                    });
                }
            } catch (Exception ignore) {
            }
            item.setImages(imgs);
            String createTime = p.getCreatedAt().atOffset(ZoneOffset.UTC).toInstant().toString();
            item.setCreateTime(createTime);
            PostStatsVO stats = new PostStatsVO();
            stats.setViews(p.getViewCount());
            stats.setLikes(p.getLikeCount());
            stats.setComments(p.getCommentCount());
            item.setStats(stats);
            list.add(item);
        }
        long total = postsPage.getTotalElements();
        boolean hasMore = postsPage.hasNext();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("hasMore", hasMore);
        return Result.ok(data);
    }

    @GetMapping("/me/points/transactions")
    public Result<Object> myPointTransactions(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (page < 1)
            page = 1;
        if (size < 1)
            size = 20;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PointTransaction> ptPage = pointTransactionRepository
                .findByUserUsernameOrderByCreatedAtDesc(principal.getUsername(), pageable);
        List<PointTransactionVO> list = new ArrayList<>();
        for (PointTransaction pt : ptPage.getContent()) {
            PointTransactionVO vo = new PointTransactionVO();
            vo.setType(typeName(pt.getType()));
            vo.setAmount(pt.getAmount());
            vo.setDescription(pt.getDescription());
            String createdTime = pt.getCreatedAt().atZone(zone).format(dtf);
            vo.setCreatedTime(createdTime);
            list.add(vo);
        }
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("list", list);
        return Result.ok(data);
    }

    private String typeName(short t) {
        return switch (t) {
            case 1 -> "SignIn";
            case 2 -> "PostReward";
            case 3 -> "CommentReward";
            case 4 -> "TopConsume";
            case 5 -> "ErrandPay";
            case 6 -> "ErrandIncome";
            default -> "Unknown";
        };
    }

    private String notificationTypeToString(short type) {
        return switch (type) {
            case 1 -> "like";
            case 2 -> "comment";
            case 3 -> "reply";
            case 4 -> "follow";
            default -> "unknown";
        };
    }

    @PostMapping("/me/checkin")
    @Transactional
    public Result<java.util.Map<String, Integer>> checkin(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User u = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        int gained = 10;
        int newPoints = u.getPoints() + gained;
        u.setPoints(newPoints);
        userRepository.save(u);
        PointTransaction pt = new PointTransaction();
        pt.setUser(u);
        pt.setAmount(gained);
        pt.setType((short) 1);
        pt.setDescription("每日签到");
        pointTransactionRepository.save(pt);
        java.util.Map<String, Integer> data = new java.util.LinkedHashMap<>();
        data.put("points", gained);
        data.put("totalPoints", u.getPoints());
        return Result.ok(data);
    }

    @GetMapping("/me/checkin/status")
    public Result<java.util.Map<String, Object>> checkinStatus(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        java.util.List<PointTransaction> signIns = pointTransactionRepository
                .findByUserUsernameAndTypeOrderByCreatedAtDesc(principal.getUsername(), (short) 1);
        java.util.LinkedHashSet<java.time.LocalDate> dates = new java.util.LinkedHashSet<>();
        for (PointTransaction s : signIns) {
            java.time.LocalDate d = s.getCreatedAt().atZone(zone).toLocalDate();
            if (!dates.contains(d)) {
                dates.add(d);
            }
        }
        java.time.LocalDate today = java.time.LocalDate.now(zone);
        boolean checkedIn = dates.contains(today);
        int streak = 0;
        java.time.LocalDate expected = checkedIn ? today : today.minusDays(1);
        for (java.time.LocalDate d : dates) {
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (d.isBefore(expected)) {
                break;
            }
        }
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("checkedIn", checkedIn);
        data.put("checkInDays", streak);
        return Result.ok(data);
    }

    @GetMapping("/me/collects")
    @Transactional(readOnly = true)
    public Result<Object> myCollects(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (page < 1)
            page = 1;
        if (size < 1)
            size = 10;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PostCollection> pcPage = postCollectionRepository
                .findByUserUsernameOrderByCreatedAtDesc(principal.getUsername(), pageable);
        List<PostItemVO> list = new ArrayList<>();
        for (PostCollection pc : pcPage.getContent()) {
            Post p = pc.getPost();
            PostItemVO item = new PostItemVO();
            item.setId(p.getId());
            item.setContent(p.getContent());
            com.campus.campus_backend.domain.User author = p.getUser();
            if (author != null) {
                item.setAuthor(new com.campus.campus_backend.vo.AuthorVO(author.getId(), author.getNickname(),
                        author.getAvatarUrl()));
            }
            List<String> imgs = new ArrayList<>();
            try {
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    imgs = mapper.readValue(p.getImages(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                            });
                }
            } catch (Exception ignore) {
            }
            item.setImages(imgs);
            String createTime = p.getCreatedAt() != null
                    ? p.getCreatedAt().atOffset(ZoneOffset.UTC).toInstant().toString()
                    : "";
            item.setCreateTime(createTime);
            String collectedAt = pc.getCreatedAt() != null
                    ? pc.getCreatedAt().atOffset(ZoneOffset.UTC).toInstant().toString()
                    : "";
            item.setCollectedAt(collectedAt);
            PostStatsVO stats = new PostStatsVO();
            stats.setViews(p.getViewCount());
            stats.setLikes(p.getLikeCount());
            stats.setComments(p.getCommentCount());
            item.setStats(stats);
            list.add(item);
        }
        long total = pcPage.getTotalElements();
        boolean hasMore = pcPage.hasNext();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("hasMore", hasMore);
        return Result.ok(data);
    }

    @GetMapping("/{id}/following")
    public Result<Object> getFollowing(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (page < 1)
            page = 1;
        if (size < 1)
            size = 20;

        Pageable pageable = PageRequest.of(page - 1, size);
        // 使用JOIN FETCH查询避免懒加载问题
        Page<UserFollow> followingPage = userFollowRepository.findByFollowerIdWithFollowingOrderByCreatedAtDesc(id,
                pageable);

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        List<UserListItemVO> list = new ArrayList<>();

        for (UserFollow follow : followingPage.getContent()) {
            User followedUser = follow.getFollowing();
            if (followedUser == null) {
                continue; // 跳过无效数据
            }

            boolean isFollowing = false;
            if (currentUser != null) {
                isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(
                        currentUser.getId(), followedUser.getId());
            }

            UserListItemVO vo = new UserListItemVO(
                    followedUser.getId(),
                    followedUser.getNickname() != null ? followedUser.getNickname() : followedUser.getUsername(),
                    followedUser.getAvatarUrl(),
                    followedUser.getBio(),
                    isFollowing);
            list.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", followingPage.getTotalElements());
        return Result.ok(data);
    }

    @GetMapping("/{id}/followers")
    public Result<Object> getFollowers(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (page < 1)
            page = 1;
        if (size < 1)
            size = 20;

        Pageable pageable = PageRequest.of(page - 1, size);
        // 使用JOIN FETCH查询避免懒加载问题
        Page<UserFollow> followersPage = userFollowRepository.findByFollowingIdWithFollowerOrderByCreatedAtDesc(id,
                pageable);

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        List<UserListItemVO> list = new ArrayList<>();

        for (UserFollow follow : followersPage.getContent()) {
            User followerUser = follow.getFollower();
            if (followerUser == null) {
                continue; // 跳过无效数据
            }

            boolean isFollowing = false;
            if (currentUser != null) {
                isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(
                        currentUser.getId(), followerUser.getId());
            }

            UserListItemVO vo = new UserListItemVO(
                    followerUser.getId(),
                    followerUser.getNickname() != null ? followerUser.getNickname() : followerUser.getUsername(),
                    followerUser.getAvatarUrl(),
                    followerUser.getBio(),
                    isFollowing);
            list.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", followersPage.getTotalElements());
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    // 获取个人主页信息
    public Result<UserProfileWithStatsVO> getUserProfile(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);

        // 查询是否已关注
        boolean isFollowing = false;
        if (currentUser != null) {
            isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(
                    currentUser.getId(), targetUser.getId());
        }

        // 计算统计数据
        Long likes = calculateUserLikes(targetUser.getId());
        Long following = userFollowRepository.countByFollowerId(targetUser.getId());
        Long followers = userFollowRepository.countByFollowingId(targetUser.getId());

        UserProfileWithStatsVO.UserStatsVO stats = new UserProfileWithStatsVO.UserStatsVO(
                likes, following, followers);

        UserProfileWithStatsVO vo = new UserProfileWithStatsVO(
                targetUser.getId(),
                targetUser.getNickname(),
                targetUser.getAvatarUrl(),
                targetUser.getSchool(),
                targetUser.getBio(),
                stats,
                isFollowing);

        return Result.ok(vo);
    }

    private Long calculateUserLikes(Long userId) {
        // 查询用户所有帖子的点赞总数
        List<Post> userPosts = postRepository.findByUserId(userId);
        return userPosts.stream()
                .mapToLong(post -> post.getLikeCount() != null ? post.getLikeCount() : 0L)
                .sum();
    }

    @PostMapping("/{id}/follow")
    @Transactional
    // 关注用户
    public Result<Void> followUser(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 不能关注自己
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "不能关注自己");
        }

        // 检查是否已关注
        if (userFollowRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "已经关注该用户");
        }

        // 创建关注关系
        UserFollow follow = new UserFollow();
        follow.setFollower(currentUser);
        follow.setFollowing(targetUser);
        userFollowRepository.save(follow);

        return Result.okMessage("关注成功");
    }

    @DeleteMapping("/{id}/follow")
    @Transactional
    // 取消关注
    public Result<Void> unfollowUser(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 删除关注关系
        userFollowRepository.deleteByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());

        return Result.okMessage("取消关注成功");
    }

    @GetMapping("/me/errands")
    @Transactional(readOnly = true)
    public Result<Object> myErrands(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "published") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Errand> errandsPage;

        // 根据type参数选择查询方法
        if ("accepted".equals(type)) {
            // 查询我接受的跑腿
            errandsPage = errandRepository.findByRunnerIdOrderByCreatedAtDesc(user.getId(), pageable);
        } else {
            // 默认查询我发布的跑腿
            errandsPage = errandRepository.findByPublisherIdOrderByCreatedAtDesc(user.getId(), pageable);
        }

        List<ErrandListItemVO> list = new ArrayList<>();
        for (Errand errand : errandsPage.getContent()) {
            User publisher = errand.getPublisher();

            if (publisher == null) {
                continue;
            }

            ErrandListItemVO vo = new ErrandListItemVO(
                    errand.getId(),
                    errand.getContent(),
                    errand.getPickupAddr(),
                    errand.getDeliveryAddr(),
                    errand.getBounty(),
                    errand.getCurrency(),
                    errand.getDeadline(),
                    errand.getStatus(),
                    errand.getCreatedAt(),
                    publisher.getId(),
                    publisher.getNickname() != null ? publisher.getNickname() : publisher.getUsername(),
                    publisher.getAvatarUrl() != null ? publisher.getAvatarUrl() : "");
            list.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", errandsPage.getTotalElements());
        data.put("page", page);
        data.put("pageSize", size);
        data.put("hasMore", errandsPage.hasNext());

        return Result.ok(data);
    }

    @GetMapping("/{id}/posts")
    public Result<Object> getUserPosts(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 1. 基础参数校验
        if (page < 1)
            page = 1;
        if (size < 1)
            size = 10;

        // 2. 检查目标用户是否存在 (可选，为了严谨最好加上)
        // userRepository.findById(id).orElseThrow(() -> new
        // BizException(ErrorCode.NOT_FOUND));

        // 3. 查询数据库
        Pageable pageable = PageRequest.of(page - 1, size);

        // ⚠️ 务必确保 Repository 里有 findPostsByUserId 这个 JPQL 方法
        Page<Post> postsPage = postRepository.findPostsByUserId(id, pageable);

        List<PostItemVO> list = new ArrayList<>();

        // 4. 安全转换数据
        for (Post p : postsPage.getContent()) {
            // 防御性判断：如果查出来的对象为空，跳过
            if (p == null)
                continue;

            PostItemVO item = new PostItemVO();
            item.setId(p.getId());
            // 防止内容为空导致前端崩
            item.setContent(p.getContent() != null ? p.getContent() : "");

            // --- 图片处理 (JSON解析防崩) ---
            List<String> imgs = new ArrayList<>();
            try {
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    imgs = mapper.readValue(p.getImages(), new TypeReference<List<String>>() {
                    });
                }
            } catch (Exception e) {
                // 解析失败就给个空列表，不要报错
                System.err.println("图片JSON解析失败 ID:" + p.getId());
            }
            item.setImages(imgs);

            // --- 时间处理 (空值防崩) ---
            if (p.getCreatedAt() != null) {
                item.setCreateTime(p.getCreatedAt().atOffset(ZoneOffset.UTC).toInstant().toString());
            } else {
                item.setCreateTime(""); // 或者给当前时间
            }

            // --- 作者信息处理 (重点：防止懒加载 FetchType.LAZY 报错) ---
            try {
                if (p.getUser() != null) {
                    // 尝试获取用户数据
                    // 如果触发 LazyInitializationException，会跳到 catch
                    String nickname = p.getUser().getNickname();
                    String avatar = p.getUser().getAvatarUrl();

                    // 兜底：如果昵称为空，用用户名代替
                    if (nickname == null || nickname.isEmpty()) {
                        nickname = p.getUser().getUsername();
                    }

                    com.campus.campus_backend.vo.AuthorVO author = new com.campus.campus_backend.vo.AuthorVO(
                            p.getUser().getId(),
                            nickname,
                            avatar);
                    item.setAuthor(author);
                } else {
                    // 如果数据库里 user_id 对应的用户被删了
                    item.setAuthor(new com.campus.campus_backend.vo.AuthorVO(0L, "用户已注销", ""));
                }
            } catch (Exception e) {
                // ⚠️ 捕获所有懒加载异常，返回默认用户
                System.err.println("获取作者信息失败 (可能是懒加载问题): " + e.getMessage());
                item.setAuthor(new com.campus.campus_backend.vo.AuthorVO(0L, "未知用户", ""));
            }

            // --- 统计数据处理 (空值转0) ---
            PostStatsVO stats = new PostStatsVO();
            stats.setViews(p.getViewCount() != null ? p.getViewCount() : 0);
            stats.setLikes(p.getLikeCount() != null ? p.getLikeCount() : 0);
            stats.setComments(p.getCommentCount() != null ? p.getCommentCount() : 0);
            item.setStats(stats);

            list.add(item);
        }

        // 5. 组装返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", postsPage.getTotalElements());
        data.put("hasMore", postsPage.hasNext());

        return Result.ok(data);
    }
}
