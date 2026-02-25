<template>
  <el-dialog
    v-model="visible"
    title="群组规则配置"
    width="700px"
    :close-on-click-modal="false"
  >
    <div v-if="loading" class="loading-container" v-loading="loading">
      <el-icon size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="error" class="error-container">
      <el-icon size="48" color="#F56C6C"><CircleClose /></el-icon>
      <p>{{ error }}</p>
    </div>

    <div v-else>
      <!-- 当前群组信息 -->
      <el-descriptions :column="2" border size="small" class="group-info">
        <el-descriptions-item label="群组名称">{{ group?.groupName }}</el-descriptions-item>
        <el-descriptions-item label="群组ID">{{ group?.groupId }}</el-descriptions-item>
      </el-descriptions>

      <!-- 已绑定规则列表 -->
      <div class="section-header">
        <h4>已绑定规则 ({{ boundRules.length }})</h4>
        <el-button type="primary" size="small" @click="showAddRule = true">
          <el-icon><Plus /></el-icon>添加规则
        </el-button>
      </div>

      <el-table :data="boundRulesWithDetails" stripe v-loading="saving" size="small">
        <el-table-column prop="ruleName" label="规则名称" min-width="120" />
        <el-table-column prop="matchType" label="匹配类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getMatchTypeText(row.matchType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pattern" label="匹配内容" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="(val) => toggleRule(row.ruleId, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="removeRule(row.ruleId)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="boundRulesWithDetails.length === 0" description="暂无绑定的规则" />
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 添加规则对话框 -->
  <el-dialog
    v-model="showAddRule"
    title="添加规则"
    width="500px"
    append-to-body
  >
    <el-select
      v-model="selectedRuleId"
      placeholder="请选择要添加的规则"
      style="width: 100%"
      filterable
    >
      <el-option
        v-for="rule in availableRules"
        :key="rule.id"
        :label="rule.name"
        :value="rule.id"
      >
        <div class="rule-option">
          <span>{{ rule.name }}</span>
          <el-tag size="small">{{ getMatchTypeText(rule.matchType) }}</el-tag>
        </div>
      </el-option>
    </el-select>

    <template #footer>
      <el-button @click="showAddRule = false">取消</el-button>
      <el-button type="primary" @click="addRule" :disabled="!selectedRuleId">添加</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CircleClose, Loading } from '@element-plus/icons-vue'
import * as groupApi from '@/api/modules/group.api'
import { listRules } from '@/api/modules/rule.api'
import type { GroupChat, GroupRuleConfig } from '@/types/group'
import type { Rule } from '@/types/rule'

const props = defineProps<{
  modelValue: boolean
  group: GroupChat | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'rulesChanged': []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const boundRules = ref<GroupRuleConfig[]>([])
const allRules = ref<Rule[]>([])
const showAddRule = ref(false)
const selectedRuleId = ref<number>()

// 计算可添加的规则（排除已绑定的）
const availableRules = computed(() => {
  const boundIds = boundRules.value.map(r => r.ruleId)
  return allRules.value.filter(r => !boundIds.includes(r.id))
})

// 绑定规则详情（合并 GroupRuleConfig 和 Rule 信息）
const boundRulesWithDetails = computed(() => {
  return boundRules.value.map(boundRule => {
    const ruleDetail = allRules.value.find(r => r.id === boundRule.ruleId)
    return {
      ...boundRule,
      ruleName: ruleDetail?.name || `规则 #${boundRule.ruleId}`,
      matchType: ruleDetail?.matchType || '',
      pattern: ruleDetail?.pattern || ''
    }
  })
})

// 监听对话框显示
watch(() => props.modelValue, async (val) => {
  if (val && props.group) {
    loading.value = true
    await Promise.all([loadRules(), loadAllRules()])
    loading.value = false
  }
})

// 监听 group 变化
watch(() => props.group, () => {
  boundRules.value = []
  allRules.value = []
})

// 加载群聊规则
const loadRules = async () => {
  if (!props.group) return
  error.value = ''
  try {
    const response = await groupApi.getGroupRules(props.group.id)
    boundRules.value = response.data || []
  } catch (e: any) {
    error.value = e.message || '加载规则失败'
    ElMessage.error(error.value)
  }
}

// 加载所有可用规则
const loadAllRules = async () => {
  try {
    const response = await listRules({ page: 1, size: 1000 })
    allRules.value = response.data.records || []
  } catch (e: any) {
    console.error('加载规则列表失败', e)
    if (!error.value) {
      error.value = e.message || '加载规则列表失败'
    }
  }
}

// 添加规则
const addRule = async () => {
  if (!props.group || !selectedRuleId.value) return
  saving.value = true
  try {
    await groupApi.addRuleToGroup(props.group.id, selectedRuleId.value)
    ElMessage.success('规则添加成功')
    showAddRule.value = false
    selectedRuleId.value = undefined
    await loadRules()
    emit('rulesChanged')
  } catch (e: any) {
    ElMessage.error(e.message || '添加规则失败')
  } finally {
    saving.value = false
  }
}

// 移除规则
const removeRule = async (ruleId: number) => {
  if (!props.group) return
  try {
    await ElMessageBox.confirm('确定要移除该规则吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    saving.value = true
    await groupApi.removeRuleFromGroup(props.group.id, ruleId)
    ElMessage.success('规则已移除')
    await loadRules()
    emit('rulesChanged')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '移除规则失败')
    }
  } finally {
    saving.value = false
  }
}

// 切换规则状态
const toggleRule = async (ruleId: number, enabled: boolean) => {
  if (!props.group) return
  try {
    await groupApi.toggleGroupRuleStatus(props.group.id, ruleId, enabled)
    ElMessage.success(enabled ? '规则已启用' : '规则已禁用')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
    // 恢复原状态
    await loadRules()
  }
}

// 获取匹配类型文本
const getMatchTypeText = (type: string): string => {
  const map: Record<string, string> = {
    'KEYWORD': '关键词',
    'REGEX': '正则',
    'EXACT': '精确',
    'PREFIX': '前缀',
    'SUFFIX': '后缀',
    'CONTAINS': '包含'
  }
  return map[type] || type
}
</script>

<style scoped lang="scss">
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 16px;
}

.group-info {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h4 {
    margin: 0;
    font-size: 14px;
    color: #606266;
  }
}

.rule-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
