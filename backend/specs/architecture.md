# aiStudy 智能学习管理系统架构图

## 1. 项目概述

aiStudy 是一套面向职业学校的智能学习管理系统，覆盖管理员、教师、学生三种角色，提供课程管理、课表排课、学习统计、AI 答疑、专注训练、考勤签到等核心能力。

## 2. 系统架构

```mermaid
graph TB
    subgraph 用户层
        A1[管理员 Admin]
        A2[教师 Teacher]
        A3[学生 Student]
    end

    subgraph 前端层
        B1[web-admin-react<br/>React + TypeScript + Tailwind + shadcn/ui<br/>管理后台（新）]
        B2[web-admin<br/>Vue3 + Element Plus<br/>管理后台（旧）]
        B3[miniapp<br/>uni-app + Vue2<br/>微信小程序]
        B4[android<br/>原生 Android + Retrofit<br/>学生 APP]
    end

    subgraph 接入层
        C1[Nginx 反向代理 / 静态资源]
        C2[Docker Compose 部署]
    end

    subgraph 后端层
        D1[Spring Boot 3.2 + Java 17]
        D2[JWT 认证与安全]
        D3[Controller 接口层]
        D4[Service 业务层]
        D5[Mapper 数据层<br/>MyBatis-Plus]
        D6[WebSocket 实时通信]
    end

    subgraph 数据层
        E1[(MySQL 8.x)]
        E2[Redis 可选缓存]
    end

    subgraph AI / 外部服务
        F1[SiliconFlow / DeepSeek<br/>大模型 API]
        F2[DashScope 通义千问]
        F3[阿里云 OSS 文件存储]
    end

    A1 --> B1
    A1 --> B2
    A2 --> B1
    A2 --> B2
    A3 --> B3
    A3 --> B4

    B1 --> C1
    B2 --> C1
    B3 --> C1
    B4 --> C1

    C1 --> D3
    D3 --> D4
    D4 --> D5
    D5 --> E1

    D1 --> D2
    D1 --> D6

    D4 --> F1
    D4 --> F2
    D4 --> F3
```

## 3. 模块说明

| 模块 | 技术栈 | 职责 |
|------|--------|------|
| `web-admin-react` | React 18 + TypeScript + Vite 5 + Tailwind CSS + shadcn/ui + Zustand | 新版管理后台，面向管理员和教师，参考千问 AI 平台风格 |
| `web-admin` | Vue 3 + Element Plus + Vite | 旧版管理后台，逐步迁移至 React 版本 |
| `miniapp` | uni-app + Vue 2 + Vite | 学生微信小程序，提供课表、学习、聊天、个人中心 |
| `android` | 原生 Android + Retrofit + WebSocket | 学生 Android APP，功能同小程序 |
| `backend` | Spring Boot 3.2 + MyBatis-Plus + JWT + WebSocket | 统一业务接口与数据处理 |
| `init-db` | SQL 脚本 + Python 生成工具 | 数据库初始化、测试数据、迁移脚本 |
| `nginx` | Nginx | 反向代理、负载均衡、静态资源托管 |

## 4. 后端分层架构

```mermaid
flowchart TB
    subgraph Controller 接口层
        C1[AuthController]
        C2[AdminUserController]
        C3[ScheduleController]
        C4[ScheduleImportController]
        C5[TeacherClassController]
        C6[TeacherScheduleAdjustController]
        C7[ChatController]
        C8[FocusController]
        C9[QuizController]
        C10[SemesterController]
    end

    subgraph Service 业务层
        S1[AuthService]
        S2[ScheduleService]
        S3[AutoScheduleService]
        S4[ScheduleConflictChecker]
        S5[ChatService]
        S6[RagService]
        S7[DashScopeService]
    end

    subgraph Mapper 数据层
        M1[UserMapper]
        M2[ScheduleMapper]
        M3[CourseMapper]
        M4[ClassInfoMapper]
        M5[TeacherMapper]
        M6[ChatMessageMapper]
        M7[FocusSessionMapper]
    end

    subgraph 实体与 DTO
        E1[Entity 数据库实体]
        E2[DTO 数据传输对象]
    end

    C1 --> S1
    C2 --> S1
    C3 --> S2
    C4 --> S2
    C5 --> S2
    C6 --> S2
    C7 --> S5
    C8 --> S5
    C9 --> S5
    C10 --> S2

    S2 --> S3
    S2 --> S4
    S3 --> S4
    S5 --> S6
    S6 --> S7

    S1 --> M1
    S2 --> M2
    S2 --> M3
    S2 --> M4
    S2 --> M5
    S5 --> M6
    S5 --> M7

    M1 --> E1
    M2 --> E1
    M3 --> E1
    M6 --> E1

    C1 --> E2
    C2 --> E2
    C3 --> E2
```

## 5. 核心数据流

### 5.1 登录流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant A as AuthController
    participant S as AuthService
    participant M as UserMapper
    participant DB as MySQL

    U->>F: 输入账号密码
    F->>A: POST /api/auth/login
    A->>S: 校验身份
    S->>M: 查询用户
    M->>DB: SELECT
    DB-->>M: 用户记录
    M-->>S: User
    S->>S: 校验密码 + 生成 JWT
    S-->>A: token + role + userId
    A-->>F: LoginResponse
    F->>F: 存储 token，按角色渲染菜单
