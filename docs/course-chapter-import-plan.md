# 课程章节一键导入功能方案

## 1. 项目现状

### 1.1 已有功能

项目已具备**课程 → 章节 → 课时**三级结构：

| 表 | 说明 |
|---|---|
| `course` | 课程基础信息 |
| `course_chapter` | 课程章节（第1章、第2章等） |
| `course_lesson` | 课时/资源（视频、文档、测验、链接等） |

后端接口（`CourseChapterController.java`）：

| 接口 | 说明 |
|---|---|
| `GET /api/course-chapter/course/{courseId}/chapters` | 查询课程章节列表（含课时） |
| `GET /api/course-chapter/chapters/{chapterId}` | 查询单个章节详情 |
| `POST /api/course-chapter/chapters` | 新增/更新章节 |
| `DELETE /api/course-chapter/chapters/{chapterId}` | 删除章节 |
| `POST /api/course-chapter/lessons` | 新增/更新课时 |
| `DELETE /api/course-chapter/lessons/{lessonId}` | 删除课时 |

### 1.2 已存在问题

#### 1.2.1 权限校验问题

- 教师身份通过 `user.real_name = teacher.real_name` 关联，**重名教师会匹配到错误记录**。
- 学生权限通过 `schedule` 表判断，已退课但未清理 `schedule` 数据的学生仍可访问。
- `@PreAuthorize("hasAuthority('chapter:view')")` 依赖 RBAC 权限数据，如果权限未初始化，接口会直接拒绝访问。

#### 1.2.2 数据校验问题

- 保存章节/课时时未校验 `courseId`/`chapterId` 是否真实存在。
- `chapterNo`/`lessonNo` 没有唯一性约束，可能出现多个"第1章"。
- `sortOrder` 为负值或极大值时排序异常。
- `resourceUrl` 最长 500 字符，可能无法容纳某些长链接。
- `content` 字段为 TEXT 类型，前端传入超长文本会截断。

#### 1.2.3 删除一致性问题

- 删除章节使用逻辑删除（`@TableLogic`）。
- 但删除章节时，下属课时使用物理删除：`lessonMapper.delete(...)`。
- 结果：章节记录还在，课时却被真正删掉了，**数据不一致**。

#### 1.2.4 接口与性能问题

- `listChaptersByCourse` 中对每个章节单独查询 `course` 表，可优化但影响较小。
- 没有分页，大课程返回数据量大。
- 404 返回结构不统一（只返回字符串 `error`）。

#### 1.2.5 前端缺失

- `web-admin` 和 `miniapp` 中均未发现章节管理页面，接口可能未被实际使用。

---

## 2. 一键导入功能设计

### 2.1 功能目标

支持教师或管理员通过上传 Excel 文件，一次性导入某门课程的章节和课时数据。

### 2.2 导入方式选择

| 方式 | 优点 | 缺点 | 推荐度 |
|---|---|---|---|
| Excel 导入 | 教师易准备、实现简单 | 需要规范模板 | ★★★★★ |
| Word/PDF 解析 | 可直接读取教材 | 解析复杂、格式不统一 | ★★★☆☆ |
| Markdown 大纲 | 适合技术人员 | 教师学习成本高 | ★★☆☆☆ |
| AI 自动拆书 | 智能化程度高 | 成本高、结果不稳定 | ★★★☆☆ |

**推荐先做 Excel 导入**。

---

## 3. Excel 导入规范

### 3.1 Excel 模板

| 列号 | 列名 | 必填 | 说明 |
|---|---|---|---|
| A | 章节序号 | 是 | 整数，如 1、2、3 |
| B | 章节名称 | 是 | 如"绪论"、"数据结构" |
| C | 章节描述 | 否 | 章节简介 |
| D | 课时序号 | 是 | 整数，如 1、2、3 |
| E | 课时名称 | 是 | 如"什么是 AI" |
| F | 资源类型 | 否 | 默认 `video`，可选：`video`/`document`/`quiz`/`link` |
| G | 资源URL | 否 | 视频/文档/链接地址 |
| H | 时长(秒) | 否 | 整数，仅视频/音频有效 |
| I | 内容 | 否 | 文档正文、测验 JSON、富文本等 |

### 3.2 示例数据

| 章节序号 | 章节名称 | 章节描述 | 课时序号 | 课时名称 | 资源类型 | 资源URL | 时长(秒) | 内容 |
|---|---|---|---|---|---|---|---|---|
| 1 | 绪论 | 课程简介 | 1 | 什么是AI | video | http://example.com/a.mp4 | 300 | |
| 1 | 绪论 | 课程简介 | 2 | AI发展历史 | document | | | 文档正文... |
| 2 | 数据结构 | | 1 | 数组与链表 | video | http://example.com/b.mp4 | 600 | |
| 2 | 数据结构 | | 2 | 链表操作练习 | quiz | | | 测验JSON... |

