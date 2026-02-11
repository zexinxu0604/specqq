<!--
=============================================================================
同步影响报告 (Sync Impact Report)
=============================================================================
版本变更: 1.1.0 → 1.1.1
修改的原则:
  - "二、测试优先原则" - 澄清测试路径 (backend/src → src)
修改的章节:
  - "技术约束/后端技术栈" - 新增后端项目结构规范
  - "技术约束/前端技术栈" - 保持不变
新增章节: N/A
已删除章节: N/A
需要更新的模板:
  ✅ .specify/templates/plan-template.md (已支持-项目结构包含标准 Maven 布局)
  ✅ .specify/templates/spec-template.md (无需更新)
  ✅ .specify/templates/tasks-template.md (无需更新)
待办事项: 无
说明: 这是一个 PATCH 版本更新,仅澄清项目路径结构,不改变核心原则语义。
     后端代码直接放在项目根目录的 src/ 下(标准 Maven 项目结构),
     前端代码放在独立的 frontend/ 目录下。
=============================================================================
-->

# SpecQQ 项目宪法

## 核心原则

### 一、代码质量标准 (非协商)

#### 后端代码质量

**MUST (必须遵守)**:
- 所有代码必须使用 JDK 17 及其特性 (records, sealed classes, pattern matching)
- 所有代码必须使用 Spring Boot 3.x 框架,禁止使用 Spring Boot 2.x 或更早版本
- 代码必须通过静态代码分析工具 (SonarQube/SpotBugs) 扫描,无 Critical/Blocker 级别问题
- 所有 public 方法和类必须有 Javadoc 注释,说明用途、参数、返回值和异常
- 代码复杂度必须控制:单个方法圈复杂度 ≤ 10,类圈复杂度 ≤ 50
- 必须遵循阿里巴巴 Java 开发手册的强制性规范
- MyBatis Mapper XML 文件必须规范:使用 resultMap 映射复杂对象,避免使用 `SELECT *`
- 所有 SQL 语句必须防止 SQL 注入,使用 `#{}` 参数绑定,避免使用 `${}`

**SHOULD (应当遵守)**:
- 优先使用 Java 17 的现代特性 (var, switch expressions, text blocks)
- 遵循 SOLID 原则,保持代码的可维护性和可扩展性
- 使用统一的代码格式化工具 (Checkstyle/Google Java Format)
- 复杂 SQL 使用 MyBatis-Plus 的 Lambda 查询简化代码

#### 前端代码质量

**MUST (必须遵守)**:
- 所有前端代码必须使用 Vue 3 (Composition API 优先)
- 必须使用 TypeScript 进行类型检查,禁止使用 `any` 类型 (特殊情况需注释说明)
- 代码必须通过 ESLint 检查,无 error 级别问题
- 组件必须有清晰的 Props 类型定义和默认值
- 组件复杂度控制:单个 `.vue` 文件 ≤ 300 行,setup 函数 ≤ 150 行
- 必须使用统一的代码格式化工具 (Prettier + ESLint)

**SHOULD (应当遵守)**:
- 优先使用 Composition API 而非 Options API
- 使用 `<script setup>` 简化组件定义
- 复杂逻辑抽取为 Composables (可复用组合式函数)
- 遵循 Vue 3 官方风格指南

**理由**: JDK 17 是 LTS 版本,提供长期支持。Spring Boot 3 基于 Jakarta EE 9+。Vue 3 提供更好的性能和 TypeScript 支持。代码质量标准确保代码库的长期可维护性。

---

### 二、测试优先原则 (非协商)

#### 后端测试

