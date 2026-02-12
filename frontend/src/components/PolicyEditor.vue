<template>
  <div class="policy-editor">
    <el-form
      ref="formRef"
      :model="formModel"
      :rules="rules"
      label-width="120px"
      label-position="left"
    >
      <!-- Scope Policy Section -->
      <el-divider content-position="left">
        <el-icon><Setting /></el-icon>
        作用域策略
      </el-divider>

      <el-form-item label="作用域" prop="scope">
        <el-select v-model="formModel.scope" placeholder="请选择作用域">
          <el-option
            v-for="(label, value) in ScopeLabels"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="白名单" prop="whitelist">
        <el-input
          v-model="whitelistInput"
          type="textarea"
          :rows="2"
          placeholder="每行一个ID，支持用户ID或群ID"
        />
        <span class="form-tip">留空表示不限制</span>
      </el-form-item>

      <el-form-item label="黑名单" prop="blacklist">
        <el-input
          v-model="blacklistInput"
          type="textarea"
          :rows="2"
          placeholder="每行一个ID，支持用户ID或群ID"
        />
        <span class="form-tip">黑名单优先级高于白名单</span>
      </el-form-item>

      <!-- Rate Limit Policy Section -->
      <el-divider content-position="left">
        <el-icon><Timer /></el-icon>
        限流策略
      </el-divider>

      <el-form-item label="启用限流">
        <el-switch v-model="formModel.rateLimitEnabled" />
      </el-form-item>

      <template v-if="formModel.rateLimitEnabled">
        <el-form-item label="最大请求数" prop="rateLimitMaxRequests">
          <el-input-number
            v-model="formModel.rateLimitMaxRequests"
            :min="1"
            :max="1000"
          />
          <span class="form-tip">时间窗口内允许的最大请求数</span>
        </el-form-item>

        <el-form-item label="时间窗口" prop="rateLimitWindowSeconds">
          <el-input-number
            v-model="formModel.rateLimitWindowSeconds"
            :min="1"
            :max="3600"
          />
          <span class="form-tip">秒</span>
        </el-form-item>
      </template>

      <!-- Time Window Policy Section -->
      <el-divider content-position="left">
        <el-icon><Clock /></el-icon>
        时间窗口策略
      </el-divider>

      <el-form-item label="启用时间窗口">
        <el-switch v-model="formModel.timeWindowEnabled" />
      </el-form-item>

      <template v-if="formModel.timeWindowEnabled">
        <el-form-item label="生效时间段" prop="timeWindow">
          <el-time-picker
            v-model="timeWindowRange"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="HH:mm"
            value-format="HH:mm"
          />
        </el-form-item>

        <el-form-item label="生效星期" prop="timeWindowWeekdays">
          <el-checkbox-group v-model="formModel.timeWindowWeekdays">
            <el-checkbox
              v-for="day in WEEKDAY_OPTIONS"
              :key="day.value"
              :label="day.value"
            >
              {{ day.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </template>

      <!-- Role Policy Section -->
      <el-divider content-position="left">
        <el-icon><User /></el-icon>
        角色策略
      </el-divider>

      <el-form-item label="启用角色限制">
        <el-switch v-model="formModel.roleEnabled" />
      </el-form-item>

      <template v-if="formModel.roleEnabled">
        <el-form-item label="允许的角色" prop="allowedRoles">
          <el-checkbox-group v-model="formModel.allowedRoles">
            <el-checkbox
              v-for="role in ROLE_OPTIONS"
              :key="role.value"
              :label="role.value"
            >
              {{ role.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </template>

      <!-- Cooldown Policy Section -->
      <el-divider content-position="left">
        <el-icon><Stopwatch /></el-icon>
        冷却策略
      </el-divider>

      <el-form-item label="启用冷却">
        <el-switch v-model="formModel.cooldownEnabled" />
      </el-form-item>

      <template v-if="formModel.cooldownEnabled">
        <el-form-item label="冷却时间" prop="cooldownSeconds">
          <el-input-number
            v-model="formModel.cooldownSeconds"
            :min="1"
            :max="86400"
          />
          <span class="form-tip">秒（触发后多久才能再次触发）</span>
        </el-form-item>
      </template>

      <!-- Template Selection -->
      <el-divider content-position="left">
        <el-icon><Document /></el-icon>
        快速模板
      </el-divider>

      <el-form-item label="应用模板">
        <el-select
          v-model="selectedTemplate"
          placeholder="选择预设模板"
          @change="applyTemplate"
        >
          <el-option
            v-for="template in POLICY_TEMPLATES"
            :key="template.name"
            :label="template.displayName"
            :value="template.name"
          >
            <span>{{ template.displayName }}</span>
            <span class="template-desc">{{ template.description }}</span>
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Setting,
  Timer,
  Clock,
  User,
  Stopwatch,
  Document
} from '@element-plus/icons-vue'
import type { PolicyDTO, PolicyFormModel } from '@/types/policy'
import {
  Scope,
  ScopeLabels,
  DEFAULT_POLICY,
  POLICY_TEMPLATES,
  ROLE_OPTIONS,
  WEEKDAY_OPTIONS
} from '@/types/policy'

/**
 * T045: PolicyEditor Component
 * Form with scope, whitelist/blacklist, rate limit, time window, role, cooldown sections
 */

interface Props {
  modelValue: PolicyDTO
}

interface Emits {
  (e: 'update:modelValue', value: PolicyDTO): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref()
const selectedTemplate = ref('')

// Helper: Convert HH:mm:ss to HH:mm
const removeSeconds = (time: string | undefined) => {
  if (!time) return '00:00'
  const parts = time.split(':')
  return parts.length >= 2 ? `${parts[0]}:${parts[1]}` : time
}

// Form model
const formModel = ref<PolicyFormModel>({
  scope: props.modelValue?.scope || Scope.USER,
  whitelist: '',
  blacklist: '',
  rateLimitEnabled: props.modelValue?.rateLimitEnabled || false,
  rateLimitMaxRequests: props.modelValue?.rateLimitMaxRequests || 10,
  rateLimitWindowSeconds: props.modelValue?.rateLimitWindowSeconds || 60,
  timeWindowEnabled: props.modelValue?.timeWindowEnabled || false,
  timeWindowStart: removeSeconds(props.modelValue?.timeWindowStart) || '00:00',
  timeWindowEnd: removeSeconds(props.modelValue?.timeWindowEnd) || '23:59',
  timeWindowWeekdays: typeof props.modelValue?.timeWindowWeekdays === 'string'
    ? props.modelValue.timeWindowWeekdays.split(',').map(Number)
    : (props.modelValue?.timeWindowWeekdays || [1, 2, 3, 4, 5, 6, 7]),
  roleEnabled: props.modelValue?.roleEnabled || false,
  allowedRoles: props.modelValue?.allowedRoles || ['owner', 'admin', 'member'],
  cooldownEnabled: props.modelValue?.cooldownEnabled || false,
  cooldownSeconds: props.modelValue?.cooldownSeconds || 300
})

// Whitelist/Blacklist text inputs
const whitelistInput = ref((props.modelValue?.whitelist || []).join('\n'))
const blacklistInput = ref((props.modelValue?.blacklist || []).join('\n'))

// Time window range picker
const timeWindowRange = ref([
  formModel.value.timeWindowStart,
  formModel.value.timeWindowEnd
])

// Watch time window range changes
watch(timeWindowRange, (newVal) => {
  if (newVal && newVal.length === 2) {
    formModel.value.timeWindowStart = newVal[0]
    formModel.value.timeWindowEnd = newVal[1]
  }
})

// Validation rules
const rules = {
  scope: [{ required: true, message: '请选择作用域', trigger: 'change' }],
  rateLimitMaxRequests: [
    { required: true, message: '请输入最大请求数', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '范围: 1-1000', trigger: 'blur' }
  ],
  rateLimitWindowSeconds: [
    { required: true, message: '请输入时间窗口', trigger: 'blur' },
    { type: 'number', min: 1, max: 3600, message: '范围: 1-3600秒', trigger: 'blur' }
  ],
  cooldownSeconds: [
    { required: true, message: '请输入冷却时间', trigger: 'blur' },
    { type: 'number', min: 1, max: 86400, message: '范围: 1-86400秒', trigger: 'blur' }
  ]
}

// Apply template
const applyTemplate = (templateName: string) => {
  const template = POLICY_TEMPLATES.find(t => t.name === templateName)
  if (!template) return

  Object.assign(formModel.value, {
    scope: template.policy.scope || Scope.USER,
    rateLimitEnabled: template.policy.rateLimitEnabled || false,
    rateLimitMaxRequests: template.policy.rateLimitMaxRequests || 10,
    rateLimitWindowSeconds: template.policy.rateLimitWindowSeconds || 60,
    timeWindowEnabled: template.policy.timeWindowEnabled || false,
    timeWindowStart: removeSeconds(template.policy.timeWindowStart) || '00:00',
    timeWindowEnd: removeSeconds(template.policy.timeWindowEnd) || '23:59',
    timeWindowWeekdays: typeof template.policy.timeWindowWeekdays === 'string'
      ? template.policy.timeWindowWeekdays.split(',').map(Number)
      : (template.policy.timeWindowWeekdays || [1, 2, 3, 4, 5, 6, 7]),
    roleEnabled: template.policy.roleEnabled || false,
    allowedRoles: template.policy.allowedRoles || ['owner', 'admin', 'member'],
    cooldownEnabled: template.policy.cooldownEnabled || false,
    cooldownSeconds: template.policy.cooldownSeconds || 300
  })

  ElMessage.success(`已应用模板: ${template.displayName}`)
}

// Convert form model to PolicyDTO
const getPolicyDTO = (): PolicyDTO => {
  const policy: PolicyDTO = {
    scope: formModel.value.scope,
    whitelist: whitelistInput.value.split('\n').filter(s => s.trim()),
    blacklist: blacklistInput.value.split('\n').filter(s => s.trim()),
    rateLimitEnabled: formModel.value.rateLimitEnabled,
    rateLimitMaxRequests: formModel.value.rateLimitMaxRequests,
    rateLimitWindowSeconds: formModel.value.rateLimitWindowSeconds,
    timeWindowEnabled: formModel.value.timeWindowEnabled,
    roleEnabled: formModel.value.roleEnabled,
    allowedRoles: formModel.value.allowedRoles,
    cooldownEnabled: formModel.value.cooldownEnabled,
    cooldownSeconds: formModel.value.cooldownSeconds
  }

  // Only include time window fields when enabled
  if (formModel.value.timeWindowEnabled) {
    // Convert HH:mm to HH:mm:ss format
    const addSeconds = (time: string) => {
      if (!time) return time
      return time.includes(':') && time.split(':').length === 2 ? `${time}:00` : time
    }

    policy.timeWindowStart = addSeconds(formModel.value.timeWindowStart)
    policy.timeWindowEnd = addSeconds(formModel.value.timeWindowEnd)

    // Convert timeWindowWeekdays array to string
    if (formModel.value.timeWindowWeekdays && formModel.value.timeWindowWeekdays.length > 0) {
      policy.timeWindowWeekdays = Array.isArray(formModel.value.timeWindowWeekdays)
        ? formModel.value.timeWindowWeekdays.join(',')
        : formModel.value.timeWindowWeekdays
    }
  }

  return policy
}

// Watch form changes and emit
watch(
  () => formModel.value,
  () => {
    emit('update:modelValue', getPolicyDTO())
  },
  { deep: true }
)

watch([whitelistInput, blacklistInput], () => {
  emit('update:modelValue', getPolicyDTO())
})

// Expose validate method
defineExpose({
  validate: () => formRef.value?.validate(),
  getPolicyDTO
})
</script>

<style scoped lang="scss">
.policy-editor {
  .form-tip {
    margin-left: 10px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .template-desc {
    float: right;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  :deep(.el-divider__text) {
    font-weight: 500;
  }
}
</style>
