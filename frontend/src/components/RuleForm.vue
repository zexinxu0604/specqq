<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="100px"
  >
    <el-form-item label="规则名称" prop="name">
      <el-input
        v-model="formData.name"
        placeholder="请输入规则名称"
        clearable
        @blur="checkNameUnique"
      />
    </el-form-item>

    <el-form-item label="匹配类型" prop="matchType">
      <el-select
        v-model="formData.matchType"
        placeholder="请选择匹配类型"
        style="width: 100%"
        @change="handleMatchTypeChange"
      >
        <el-option
          v-for="(label, value) in MatchTypeLabels"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
      <div class="form-tip">
        <el-text size="small" type="info">
          {{ getMatchTypeTip(formData.matchType) }}
        </el-text>
      </div>
    </el-form-item>

    <el-form-item label="匹配模式" prop="matchPattern">
      <el-input
        v-model="formData.matchPattern"
        placeholder="请输入匹配模式"
        clearable
        @blur="handlePatternBlur"
      >
        <template #append>
          <el-button
            :icon="CircleCheck"
            :loading="validating"
            @click="validatePattern"
          >
            验证
          </el-button>
        </template>
      </el-input>
      <div v-if="patternValidation.message" class="form-tip">
        <el-text
          size="small"
          :type="patternValidation.valid ? 'success' : 'danger'"
        >
          {{ patternValidation.message }}
        </el-text>
      </div>
    </el-form-item>

    <el-form-item label="回复模板" prop="replyTemplate">
      <el-input
        v-model="formData.replyTemplate"
        type="textarea"
        :rows="4"
        placeholder="请输入回复模板"
        maxlength="500"
        show-word-limit
      />
      <div class="form-tip">
        <el-text size="small" type="info">
          支持变量: {user} - 用户昵称, {message} - 原消息内容
        </el-text>
      </div>
    </el-form-item>

    <el-form-item label="优先级" prop="priority">
      <el-slider
        v-model="formData.priority"
        :min="0"
        :max="100"
        :marks="priorityMarks"
        show-input
      />
      <div class="form-tip">
        <el-text size="small" type="info">
          优先级越高，规则越先匹配（0-100）
        </el-text>
      </div>
    </el-form-item>

    <el-form-item label="规则描述" prop="description">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="2"
        placeholder="请输入规则描述（可选）"
        maxlength="200"
        show-word-limit
      />
    </el-form-item>

    <!-- 测试区域 -->
    <el-divider content-position="left">规则测试</el-divider>

    <el-form-item label="测试消息">
      <el-input
        v-model="testMessage"
        placeholder="输入测试消息，查看是否匹配"
        clearable
      >
        <template #append>
          <el-button
            :icon="Promotion"
            :loading="testing"
            @click="testRule"
          >
            测试
          </el-button>
        </template>
      </el-input>
      <div v-if="testResult" class="form-tip">
        <el-text
          size="small"
          :type="testResult.matched ? 'success' : 'warning'"
        >
          {{ testResult.message }}
        </el-text>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { CircleCheck, Promotion } from '@element-plus/icons-vue'
import { validatePattern as validatePatternApi, testRuleMatch } from '@/api/modules/rule.api'
import { MatchType, MatchTypeLabels } from '@/types/rule'
import type { CreateRuleRequest, UpdateRuleRequest, TestRuleResponse, ValidatePatternResponse } from '@/types/rule'

interface Props {
  modelValue: CreateRuleRequest | UpdateRuleRequest
  isEdit?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: CreateRuleRequest | UpdateRuleRequest): void
}

const props = withDefaults(defineProps<Props>(), {
  isEdit: false
})

const emit = defineEmits<Emits>()

// 表单引用
const formRef = ref<FormInstance>()

// 表单数据
const formData = reactive<CreateRuleRequest | UpdateRuleRequest>({
  name: '',
  matchType: MatchType.KEYWORD,
  matchPattern: '',
  replyTemplate: '',
  priority: 50,
  description: '',
  ...props.modelValue
})

