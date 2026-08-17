# 智能学习 App 子代理开发与测试返工流程

本文档定义如何在 Trae IDE 中使用子代理（subagent）对 `aiStudy` 项目进行开发、测试验收与返工。

## 1. 项目模块说明

| 模块 | 技术栈 | 路径 | 主要职责 |
|---|---|---|---|
| backend | Spring Boot + Maven + MyBatis | `backend/` | RESTful API、业务逻辑、数据库访问 |
| android | 原生 Android + Gradle + Retrofit | `android/` | 学生端 Android App |
| miniapp | uni-app + Vue + Vite | `miniapp/` | 微信小程序 |
| web-admin | HTML + JS | `web-admin/` | 教师/管理后台页面 |
| init-db | SQL | `init-db/` | 数据库初始化脚本 |

后端是 android、miniapp、web-admin 的公共依赖，改动时应优先保证后端接口稳定。

## 2. 子代理角色定义

子代理通过 Trae 的 `Task` 工具调用，每个子代理一次只负责一个模块的一个环节。

| 子代理 | 负责模块 | 推荐 subagent_type | 核心职责 |
|---|---|---|---|
| backend-dev | backend | general_purpose_task | 后端功能开发 / bug 修复 |
| backend-test | backend | general_purpose_task | Maven 编译、单元测试、接口契约校验 |
| android-dev | android | general_purpose_task | Android 功能开发 / bug 修复 |
| android-test | android | general_purpose_task | Gradle 编译、lint、页面逻辑验证 |
| miniapp-dev | miniapp | general_purpose_task | 小程序页面 / API 对接开发 |
| miniapp-test | miniapp | general_purpose_task | 编译检查、路由与接口对齐 |
| web-admin-dev | web-admin | general_purpose_task | 管理台页面开发 |
| web-admin-test | web-admin | browser_use | 浏览器点击验证 |
| integration-test | 跨端 | general_purpose_task | 检查前后端接口契约一致性 |
| qa-coordinator | 跨端 | general_purpose_task | 汇总问题、生成返工清单、跟踪闭环 |

## 3. 标准工作流

每轮迭代按以下 5 步执行：

1. **需求拆解与模块分配**：主代理判断改动涉及哪些模块，向对应 dev 子代理派发任务。
2. **模块级开发 + 自测**：dev 子代理完成开发，输出改动文件、实现思路、自测方式。
3. **模块级测试验收**：test 子代理独立验证，输出通过/失败列表与日志。
4. **跨端集成验收**：integration-test 检查多端接口契约、字段、路径是否一致。
5. **返工闭环**：qa-coordinator 汇总问题，生成返工单；主代理将返工单分发给对应 dev 子代理，进入下一轮。

```text
需求/缺陷 → backend-dev → backend-test ─┐
         ├ android-dev → android-test ─┤
         ├ miniapp-dev → miniapp-test ─┼→ integration-test → qa-coordinator → 返工
         └ web-admin-dev → web-admin-test ─┘
```

退出条件：

- 所有模块测试通过，跨端契约一致。
- 连续两轮仍有未解决问题，由用户人工判断。

## 4. Prompt 模板

### 4.1 开发类 Prompt

#### backend-dev

```text
任务：在 backend 模块中实现 [具体需求]。
要求：
1. 遵循现有分层：controller → service → mapper → entity。
2. 在 SecurityConfig / JwtAuthFilter 中确认权限控制。
3. 如新增数据库表，同步更新 init-db/ 和 znxsgltest.sql。
4. 完成后列出新增/修改的文件、接口路径、请求/响应格式、自测命令。
```

#### android-dev

```text
任务：在 android 模块中实现 [具体需求]。
要求：
1. 遵循现有包结构：activity / fragment / model / network。
2. 新增 Activity 需在 AndroidManifest.xml 注册。
3. 网络请求通过 RetrofitClient + ApiService 实现，路径与 backend 对齐。
4. 完成后列出新增/修改的文件、关键页面、自测方式。
```

#### miniapp-dev

```text
任务：在 miniapp 模块中实现 [具体需求]。
要求：
1. 新增页面需在 pages.json 中注册。
2. API 调用统一走 utils/api.js，路径与 backend 对齐。
3. 完成后列出新增/修改的文件、页面路由、自测方式。
```

#### web-admin-dev

```text
任务：在 web-admin 模块中实现 [具体需求]。
要求：
1. 保持现有 HTML/JS 风格。
2. 登录态与 backend 接口对齐。
3. 完成后列出新增/修改的文件、验证方式。
```

### 4.2 测试类 Prompt

#### backend-test

