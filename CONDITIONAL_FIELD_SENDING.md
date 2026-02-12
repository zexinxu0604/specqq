# 条件字段发送优化

**原则**: 策略未开启时，不发送该策略的相关配置字段

**日期**: 2026-02-12

---

## 设计原则

### ✅ 正确的做法

**不开启某个策略时，该策略的所有相关字段都不应该传**

```json
{
  "rateLimitEnabled": false
  // ✅ 不包含 rateLimitMaxRequests, rateLimitWindowSeconds
}
```

**原因**:
1. **语义清晰**: 未开启的策略不需要配置参数
2. **减少验证**: 后端不需要验证未使用的字段
3. **数据精简**: 减少不必要的数据传输
4. **逻辑一致**: 所有策略遵循统一的规则

### ❌ 错误的做法

**发送空值或默认值**

```json
{
  "rateLimitEnabled": false,
  "rateLimitMaxRequests": 10,        // ❌ 未开启但仍然发送
  "rateLimitWindowSeconds": 60       // ❌ 未开启但仍然发送
}
```

**问题**:
1. 语义不清: 为什么禁用的策略还有配置？
2. 浪费带宽: 发送了不需要的数据
3. 容易出错: 可能触发不必要的验证

---

## 策略字段映射

### 1. Rate Limit 策略

| 字段 | 类型 | 发送条件 |
|------|------|---------|
| `rateLimitEnabled` | Boolean | ✅ 总是发送 |
| `rateLimitMaxRequests` | Integer | ⚠️ 仅当 `rateLimitEnabled=true` |
| `rateLimitWindowSeconds` | Integer | ⚠️ 仅当 `rateLimitEnabled=true` |

### 2. Time Window 策略

| 字段 | 类型 | 发送条件 |
|------|------|---------|
| `timeWindowEnabled` | Boolean | ✅ 总是发送 |
| `timeWindowStart` | String (HH:mm:ss) | ⚠️ 仅当 `timeWindowEnabled=true` |
| `timeWindowEnd` | String (HH:mm:ss) | ⚠️ 仅当 `timeWindowEnabled=true` |
| `timeWindowWeekdays` | String (1,2,3...) | ⚠️ 仅当 `timeWindowEnabled=true` |

### 3. Role 策略

| 字段 | 类型 | 发送条件 |
|------|------|---------|
| `roleEnabled` | Boolean | ✅ 总是发送 |
| `allowedRoles` | Array<String> | ⚠️ 仅当 `roleEnabled=true` |

### 4. Cooldown 策略

| 字段 | 类型 | 发送条件 |
|------|------|---------|
| `cooldownEnabled` | Boolean | ✅ 总是发送 |
| `cooldownSeconds` | Integer | ⚠️ 仅当 `cooldownEnabled=true` |

### 5. Scope 策略

| 字段 | 类型 | 发送条件 |
|------|------|---------|
| `scope` | String (USER/GROUP/GLOBAL) | ✅ 总是发送 |
| `whitelist` | Array<String> | ✅ 总是发送（可为空数组）|
| `blacklist` | Array<String> | ✅ 总是发送（可为空数组）|

---

## 实现代码

### 前端 (PolicyEditor.vue)

```typescript
const getPolicyDTO = (): PolicyDTO => {
  // Base policy with always-required fields
  const policy: PolicyDTO = {
    scope: formModel.value.scope,
    whitelist: whitelistInput.value.split('\n').filter(s => s.trim()),
    blacklist: blacklistInput.value.split('\n').filter(s => s.trim()),

    // Enabled flags (always send)
    rateLimitEnabled: formModel.value.rateLimitEnabled,
    timeWindowEnabled: formModel.value.timeWindowEnabled,
    roleEnabled: formModel.value.roleEnabled,
    cooldownEnabled: formModel.value.cooldownEnabled
  }

  // Conditional fields - only send when enabled

  if (formModel.value.rateLimitEnabled) {
    policy.rateLimitMaxRequests = formModel.value.rateLimitMaxRequests
    policy.rateLimitWindowSeconds = formModel.value.rateLimitWindowSeconds
  }

  if (formModel.value.timeWindowEnabled) {
    policy.timeWindowStart = addSeconds(formModel.value.timeWindowStart)
    policy.timeWindowEnd = addSeconds(formModel.value.timeWindowEnd)
    if (formModel.value.timeWindowWeekdays?.length > 0) {
      policy.timeWindowWeekdays = formModel.value.timeWindowWeekdays.join(',')
    }
  }

  if (formModel.value.roleEnabled) {
    policy.allowedRoles = formModel.value.allowedRoles
  }

  if (formModel.value.cooldownEnabled) {
    policy.cooldownSeconds = formModel.value.cooldownSeconds
  }

  return policy
}
```