**MUST (必须遵守)**:
- TDD 强制执行:先编写测试 → 用户审批 → 测试失败 → 实现代码 → 测试通过
- 严格遵循 Red-Green-Refactor 循环
- 单元测试覆盖率必须 ≥ 80% (语句覆盖率),核心业务逻辑必须 ≥ 90%
- 所有 REST API 必须有集成测试,使用 @SpringBootTest 或 TestRestTemplate
- 所有数据库操作必须有集成测试,使用 @MybatisTest 或 TestContainers (MySQL)
- 测试必须独立运行,不依赖外部环境或执行顺序
- 测试命名必须清晰表达意图: `should_ReturnExpectedResult_When_GivenSpecificCondition()`

**测试分层要求**:
- **单元测试** (src/test/java/.../unit/): 测试单个类或方法,使用 Mockito 模拟依赖
- **集成测试** (src/test/java/.../integration/): 测试多个组件协作,使用 TestContainers (MySQL)
- **契约测试** (src/test/java/.../contract/): 测试 API 接口契约,使用 REST Assured

#### 前端测试

**MUST (必须遵守)**:
- 关键业务组件必须有单元测试,使用 Vitest + Vue Test Utils
- 所有 API 调用层 (Service/API) 必须有单元测试,模拟 HTTP 请求
- E2E 测试覆盖核心用户流程,使用 Playwright 或 Cypress
- 测试覆盖率 ≥ 70%,核心业务组件 ≥ 85%

**测试分层要求**:
- **组件测试** (frontend/src/tests/unit/): 测试单个 Vue 组件
- **集成测试** (frontend/src/tests/integration/): 测试多个组件协作
- **E2E 测试** (frontend/tests/e2e/): 端到端用户流程测试

**理由**: TDD 确保代码设计的可测试性,减少缺陷引入。高覆盖率保证代码变更的安全性。分层测试提供不同层次的质量保障。

---

### 三、用户体验一致性

#### API 接口规范