// 监听表单数据变化
watch(formData, (newVal) => {
  emit('update:modelValue', newVal)
}, { deep: true })

// 优先级标记
const priorityMarks = {
  0: '最低',
  25: '低',
  50: '中',
  75: '高',
  100: '最高'
}

// 表单验证规则
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入规则名称', trigger: 'blur' },
    { min: 2, max: 50, message: '规则名称长度在2-50个字符', trigger: 'blur' }
  ],
  matchType: [
    { required: true, message: '请选择匹配类型', trigger: 'change' }
  ],
  matchPattern: [
    { required: true, message: '请输入匹配模式', trigger: 'blur' }
  ],
  replyTemplate: [
    { required: true, message: '请输入回复模板', trigger: 'blur' },
    { min: 1, max: 500, message: '回复模板长度在1-500个字符', trigger: 'blur' }
  ],
  priority: [
    { required: true, message: '请设置优先级', trigger: 'blur' }
  ]
}

// 匹配类型提示
const getMatchTypeTip = (type: MatchType): string => {
  const tips: Record<MatchType, string> = {
    [MatchType.KEYWORD]: '消息中包含关键词即可匹配',
    [MatchType.REGEX]: '使用正则表达式进行匹配',
    [MatchType.PREFIX]: '消息以指定前缀开头时匹配',
    [MatchType.SUFFIX]: '消息以指定后缀结尾时匹配',
    [MatchType.EXACT]: '消息内容完全相同时匹配'
  }
  return tips[type] || ''
}

// 匹配类型变化处理
const handleMatchTypeChange = () => {
  // 清空验证结果
  patternValidation.value = { valid: false, message: '' }
}

// 模式验证
const validating = ref(false)
const patternValidation = ref<ValidatePatternResponse>({ valid: false, message: '' })

const validatePattern = async () => {
  if (!formData.matchPattern) {
    ElMessage.warning('请先输入匹配模式')
    return
  }

  // 只有正则表达式需要验证
  if (formData.matchType !== MatchType.REGEX) {
    patternValidation.value = { valid: true, message: '✓ 模式格式正确' }
    return
  }

  validating.value = true
  try {
    const response = await validatePatternApi(formData.matchPattern)
    patternValidation.value = response.data
    if (response.data.valid) {
      ElMessage.success('正则表达式验证通过')
    }
  } catch (error: any) {
    patternValidation.value = { valid: false, message: error.message || '验证失败' }
  } finally {
    validating.value = false
  }
}

// 模式失焦时自动验证
const handlePatternBlur = () => {
  if (formData.matchPattern && formData.matchType === MatchType.REGEX) {
    validatePattern()
  }
}

// 规则名称唯一性检查
const checkNameUnique = async () => {
  // TODO: 实现异步唯一性验证
  // 暂时跳过，由后端统一处理
}

// 测试规则
const testing = ref(false)
const testMessage = ref('')
const testResult = ref<TestRuleResponse | null>(null)

const testRule = async () => {
  if (!testMessage.value) {
    ElMessage.warning('请输入测试消息')
    return
  }

  if (!formData.matchType || !formData.matchPattern) {
    ElMessage.warning('请先填写匹配类型和匹配模式')
    return
  }

  testing.value = true
  try {
    const response = await testRuleMatch({
      matchType: formData.matchType,
      matchPattern: formData.matchPattern,
      testMessage: testMessage.value
    })
    testResult.value = response.data
  } catch (error: any) {
    ElMessage.error(error.message || '测试失败')
  } finally {
    testing.value = false
  }
}

// 暴露验证方法
const validate = async (): Promise<boolean> => {
  if (!formRef.value) return false

  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

// 暴露重置方法
const resetFields = () => {
  formRef.value?.resetFields()
  patternValidation.value = { valid: false, message: '' }
  testResult.value = null
  testMessage.value = ''
}

defineExpose({
  validate,
  resetFields
})
</script>

<style scoped lang="scss">
.form-tip {
  margin-top: 4px;
  line-height: 1.5;
}

:deep(.el-slider__marks-text) {
  font-size: 12px;
}
</style>
