# Handler 与 ResponseTemplate 的关系说明

**日期**: 2026-02-12

---

## 当前设计

### 优先级规则

**Handler > ResponseTemplate**

- 当配置了 `handler` 时，系统会执行 handler 逻辑，**完全忽略** `responseTemplate`
- 当没有配置 `handler` 时（`handlerConfig` 为 null 或空字符串），系统会使用 `responseTemplate` 作为回复内容

### 代码实现

**后端路由逻辑** (`MessageRouterService.java:82-101`):

```java
String handlerConfig = rule.getHandlerConfig();
if (handlerConfig == null || handlerConfig.isEmpty()) {
    // 只有当 handlerConfig 为空时，才使用 responseTemplate
    String replyContent = rule.getResponseTemplate();
    if (replyContent == null || replyContent.isEmpty()) {
        log.warn("Rule has no handler or response template: ruleId={}", rule.getId());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId(message.getGroupId())
            .replyContent(replyContent)
            .build();
    // ... 发送回复
    return CompletableFuture.completedFuture(Optional.of(reply));
}

// 如果 handlerConfig 存在，执行 handler 逻辑
// responseTemplate 会被完全忽略
```

---

## 使用场景

### 场景 1: 简单固定回复

**配置**:
- ✅ `responseTemplate`: "你好，欢迎使用本机器人"
- ❌ `handler`: 未配置

**行为**:
- 匹配到规则后，直接回复 `responseTemplate` 的内容
- 支持变量替换：`{user}`, `{message}` 等

**适用于**:
- 简单的关键词自动回复
- 固定的欢迎语、帮助信息
- 不需要复杂逻辑的场景

---

### 场景 2: 复杂动态回复

**配置**:
- ❌ `responseTemplate`: 可以填写，但会被忽略
- ✅ `handler`: 配置了处理器类型和参数

**行为**:
- 匹配到规则后，执行 handler 的业务逻辑
- Handler 内部决定回复内容（可能调用外部 API、查询数据库等）
- `responseTemplate` 完全不会被使用

**适用于**:
- 需要调用外部 API（如天气查询、翻译服务）
- 需要查询数据库或缓存
- 需要复杂的业务逻辑判断
- 需要异步处理或延迟回复

---

## 当前问题

### 1. 前端验证规则不合理

**问题**: `responseTemplate` 被设置为必填字段

```typescript
responseTemplate: [
  { required: true, message: '请输入回复模板', trigger: 'blur' },
  { min: 1, max: 500, message: '回复模板长度在1-500个字符', trigger: 'blur' }
]
```

**影响**:
- 即使用户配置了 Handler，仍然必须填写 `responseTemplate`
- 用户可能误以为两者会同时生效
- 浪费用户时间填写无用的字段

---

### 2. 缺少用户提示

**问题**: 前端没有明确告知用户 Handler 和 ResponseTemplate 的互斥关系

**影响**:
- 用户不清楚配置 Handler 后，ResponseTemplate 会被忽略
- 可能导致用户困惑："为什么我配置的回复模板没有生效？"

---

## 改进建议

### 建议 1: 动态验证规则 ⭐ 推荐

**实现**: 根据是否配置 Handler 来动态调整 `responseTemplate` 的验证规则

```typescript
// RuleForm.vue
const formRules = computed<FormRules>(() => {
  const rules: FormRules = {
    name: [
      { required: true, message: '请输入规则名称', trigger: 'blur' },
      { min: 2, max: 50, message: '规则名称长度在2-50个字符', trigger: 'blur' },
      { validator: validateNameUnique, trigger: 'blur' }
    ],
    matchType: [
      { required: true, message: '请选择匹配类型', trigger: 'change' }
    ],
    pattern: [
      { required: true, message: '请输入匹配模式', trigger: 'blur' }
    ],
    priority: [
      { required: true, message: '请设置优先级', trigger: 'blur' }
    ]
  }

  // 动态调整 responseTemplate 验证规则
  if (!formData.handlerType) {
    // 如果没有配置 Handler，responseTemplate 必填
    rules.responseTemplate = [
      { required: true, message: '请输入回复模板', trigger: 'blur' },
      { min: 1, max: 500, message: '回复模板长度在1-500个字符', trigger: 'blur' }
    ]
  } else {
    // 如果配置了 Handler，responseTemplate 可选
    rules.responseTemplate = [
      { min: 0, max: 500, message: '回复模板长度不能超过500个字符', trigger: 'blur' }
    ]
  }

  return rules
})
```