### 后端验证 (RuleUpdateDTO.java)

后端的 `@Pattern` 验证已经支持空字符串，提供容错：

```java
@Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "时间格式必须为 HH:mm:ss")
private String timeWindowStart;
```

**注意**: 虽然后端允许空字符串，但**前端应该遵循不发送的原则**。后端的容错是为了处理边界情况和旧数据。

---

## 数据示例

### 场景 1: 所有策略都禁用

**请求体**:
```json
{
  "policy": {
    "scope": "GLOBAL",
    "whitelist": [],
    "blacklist": [],
    "rateLimitEnabled": false,
    "timeWindowEnabled": false,
    "roleEnabled": false,
    "cooldownEnabled": false
  }
}
```

**特点**:
- ✅ 只发送 enabled 标志和基础字段
- ✅ 不包含任何策略的配置参数
- ✅ 数据精简，语义清晰

---

### 场景 2: 只启用限流策略

**请求体**:
```json
{
  "policy": {
    "scope": "USER",
    "whitelist": [],
    "blacklist": [],
    "rateLimitEnabled": true,
    "rateLimitMaxRequests": 10,
    "rateLimitWindowSeconds": 60,
    "timeWindowEnabled": false,
    "roleEnabled": false,
    "cooldownEnabled": false
  }
}
```

**特点**:
- ✅ 只包含限流策略的配置参数
- ✅ 其他策略只有 enabled 标志

---

### 场景 3: 启用多个策略

**请求体**:
```json
{
  "policy": {
    "scope": "USER",
    "whitelist": ["123456"],
    "blacklist": [],
    "rateLimitEnabled": true,
    "rateLimitMaxRequests": 10,
    "rateLimitWindowSeconds": 60,
    "timeWindowEnabled": true,
    "timeWindowStart": "09:00:00",
    "timeWindowEnd": "18:00:00",
    "timeWindowWeekdays": "1,2,3,4,5",
    "roleEnabled": true,
    "allowedRoles": ["owner", "admin"],
    "cooldownEnabled": false
  }
}
```

**特点**:
- ✅ 包含所有启用策略的配置参数
- ✅ 冷却策略禁用，不包含 cooldownSeconds

---

## 测试验证

### Test 1: 所有策略禁用

```bash
curl -X PUT http://localhost:8080/api/rules/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "policy": {
      "scope": "GLOBAL",
      "whitelist": [],
      "blacklist": [],
      "rateLimitEnabled": false,
      "timeWindowEnabled": false,
      "roleEnabled": false,
      "cooldownEnabled": false
    }
  }'
```

**预期**: ✅ 成功
**实际**: ✅ 成功

---

### Test 2: 部分策略启用

```bash
curl -X PUT http://localhost:8080/api/rules/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "policy": {
      "scope": "USER",
      "rateLimitEnabled": true,
      "rateLimitMaxRequests": 5,
      "rateLimitWindowSeconds": 30,
      "timeWindowEnabled": false,
      "roleEnabled": false,
      "cooldownEnabled": false
    }
  }'
```

**预期**: ✅ 成功
**实际**: ✅ 成功

---

## 对比：修改前后

### 修改前（❌ 不推荐）

