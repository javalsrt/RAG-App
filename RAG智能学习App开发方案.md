# RAG 智能学习 App 开发方案

> **版本**：v2.0 | **最后更新**：2026年6月8日
> 
> **状态**：✅ 核心功能已完成 | 🚧 小程序端规划中

---

## 一、项目概述

本项目是一款基于 RAG（检索增强生成）技术的智能学习应用，通过 AI 技术提升学习效率与体验。系统采用 Docker Compose 容器化部署，后端 Spring Boot 单体应用，Android 客户端，纯 HTML 管理端。

### 核心价值
- **AI 赋能学习**：通义千问驱动的智能问答、出题、批改、学情分析
- **RAG 语义检索**：`text-embedding-v3` 向量化 + 余弦相似度检索，精准回答课程问题
- **数据驱动评价**：六维能力评估、专注时长统计、多轮测评追踪
- **实时互动**：WebSocket 实时推送、课程群聊、@mention、签到

---

## 二、当前技术架构

### 2.1 部署架构

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Nginx:80   │───▶│ Backend:8080 │───▶│  MySQL:3306  │
│  静态文件+代理 │    │  Spring Boot │    │  业务数据库   │
└──────────────┘    └──────────────┘    └──────────────┘
       │                    │
       ▼                    ▼
  web-admin/           WebSocket
  (教师管理端)         (实时推送)
                           │
                           ▼
                    Android App
                    (学生端)