**MUST (必须遵守)**:
- 所有 REST API 必须遵循统一的响应格式:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": { },
    "timestamp": "2026-02-06T10:30:00Z"
  }
  ```
- 错误响应必须包含清晰的错误代码和用户友好的错误消息
- API 版本管理:使用 URL 路径版本控制 (如 `/api/v1/users`),主版本不兼容变更必须递增
- 所有面向用户的字符串必须支持国际化 (i18n),后端使用 MessageSource,前端使用 Vue I18n
- 日期时间必须使用 ISO 8601 格式 (如 `2026-02-06T10:30:00Z`)
- 分页参数必须统一: `page` (从1开始), `size`, `sort` (与 MyBatis-Plus PageHelper 对齐)

**SHOULD (应当遵守)**:
- API 设计遵循 RESTful 规范,使用合适的 HTTP 方法和状态码
- 提供清晰的 API 文档,使用 SpringDoc OpenAPI 3.0
- 错误消息应提供可操作的建议

#### 前端 UI/UX 规范

**MUST (必须遵守)**:
- 必须使用统一的 UI 组件库:Element Plus 或 Ant Design Vue
- 必须遵循统一的设计规范:颜色、字体、间距、圆角等
- 表单验证规则必须前后端一致
- 加载状态 (Loading)、空状态 (Empty)、错误状态必须有统一的 UI 处理
- 移动端适配:关键页面必须支持响应式布局
- 无障碍访问 (Accessibility):表单必须有 label,按钮必须有 aria-label

**SHOULD (应当遵守)**:
- 使用统一的状态管理方案 (Pinia)
- 路由切换使用过渡动画
- 关键操作提供确认提示
- 长列表使用虚拟滚动优化性能

**理由**: 统一的 API 设计和响应格式降低前端集成成本。统一的 UI 组件库和设计规范确保用户体验一致性,减少用户学习成本。

---

### 四、性能要求

#### 后端性能

**MUST (必须遵守)**:
- API 响应时间 P95 < 200ms (单次数据库查询), P99 < 500ms
- 数据库连接池必须配置合理:最小连接数 ≥ 5,最大连接数根据负载调整 (建议 ≤ 20)
- 必须使用缓存机制 (Redis/Caffeine) 降低数据库压力,热点数据缓存命中率 ≥ 90%
- 所有 N+1 查询问题必须解决,使用 MyBatis 的 Collection/Association 或批量查询
- 列表查询必须强制分页,使用 MyBatis-Plus 的 Page 插件,单次查询记录数不得超过 100 条
- 慢查询 (> 100ms) 必须记录日志并优化,生产环境不允许存在 > 1s 的查询
- JVM 堆内存使用率 < 70%,GC 暂停时间 P99 < 100ms

**SHOULD (应当遵守)**:
- 使用异步处理优化长时间操作 (如文件上传、邮件发送)
- 使用批量操作减少数据库往返次数 (MyBatis-Plus 的 saveBatch)
- 关键路径使用性能监控 (如 Micrometer + Prometheus)

#### 前端性能

**MUST (必须遵守)**:
- 首屏加载时间 < 2s (4G 网络)
- 路由懒加载:非首屏路由必须使用动态 import
- 大型组件库按需引入,禁止全量引入
- 图片必须压缩优化,使用 WebP 格式,大图使用懒加载
- 长列表必须使用虚拟滚动 (如 vue-virtual-scroller)
- 打包体积:vendor chunk < 500KB,单个页面 chunk < 200KB

**SHOULD (应当遵守)**:
- 使用 CDN 加速静态资源
- 启用 Gzip/Brotli 压缩
- 使用 HTTP/2 或 HTTP/3
- 关键资源使用预加载 (preload)

#### 性能测试要求

- 后端:所有新功能必须通过性能基准测试,使用 JMeter 或 Gatling
- 前端:使用 Lighthouse 进行性能评分,Performance 分数 ≥ 90
- 并发用户数 ≥ 100 时系统稳定性验证

**理由**: 性能指标直接影响用户体验和系统成本。明确的性能要求确保系统在生产环境的可靠性。前端性能影响用户留存率。

---

### 五、可观测性与安全

#### 后端可观测性与安全

**MUST (必须遵守)**:
- 所有业务操作必须记录结构化日志,使用 SLF4J + Logback
- 日志级别明确:ERROR (系统错误需人工介入), WARN (潜在问题), INFO (关键业务流程), DEBUG (调试信息)
- 关键业务流程必须记录操作日志:谁、何时、做了什么、结果如何
- 敏感信息 (密码、Token、身份证号等) 禁止明文记录日志
- 必须集成监控指标 (Metrics),暴露 `/actuator/prometheus` 端点
- 必须实现健康检查端点 `/actuator/health`,包含数据库、Redis 等依赖检查
- 所有外部调用 (HTTP、RPC、MQ) 必须有超时配置和熔断机制 (Resilience4j)
- 输入验证:所有用户输入必须验证,使用 Jakarta Validation (如 @NotNull, @Size)
- 必须防御常见安全漏洞:SQL 注入、XSS、CSRF、敏感信息泄露
- 密码必须使用 BCrypt 或 Argon2 加密存储,禁止明文或可逆加密
- MyBatis 必须防止 SQL 注入:使用 `#{}` 而非 `${}`

**SHOULD (应当遵守)**:
- 使用分布式追踪 (如 Spring Cloud Sleuth + Zipkin) 追踪请求链路
- 实现告警机制,关键指标异常时发送通知
- 定期进行安全扫描 (OWASP Dependency Check)

#### 前端安全

**MUST (必须遵守)**:
- 所有用户输入必须进行前端验证,但不能仅依赖前端验证
- 敏感信息 (Token) 必须存储在 HttpOnly Cookie 或使用安全的存储方式
- 防御 XSS 攻击:使用 Vue 的 v-html 时必须对内容进行清理 (DOMPurify)
- 防御 CSRF 攻击:使用 CSRF Token 或 SameSite Cookie
- HTTPS 强制:生产环境必须使用 HTTPS
- 第三方依赖安全:定期扫描前端依赖漏洞 (npm audit)

