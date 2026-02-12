<template>
  <div class="rule-management">
    <!-- 搜索和操作栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索规则名称或模式"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>

        <el-form-item label="匹配类型">
          <el-select
            v-model="searchForm.matchType"
            placeholder="全部"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option
              v-for="(label, value) in MatchTypeLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="searchForm.enabled"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="handleSearch"
          >
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>

        <el-form-item style="float: right">
          <el-button type="primary" :icon="Plus" @click="handleCreate">
            新建规则
          </el-button>
          <el-button
            type="danger"
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 规则列表 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-table
        v-loading="loading"
        :data="rules"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />

        <el-table-column prop="name" label="规则名称" min-width="150">
          <template #default="{ row }">
            <el-text>{{ row.name }}</el-text>
            <el-tag v-if="!row.enabled" size="small" type="info" style="margin-left: 8px">
              已禁用
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="matchType" label="匹配类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getMatchTypeColor(row.matchType)">
              {{ MatchTypeLabels[row.matchType] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="pattern" label="匹配模式" min-width="180" show-overflow-tooltip />

        <el-table-column prop="responseTemplate" label="回复模板" min-width="200" show-overflow-tooltip />

        <el-table-column label="策略摘要" min-width="180">
          <template #default="{ row }">
            <div v-if="row.policy" class="policy-summary">
              <el-tag
                v-if="row.policy.rateLimitEnabled"
                size="small"
                type="warning"
                effect="plain"
              >
                限流: {{ row.policy.rateLimitMaxRequests }}/{{ row.policy.rateLimitWindowSeconds }}s
              </el-tag>
              <el-tag
                v-if="row.policy.timeWindowEnabled"
                size="small"
                type="info"
                effect="plain"
              >
                时间窗口
              </el-tag>
              <el-tag
                v-if="row.policy.roleEnabled"
                size="small"
                type="success"
                effect="plain"
              >
                角色限制
              </el-tag>
              <el-tag
                v-if="row.policy.cooldownEnabled"
                size="small"
                effect="plain"
              >
                冷却: {{ row.policy.cooldownSeconds }}s
              </el-tag>
              <el-text v-if="!hasPolicyEnabled(row.policy)" size="small" type="info">
                无限制
              </el-text>
            </div>
            <el-text v-else size="small" type="info">
              无限制
            </el-text>
          </template>
        </el-table-column>

        <el-table-column prop="priority" label="优先级" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="getPriorityColor(row.priority)">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              :icon="CopyDocument"
              @click="handleCopy(row)"
            >
              复制
            </el-button>
            <el-button
              link
              :type="row.enabled ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
    >
      <RuleForm
        ref="ruleFormRef"
        v-model="currentRule"
        :is-edit="isEdit"
        :rule-id="currentRuleId"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Delete,
  Edit,
  CopyDocument
} from '@element-plus/icons-vue'
import RuleForm from '@/components/RuleForm.vue'
import {
  listRules,
  getRuleById,
  createRule,
  updateRule,
  deleteRule,
  toggleRuleStatus,
  copyRule,
  batchDeleteRules
} from '@/api/modules/rule.api'
import { useRulesStore } from '@/stores/rules.store'
import { MatchType, MatchTypeLabels } from '@/types/rule'
import type { Rule, CreateRuleRequest, UpdateRuleRequest } from '@/types/rule'
import type { PolicyDTO } from '@/types/policy'

const rulesStore = useRulesStore()

// 搜索表单
const searchForm = reactive({
  keyword: '',
  matchType: undefined as MatchType | undefined,
  enabled: undefined as boolean | undefined
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 规则列表
const rules = ref<Rule[]>([])
const loading = ref(false)

// 选中的规则ID
const selectedIds = ref<number[]>([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => isEdit.value ? '编辑规则' : '新建规则')
const isEdit = ref(false)
const submitting = ref(false)
const ruleFormRef = ref()

// 当前编辑的规则
const currentRule = ref<CreateRuleRequest | UpdateRuleRequest>({
  name: '',
  matchType: MatchType.CONTAINS,
  pattern: '',
  responseTemplate: '',
  priority: 50,
  description: ''
})

// 当前编辑规则的ID (单独存储)
const currentRuleId = ref<number | undefined>(undefined)

// 加载规则列表
const loadRules = async () => {
  loading.value = true
  try {
    const response = await listRules({
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    })

    rules.value = response.data.records
    pagination.total = response.data.total

    // 更新store缓存
    rulesStore.rules = response.data.records
    rulesStore.total = response.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载规则列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadRules()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.matchType = undefined
  searchForm.enabled = undefined
  handleSearch()
}

// 新建规则
const handleCreate = () => {
  isEdit.value = false
  currentRuleId.value = undefined
  currentRule.value = {
    name: '',
    matchType: MatchType.CONTAINS,
    pattern: '',
    responseTemplate: '',
    priority: 50,
    description: ''
  }
  dialogVisible.value = true
}

// 编辑规则
const handleEdit = async (rule: Rule) => {
  console.log('编辑规则, ID:', rule.id, '规则:', rule)
  isEdit.value = true
  currentRuleId.value = rule.id  // 单独存储ID

  try {
    // 获取完整的规则详情(包括policy)
    const response = await getRuleById(rule.id)
    const fullRule = response.data

    // 解析 handlerConfig JSON 为 handlerType 和 handlerParams
    let handlerType: string | undefined
    let handlerParams: Record<string, any> = {}

    if (fullRule.handlerConfig) {
      try {
        const config = typeof fullRule.handlerConfig === 'string'
          ? JSON.parse(fullRule.handlerConfig)
          : fullRule.handlerConfig
        handlerType = config.handlerType
        handlerParams = config.params || {}
      } catch (e) {
        console.warn('Failed to parse handlerConfig:', e)
      }
    }

    currentRule.value = {
      name: fullRule.name,
      matchType: fullRule.matchType,
      pattern: fullRule.pattern,
      responseTemplate: fullRule.responseTemplate,
      priority: fullRule.priority,
      description: fullRule.description,
      handlerType,
      handlerParams,
      policy: fullRule.policy
    }
    console.log('设置currentRuleId:', currentRuleId.value)
    console.log('设置policy:', fullRule.policy)
    dialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取规则详情失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  // 验证表单
  const valid = await ruleFormRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    // 从 RuleForm 获取处理后的数据（应用条件字段发送逻辑）
    const formData = ruleFormRef.value?.getFormData ? ruleFormRef.value.getFormData() : currentRule.value
    const submitData: any = { ...formData }

    // 如果有 handlerType，构建 handlerConfig JSON
    if (submitData.handlerType) {
      submitData.handlerConfig = JSON.stringify({
        handlerType: submitData.handlerType,
        params: submitData.handlerParams || {}
      })
      // 删除前端专用字段
      delete submitData.handlerType
      delete submitData.handlerParams
    }

    if (isEdit.value) {
      if (!currentRuleId.value) {
        ElMessage.error('规则ID不存在,无法更新')
        return
      }
      console.log('更新规则, ID:', currentRuleId.value, '数据:', submitData)
      await updateRule(currentRuleId.value, submitData)
      ElMessage.success('规则更新成功')
    } else {
      console.log('创建规则, 数据:', submitData)
      await createRule(submitData as CreateRuleRequest)
      ElMessage.success('规则创建成功')
    }

    dialogVisible.value = false
    loadRules()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 复制规则
const handleCopy = async (rule: Rule) => {
  try {
    await copyRule(rule.id)
    ElMessage.success('规则复制成功')
    loadRules()
  } catch (error: any) {
    ElMessage.error(error.message || '复制失败')
  }
}

// 切换启用状态
const handleToggleStatus = async (rule: Rule) => {
  try {
    await toggleRuleStatus(rule.id, !rule.enabled)
    ElMessage.success(`规则已${rule.enabled ? '禁用' : '启用'}`)
    rule.enabled = !rule.enabled
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 删除规则
const handleDelete = (rule: Rule) => {
  ElMessageBox.confirm(
    `确定要删除规则 "${rule.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteRule(rule.id)
      ElMessage.success('规则删除成功')
      loadRules()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条规则吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await batchDeleteRules(selectedIds.value)
      ElMessage.success('批量删除成功')
      selectedIds.value = []
      loadRules()
    } catch (error: any) {
      ElMessage.error(error.message || '批量删除失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

// 选择变化
const handleSelectionChange = (selection: Rule[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 匹配类型颜色
const getMatchTypeColor = (type: MatchType): string => {
  const colors: Record<MatchType, string> = {
    [MatchType.CONTAINS]: '',
    [MatchType.REGEX]: 'warning',
    [MatchType.PREFIX]: 'success',
    [MatchType.SUFFIX]: 'info',
    [MatchType.EXACT]: 'danger'
  }
  return colors[type] || ''
}

// 优先级颜色
const getPriorityColor = (priority: number): string => {
  if (priority >= 75) return 'danger'
  if (priority >= 50) return 'warning'
  if (priority >= 25) return ''
  return 'info'
}

// 格式化日期时间
const formatDateTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Check if any policy is enabled
const hasPolicyEnabled = (policy: PolicyDTO | undefined): boolean => {
  if (!policy) return false
  return !!(
    policy.rateLimitEnabled ||
    policy.timeWindowEnabled ||
    policy.roleEnabled ||
    policy.cooldownEnabled ||
    (policy.whitelist && policy.whitelist.length > 0) ||
    (policy.blacklist && policy.blacklist.length > 0)
  )
}

// 组件挂载时加载数据
onMounted(() => {
  loadRules()
})
</script>

<style scoped lang="scss">
.rule-management {
  .search-card {
    :deep(.el-card__body) {
      padding: 16px;
    }

    .el-form {
      margin-bottom: 0;
    }
  }

  .policy-summary {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
}
</style>
