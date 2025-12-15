# 码住校园后端（Spring Boot）

- 技术栈：Spring Boot + Spring Security (JWT) + Spring Data JPA + MySQL + Redis
- 模块：认证与用户、帖子与评论、积分与签到、分类、通知/举报

## 快速开始

- 环境要求：`JDK 21`、`MySQL 8+`、`Redis 6+`
- 数据库与配置：

  - 编辑 `src/main/resources/application.properties`，填写数据库和 Redis 配置（已编辑连接好服务器数据库.redis 还没配）
  - 可使用 `application-init.properties` 首次初始化（`spring.jpa.hibernate.ddl-auto=update`）（已初始化过）

- 启动方式：

  - 在 IDEA 中运行

  ```
  com.campus.campus_backend.CampusBackendApplication
  ```

## 主要接口

- 见群内 API 文档链接

## 前端联调

- 本地开发后端地址：`http://localhost:8080/api`
- 服务器后端地址：`http://127.0.0.1:8080/api`
- 已启用 CORS 与 `OPTIONS` 预检放行，浏览器 H5 可直接调用
- 前端请求会自动注入 `Authorization: Bearer <token>`（登录成功后）

## 安全与配置建议

- 将 `jwt.secret`、数据库密码等改为环境变量或外部化配置，避免泄露
- 生产环境请设置强随机的 `jwt.secret` 和合理的过期时间
- `.gitignore` 已忽略 `target/`、IDE 缓存等
