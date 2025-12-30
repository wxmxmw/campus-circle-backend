package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Errand;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.PointTransaction;
import com.campus.campus_backend.dto.CreateErrandRequest;
import com.campus.campus_backend.repository.ErrandRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.repository.PointTransactionRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import com.campus.campus_backend.vo.ErrandListItemVO;

@RestController
@RequestMapping("/api/errands")
public class ErrandsController {
    private final ErrandRepository errandRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ErrandsController(ErrandRepository errandRepository,
                             UserRepository userRepository,
                             PointTransactionRepository pointTransactionRepository) {
        this.errandRepository = errandRepository;
        this.userRepository = userRepository;
        this.pointTransactionRepository = pointTransactionRepository;
    }

    @PostMapping
    @Transactional
    public Result<Object> createErrand(@AuthenticationPrincipal UserDetails principal,
                                       @Validated @RequestBody CreateErrandRequest req) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 如果是积分支付，检查用户积分是否足够
        if (req.getCurrency() == 1) {
            int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            if (currentPoints < req.getBounty()) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "积分不足");
            }

            // 扣除积分
            user.setPoints(currentPoints - req.getBounty());
            userRepository.save(user);

            // 记录积分流水 (类型5: errandpay)
            PointTransaction pt = new PointTransaction();
            pt.setUser(user);
            pt.setAmount(-req.getBounty());
            pt.setType((short) 5);
            pt.setDescription("发布跑腿需求");
            pointTransactionRepository.save(pt);
        }

        // 创建跑腿需求
        Errand errand = new Errand();
        errand.setPublisher(user);
        errand.setContent(req.getContent());
        errand.setPickupAddr(req.getPickupAddr());
        errand.setDeliveryAddr(req.getDeliveryAddr());
        errand.setHiddenInfo(req.getHiddenInfo());
        errand.setBounty(java.math.BigDecimal.valueOf(req.getBounty()));
        errand.setCurrency(req.getCurrency().shortValue());
        errand.setDeadline(LocalDateTime.parse(req.getDeadline(), dtf));
        errand.setStatus((short) 0); // 待接单

        errand = errandRepository.save(errand);

        // 返回正确的响应格式
        Map<String, Object> data = new HashMap<>();
        data.put("id", errand.getId());
        data.put("content", errand.getContent());
        data.put("pickupAddr", errand.getPickupAddr());
        data.put("deliveryAddr", errand.getDeliveryAddr());
        data.put("bounty", errand.getBounty());
        data.put("currency", errand.getCurrency());
        data.put("deadline", errand.getDeadline());
        data.put("status", errand.getStatus());
        data.put("createTime", errand.getCreatedAt());

        return Result.ok("发布成功", data);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Result<Object> listErrands(
            @RequestParam(required = false, defaultValue = "0") Integer status,
            @RequestParam(required = false, defaultValue = "bounty_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 1)
            page = 1;
        if (size < 1)
            size = 10;

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Errand> errandsPage;

        // 根据排序参数选择查询方法
        if ("bounty_desc".equals(sort)) {
            errandsPage = errandRepository.findByStatusOrderByBountyDesc(status, pageable);
        } else {
            errandsPage = errandRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }

        List<ErrandListItemVO> list = new ArrayList<>();
        for (Errand errand : errandsPage.getContent()) {
            User publisher = errand.getPublisher();

            // 添加null检查防止异常
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

    @PostMapping("/{id}/accept")
    @Transactional
    public Result<Object> acceptErrand(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User runner = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 使用简单查询
        Errand errand = errandRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 检查跑腿状态 - 只有待接单状态才能接单
        if (errand.getStatus() != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿已被接单或已完成");
        }

        // 检查发布者信息，进行null检查
        User publisher = errand.getPublisher();
        if (publisher == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿信息不完整");
        }

        // 不能接自己的单
        if (publisher.getId().equals(runner.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "不能接自己的跑腿");
        }

        // 更新跑腿状态和跑腿员
        errand.setRunner(runner);
        errand.setStatus((short) 1); // 进行中
        errand = errandRepository.save(errand);

        // 给跑腿员增加积分 (类型6: errandincome)
        if (errand.getCurrency() == 1) {
            int currentPoints = runner.getPoints() != null ? runner.getPoints() : 0;
            runner.setPoints(currentPoints + errand.getBounty().intValue());
            userRepository.save(runner);

            PointTransaction pt = new PointTransaction();
            pt.setUser(runner);
            pt.setAmount(errand.getBounty().intValue());
            pt.setType((short) 6); // errandincome类型
            pt.setDescription("完成跑腿任务");
            pointTransactionRepository.save(pt);
        }

        // 构建响应数据，避免直接序列化实体
        Map<String, Object> data = new HashMap<>();
        data.put("id", errand.getId());
        data.put("status", errand.getStatus());
        data.put("runnerId", errand.getRunner().getId());
        data.put("runnerName", errand.getRunner().getNickname() != null ?
                errand.getRunner().getNickname() : errand.getRunner().getUsername());

        return Result.ok("接单成功", data);
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public Result<Object> completeErrand(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable Long id){
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User publisher = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 使用简单查询
        Errand errand = errandRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 检查跑腿状态 - 只有进行中状态才能完成
        if (errand.getStatus() != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿状态不正确，无法完成");
        }

        // 检查发布者信息，进行null检查
        User errandPublisher = errand.getPublisher();
        if (errandPublisher == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿信息不完整");
        }

        // 只有发布者可以确认完成
        if (!errandPublisher.getId().equals(publisher.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "只有发布者可以确认完成");
        }

        // 检查是否有跑腿员
        if (errand.getRunner() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿尚未被接单");
        }

        // 更新跑腿状态为已完成
        errand.setStatus((short) 2); // 已完成
        errand.setCompletedAt(LocalDateTime.now());
        errand = errandRepository.save(errand);

        // 构建响应数据，避免直接序列化实体
        Map<String, Object> data = new HashMap<>();
        data.put("id", errand.getId());
        data.put("status", errand.getStatus());
        data.put("completedAt", errand.getCompletedAt());

        return Result.ok("确认完成成功", data);
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public Result<Errand> cancelErrand(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User publisher = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 使用简单查询
        Errand errand = errandRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 检查跑腿状态 - 只有待接单状态才能取消
        if (errand.getStatus() != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿已被接单，无法取消");
        }

        // 检查发布者信息，进行null检查
        User errandPublisher = errand.getPublisher();
        if (errandPublisher == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿信息不完整");
        }

        // 只有发布者可以取消
        if (!errandPublisher.getId().equals(publisher.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "只有发布者可以取消跑腿");
        }

        // 更新跑腿状态为已取消
        errand.setStatus((short) 3); // 已取消
        errand = errandRepository.save(errand);

        // 退还赏金 (如果是积分支付)
        if (errand.getCurrency() == 1) {
            int currentPoints = publisher.getPoints() != null ? publisher.getPoints() : 0;
            publisher.setPoints(currentPoints + errand.getBounty().intValue());
            userRepository.save(publisher);

            // 记录积分流水 (类型5: errandpay，但金额为正数表示退还)
            PointTransaction pt = new PointTransaction();
            pt.setUser(publisher);
            pt.setAmount(errand.getBounty().intValue());
            pt.setType((short) 5);
            pt.setDescription("取消跑腿退还赏金");
            pointTransactionRepository.save(pt);
        }

        return Result.ok(errand);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public Result<Object> getErrandDetail(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 使用JOIN FETCH查询避免LAZY加载问题
        Errand errand = errandRepository.findByIdWithPublisher(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        // 检查发布者信息，进行null检查
        User publisher = errand.getPublisher();
        if (publisher == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "跑腿信息不完整");
        }

        // 构建符合API规范的响应数据
        Map<String, Object> data = new HashMap<>();
        data.put("id", errand.getId());

        // 构建发布者信息
        Map<String, Object> publisherInfo = new HashMap<>();
        if (publisher != null) {
            publisherInfo.put("id", publisher.getId());
            publisherInfo.put("nickname", publisher.getNickname() != null ? publisher.getNickname() : publisher.getUsername());
            publisherInfo.put("avatar", publisher.getAvatarUrl());
        }
        data.put("publisher", publisherInfo);

        data.put("content", errand.getContent());
        data.put("pickupAddr", errand.getPickupAddr());
        data.put("deliveryAddr", errand.getDeliveryAddr());
        data.put("bounty", errand.getBounty());
        data.put("status", errand.getStatus());
        data.put("hiddenInfo", errand.getHiddenInfo());
        data.put("createTime", errand.getCreatedAt());

        // 返回跑腿员ID而不是完整对象
        data.put("runnerId", errand.getRunner() != null ? errand.getRunner().getId() : null);

        return Result.ok(data);
    }
}
