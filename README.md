# Campus Backend (Spring Boot)

- **Deployment URL:** `http://120.26.30.91` (Base API path: `/api`)
- **Tech Stack:** Spring Boot, Spring Security (JWT), Spring Data JPA, MySQL, Redis

## Implemented Features

### Authentication & Security
- User registration, login, and JWT token refresh
- API authentication and authorization using Spring Security, with centralized exception handling
- CORS configuration and support for `OPTIONS` preflight requests

### User Management
- Retrieve current user information: `GET /api/users/me` (including follower, following, and total likes statistics)
- Update user profile: `PUT /api/users/me` (nickname, bio, avatarUrl, school)
- Follow/unfollow users: `POST /api/users/{id}/follow`, `DELETE /api/users/{id}/follow`
- Retrieve following and follower lists: `GET /api/users/{id}/following`, `GET /api/users/{id}/followers`
- Manage personal posts, bookmarked posts, points transaction history, daily check-ins, and check-in status
- Retrieve paginated lists of errands posted or accepted by the current user

### Post Management
- Create posts with text, images, and product information (JSON), with uploaded images linked to posts through `SysFile`
- Browse, search, filter by category, and view post details with view count tracking
- Like/unlike and bookmark/unbookmark posts, with `isLiked`, `isCollected`, and `collectCount` returned in API responses
- Comment on posts and reply to comments, including comment like status (`isLiked`)
- Pin posts with points deduction rules
- Retrieve trending posts: `GET /api/posts/hot?limit=5`, with calculated and updated `hotScore` values used for ranking

### Comment Management
- Create comments and replies, and retrieve hierarchical comment lists (top-level comments and nested replies)
- Track comment like counts and the current user's like status

### Category Management
- Retrieve available post categories
- Organize posts by category

### Notifications & Reporting
- Notifications for post likes and comments
- Content reporting API

### Monitoring & API Documentation
- Spring Boot Actuator endpoints for health checks and application metrics
- Automatically generated OpenAPI documentation and Swagger UI using Springdoc OpenAPI

## Engineering Highlights

### API Documentation (Springdoc OpenAPI)
- Integrated `springdoc-openapi` to automatically generate API documentation and provide an interactive Swagger UI, facilitating frontend-backend integration and troubleshooting
- Configured Spring Security to allow access to documentation endpoints, with optional access restrictions for production environments

### Observability & Monitoring (Spring Boot Actuator + Prometheus)
- Integrated Spring Boot Actuator and Micrometer Prometheus Registry
- Exposed health check and metrics endpoints to support monitoring and metrics collection
- Compatible with Prometheus and Grafana for metrics visualization and monitoring dashboards

## System Architecture

### Client-Side Interface
- **Web Frontend:** `campus_frontend-main`, communicating with the backend through `http://<host>:8080/api`
- Completed frontend-backend integration testing in both local and server environments, with consistent authentication and CORS policies

### Backend Services
- **Spring Boot Application:** REST controllers, JWT authentication with Spring Security, business logic services, and JPA repositories
- **Configuration & Middleware:** CORS handling, password encryption, centralized exception handling, monitoring endpoints, and OpenAPI documentation

### Data Storage
- **MySQL 8+:** Persistent storage for business data, including users, posts, comments, notifications, points transactions, and errands
- **Redis 6+:** Optional support for caching, session management, and online status tracking
