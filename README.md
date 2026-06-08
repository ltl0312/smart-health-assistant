# 智能健康助手 Smart Health Assistant

基于 Spring Boot 3 + Vue3 + MySQL + Redis + DeepSeek 大模型的个性化饮食与运动规划系统。

## 🚀 快速启动

### 1. 数据库

```bash
mysql -u root -p < health-assistant-backend/src/main/resources/init.sql
```

### 2. Docker 部署（推荐）

```bash
docker compose up -d --build
```

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 (Nginx) | 80 | Vue3 SPA |
| 后端 (Spring Boot) | 8080 | REST API |
| MySQL 8.0 | 3307 | 数据库 |
| Redis 7 | 6379 | 排行榜缓存 |

访问 `http://localhost`

### 3. 本地开发

```bash
# 后端
cd health-assistant-backend
mvn spring-boot:run

# 前端
cd health-assistant-frontend
npm install && npm run dev
```

前端 `http://localhost:5173`，后端 `http://localhost:8080/api`

---

## 📡 API 文档

### 认证模块 `/api/auth`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/auth/register` | 用户注册 | 无 |
| POST | `/auth/login` | 用户登录，返回 JWT (含 role) | 无 |

**注册请求：**
```json
{ "username": "test", "password": "123456", "email": "test@example.com" }
```

**登录响应：**
```json
{ "code": 200, "data": { "token": "eyJ...", "userId": 1, "username": "test", "role": "USER" } }
```

---

### 用户模块 `/api/user`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/user/profile` | 获取个人信息 | JWT |
| PUT | `/user/profile` | 更新昵称/手机/邮箱/简介 | JWT |
| PUT | `/user/password` | 修改密码（需旧密码） | JWT |
| POST | `/user/avatar` | 上传头像 (multipart) | JWT |
| DELETE | `/user/account` | 注销账户 | JWT |

---

### 健康档案 `/api/profile`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/profile` | 获取健康档案 | JWT |
| POST | `/profile/setup` | 创建/更新档案 | JWT |
| PUT | `/profile/height` | 更新身高（周限3次） | JWT |
| GET | `/profile/export` | 导出健康档案 (.md) | JWT |

**档案请求：**
```json
{
  "age": 28, "gender": 1, "heightCm": 175, "baselineWeight": 80,
  "activityLevel": "MODERATE", "dietPreference": "BALANCED", "healthGoal": "FAT_LOSS"
}
```

---

### 体重记录 `/api/weight`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/weight/record` | 记录体重（自动算BMI） | JWT |
| PUT | `/weight/record` | 修改当日体重（日限2次） | JWT |
| GET | `/weight/history?days=30` | 获取历史体重 | JWT |

---

### AI 计划 `/api/plan`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/plan/generate` | 生成 AI 干预计划 | JWT |

**请求：** `{ "cycleStartDate": "2026-06-09" }`

---

### 健康档案 `/api/records`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/records` | 历史 AI 计划列表（3个月） | JWT |
| GET | `/records/{id}/download` | 下载报告 (.md) | JWT |
| DELETE | `/records/{id}` | 删除报告 | JWT |

---

### 排行榜 `/api/rank`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/rank/health` | 健康得分排行榜 | 无 |

**得分公式：** `100 + 连续周×2 - |BMI-22|×5 + 目标达成奖励`

---

### 管理员 `/api/admin`（需 ADMIN 角色）

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/admin/stats` | 系统统计数据 | JWT + ADMIN |
| GET | `/admin/users` | 用户列表 | JWT + ADMIN |

---

## 📁 项目结构

```
smart-health-assistant/
├── docker-compose.yml
├── .gitignore
├── README.md
├── health-assistant-backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hnust/health/
│       │   ├── SmartHealthApplication.java
│       │   ├── annotation/RequireRole.java
│       │   ├── aop/RoleCheckAspect.java
│       │   ├── config/       # CORS, WebMvc, DeepSeek, Retry
│       │   ├── constant/     # 常量与枚举
│       │   ├── controller/   # REST 控制器
│       │   ├── dto/          # Request/Response DTO
│       │   ├── exception/    # 全局异常处理
│       │   ├── interceptor/  # JWT 认证拦截器
│       │   ├── mapper/       # MyBatis-Plus 数据访问
│       │   ├── model/        # 领域实体
│       │   ├── security/     # JWT + BCrypt
│       │   ├── service/      # 业务接口
│       │   │   └── impl/     # 业务实现
│       │   ├── util/         # 工具类
│       │   └── utils/        # 辅助工具 (PromptCleaner)
│       └── resources/
│           ├── application.yml
│           └── init.sql
└── health-assistant-frontend/
    ├── Dockerfile
    ├── nginx.conf
    ├── tailwind.config.js
    └── src/
        ├── App.vue
        ├── style.css
        ├── api/request.js
        ├── stores/user.js, theme.js
        ├── router/index.js
        ├── views/
        │   ├── LoginView.vue
        │   ├── DashboardView.vue
        │   ├── ProfileSetupView.vue
        │   ├── ProfileView.vue
        │   ├── RankView.vue
        │   ├── AdminView.vue
        │   └── HealthRecordsView.vue
        └── components/
            ├── WeightTrendChart.vue
            └── AiPlanCard.vue
```

## 🛠 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue3 + Vite + Tailwind CSS + ECharts + Pinia |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + JWT + BCrypt |
| 数据库 | MySQL 8.0 + Redis 7 |
| AI | DeepSeek V4 Flash API |
| 部署 | Docker Compose + Nginx |
| 测试 | JUnit 5 + Mockito + Vitest |
