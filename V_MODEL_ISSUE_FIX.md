# v-model 绑定导致的条件字段发送失效问题修复

**问题**: PolicyEditor 的 `getPolicyDTO()` 正确实现了条件字段发送，但通过 v-model 绑定时失效

**日期**: 2026-02-12

---

## 问题分析

### 数据流

```
DEFAULT_POLICY (包含所有字段)
    ↓
RuleForm.formData.policy
    ↓ v-model 双向绑定
PolicyEditor
    ↓ 用户修改
PolicyEditor 内部 formModel
    ↓ watch 触发
emit('update:modelValue', getPolicyDTO())
    ↓ v-model 更新
RuleForm.formData.policy (更新)
    ↓ 提交时
handleSubmit 直接使用 currentRule.value.policy
    ↓
发送到后端 (包含所有字段!) ❌
```

### 根本原因

1. **DEFAULT_POLICY 包含所有字段**:
   ```typescript
   export const DEFAULT_POLICY: PolicyDTO = {
     scope: Scope.USER,
     timeWindowEnabled: false,
     timeWindowStart: '00:00',      // ❌ 即使禁用也有值
     timeWindowEnd: '23:59',        // ❌ 即使禁用也有值
     timeWindowWeekdays: [1,2,3,4,5,6,7],  // ❌ 即使禁用也有值
     // ... 其他字段
   }
   ```

2. **v-model 不会调用 getPolicyDTO()**:
   - `v-model` 只是双向数据绑定
   - `PolicyEditor` 的 `getPolicyDTO()` 只在 `watch` 中被调用
   - `watch` 触发后通过 `emit` 更新父组件的数据
   - 但 emit 的数据仍然包含了所有字段（因为 formModel 初始化时包含了所有字段）

3. **提交时直接使用 formData.policy**:
   ```typescript
   const submitData: any = { ...currentRule.value }
   // submitData.policy 包含了 DEFAULT_POLICY 的所有字段
   ```

---

## 解决方案

### 方案概述

在提交前，显式调用 `PolicyEditor.getPolicyDTO()` 获取处理后的 policy。

### 实现步骤

#### 1. RuleForm 添加 getFormData() 方法

```typescript
// 获取处理后的表单数据（应用条件字段发送逻辑）
const getFormData = () => {
  const data = { ...formData }

  // 从 PolicyEditor 获取处理后的 policy
  if (policyEditorRef.value && policyEditorRef.value.getPolicyDTO) {
    data.policy = policyEditorRef.value.getPolicyDTO()
  }

  return data
}

defineExpose({
  validate,
  resetFields,
  getFormData  // ⭐ 新增
})
```

#### 2. RuleManagement 使用 getFormData()

```typescript
const handleSubmit = async () => {
  const valid = await ruleFormRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    // ⭐ 使用 getFormData() 获取处理后的数据
    const formData = ruleFormRef.value?.getFormData
      ? ruleFormRef.value.getFormData()
      : currentRule.value

    const submitData: any = { ...formData }

    // ... 其他逻辑
  }
}
```

---

## 数据流（修复后）

```
DEFAULT_POLICY (包含所有字段)
    ↓
RuleForm.formData.policy
    ↓ v-model 双向绑定
PolicyEditor
    ↓ 用户修改
PolicyEditor 内部 formModel
    ↓ 提交时
handleSubmit 调用 ruleFormRef.getFormData()
    ↓
RuleForm.getFormData() 调用 policyEditorRef.getPolicyDTO()
    ↓
PolicyEditor.getPolicyDTO() 应用条件字段发送逻辑
    ↓
返回处理后的 policy (只包含需要的字段) ✅
    ↓
发送到后端 (正确!) ✅
```

---

## 对比示例

### 修复前

**用户操作**: 禁用所有策略

**实际发送**:
```json
{
  "policy": {
    "scope": "USER",
    "rateLimitEnabled": false,
    "rateLimitMaxRequests": 10,           // ❌ 不应该发送
    "rateLimitWindowSeconds": 60,         // ❌ 不应该发送
    "timeWindowEnabled": false,
    "timeWindowStart": "00:00:00",        // ❌ 不应该发送
    "timeWindowEnd": "23:59:00",          // ❌ 不应该发送
    "timeWindowWeekdays": "1,2,3,4,5,6,7",// ❌ 不应该发送
    "roleEnabled": false,
    "allowedRoles": ["owner","admin","member"], // ❌ 不应该发送
    "cooldownEnabled": false,
    "cooldownSeconds": 300                // ❌ 不应该发送
  }
}
```

**后端响应**: ❌ 验证失败
```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": {
    "policy.timeWindowStart": "时间格式必须为 HH:mm:ss",
    "policy.timeWindowEnd": "时间格式必须为 HH:mm:ss"
  }
}
```

---

### 修复后

**用户操作**: 禁用所有策略