```

### 2.2 技术栈

| 层 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.0 |
| **语言** | Java | 17 |
| **数据库** | MySQL | 8.0 |
| **ORM** | MyBatis Plus | 3.5.5 |
| **安全** | Spring Security + JWT (jjwt) | 0.12.3 |
| **AI 大模型** | 通义千问 (qwen-turbo) | DashScope API |
| **向量嵌入** | text-embedding-v3 | OpenAI 兼容接口 |
| **WebSocket** | Spring WebSocket | 原生 |
| **容器化** | Docker Compose | 3 容器 |
| **Nginx** | alpine | 代理 + 静态文件 |
| **Android** | Java + Groovy DSL | minSdk=24, targetSdk=34 |
| **Android HTTP** | Retrofit + OkHttp | 4.x |
| **Web 前端** | 纯 HTML/JS/CSS | Chart.js 图表 |

### 2.3 容器编排

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| `mysql` | mysql:8.0 | 3307→3306 | root/123456, 数据库 znxsglTest |
| `backend` | 自构建（多阶段） | 8080:8080 | Spring Boot JAR, 等待 mysql healthy |
| `nginx` | nginx:alpine | 80:80 | 代理 `/api/*`, `/ws/*`, `/uploads/*` |

---

## 三、已完成功能清单

### 3.1 认证与安全

| 功能 | 实现 |
|------|------|
| JWT 登录 | Spring Security + HS256, 7天有效期 |
| 单设备登录 | `user.current_token` 校验，异地登录自动踢出 |
| 三角色支持 | student(1) / teacher(2) / admin(3) |
| Token 过期处理 | 后端返回 401, Android/Web 拦截跳转登录 |

### 3.2 课表模块

| 功能 | 实现 |
|------|------|
| 学生课表 | 8×8 网格, 6 大节, 周选择器(1-18周), 左右滑动 |
| AI 智能导入 | 上传 xlsx/pdf/doc/docx/图片, AI 提取+校验+冲突检测 |
| 课程下架/上架 | 软删除（status=0）, 可恢复 |
| 调课排课弹窗 | 周次+节次网格, 绿色选中/灰色已排/蓝色占用, 拖拽调课 |
| 冲突检测 | 同班级同时间冲突校验, 课时超限提醒 |

### 3.3 课程聊天系统

| 功能 | 实现 |
|------|------|
| 学生端聊天 | 个人 AI 对话, 课程限定提问, 图片/文件发送 |
| 教师 Web 端群聊 | 课程群聊弹窗, 实时消息轮询(5秒) |
| @mention | 输入 `@` 弹出学生列表+AI, 支持前缀过滤, ↑↓选择 |
| @学生 | 私密消息, 仅目标学生可见, WebSocket 单独推送 |
| @AI | 教师 @AI 触发 AI 回复, 简洁格式(去 Markdown) |
| 签到 | 教师创建密码签到(15分钟有效), 学生验证 |
| 图片/文件 | 上传→存服务器→[image]/[file]标记→前端渲染 |
| 消息推送 | WebSocket `chat_update` 实时推送 |
| 未读红点 | `mention_user_id` + `is_read` 精确统计 |

### 3.4 RAG 智能问答

| 功能 | 实现 |
|------|------|
| AI 对话 | 通义千问 qwen-turbo, OpenAI 兼容接口 |
| 向量检索 | text-embedding-v3 → MySQL JSON 存储 → 余弦相似度 top-5 |
| 关键词降级 | embedding API 失败时自动回退 LIKE 匹配 |
| 课程限定 | system prompt 注入"只回答《{courseName}》内容" |
| 文档分析 | 上传 pdf/doc/docx, AI 提取+分块+向量化 |
| 流式输出 | Android 端逐字打字效果(40ms/字) |

### 3.5 多维度答题系统

| 功能 | 实现 |
|------|------|
| 6 学科板块 | 4 专业(Java/数据结构/网络/数据库) + 2 公共(思政/人文) |
| AI 出题 | 15 题: 7 单选 + 3 判断 + 3 解析 + 2 填空 |
| ViewPager2 竖滑 | 四种题型卡片, 自动跳转/手动提交 |
| 六维评估 | 逻辑/判断/专注/专业/检索/自律, 含建议+7 天计划 |
| 错题解析 | AI 逐题解析+知识点标注, 收藏/明白标记 |
| 测评历史 | 多轮测评纵向对比, 雷达图展示 |

### 3.6 专注模式

| 功能 | 实现 |
|------|------|
| 计时器 | 极简页面, 圆盘点击开始/暂停, ≥60分钟自动格式化 |
| 自动保存 | onPause 自动保存到后端, 今日累计 15 秒刷新 |
| 班级排行 | 按今日专注时长排序 |

### 3.7 教师端（Web 管理后台）

| 功能 | 实现 |
|------|------|
| 数据总览 | 学生总数/在线数/平均时长/学习趋势折线图 |
| 课程管理 | 在线课程卡片, 班级管理, 排课/下架按钮 |
| 学情分析 | 六维雷达图, 学生个人统计, 班级对比 |
| 学生在线检测 | WebSocket 实时检测, 教师 WebSocket 连接 |
| 课程群聊 | 聊天弹窗, @mention, @AI, 签到, 图片/文件 |
| 课表导入 | AI 预览+确认, HTML 教务系统解析 |

### 3.8 代码规模统计

| 层 | 文件数 | 说明 |
|------|------|------|
| 后端 Controller | 9 | Auth/Chat/CheckIn/Focus/Quiz/Schedule/ScheduleImport/TeacherClass/TeacherStats |
| 后端 Service | 5 | Auth/Chat/DashScope/Rag/Schedule |
| 后端 Mapper | 12 | User/ChatMessage/Course/Schedule/FocusSession/DocumentVector/Quiz+ |
| 后端 Entity | 11 | User/ChatMessage/Course/Schedule/FocusSession/DocumentVector/Quiz+ |
| 后端 DTO | 7 | Login/ChatMessage/StudentAskStats/StudentCourse/StudentSchedule/TeacherCourse |
| 后端 WebSocket | 1 | ScheduleWebSocketHandler（学生+教师双通道） |
| 前端 HTML | 2 | login.html + dashboard.html(119KB) |
| Android | 30+ | Activity/Fragment/Adapter/Model/Network |

---

## 四、数据库表结构

### 4.1 核心表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户 | id, student_no, username, password_hash, real_name, role, class_id, current_token |
| `course` | 课程 | id, course_name, teacher_id, semester, course_type, credit |
| `schedule` | 课表 | id, course_name, day_of_week, start_node, step, weeks(JSON), classroom, status |
| `chat_message` | 聊天消息 | id, course_name, sender_role, content, is_read, mention_user_id |
| `focus_session` | 专注记录 | id, user_id, duration_seconds, started_at, finished_at |
| `document_vector` | 文档向量 | id, course_name, content_chunk, embedding(MEDIUMTEXT) |
| `quiz_session` | 测评会话 | id, user_id, subject, scores(JSON), suggestion, study_plan |
| `quiz_answer` | 答题记录 | id, session_id, question, user_answer, correct_answer, is_correct |
| `check_in` | 签到 | id, course_name, password, expires_at, active |
| `check_in_record` | 签到记录 | id, check_in_id, student_id, student_name |

### 4.2 关联表

| 表名 | 说明 |
|------|------|
| `class_info` | 班级信息 |
| `course_class` | 课程-班级关联 |
| `teacher` | 教师 |
| `question_bookmark` | 题目收藏 |

---

## 五、部署指南

### 5.1 本地开发

```bash
# 1. 启动 MySQL（本地或 Docker）
# 2. 导入种子数据
mysql -u root -p123456 znxsglTest < znxsgltest.sql

# 3. 执行增量 SQL
# check_in.sql（签到表）
# ALTER TABLE chat_message ADD COLUMN mention_user_id BIGINT NULL;

# 4. 启动后端（IDEA 直接运行 ZnxsglApplication）
# 5. 打开 web-admin/login.html 测试 Web 端
# 6. Android Studio 运行 App
```

### 5.2 服务器部署

```bash
# 1. 拉取代码
cd ~/test1pj && git pull

# 2. 构建并启动
docker compose up -d --build

# 3. 增量数据库迁移（首次部署需要）
docker compose exec -T mysql mysql -u root -p123456 znxsglTest < backend/src/main/resources/sql/check_in.sql
docker compose exec mysql mysql -u root -p123456 znxsglTest -e "ALTER TABLE chat_message ADD COLUMN mention_user_id BIGINT NULL;"

# 4. 查看日志
docker compose logs -f backend
```

### 5.3 测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | admin | 123456 | 全功能 |
| 教师 | t001 | 123456 | 管理班级/课程/课表 |
| 学生 | 20240101001 | 123456 | 学习功能 |

---

## 六、微信小程序端开发方案

### 6.1 方案对比

| 方案 | 技术栈 | 优点 | 缺点 | 推荐度 |
|------|------|------|------|--------|
| **A. 原生小程序** | WXML + WXSS + JS/TS | 性能好, 组件完整, API 全 | 需单独开发 UI | ⭐⭐⭐⭐⭐ |
| **B. Uni-App** | Vue 3 | 一套代码多端(小程序+H5+App) | 部分 API 受限, 包体积大 | ⭐⭐⭐⭐ |
| **C. Taro** | React/Vue | 类 React 开发体验, 社区活跃 | 学习成本中等 | ⭐⭐⭐⭐ |
| **D. 微信云开发** | 云函数+云数据库 | 免运维, 快速上线 | 与现有后端割裂, 数据难同步 | ⭐⭐ |

### 6.2 推荐方案：Uni-App（方案 B）

**选择理由**：
1. 教师 Web 端是纯 HTML，可复用到 H5 版本
2. 一套代码生成微信小程序 + H5 + 其他平台小程序
3. Vue 3 语法简洁，开发效率高
4. 共享已有的 Spring Boot 后端 API，无需云开发

### 6.3 技术架构（Uni-App）

```
┌─────────────────────────────────────────────┐
│                Uni-App 前端                   │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │ 微信小程序 │  │  H5 版本  │  │ 其他平台   │  │
│  └──────────┘  └──────────┘  └───────────┘  │
│         │             │             │         │
│         └─────────────┼─────────────┘         │
│                       │                       │
│              uni.request / uni.connectSocket  │
└───────────────────────┼───────────────────────┘
                        │
              ┌─────────▼─────────┐
              │  Spring Boot 后端  │
              │   (已有，复用)      │
              └───────────────────┘
```

### 6.4 功能模块规划

#### 学生端小程序

```
底部 Tab:
┌──────────┬──────────┬──────────┐
│   学习    │   课表    │   我的    │
└──────────┘──────────┴──────────┘
```

| Tab | 功能 | 相当于 Android 端 |
|------|------|------|
| **学习** | 课程列表, 课程聊天, AI 问答, 专注入口, 答题入口 | ProfileFragment + CourseDetailActivity |
| **课表** | 周视图网格, 课程详情, 周切换 | ScheduleFragment |
| **我的** | 个人信息, 学习统计, 测评历史, 收藏题目 | ProfileFragment 统计区 |

#### 每门课程聊天页面

| 功能 | 小程序实现方式 |
|------|------|
| 消息展示 | scroll-view + 左右对齐气泡 |
| 文字输入 | input 组件 + 发送按钮 |
| 图片发送 | wx.chooseImage → uploadFile → 发送 |
| 文件发送 | wx.chooseMessageFile → uploadFile |
| AI 对话 | 调用 /api/chat/rag 接口 |
| @AI 问答 | 输入框 @AI 按钮 |
| 签到 | 弹窗输入密码 → /api/checkin/verify |
| 流式显示 | WebSocket 接收 → 逐字渲染 |
| 未读红点 | API /api/chat/unread, 页面 onShow 刷新 |

#### 教师端小程序（可选）

| 功能 | 说明 |
|------|------|
| 课程管理 | 查看课程+班级, 排课 |
| 课堂群聊 | chat 弹窗, @mention, 签到 |
| 数据概览 | 学生总数/在线/专注时长 |
| 学情查看 | 学生个人统计, 六维雷达图 |

### 6.5 Uni-App 项目初始化

```bash
# 1. 安装 HBuilderX 或 CLI
npm install -g @dcloudio/uvm

# 2. 创建项目
npx degit dcloudio/uni-preset-vue#vite uni-student

# 3. 安装依赖
cd uni-student
npm install

# 4. 配置 API 地址
# manifest.json → 微信小程序 AppID
# .env → VUE_APP_API_BASE = https://your-server.com

# 5. 运行开发
npm run dev:mp-weixin  # 微信小程序
npm run dev:h5          # H5 浏览器
```

### 6.6 核心代码示例

#### API 封装（`utils/api.js`）

```javascript
const BASE_URL = 'https://your-server.com/api'

// 请求拦截器
const request = (url, options = {}) => {
  const token = uni.getStorageSync('token')
  return uni.request({
    url: BASE_URL + url,
    header: {
      'Authorization': 'Bearer ' + token,
      ...options.header
    },
    ...options
  })
}

// WebSocket 连接
let socketTask = null
function connectWebSocket(userId) {
  socketTask = uni.connectSocket({
    url: `wss://your-server.com/ws/schedule?userId=${userId}&role=student`
  })
  socketTask.onMessage(res => {
    const msg = JSON.parse(res.data)
    if (msg.type === 'chat_update') {
      // 更新未读红点 + 显示通知
      uni.showToast({ title: `${msg.data.courseName} 新消息`, icon: 'none' })
    }
  })
  // 心跳
  setInterval(() => {
    if (socketTask) socketTask.send({ data: 'ping' })
  }, 30000)
}

export { request, connectWebSocket }
```

#### 课程列表页（`pages/courses/index.vue`）

```vue
<template>
  <view class="page">
    <view class="course-card" v-for="c in courses" :key="c.courseId"
          @click="openChat(c)">
      <text class="icon">{{ getIcon(c.courseId) }}</text>
      <view class="info">
        <text class="name">{{ c.courseName }}</text>
        <text class="detail">{{ c.scheduleInfo || '暂无排课' }}</text>
      </view>
      <view class="badge" v-if="getUnread(c.courseName) > 0">
        {{ getUnread(c.courseName) > 99 ? '99+' : getUnread(c.courseName) }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onShow } from 'vue'
import { request, connectWebSocket } from '@/utils/api'

const courses = ref([])
const unreadMap = ref({})

const ICONS = ['📖','💻','📱','🌐','🎨','🔧','✍️','🗣️','🎬','📋','👥','📜','🏛️','🛡️']

const getIcon = (id) => ICONS[id % ICONS.length]

const getUnread = (name) => unreadMap.value[name] || 0

const loadCourses = async () => {
  const res = await request('/schedule/student/courses')
  courses.value = res.data
  loadUnread()
}

const loadUnread = async () => {
  const res = await request('/chat/unread')
  const map = {}
  res.data.forEach(r => { map[r.courseName] = r.count })
  unreadMap.value = map
}

const openChat = (course) => {
  uni.navigateTo({
    url: `/pages/chat/index?courseName=${encodeURIComponent(course.courseName)}&courseId=${course.courseId}`
  })
}

onShow(() => {
  loadCourses()
  connectWebSocket(uni.getStorageSync('userId'))
})
</script>
```

#### 课程聊天页（`pages/chat/index.vue`）

```vue
<template>
  <view class="chat-page">
    <scroll-view class="msg-list" scroll-y :scroll-into-view="lastId">
      <view v-for="(m, i) in messages" :key="i" :id="'msg-' + i"
            :class="['msg', m.senderRole === 'student' ? 'mine' : 'other']">
        <!-- 文字消息 -->
        <text v-if="!m.content.startsWith('[image]') && !m.content.startsWith('[file]')"
              class="msg-text">{{ m.content }}</text>
        <!-- 图片消息 -->
        <image v-else-if="m.content.startsWith('[image]')"
               :src="BASE_URL + m.content.substring(7)"
               mode="widthFix" class="msg-img" @click="previewImage" />
        <!-- 文件消息 -->
        <view v-else class="msg-file" @click="openFile(m.content)">
          📄 {{ m.fileName || '文件' }}
        </view>
        <text class="msg-time">{{ formatTime(m.createdAt) }}</text>
      </view>
    </scroll-view>

    <view class="input-bar">
      <button class="btn-tool" @click="pickImage">🖼</button>
      <button class="btn-tool" @click="pickFile">📎</button>
      <button class="btn-tool" @click="showCheckIn">📍</button>
      <button class="btn-tool" @click="askAI">@AI</button>
      <input v-model="inputText" placeholder="输入消息..." confirm-type="send"
             @confirm="sendText" class="input" />
      <button class="btn-send" @click="sendText">发送</button>
    </view>
  </view>
</template>
```

### 6.7 WebSocket 实时通信

| 事件 | 方向 | 数据 | 处理 |
|------|------|------|------|
| `chat_update` | 服务端→客户端 | {courseName, senderName, content, senderRole} | Toast + 更新未读 |
| `student_online/offline` | 学生↔教师 | {userId, online} | 教师端更新在线状态 |
| `schedule_update` | 教师→学生 | {courseName, content, scheduleInfo} | 课表变动通知 |
| `ping` | 客户端→服务端 | "ping" | 心跳保活(30s) |
| `pong` | 服务端→客户端 | "pong" | 心跳响应 |

### 6.8 开发里程碑（小程序）

| 阶段 | 内容 | 预计工时 |
|------|------|----------|
| **P1: 基础框架** | Uni-App 项目创建, 登录/Token, Tab 导航 | 2 天 |
| **P2: 课表模块** | 周视图网格, 课程详情, 周切换 | 2 天 |
| **P3: 课程聊天** | 消息列表, 发送文字/图片/文件, 签到弹出 | 3 天 |
| **P4: AI 问答** | 聊天窗集成 AI, @AI 按钮, 流式显示 | 1 天 |
| **P5: 专注+答题** | 计时器页面, 答题页面, 测评结果展示 | 3 天 |
| **P6: 我的页面** | 个人信息, 统计, 测评历史, 收藏 | 1 天 |
| **P7: 教师端(可选)** | 课程管理, 群聊, 数据概览 | 2 天 |
| **P8: 测试+上线** | 真机测试, 审核提交 | 2 天 |
| **总计** | | **14-16 天** |

### 6.9 小程序注意事项

| 事项 | 说明 |
|------|------|
| 域名白名单 | 小程序后台配置 `request` + `socket` + `uploadFile` 合法域名 |
| HTTPS | 服务器必须配置 SSL 证书（`wss://` 和 `https://`） |
| 包大小限制 | 主包 ≤ 2MB, 分包 ≤ 2MB, 总 ≤ 20MB |
| WebSocket 限制 | 同时最多 5 个连接 |
| 文件上传 | `wx.chooseImage` → `wx.uploadFile`, 小程序不支持 FormData |
| 登录流程 | `wx.login()` → code → 后端 `/api/auth/wx-login` → 绑定账号 |

---

## 七、后续扩展规划

### 7.1 短期（1-2 个月）

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 小程序学生端 | Uni-App 完整功能 | ⭐⭐⭐⭐⭐ |
| 流式 SSE 输出 | 后端 SSE → 前端实时流式渲染 | ⭐⭐⭐⭐ |
| 作业模块 | AI 出题 → 发布作业 → 学生作答 → 批改 | ⭐⭐⭐⭐ |
| 通知推送 | 阿里云移动推送 / 微信订阅消息 | ⭐⭐⭐ |

### 7.2 中期（3-6 个月）

| 功能 | 说明 |
|------|------|
| 小程序教师端 | 教师移动端管理 |
| 多模态 RAG | 图片语义搜索, 视频理解 |
| 学习计划 | AI 个性化学习路径规划 |
| 组队学习 | 学习小组, 共享文档, 协同问答 |
| 重排序模型 | bge-reranker 对 RAG 检索 top-20 二次排序 |

### 7.3 长期（6-12 个月）

| 功能 | 说明 |
|------|------|
| 向量数据库 | 迁移到 Chroma/Milvus, 支持 ANN 加速 |
| AI 虚拟导师 | 长期学习轨迹分析 + 个性化辅导 |
| 知识图谱 | 自动构建课程知识点关联 |
| 多校区部署 | 微服务拆分 + K8s 编排 |

---

## 八、项目文件索引

### 后端核心文件

```
backend/src/main/java/com/znxsgl/
├── ZnxsglApplication.java          # 启动类
├── config/
│   ├── SecurityConfig.java          # Spring Security 配置
│   ├── JwtAuthFilter.java           # JWT 认证过滤器
│   ├── JwtUtil.java                 # JWT 工具类
│   ├── DataInitializer.java         # 启动数据初始化
│   ├── WebConfig.java               # 静态资源映射
│   └── WebSocketConfig.java         # WebSocket 注册
├── controller/
│   ├── AuthController.java          # /api/auth/login
│   ├── ChatController.java          # /api/chat/* (消息/RAG/上传/@mention/未读)
│   ├── CheckInController.java       # /api/checkin/* (签到)
│   ├── FocusController.java         # /api/focus/* (专注)
│   ├── QuizController.java          # /api/quiz/* (出题/评估/错题)
│   ├── ScheduleController.java      # /api/schedule/* (课表查询)
│   ├── ScheduleImportController.java # /api/schedule/import/* (导入/排课)
│   ├── TeacherClassController.java  # /api/teacher/class/* (班级管理)
│   └── TeacherStatsController.java  # /api/teacher/* (数据统计)
├── service/
│   ├── AuthService.java             # 登录+JWT生成
│   ├── ChatService.java             # 消息CRUD+@mention解析
│   ├── DashScopeService.java        # 通义千问 API (chat+embed)
│   ├── RagService.java              # RAG 检索+向量存储
│   └── ScheduleService.java         # 课表业务
└── websocket/
    └── ScheduleWebSocketHandler.java # WebSocket 处理器
```

### Android 核心文件

```
android/app/src/main/java/com/znxsgl/student/
├── LoginActivity.java               # 登录
├── MainActivity.java                 # 主框架 + 底部导航
├── CourseDetailActivity.java         # 课程聊天窗
├── FocusActivity.java                # 专注计时器
├── fragment/
│   ├── ScheduleFragment.java         # 课表
│   ├── FocusFragment.java            # 学习页(课程+答题)
│   └── ProfileFragment.java          # 我的(统计+课程列表)
├── adapter/                          # RecyclerView 适配器
├── model/                            # 数据模型
└── network/
    ├── RetrofitClient.java           # HTTP 客户端
    ├── ApiService.java               # API 接口定义
    └── WebSocketManager.java         # WebSocket 连接管理
```

### Web 端文件

```
web-admin/
├── login.html                        # 教师登录(9KB, iOS 风格)
└── dashboard.html                    # 管理后台(119KB, 全功能)
```

---

*文档版本: v2.0 | 最后更新: 2026年6月8日*
*项目路径: C:\Users\jay\Desktop\myBishe\test1pj*
