# 码住校园后端（Spring Boot）

- 部署地址：`http://120.26.30.91`（后端统一前缀：`/api`）
- 技术栈：Spring Boot、Spring Security（JWT）、Spring Data JPA、MySQL、Redis

## 已实现的功能与特性

- 认证与安全
  - 用户注册、登录、刷新 Token（JWT）
  - 基于 Spring Security 的接口鉴权与异常处理
  - CORS 配置与 `OPTIONS` 预检放行
- 用户模块
  - 获取当前用户信息：`GET /api/users/me`（含粉丝/关注/获赞统计）
  - 更新个人资料：`PUT /api/users/me`（nickname、bio、avatarUrl、school）
  - 关注/取关用户：`POST /api/users/{id}/follow`、`DELETE /api/users/{id}/follow`
  - 关注列表与粉丝列表：`GET /api/users/{id}/following`、`GET /api/users/{id}/followers`
  - 我的帖子、我的收藏、我的积分流水、每日签到与签到状态
  - 我的跑腿（我发布/我接受）：分页查询
- 帖子模块
  - 发帖：文本、图片、商品信息（JSON）；图片与帖子业务绑定（`SysFile`）
  - 列表/搜索/分类筛选、详情、阅读量统计
  - 点赞/取消、收藏/取消，返回 `isLiked`、`isCollected`、`collectCount`
  - 评论与回复、评论点赞状态返回（`isLiked`）
  - 置顶（含积分消耗规则）
  - 热门帖子：`GET /api/posts/hot?limit=5`，计算并更新 `hotScore` 后排序返回
- 评论模块
  - 发表评论与回复、评论列表（顶级与子回复）
  - 点赞数量与当前用户点赞状态
- 分类模块
  - 获取分类列表，发帖按分类归属
- 通知与举报
  - 点赞/评论通知
  - 举报接口
- 可观测与文档
  - Actuator 监控端点（健康、指标等）
  - Springdoc OpenAPI 文档与 Swagger UI

## 工程亮点（Engineering Highlights）

- API 文档（Springdoc OpenAPI）

  - 使用 `springdoc-openapi` 自动生成接口文档与 Swagger UI，便于前后端联调与线上排查
  - 在安全配置中放行文档相关端点，生产环境可按需限制

- 可观测性与监控（Spring Boot Actuator + Prometheus）
  - 集成 Spring Boot Actuator 与 Micrometer Prometheus 注册表
  - 暴露健康检查与指标端点，支持采集与监控看板集成（可与 Prometheus/Grafana 结合）

## 系统组成说明

- 客户端用户界面
  - Web 前端项目（`campus_frontend-main`），通过 `http://<host>:8080/api` 调用后端
  - 已完成本地与服务器联调（统一鉴权与跨域策略）
- 服务端组件
  - Spring Boot 应用：控制器（Controller）、安全（JWT + Security）、业务服务与仓库（JPA Repository）
  - 配置与中间件：CORS、密码加密、异常处理、监控端点、OpenAPI 文档
- 数据存储系统
  - MySQL 8+：所有业务数据（用户、帖子、评论、通知、积分流水、跑腿等）
  - Redis 6+：缓存与会话/在线状态等通用能力（可选按需启用）