### 3.3 处理规则

- **章节合并**：`章节序号` 相同的多行合并为同一个章节。
- **空行跳过**：整行为空时忽略。
- **默认值**：`资源类型` 为空时默认 `video`；`排序` 为空时使用序号值。
- **重复策略**：如果课程下已存在相同 `chapterNo` 的章节，更新其名称和描述，保留原 ID；如果已存在相同 `lessonNo` 的课时，更新其内容。
- **事务策略**：默认全部回滚，任意一行失败则整体失败。
- **错误明细**：返回失败行号和原因。

---

## 4. 接口设计

### 4.1 导入接口

```http
POST /api/course-chapter/import-excel?courseId={courseId}
Content-Type: multipart/form-data

file: [Excel文件]
```

### 4.2 响应结构

```json
{
  "success": true,
  "message": "导入成功",
  "data": {
    "chapterCount": 2,
    "lessonCount": 4,
    "failCount": 0,
    "failures": []
  }
}
```

失败示例：

```json
{
  "success": false,
  "message": "导入失败：存在数据校验错误",
  "data": {
    "chapterCount": 0,
    "lessonCount": 0,
    "failCount": 2,
    "failures": [
      {"row": 3, "reason": "课时序号不能为空"},
      {"row": 5, "reason": "资源类型不合法：mp4"}
    ]
  }
}
```

### 4.3 权限

- 管理员：可导入所有课程。
- 教师：只能导入自己所授课程。
- 学生：无权限。

---

## 5. 开发步骤

### 第一步：添加依赖

检查 `backend/pom.xml` 是否已有 Apache POI 依赖，没有则添加：

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

### 第二步：新增 DTO

- `ChapterImportRowDTO`：Excel 每行解析后的数据。
- `ChapterImportResultDTO`：导入结果统计。
- `ImportFailureDTO`：失败行信息。

### 第三步：新增 Service 方法

在 `CourseChapterService` 中新增：

```java
@Transactional(rollbackFor = Exception.class)
public ChapterImportResultDTO importFromExcel(Long courseId, InputStream excelInput,
                                              Long userId, boolean isAdmin) {
    // 1. 校验课程权限
    // 2. 解析 Excel
    // 3. 数据校验与分组
    // 4. 查询已存在章节/课时
    // 5. 批量插入或更新
    // 6. 返回结果
}
```

### 第四步：新增 Controller 接口

在 `CourseChapterController` 中新增：

```java
@PostMapping("/import-excel")
@PreAuthorize("hasAuthority('chapter:import')")
public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file,
                                     @RequestParam("courseId") Long courseId,
                                     Authentication auth) {
    // 调用 Service 返回结果
}
```

### 第五步：权限配置

在 `init-db/09-rbac-permission.sql` 和 `09-rbac-permission-data.sql` 中补充 `chapter:import` 权限。

### 第六步：修复现有问题（建议同步做）

1. 删除章节时，下属课时也改为逻辑删除。
2. 保存章节/课时时校验 `courseId`/`chapterId` 是否存在。

### 第七步：测试

准备测试 Excel，覆盖以下场景：

| 场景 | 预期 |
|---|---|
| 正常导入 | 章节和课时全部写入 |
| 重复导入 | 已存在章节更新，新增章节插入 |
| 空文件 | 返回错误：文件为空 |
| 格式错误 | 返回失败行和原因 |
| 无权限 | 返回 403 |
| 课程不存在 | 返回 404 |
| 大文件 | 在合理时间内完成 |

### 第八步：前端页面（可选）

在 `web-admin` 增加"章节导入"页面：

- 选择课程。
- 上传 Excel。
- 显示导入结果。
- 提供模板下载。

---

## 6. 风险与应对

| 风险 | 应对措施 |
|---|---|
| Excel 格式不统一 | 提供下载模板，严格校验列名 |
| 大数据量导入超时 | 限制单次最多 1000 行，大量数据分批处理 |
| 重复导入导致数据混乱 | 默认按章节序号/课时序号去重更新 |
| 事务回滚影响用户体验 | 提供"跳过错误继续"可选模式 |
| 权限漏洞 | 导入前必须校验课程归属 |

---

## 7. 建议的实施顺序

1. **先修复现有问题**：删除逻辑一致性、参数存在性校验。
2. **再做 Excel 导入**：按上述步骤开发后端接口。
3. **最后补前端页面**：在 web-admin 增加导入入口。

这样可以让导入功能建立在更稳定的基础上，同时避免旧 bug 被新功能放大。