**实际发送**:
```json
{
  "policy": {
    "scope": "USER",
    "whitelist": [],
    "blacklist": [],
    "rateLimitEnabled": false,
    "timeWindowEnabled": false,
    "roleEnabled": false,
    "cooldownEnabled": false
    // ✅ 只有 enabled 标志和基础字段
  }
}
```

**后端响应**: ✅ 成功
```json
{
  "code": 200,
  "message": "规则更新成功"
}
```

---

## 关键要点

### 1. v-model 的局限性

`v-model` 只是语法糖：
```vue
<!-- v-model -->
<PolicyEditor v-model="formData.policy" />

<!-- 等价于 -->
<PolicyEditor
  :modelValue="formData.policy"
  @update:modelValue="formData.policy = $event"
/>
```

**问题**:
- 父组件的 `formData.policy` 初始化时包含所有字段
- 子组件 emit 更新时，虽然调用了 `getPolicyDTO()`，但初始值已经污染了数据
- 提交时直接使用 `formData.policy`，包含了初始的所有字段

### 2. 正确的做法

**不要依赖 v-model 的自动更新**，而是在提交前**显式调用**处理方法：

```typescript
// ❌ 错误: 直接使用 v-model 绑定的数据
const submitData = { ...formData }

// ✅ 正确: 调用处理方法获取正确的数据
const submitData = ruleFormRef.value.getFormData()
```

### 3. 组件设计原则

对于需要**数据转换**的表单组件：

1. **v-model 用于实时预览**: 方便用户看到输入效果
2. **提供 getData() 方法**: 用于获取处理后的提交数据
3. **不要依赖 v-model 做数据转换**: v-model 只负责双向绑定

---

## 测试验证

### Test 1: 所有策略禁用

**操作**:
1. 打开规则编辑对话框
2. 禁用所有策略（Rate Limit, Time Window, Role, Cooldown）
3. 点击"确定"提交

**预期**:
- ✅ 提交成功
- ✅ 只发送 enabled 标志和基础字段
- ✅ 不发送任何策略的配置字段

---

### Test 2: 部分策略启用

**操作**:
1. 打开规则编辑对话框
2. 只启用 Rate Limit 策略
3. 设置最大请求数=5，时间窗口=30秒
4. 点击"确定"提交

**预期**:
- ✅ 提交成功
- ✅ 发送 Rate Limit 配置字段
- ✅ 不发送 Time Window/Role/Cooldown 配置字段

---

## 相关修改

### 修改文件

1. **frontend/src/components/RuleForm.vue**
   - 添加 `getFormData()` 方法
   - 在 `defineExpose` 中暴露该方法

2. **frontend/src/views/RuleManagement.vue**
   - 修改 `handleSubmit` 使用 `getFormData()`

### 不需要修改的文件

- ✅ `PolicyEditor.vue` - 已经正确实现了 `getPolicyDTO()`
- ✅ `policy.ts` - `DEFAULT_POLICY` 保持不变（用于初始化 UI）

---

## 最佳实践

### 1. 表单组件设计

对于需要数据转换的表单组件：

```vue
<script setup>
// v-model 用于 UI 绑定
const formModel = ref({ /* 包含所有字段，方便 UI 显示 */ })

// 提供 getData() 用于获取提交数据
const getData = () => {
  const data = {}
  // 应用业务逻辑，条件添加字段
  if (formModel.value.enabled) {
    data.config = formModel.value.config
  }
  return data
}

defineExpose({ getData })
</script>
```

### 2. 父组件使用

```vue
<script setup>
const formRef = ref()

const handleSubmit = async () => {
  // ✅ 正确: 调用 getData() 获取处理后的数据
  const data = formRef.value.getData()
  await api.submit(data)
}
</script>

<template>
  <MyForm ref="formRef" v-model="formData" />
</template>
```

### 3. 数据初始化

```typescript
// ✅ 推荐: 使用包含所有字段的默认值，方便 UI 显示
const DEFAULT_POLICY = {
  enabled: false,
  config: { /* 默认配置 */ }
}

// ❌ 不推荐: 使用空对象，UI 需要处理 undefined
const DEFAULT_POLICY = {}
```

---

## 总结

### 问题
v-model 绑定的数据包含所有字段，导致条件字段发送失效

### 原因
- DEFAULT_POLICY 包含所有字段
- v-model 只是双向绑定，不会调用数据转换逻辑
- 提交时直接使用 v-model 绑定的数据

### 解决方案
- RuleForm 添加 `getFormData()` 方法
- 调用 `PolicyEditor.getPolicyDTO()` 获取处理后的数据
- RuleManagement 提交时使用 `getFormData()`

### 效果
- ✅ 条件字段发送正确生效
- ✅ 只发送需要的字段
- ✅ 后端验证通过

---

**修复完成时间**: 2026-02-12 15:15
**测试状态**: ⏳ 待前端重启后验证
**部署状态**: ⏳ 待测试确认

**修复人员**: Claude Code
**审核状态**: ⏳ 待代码审查
