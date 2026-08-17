# 课程章节 Word 导入功能方案

## 一、需求背景

目前课程章节已支持 Excel 导入，但教师日常更常使用 Word 编写课程大纲、教学进度表等文档。因此需要补充 Word 导入能力，降低教师录入成本。

## 二、学习通章节结构参考

经网络检索，学习通（超星）主要以在线编辑器方式组织课程章节，典型结构为：

```
课程
├── 第1章 章节标题
│   ├── 1.1 课时标题（视频/PPT/文档）
│   ├── 1.2 课时标题
│   └── 1.3 课时标题
├── 第2章 章节标题
│   ├── 2.1 课时标题
│   └── 2.2 课时标题
```

教师在 Word 中通常使用以下两种方式编写：

1. **标题层级式**：用 Word 内置"标题 1"、"标题 2"样式区分章节与课时。
2. **表格清单式**：用表格列出章节序号、章节名称、课时序号、课时名称、资源类型、资源 URL、时长等。

本功能同时支持以上两种格式，并自动识别。

## 三、Word 导入格式规范

### 3.1 标题层级式

示例：

```text
第1章 Python 基础
  1.1 Python 简介
      本课时介绍 Python 的发展历史与应用场景。
  1.2 变量与数据类型
      讲解变量、字符串、数字、布尔类型。

第2章 流程控制
  2.1 条件语句
      if/else/elif 的使用。
  2.2 循环语句
      for/while 循环。
```

解析规则：

- 章节：以"第 X 章"、"第 X 篇"、"第 X 单元"、"X." 或 Word "标题 1" 样式开头的段落。
- 课时：以"X.Y"、"第 X.Y 节"、"课时 X" 或 Word "标题 2" 样式开头的段落，并归属于上一个章节。
- 普通段落：紧跟在课时下方的正文，作为该课时的 `content`。
- 章节/课时名称后的正文段落视为 `description`（章节）或 `content`（课时）。

### 3.2 表格清单式

示例表格：

| 章节序号 | 章节名称 | 课时序号 | 课时名称 | 资源类型 | 资源URL | 时长 |
|---|---|---|---|---|---|---|
| 1 | Python 基础 | 1 | Python 简介 | video | https://example.com/1.mp4 | 10 |
| 1 | Python 基础 | 2 | 变量与数据类型 | video | https://example.com/2.mp4 | 15 |
| 2 | 流程控制 | 1 | 条件语句 | video | https://example.com/3.mp4 | 12 |

列名映射（支持中文/英文/简写）：

| 字段 | 可识别列名 |
|---|---|
| 章节序号 | 章节序号、章、chapter_no、chapterNo |
| 章节名称 | 章节名称、章名称、chapter_name、chapterName |
| 课时序号 | 课时序号、节、lesson_no、lessonNo |
| 课时名称 | 课时名称、节名称、lesson_name、lessonName |
| 资源类型 | 资源类型、type、resource_type、resourceType |
| 资源URL | 资源URL、url、resource_url、resourceUrl、视频链接 |
| 时长 | 时长、duration、时长分钟 |
| 内容 | 内容、content、备注、说明 |

资源类型合法值：`video`、`ppt`、`pdf`、`doc`、`audio`、`text`、`link`、`image`，默认 `text`。

## 四、接口设计

### 4.1 新增接口

```http
POST /api/course-chapter/import-word
Content-Type: multipart/form-data

file: Word 文件 (.docx)
courseId: 135
```

权限：`chapter:import`（与 Excel 导入共用同一权限）。

### 4.2 响应结构

与 Excel 导入保持一致：

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

## 五、开发步骤

1. **添加 Apache POI Word 解析依赖**：`poi-ooxml` 已包含 `XWPFDocument`，可直接复用。
2. **新增 DTO/解析器**：
   - 创建 `WordChapterParser` 工具类，支持标题层级式与表格清单式两种解析。
   - 复用现有 `ChapterImportRowDTO`。
3. **Service 层扩展**：
   - 在 `CourseChapterService` 中新增 `importFromWord(Long courseId, InputStream wordInput, Long userId, boolean isAdmin)`。
   - 复用 Excel 导入中的章节合并、去重、写入逻辑。
4. **Controller 层新增接口**：
   - 在 `CourseChapterController` 新增 `POST /api/course-chapter/import-word`。
5. **前端补充**：
   - 在 `course-chapters.tsx` 导入弹窗中增加"导入 Word"选项或按钮。
   - 添加 `importWordChapters` API。
   - 提供 `chapter-import-template.docx` 模板。
6. **测试**：
   - 编写 `test/chapter_word_import_test.py` 测试标题式与表格式 Word 导入。

## 六、数据校验与事务

- 沿用 Excel 导入的校验规则：必填字段检查、资源类型校验、1000 行上限、事务回滚。
- Word 文件大小限制：10 MB。
- 仅支持 `.docx` 格式，`.doc` 提示另存为 `.docx`。

## 七、风险与应对

| 风险 | 应对 |
|---|---|
| Word 样式多样导致解析不准 | 同时支持标题式与表格式；标题式兼容多种编号写法 |
| 教师未使用标准模板 | 导入失败时返回具体行号/段落号错误信息 |
| 大文件解析慢 | 限制 10 MB；流式读取段落/表格 |