**优点**:
- 符合业务逻辑：只有在需要时才要求填写 ResponseTemplate
- 用户体验好：不会强制填写无用的字段
- 灵活性高：支持两种使用模式

---

### 建议 2: 添加用户提示

**实现**: 在表单中添加提示信息，说明 Handler 和 ResponseTemplate 的关系

```vue
<!-- RuleForm.vue -->
<el-form-item label="回复模板" prop="responseTemplate">
  <el-input
    v-model="formData.responseTemplate"
    type="textarea"
    :rows="4"
    placeholder="请输入回复模板"
    maxlength="500"
    show-word-limit
    :disabled="!!formData.handlerType"
  />
  <div class="form-tip">
    <el-text size="small" type="info">
      支持变量: {user} - 用户昵称, {message} - 原消息内容
    </el-text>
  </div>
  <!-- 新增提示 -->
  <div v-if="formData.handlerType" class="form-tip">
    <el-text size="small" type="warning">
      ⚠️ 已配置处理器，回复模板将被忽略
    </el-text>
  </div>
  <div v-else class="form-tip">
    <el-text size="small" type="info">
      💡 如果不配置处理器，将使用回复模板作为回复内容
    </el-text>
  </div>
</el-form-item>
```

**优点**:
- 用户一目了然：清楚地知道当前配置的效果
- 避免混淆：明确告知哪个配置会生效
- 引导用户：提示用户如何正确配置

---

### 建议 3: 禁用互斥字段

**实现**: 当配置了 Handler 时，禁用 ResponseTemplate 输入框

```vue
<el-input
  v-model="formData.responseTemplate"
  type="textarea"
  :rows="4"
  placeholder="请输入回复模板"
  maxlength="500"
  show-word-limit
  :disabled="!!formData.handlerType"
/>
```

**优点**:
- 强制互斥：从 UI 层面阻止用户同时配置两者
- 避免误操作：用户无法填写不会生效的字段
- 视觉反馈：灰色的输入框提示用户该字段不可用

**缺点**:
- 灵活性降低：如果未来需要同时支持两者，需要修改代码
- 用户可能想保留 ResponseTemplate 作为备份

---

### 建议 4: 支持混合模式（未来扩展）

**实现**: 允许 Handler 和 ResponseTemplate 同时生效

**可能的策略**:

1. **追加模式**: Handler 返回内容 + ResponseTemplate
2. **后备模式**: Handler 失败时使用 ResponseTemplate
3. **模板模式**: Handler 返回数据，ResponseTemplate 作为格式化模板

**示例代码** (后端):

```java
// 策略 1: 追加模式
if (handlerConfig != null && !handlerConfig.isEmpty()) {
    String handlerResult = executeHandler(handlerConfig);
    String template = rule.getResponseTemplate();
    String finalReply = (template != null && !template.isEmpty())
        ? handlerResult + "\n\n" + template
        : handlerResult;
    return reply(finalReply);
}

// 策略 2: 后备模式
if (handlerConfig != null && !handlerConfig.isEmpty()) {
    try {
        String handlerResult = executeHandler(handlerConfig);
        return reply(handlerResult);
    } catch (Exception e) {
        log.warn("Handler failed, fallback to template: {}", e.getMessage());
        String template = rule.getResponseTemplate();
        return reply(template);
    }
}

// 策略 3: 模板模式
if (handlerConfig != null && !handlerConfig.isEmpty()) {
    Map<String, Object> data = executeHandlerForData(handlerConfig);
    String template = rule.getResponseTemplate();
    String finalReply = renderTemplate(template, data);
    return reply(finalReply);
}
```