```typescript
const policy = {
  scope: formModel.value.scope,
  rateLimitEnabled: formModel.value.rateLimitEnabled,
  rateLimitMaxRequests: formModel.value.rateLimitMaxRequests,      // 总是发送
  rateLimitWindowSeconds: formModel.value.rateLimitWindowSeconds,  // 总是发送
  timeWindowEnabled: formModel.value.timeWindowEnabled,
  timeWindowStart: formModel.value.timeWindowStart,                // 总是发送
  timeWindowEnd: formModel.value.timeWindowEnd,                    // 总是发送
  // ... 其他字段总是发送
}
```

**问题**:
- 禁用的策略仍然发送配置参数
- 可能触发不必要的验证
- 数据冗余

---

### 修改后（✅ 推荐）

```typescript
const policy = {
  scope: formModel.value.scope,
  rateLimitEnabled: formModel.value.rateLimitEnabled,
  // rateLimitMaxRequests/WindowSeconds 只在 enabled=true 时添加
  timeWindowEnabled: formModel.value.timeWindowEnabled,
  // timeWindowStart/End/Weekdays 只在 enabled=true 时添加
}

if (formModel.value.rateLimitEnabled) {
  policy.rateLimitMaxRequests = formModel.value.rateLimitMaxRequests
  policy.rateLimitWindowSeconds = formModel.value.rateLimitWindowSeconds
}

// 其他策略同理
```

**优点**:
- 语义清晰：只发送需要的字段
- 减少验证：后端不验证未使用的字段
- 数据精简：减少传输量

---

## 后端处理建议

虽然前端已经实现条件发送，但后端也应该有相应的处理逻辑：

### 1. 验证逻辑

```java
// Service 层
public void validatePolicy(PolicyDTO policy) {
    // 只验证启用的策略
    if (Boolean.TRUE.equals(policy.getRateLimitEnabled())) {
        if (policy.getRateLimitMaxRequests() == null || policy.getRateLimitMaxRequests() < 1) {
            throw new ValidationException("限流策略启用时，最大请求数必须大于0");
        }
    }

    if (Boolean.TRUE.equals(policy.getTimeWindowEnabled())) {
        if (StringUtils.isBlank(policy.getTimeWindowStart())) {
            throw new ValidationException("时间窗口策略启用时，开始时间不能为空");
        }
    }

    // 其他策略同理
}
```

### 2. 数据清理

```java
// Service 层：保存前清理未启用策略的字段
public void cleanupPolicy(PolicyDTO policy) {
    if (!Boolean.TRUE.equals(policy.getRateLimitEnabled())) {
        policy.setRateLimitMaxRequests(null);
        policy.setRateLimitWindowSeconds(null);
    }

    if (!Boolean.TRUE.equals(policy.getTimeWindowEnabled())) {
        policy.setTimeWindowStart(null);
        policy.setTimeWindowEnd(null);
        policy.setTimeWindowWeekdays(null);
    }

    // 其他策略同理
}
```

---

## 最佳实践总结

### 1. 前端原则

- ✅ 只发送启用策略的配置字段
- ✅ enabled 标志总是发送
- ✅ 基础字段（scope, whitelist, blacklist）总是发送
- ❌ 不发送未启用策略的配置字段

### 2. 后端原则

- ✅ 使用宽松的验证（允许空字符串）提供容错
- ✅ 在 Service 层根据 enabled 标志进行业务验证
- ✅ 保存前清理未启用策略的字段
- ❌ 不在 DTO 层强制验证可选字段

### 3. 数据一致性

- ✅ 数据库中未启用策略的字段应为 null
- ✅ 查询时返回完整的 policy 对象（包括 null 字段）
- ✅ 前端接收时正确处理 null 值

---

## 相关文档

1. **POLICY_VALIDATION_COMPLETE_FIX.md** - 完整修复总结
2. **TIME_FORMAT_FIX.md** - 时间格式转换
3. **EMPTY_TIME_VALIDATION_FIX.md** - 空时间字段验证
4. **CONDITIONAL_FIELD_SENDING.md** (本文档) - 条件字段发送原则

---

**文档创建时间**: 2026-02-12 15:05
**原则制定**: 基于用户反馈
**实现状态**: ✅ 已实现
**测试状态**: ✅ 已验证