**SHOULD (应当遵守)**:
- 实现前端错误监控 (如 Sentry)
- 收集用户行为数据用于性能优化
- 实现前端日志上报

**理由**: 可观测性是生产环境问题排查的基础。结构化日志便于自动化分析。安全防护确保用户数据和系统的安全性。

---

## 技术约束

### 后端技术栈

**强制技术栈**:
- **Java 版本**: JDK 17 (LTS),禁止使用 JDK 11 或更低版本
- **Spring Boot 版本**: 3.x (最新稳定版),基于 Jakarta EE 9+
- **构建工具**: Maven 3.8+ 或 Gradle 7.5+
- **数据库**: MySQL 8.0+ (强制,利用 8.0 新特性如 CTE、窗口函数)
- **ORM 框架**: MyBatis-Plus 3.5+ (核心 ORM)
- **代码生成**: MyBatis-Generator 1.4+ (自动生成 Mapper、Entity、XML)
- **缓存**: Redis 7.x (分布式缓存) + Caffeine (本地缓存)
- **消息队列**: RabbitMQ 或 Kafka (根据业务场景选择)
- **API 文档**: SpringDoc OpenAPI 3.0 (替代 Swagger 2.x)
- **测试框架**: JUnit 5 + Mockito + AssertJ + TestContainers (MySQL)
- **日志框架**: SLF4J + Logback
- **监控**: Micrometer + Prometheus + Grafana

**MyBatis-Plus 配置规范**:
- 必须配置逻辑删除 (deleted 字段)
- 必须配置自动填充 (create_time, update_time)
- 必须配置分页插件 (PaginationInnerInterceptor)
- 必须配置乐观锁插件 (OptimisticLockerInnerInterceptor) 用于并发控制
- 推荐使用 MyBatis-Plus 的 Lambda 查询构造器简化复杂查询

**MyBatis-Generator 配置规范**:
- 生成的 Entity 必须包含必要的注解 (@TableName, @TableId, @TableField)
- 生成的 Mapper 接口必须继承 BaseMapper<T>
- 自定义 SQL 写在独立的 XML 文件中,不在生成的 XML 中修改
- 使用 MyBatis-Generator 插件增强生成代码 (如 Lombok 插件)

**后端项目结构规范** (标准 Maven 项目):
```
项目根目录/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/specqq/
│   │   │       ├── controller/      # REST API 控制器
│   │   │       ├── service/         # 业务逻辑层
│   │   │       ├── mapper/          # MyBatis Mapper 接口
│   │   │       ├── entity/          # 实体类 (数据库表映射)
│   │   │       ├── dto/             # 数据传输对象
│   │   │       ├── vo/              # 视图对象 (返回给前端)
│   │   │       ├── config/          # Spring 配置类
│   │   │       ├── common/          # 公共工具类、常量、枚举
│   │   │       ├── exception/       # 异常类和全局异常处理
│   │   │       └── Application.java # Spring Boot 启动类
│   │   ├── resources/
│   │   │   ├── mapper/              # MyBatis XML 映射文件
│   │   │   ├── application.yml      # 配置文件
│   │   │   ├── application-dev.yml  # 开发环境配置
│   │   │   ├── application-prod.yml # 生产环境配置
│   │   │   ├── logback-spring.xml   # 日志配置
│   │   │   └── static/              # 静态资源 (如有需要)
│   │   └── webapp/                  # Web 资源 (如 JSP,不推荐)
│   └── test/
│       └── java/
│           └── com/example/specqq/
│               ├── unit/            # 单元测试
│               ├── integration/     # 集成测试
│               └── contract/        # 契约测试
├── pom.xml                          # Maven 依赖配置
└── README.md                        # 项目说明文档
```

### 前端技术栈