**优点**:
- 功能更强大：支持更复杂的业务场景
- 灵活性高：用户可以根据需求选择不同策略

**缺点**:
- 复杂度增加：需要设计策略选择机制
- 用户理解成本：需要更多文档说明

---

## 推荐方案

### 短期方案（立即实施）

✅ **建议 1 + 建议 2**: 动态验证规则 + 用户提示

**原因**:
- 最小改动：只需修改前端验证逻辑和添加提示文字
- 用户体验好：清晰的提示 + 合理的验证规则
- 保持灵活性：不强制禁用字段，用户可以选择

**实施步骤**:
1. 修改 `RuleForm.vue` 的 `formRules`，使用 `computed` 动态生成
2. 在 ResponseTemplate 输入框下方添加条件提示
3. 测试验证规则是否正常工作

---

### 长期方案（未来考虑）

⚠️ **建议 4**: 支持混合模式

**原因**:
- 功能扩展性：为未来的复杂需求预留空间
- 用户需求驱动：根据用户反馈决定是否实施

**前置条件**:
- 收集用户反馈：是否有同时使用两者的需求
- 设计策略选择：如何让用户选择混合模式
- 后端支持：修改 `MessageRouterService` 支持不同策略

---

## 数据示例

### 示例 1: 只配置 ResponseTemplate

**请求体**:
```json
{
  "name": "欢迎新成员",
  "matchType": "CONTAINS",
  "pattern": "你好",
  "responseTemplate": "欢迎加入本群！有任何问题请随时提问。",
  "handlerType": null,
  "handlerParams": {}
}
```

**行为**:
- 用户发送包含"你好"的消息
- 机器人回复："欢迎加入本群！有任何问题请随时提问。"

---

### 示例 2: 只配置 Handler

**请求体**:
```json
{
  "name": "天气查询",
  "matchType": "PREFIX",
  "pattern": "天气",
  "responseTemplate": "",  // 可以为空
  "handlerType": "WEATHER_QUERY",
  "handlerParams": {
    "apiKey": "your-api-key",
    "defaultCity": "北京"
  }
}
```

**行为**:
- 用户发送"天气 上海"
- Handler 解析城市名称，调用天气 API
- 机器人回复："上海今天晴，温度 15-25℃"

---

### 示例 3: 同时配置（当前行为）

**请求体**:
```json
{
  "name": "混合配置",
  "matchType": "EXACT",
  "pattern": "帮助",
  "responseTemplate": "这是帮助信息：...",  // ❌ 会被忽略
  "handlerType": "HELP_MENU",
  "handlerParams": {}
}
```

**行为**:
- 用户发送"帮助"
- Handler 执行，返回动态生成的帮助菜单
- ResponseTemplate **完全被忽略**

---

## 相关文档

1. **MessageRouterService.java** - 消息路由和 Handler 执行逻辑
2. **RuleForm.vue** - 规则表单组件
3. **HandlerSelector.vue** - Handler 选择器组件

---

## 总结

### 当前设计

✅ **清晰的优先级**: Handler > ResponseTemplate
✅ **简单的实现**: 后端逻辑清晰，易于维护
❌ **前端验证不合理**: ResponseTemplate 强制必填
❌ **缺少用户提示**: 用户不清楚互斥关系

### 推荐改进

1. **立即实施**: 动态验证规则 + 用户提示（建议 1 + 2）
2. **未来考虑**: 支持混合模式（建议 4）

### 预期效果

- ✅ 用户体验提升：清晰的提示，合理的验证
- ✅ 减少困惑：用户明确知道哪个配置会生效
- ✅ 保持灵活性：不强制禁用字段，用户可以自由选择

---

**文档创建时间**: 2026-02-12 16:30
**作者**: Claude Code
**状态**: 待评审
**下一步**: 等待用户确认改进方案