```

### 5.2 排课流程

```mermaid
sequenceDiagram
    participant T as 教师
    participant R as web-admin-react
    participant SC as ScheduleController
    participant SI as ScheduleImportController
    participant SS as ScheduleService
    participant SCC as ScheduleConflictChecker
    participant DB as MySQL

    T->>R: 上传课表 Excel / 手动排课
    R->>SI: POST /api/schedule/import-preview
    SI->>SI: Apache POI 解析 + AI 识别
    SI-->>R: 预览数据
    R->>SI: POST /api/schedule/import-confirm
    SI->>SCC: 冲突检测
    SCC->>DB: 查询现有排课
    DB-->>SCC: 排课记录
    SCC-->>SI: 无冲突
    SI->>DB: INSERT schedule
    DB-->>SI: 成功
    SI-->>R: 排课结果
    T->>R: 查看班级课表
    R->>SC: GET /api/schedule/teacher/class-schedule
    SC->>SS: 查询课表
    SS->>DB: SELECT
    DB-->>SS: 排课记录
    SS-->>SC: List<StudentScheduleDTO>
    SC-->>R: 课表数据
```

### 5.3 AI 答疑流程

```mermaid
sequenceDiagram
    participant S as 学生
    participant M as miniapp / android
    participant C as ChatController
    participant CS as ChatService
    participant RS as RagService
    participant AI as 大模型 API
    participant DB as MySQL

    S->>M: 发送问题
    M->>C: POST /api/chat/send
    C->>CS: 处理消息
    CS->>RS: RAG 检索相关知识
    RS->>DB: 查询文档向量
    DB-->>RS: 相关知识
    RS->>AI: 构造 prompt 请求
    AI-->>RS: 模型回复
    RS-->>CS: 生成答案
    CS->>DB: 保存聊天记录
    DB-->>CS: 成功
    CS-->>C: ChatMessageDTO
    C-->>M: 返回回复
    M->>S: 展示答案
```

## 6. 数据库核心表

```mermaid
erDiagram
    USER ||--o{ FOCUS_SESSION : has
    USER ||--o{ SCHEDULE : has
    USER ||--o{ CHAT_MESSAGE : sends
    USER }o--|| CLASS_INFO : belongs_to
    COURSE ||--o{ COURSE_CLASS : contains
    CLASS_INFO ||--o{ COURSE_CLASS : contains
    COURSE ||--o{ SCHEDULE : scheduled
    TEACHER ||--o{ COURSE : teaches

    USER {
        bigint id PK
        varchar username
        varchar password_hash
        varchar real_name
        tinyint role
        bigint class_id FK
    }

    CLASS_INFO {
        bigint id PK
        varchar name
        varchar grade
        varchar major
    }

    COURSE {
        bigint id PK
        varchar course_name
        int credit
        tinyint status
        bigint teacher_id FK
    }

    COURSE_CLASS {
        bigint id PK
        bigint course_id FK
        bigint class_id FK
        varchar semester
    }

    SCHEDULE {
        bigint id PK
        bigint user_id FK
        bigint course_id FK
        varchar course_name
        int day_of_week
        int start_node
        int step
        time start_time
        time end_time
        varchar classroom
        json weeks
        varchar semester
        tinyint status
    }

    TEACHER {
        bigint id PK
        bigint user_id FK
        varchar real_name
        varchar subject
    }

    CHAT_MESSAGE {
        bigint id PK
        bigint user_id FK
        varchar role
        text content
        datetime created_at
    }

    FOCUS_SESSION {
        bigint id PK
        bigint user_id FK
        int duration
        datetime start_time
        datetime end_time
    }
```

## 7. 部署拓扑

```mermaid
graph LR
    subgraph 生产环境
        A1[用户浏览器]
        A2[微信小程序]
        A3[Android APP]
    end

    subgraph 服务器
        B1[Nginx]
        B2[web-admin-react 静态资源]
        B3[web-admin 静态资源]
        B4[Spring Boot Jar]
        B5[(MySQL)]
    end

    A1 -->|HTTPS| B1
    A2 -->|HTTPS| B1
    A3 -->|HTTPS| B1
    B1 -->|/api/*| B4
    B1 -->|/| B2
    B1 -->|/legacy| B3
    B4 --> B5
```

## 8. 技术栈汇总

| 层级 | 选型 |
|------|------|
| 前端（新管理端） | React 18 + TypeScript + Vite 5 + Tailwind CSS + shadcn/ui + Zustand + React Router + Recharts |
| 前端（旧管理端） | Vue 3 + Element Plus + Vite |
| 前端（小程序） | uni-app + Vue 2 + Vite |
| 前端（Android） | 原生 Android + Retrofit + WebSocket |
| 后端 | Spring Boot 3.2 + Java 17 + MyBatis-Plus + Spring Security + JWT + WebSocket |
| 数据库 | MySQL 8.x |
| AI 接口 | SiliconFlow / DeepSeek / DashScope |
| 文件存储 | 阿里云 OSS（推荐） / 本地磁盘 |
| 部署 | Docker + Docker Compose + Nginx |

## 9. 演进方向

1. **管理端迁移**：逐步将 `web-admin` 的功能迁移到 `web-admin-react`，最终下线旧版。
2. **组件库统一**：将 `web-admin-react` 的 UI 组件沉淀为项目级设计系统。
3. **服务拆分**：随着业务增长，可将 AI 服务、文件服务、通知服务拆分为独立微服务。
4. **数据可视化**：引入更丰富的学习分析图表，支持班级对比、趋势预测。