**强制技术栈**:
- **框架**: Vue 3.4+ (Composition API)
- **语言**: TypeScript 5.x
- **构建工具**: Vite 5.x (快速开发和构建)
- **状态管理**: Pinia 2.x (Vue 3 官方推荐)
- **路由**: Vue Router 4.x
- **UI 组件库**: Element Plus 2.x 或 Ant Design Vue 4.x (项目初期确定,不可混用)
- **HTTP 客户端**: Axios 1.x
- **表单验证**: VeeValidate 或组件库内置验证
- **国际化**: Vue I18n 9.x
- **测试框架**: Vitest + Vue Test Utils + Playwright/Cypress
- **代码质量**: ESLint + Prettier + TypeScript

**前端项目结构规范**:
```
frontend/
├── src/
│   ├── api/          # API 接口定义
│   ├── assets/       # 静态资源 (图片、字体)
│   ├── components/   # 公共组件
│   ├── composables/  # 可复用组合式函数
│   ├── layouts/      # 布局组件
│   ├── router/       # 路由配置
│   ├── stores/       # Pinia 状态管理
│   ├── styles/       # 全局样式
│   ├── types/        # TypeScript 类型定义
│   ├── utils/        # 工具函数
│   ├── views/        # 页面组件
│   ├── App.vue       # 根组件
│   └── main.ts       # 入口文件
├── tests/            # 测试文件
├── public/           # 公共静态资源
├── index.html        # HTML 模板
├── vite.config.ts    # Vite 配置
├── tsconfig.json     # TypeScript 配置
└── package.json      # 依赖配置
```

### 禁止使用

**后端**:
- Spring Data JPA + Hibernate (本项目使用 MyBatis-Plus)
- PostgreSQL (本项目使用 MySQL 8.0+)
- Spring Boot 2.x 或更早版本 (已不再维护)
- JDK 11 或更低版本 (缺少现代特性)
- javax.* 包 (必须使用 jakarta.* 包)
- Swagger 2.x (已过时,使用 SpringDoc OpenAPI)
- synchronized 关键字 (优先使用 java.util.concurrent 工具)

**前端**:
- Vue 2.x (已停止维护)
- Options API 作为主要开发方式 (Composition API 优先)
- JavaScript (必须使用 TypeScript)
- Webpack (使用 Vite)
- Vuex (使用 Pinia)
- 全量引入 UI 组件库

### 依赖管理原则

**后端**:
- 所有依赖必须在 `pom.xml` 或 `build.gradle` 中显式声明版本
- 使用 Spring Boot Dependency Management 统一管理版本
- 禁止使用快照版本 (SNAPSHOT) 依赖到生产环境
- 定期更新依赖到最新稳定版,修复安全漏洞
- 新增依赖必须评估:许可证兼容性、维护状态、社区活跃度

**前端**:
- 使用 pnpm 或 npm 管理依赖,锁定版本 (package-lock.json 或 pnpm-lock.yaml)
- 定期运行 `npm audit` 或 `pnpm audit` 检查安全漏洞
- 大型库优先使用 CDN (生产环境)
- 新增依赖评估:打包体积影响、Tree-shaking 支持、TypeScript 类型支持

---

## 开发流程

### 代码审查要求

**MUST (必须遵守)**:
- 所有代码必须经过至少 1 名其他开发者审查后才能合并
- 审查者必须验证:代码符合宪法原则、测试覆盖率达标、无安全漏洞
- 必须通过所有 CI 检查:编译、测试、代码质量扫描、安全扫描
- Pull Request 描述必须清晰:改动原因、实现方案、测试结果
- 禁止直接向 `main` 或 `master` 分支提交代码,必须通过 PR 流程
- 前后端分离项目:前端变更和后端变更应分开 PR (除非是紧密关联的全栈功能)

### 分支策略

- **main/master**: 主分支,受保护,仅接受经过审查和测试的代码
- **feature/###-feature-name**: 功能分支,开发新功能
- **bugfix/###-bug-description**: 修复分支,修复 bug
- **hotfix/###-critical-issue**: 热修复分支,紧急修复生产问题