```text
任务：验证 backend 模块本次改动。
要求：
1. 执行 mvn clean compile，确认无编译错误。
2. 如存在测试，执行 mvn test。
3. 检查新增接口的 URL、请求参数、响应字段是否与需求一致。
4. 检查数据库实体与 init-db/ SQL 脚本是否一致。
5. 输出：通过项 / 失败项 / 具体错误日志 / 建议修复点。
```

#### android-test

```text
任务：验证 android 模块本次改动。
要求：
1. 执行 gradlew assembleDebug，确认编译通过。
2. 检查新增 Activity/Fragment/布局文件是否正确。
3. 检查 ApiService.java 中的接口与 backend 是否对齐。
4. 检查 AndroidManifest.xml 是否注册新页面。
5. 输出：编译结果、lint 警告、接口差异、建议修复点。
```

#### miniapp-test

```text
任务：验证 miniapp 模块本次改动。
要求：
1. 执行 npm install && npm run build:dev，确认编译通过。
2. 检查 pages.json 是否注册新页面。
3. 检查 utils/api.js 中接口调用与 backend 是否一致。
4. 检查静态资源路径和 tabBar 配置。
5. 输出：编译结果、遗漏项、建议修复点。
```

#### web-admin-test

```text
任务：在浏览器中验证 web-admin 模块。
要求：
1. 打开登录页，验证登录流程。
2. 验证本次改动涉及的后台页面功能。
3. 检查页面调用后端接口是否正常。
4. 输出：通过项 / 失败项 / 截图或错误描述。
```

### 4.3 跨端与协调类 Prompt

#### integration-test

```text
任务：检查 backend / android / miniapp / web-admin 之间接口契约一致性。
检查点：
1. backend controller 中的 URL、请求参数、响应字段。
2. android ApiService.java 中声明的接口是否与 backend 一致。
3. miniapp utils/api.js 中调用是否与 backend 一致。
4. 数据库实体类字段与前端展示字段是否一致。
5. web-admin 中接口调用是否与 backend 一致。
输出：不一致清单 + 责任模块 + 建议修复。
```

#### qa-coordinator

```text
任务：汇总本轮开发与测试结果，生成返工清单。
输入：backend-test / android-test / miniapp-test / web-admin-test / integration-test 的输出。
要求：
1. 按模块分类列出所有问题。
2. 每个问题包含：现象、错误日志、责任模块、建议修复动作。
3. 标注哪些问题必须修复，哪些是优化项。
4. 输出格式见本文档第 6 节。
```

## 5. 验收 Checklist

### 5.1 backend

- [ ] `mvn clean compile` 通过
- [ ] `mvn test` 通过（如已有测试）
- [ ] 新增接口在对应 Controller 中
- [ ] 权限控制（JwtAuthFilter / SecurityConfig）已考虑
- [ ] 数据库实体与 `init-db/` SQL 脚本一致
- [ ] `application.yml` 配置无遗漏
- [ ] 关键 Service 有异常处理

### 5.2 android

- [ ] `gradlew assembleDebug` 通过
- [ ] 新增 Activity / Fragment 在 `AndroidManifest.xml` 注册
- [ ] Retrofit 接口与 backend 一致
- [ ] 新增资源文件（layout / drawable / values）无引用错误
- [ ] 关键页面有空指针 / 网络异常处理

### 5.3 miniapp

- [ ] `npm run build:dev` 通过
- [ ] 新页面在 `pages.json` 注册
- [ ] `utils/api.js` 接口路径与 backend 一致
- [ ] 静态资源路径正确
- [ ] tabBar / 路由跳转正常

### 5.4 web-admin

- [ ] 页面能正常打开
- [ ] 登录态处理正确
- [ ] 调用后端接口正常

## 6. 返工清单模板

```markdown
## 返工清单（第 N 轮）

### backend
- [ ] Bug：xxx 接口 500，错误日志：...
- [ ] 优化：缺少 @Valid 校验

### android
- [ ] Bug：CourseDetailActivity 未处理空指针
- [ ] 遗漏：未调用新接口

### miniapp
- [ ] 编译报错：pages.json 未注册新页面

### web-admin
- [ ] Bug：登录后 token 未写入请求头

### 跨端
- [ ] 字段不一致：backend 返回 courseName，android 使用 name
```

## 7. 使用建议

1. **一次不要启动太多子代理**：建议最多 3-4 个并行，避免上下文混乱。
2. **后端优先**：backend 是多端依赖，先确保后端接口稳定再验收前端。
3. **先跑通最小闭环**：不要一次性改 4 个模块，先让 backend + 一个前端模块跑通。
4. **用户作为仲裁者**：子代理对责任模块有争议时，由用户拍板。
5. **跨窗口复现**：其他 Trae 窗口无法调用当前窗口的子代理，但可以让新窗口的主代理读取本文档后，按相同流程重新启动子代理。
