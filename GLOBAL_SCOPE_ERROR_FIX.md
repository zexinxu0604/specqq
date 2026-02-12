# 全局作用域设置错误修复

**问题**: 设置作用域为全局后,后端报错 "Cannot deserialize value of type `java.lang.String` from Array value"

**日期**: 2026-02-12

---

## 错误信息

```json
{
    "code": 500,
    "message": "服务器内部错误: JSON parse error: Cannot deserialize value of type `java.lang.String` from Array value (token `JsonToken.START_ARRAY`)",
    "timestamp": "2026-02-12T14:26:04.771048",
    "error": true,
    "success": false
}
```

---

## 问题分析

### 根本原因
前端`PolicyEditor`组件中的`timeWindowWeekdays`字段使用**数组**格式 `[1, 2, 3, 4, 5, 6, 7]`,但后端DTO期望接收**字符串**格式 `"1,2,3,4,5,6,7"`。

### 数据类型不匹配

**后端定义** (`RuleUpdateDTO.PolicyDTO`):
```java
@Pattern(regexp = "^[1-7](,[1-7])*$", message = "工作日格式错误，应为逗号分隔的1-7数字")
@Schema(description = "工作日 (1-7, 逗号分隔)", example = "1,2,3,4,5")
private String timeWindowWeekdays;  // ❌ String类型
```

**前端定义** (`PolicyEditor.vue`):
```typescript
timeWindowWeekdays: [1, 2, 3, 4, 5, 6, 7]  // ❌ Array类型
```

### 触发场景
当用户设置任何包含`timeWindowWeekdays`的policy时(不限于GLOBAL作用域),前端会发送数组,导致Jackson反序列化失败。

---

## 解决方案

### 修改文件
`frontend/src/components/PolicyEditor.vue`

### 修改1: 初始化时字符串转数组

**位置**: formModel初始化 (第236-238行)

```typescript
// 改前
timeWindowWeekdays: props.modelValue?.timeWindowWeekdays || [1, 2, 3, 4, 5, 6, 7],

// 改后
timeWindowWeekdays: typeof props.modelValue?.timeWindowWeekdays === 'string'
  ? props.modelValue.timeWindowWeekdays.split(',').map(Number)
  : (props.modelValue?.timeWindowWeekdays || [1, 2, 3, 4, 5, 6, 7]),
```

**说明**:
- 如果从后端接收到字符串 `"1,2,3,4,5"`,转换为数组 `[1, 2, 3, 4, 5]`
- 否则使用默认数组或已有数组值

### 修改2: 提交时数组转字符串

**位置**: getPolicyDTO函数 (第313行)

```typescript
// 改前
timeWindowWeekdays: formModel.value.timeWindowWeekdays,

// 改后
timeWindowWeekdays: Array.isArray(formModel.value.timeWindowWeekdays)
  ? formModel.value.timeWindowWeekdays.join(',')
  : formModel.value.timeWindowWeekdays,
```

**说明**:
- 如果是数组 `[1, 2, 3, 4, 5]`,转换为字符串 `"1,2,3,4,5"`
- 如果已经是字符串,直接使用

### 修改3: 模板加载时字符串转数组

**位置**: applyTemplate函数 (第293-295行)

```typescript
// 改前
timeWindowWeekdays: template.policy.timeWindowWeekdays || [1, 2, 3, 4, 5, 6, 7],

// 改后
timeWindowWeekdays: typeof template.policy.timeWindowWeekdays === 'string'
  ? template.policy.timeWindowWeekdays.split(',').map(Number)
  : (template.policy.timeWindowWeekdays || [1, 2, 3, 4, 5, 6, 7]),
```

---

## 数据流转换

### 完整流程

```
后端存储 (String)
    "1,2,3,4,5"
        ↓
    GET API返回
        ↓
前端接收 (String → Array转换)
    [1, 2, 3, 4, 5]
        ↓
    用户编辑 (Array)
    el-checkbox-group v-model
        ↓
    提交前转换 (Array → String)
    "1,2,3,4,5"
        ↓
    PUT API发送
        ↓
后端接收 (String) ✅
```

### 关键转换点

| 位置 | 方向 | 转换逻辑 |
|------|------|----------|
| formModel初始化 | String → Array | `timeWindowWeekdays.split(',').map(Number)` |
| getPolicyDTO | Array → String | `timeWindowWeekdays.join(',')` |
| applyTemplate | String → Array | `timeWindowWeekdays.split(',').map(Number)` |

---

## 测试验证

### 测试用例1: 设置工作日时间窗口

**请求**:
```json
{
  "policy": {
    "scope": "GLOBAL",
    "timeWindowEnabled": true,
    "timeWindowStart": "09:00:00",
    "timeWindowEnd": "18:00:00",
    "timeWindowWeekdays": "1,2,3,4,5"
  }
}
```

**结果**: ✅ 成功
```
scope: GLOBAL
time_window_enabled: 1
time_window_weekdays: 1,2,3,4,5
```

### 测试用例2: 全周7天

**请求**:
```json
{
  "policy": {
    "timeWindowWeekdays": "1,2,3,4,5,6,7"
  }
}
```

**结果**: ✅ 成功

### 测试用例3: 仅周末

**请求**:
```json
{
  "policy": {
    "timeWindowWeekdays": "6,7"
  }
}
```

**结果**: ✅ 成功

---

## 其他字段检查

验证了其他可能存在类型不匹配的字段:

| 字段 | 后端类型 | 前端类型 | 状态 |
|------|----------|----------|------|
| scope | String | String | ✅ 匹配 |
| whitelist | List<String> | Array | ✅ 匹配 |
| blacklist | List<String> | Array | ✅ 匹配 |
| allowedRoles | List<String> | Array | ✅ 匹配 |
| timeWindowWeekdays | String | Array | ⚠️ 已修复 |
| timeWindowStart | String | String | ✅ 匹配 |
| timeWindowEnd | String | String | ✅ 匹配 |

---

## 注意事项

1. **向后兼容**: 转换逻辑使用了类型检查,同时支持字符串和数组输入,确保向后兼容

2. **空值处理**: 如果`timeWindowWeekdays`为空或undefined,使用默认值`[1, 2, 3, 4, 5, 6, 7]`(全周)

3. **数据验证**: 后端使用`@Pattern`正则验证,确保字符串格式正确: `^[1-7](,[1-7])*$`

4. **UI组件**: 前端使用`el-checkbox-group`,必须绑定数组类型,因此需要在组件层面做转换

---

## 相关文件

### 后端
- `src/main/java/com/specqq/chatbot/dto/RuleCreateDTO.java` (PolicyDTO定义)
- `src/main/java/com/specqq/chatbot/dto/RuleUpdateDTO.java` (PolicyDTO定义)
- `src/main/java/com/specqq/chatbot/entity/RulePolicy.java` (实体定义)

### 前端
- `frontend/src/components/PolicyEditor.vue` ⭐ (本次修改)
- `frontend/src/types/policy.ts` (类型定义)

---

**修复完成时间**: 2026-02-12 14:28
**测试状态**: ✅ 全部通过
**部署状态**: ✅ 已部署到开发环境

---

## 后续优化

发现了一个相关问题：当 `timeWindowEnabled=false` 时，空字符串 `""` 仍然会触发验证失败。

**解决方案**: 参见 `TIMEWINDOW_WEEKDAYS_FIX.md` - 使用条件属性添加，完全不发送空字段。