### 提交规范

提交消息必须遵循 Conventional Commits 规范:
- `feat: 添加用户注册功能`
- `feat(frontend): 添加用户登录页面`
- `feat(backend): 实现用户认证 API`
- `fix: 修复登录时的空指针异常`
- `fix(frontend): 修复表单验证问题`
- `docs: 更新 API 文档`
- `test: 增加用户服务单元测试`
- `refactor: 重构订单处理逻辑`
- `perf: 优化商品查询性能`
- `style(frontend): 调整按钮样式`
- `chore: 升级 Spring Boot 到 3.2.2`
- `chore(frontend): 升级 Vue 到 3.4.0`

### 质量门禁

**部署前检查**:
- ✅ 所有测试通过 (单元测试 + 集成测试 + 契约测试 + E2E 测试)
- ✅ 代码覆盖率达标 (后端 ≥ 80%, 前端 ≥ 70%)
- ✅ 代码质量扫描通过 (无 Critical/Blocker 问题)
- ✅ 安全扫描通过 (后端无高危漏洞, 前端 npm audit 通过)
- ✅ 性能测试通过 (后端满足响应时间指标, 前端 Lighthouse ≥ 90)
- ✅ API 文档已更新 (OpenAPI 文档)
- ✅ 变更日志已记录 (CHANGELOG.md)
- ✅ 前端打包体积检查通过 (vendor < 500KB, 页面 < 200KB)

---

## 治理规则

### 宪法效力

本宪法是项目开发的最高准则,所有开发活动必须遵守本宪法的规定。当其他文档 (如编码规范、技术方案) 与本宪法冲突时,以本宪法为准。

### 宪法修订流程

1. **提议**: 任何团队成员可提出修订提议,需说明修订原因和影响范围
2. **讨论**: 团队内部讨论,评估修订的必要性和可行性
3. **批准**: 修订提议需获得 ≥ 2/3 团队成员同意
4. **迁移计划**: 对于重大修订,必须制定迁移计划,确保平稳过渡
5. **版本更新**: 修订后更新版本号和修订日期,记录变更内容

### 版本管理

宪法版本号遵循语义化版本控制 (Semantic Versioning):

- **MAJOR (主版本)**: 不兼容的原则移除或重新定义 (如删除核心原则、改变技术栈)
- **MINOR (次版本)**: 新增原则/章节或实质性扩展指导 (如新增性能要求、扩展测试标准)
- **PATCH (补丁版本)**: 澄清说明、措辞改进、错误修正,不改变语义 (如修正示例代码、优化表述)

### 合规性审查

- 所有 Pull Request 必须声明符合本宪法的哪些原则
- 如有违反宪法的情况,必须在 PR 中说明原因和替代方案,需额外审批
- 定期 (每季度) 进行代码库合规性审查,识别和修复不符合宪法的代码
- 对于历史遗留代码,制定渐进式改进计划,逐步达到宪法标准

### 复杂度例外处理

当业务需求确实需要违反宪法某些原则时 (如引入第四个项目、使用复杂设计模式),必须:

1. 在实施计划 (plan.md) 的 "复杂度追踪" 表中记录
2. 说明为什么需要违反原则
3. 说明为什么更简单的替代方案不可行
4. 获得技术负责人批准

### 运行时开发指导

开发过程中遇到技术决策问题时,优先参考:

1. 本宪法 (最高准则)
2. 功能规格说明 (spec.md)
3. 实施计划 (plan.md)
4. 阿里巴巴 Java 开发手册 (后端)
5. Vue 3 官方文档 + TypeScript 官方文档 (前端)
6. Spring Boot 官方文档 (后端)
7. MyBatis-Plus 官方文档 (后端)

当多个文档有冲突时,优先级从高到低依次为上述顺序。

---

**版本**: 1.1.1 | **批准日期**: 2026-02-06 | **最后修订**: 2026-02-06
