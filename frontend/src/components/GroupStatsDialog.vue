<template>
  <el-dialog
    v-model="visible"
    title="群组统计信息"
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

    <div v-else-if="stats" class="stats-content">
      <!-- 当前群组信息 -->
      <el-descriptions :column="2" border size="small" class="group-info">
        <el-descriptions-item label="群组名称">{{ group?.groupName }}</el-descriptions-item>
        <el-descriptions-item label="群组ID">{{ group?.groupId }}</el-descriptions-item>
      </el-descriptions>

      <!-- 统计时间范围 -->
      <div class="time-range-section">
        <span class="time-label">统计时间范围：</span>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          :shortcuts="dateShortcuts"
          @change="handleDateChange"
        />
      </div>

      <!-- 统计概览卡片 -->
      <el-row :gutter="16" class="stats-overview">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.totalMessages }}</div>
            <div class="stat-label">总消息数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card success">
            <div class="stat-value">{{ stats.successReplies }}</div>
            <div class="stat-label">成功回复</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card
            shadow="hover"
            class="stat-card danger clickable"
            :class="{ disabled: stats.failedReplies === 0 }"
            @click="handleShowFailedDetails"
          >
            <div class="stat-value">{{ stats.failedReplies }}</div>
            <div class="stat-label">
              失败回复
              <el-icon v-if="stats.failedReplies > 0" size="12"><View /></el-icon>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card info">
            <div class="stat-value">{{ stats.skippedReplies }}</div>
            <div class="stat-label">跳过回复</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 成功率进度条 -->
      <div class="success-rate-section">
        <div class="rate-header">
          <span>回复成功率</span>
          <span class="rate-value">{{ successRate.toFixed(1) }}%</span>
        </div>
        <el-progress
          :percentage="successRate"
          :status="successRate >= 95 ? 'success' : successRate >= 80 ? 'warning' : 'exception'"
          :stroke-width="20"
        />
      </div>

      <!-- 详细数据表格 -->
      <el-table :data="tableData" stripe size="small" class="detail-table">
        <el-table-column prop="metric" label="指标" width="150" />
        <el-table-column prop="value" label="数值" width="120" align="center" />
        <el-table-column prop="percentage" label="占比" align="center">
          <template #default="{ row }">
            <el-progress
              v-if="row.percentage !== undefined"
              :percentage="row.percentage"
              :color="row.color"
              :stroke-width="8"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="refreshStats">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
    </template>
  </el-dialog>

  <!-- 失败详情抽屉 -->
  <el-drawer
    v-model="showFailedDrawer"
    title="失败消息详情"
    size="800px"
    :destroy-on-close="true"
  >
    <!-- 筛选条件 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="filterForm" inline size="small">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="用户">
          <el-input
            v-model="filterForm.userKeyword"
            placeholder="用户昵称/ID"
            clearable
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input
            v-model="filterForm.messageKeyword"
            placeholder="关键词搜索"
            clearable
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="错误类型">
          <el-select
            v-model="filterForm.errorType"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="无匹配规则" value="No matching rule" />
            <el-option label="路由超时" value="routing timeout" />
            <el-option label="发送超时" value="TimeoutException" />
            <el-option label="其他错误" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleResetFilter">
            <el-icon><RefreshRight /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-table
      v-loading="failedLogsLoading"
      :data="failedLogs"
      stripe
      size="small"
      style="width: 100%; margin-top: 16px"
    >
      <el-table-column prop="timestamp" label="时间" width="150">
        <template #default="{ row }">
          {{ formatTime(row.timestamp) }}
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="用户ID" width="100" show-overflow-tooltip />
      <el-table-column prop="userNickname" label="用户昵称" width="100" show-overflow-tooltip />
      <el-table-column prop="messageContent" label="消息内容" min-width="180" show-overflow-tooltip />
      <el-table-column prop="errorMessage" label="失败原因" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="getErrorType(row.errorMessage)" :type="getErrorType(row.errorMessage)?.type" size="small">
            {{ getErrorType(row.errorMessage)?.label }}
          </el-tag>
          <span class="error-text">{{ row.errorMessage || '未知错误' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="processingTimeMs" label="处理耗时" width="90" align="center">
        <template #default="{ row }">
          <span :class="{ 'timeout-warning': row.processingTimeMs > 5000 }">
            {{ row.processingTimeMs }}ms
          </span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="failedLogsPage"
      v-model:page-size="failedLogsSize"
      :total="failedLogsTotal"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px; justify-content: flex-end"
      @size-change="handleFailedLogsSizeChange"
      @current-change="handleFailedLogsPageChange"
    />

    <template #footer>
      <el-button @click="showFailedDrawer = false">关闭</el-button>
      <el-button type="primary" @click="handleExportFailedLogs">
        <el-icon><Download /></el-icon>导出当前筛选结果
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleClose, Refresh, Loading, View, Search, RefreshRight, Download } from '@element-plus/icons-vue'
import { reactive } from 'vue'
import { getGroupStats } from '@/api/modules/group.api'
import { listLogs } from '@/api/modules/log.api'
import type { GroupChat, GroupStats } from '@/types/group'
import type { MessageLog } from '@/types/log'
import { SendStatus } from '@/types/log'

const props = defineProps<{
  modelValue: boolean
  group: GroupChat | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const error = ref('')
const stats = ref<GroupStats | null>(null)
const dateRange = ref<[Date, Date] | null>(null)

// 失败详情相关
const showFailedDrawer = ref(false)
const failedLogsLoading = ref(false)
const failedLogs = ref<MessageLog[]>([])
const failedLogsTotal = ref(0)
const failedLogsPage = ref(1)
const failedLogsSize = ref(10)

// 筛选表单
const filterForm = reactive({
  dateRange: null as [Date, Date] | null,
  userKeyword: '',
  messageKeyword: '',
  errorType: ''
})

// 日期快捷选项
const dateShortcuts = [
  {
    text: '今天',
    value: () => {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const tomorrow = new Date(today)
      tomorrow.setDate(tomorrow.getDate() + 1)
      return [today, tomorrow]
    }
  },
  {
    text: '昨天',
    value: () => {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const yesterday = new Date(today)
      yesterday.setDate(yesterday.getDate() - 1)
      return [yesterday, today]
    }
  },
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      start.setHours(0, 0, 0, 0)
      return [start, end]
    }
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 30)
      start.setHours(0, 0, 0, 0)
      return [start, end]
    }
  }
]

// 计算成功率
const successRate = computed(() => {
  if (!stats.value) return 0
  const total = stats.value.successReplies + stats.value.failedReplies
  if (total === 0) return 0
  return (stats.value.successReplies / total) * 100
})

// 表格数据
const tableData = computed(() => {
  if (!stats.value) return []

  const total = stats.value.totalMessages || 1
  const replyTotal = stats.value.successReplies + stats.value.failedReplies + stats.value.skippedReplies || 1

  return [
    {
      metric: '总消息数',
      value: stats.value.totalMessages,
      percentage: 100,
      color: '#409EFF'
    },
    {
      metric: '成功回复',
      value: stats.value.successReplies,
      percentage: Math.round((stats.value.successReplies / replyTotal) * 100),
      color: '#67C23A'
    },
    {
      metric: '失败回复',
      value: stats.value.failedReplies,
      percentage: Math.round((stats.value.failedReplies / replyTotal) * 100),
      color: '#F56C6C'
    },
    {
      metric: '跳过回复',
      value: stats.value.skippedReplies,
      percentage: Math.round((stats.value.skippedReplies / replyTotal) * 100),
      color: '#909399'
    }
  ]
})

// 监听对话框显示
watch(() => props.modelValue, (val) => {
  if (val && props.group) {
    // 默认查询最近7天（从7天前的0点开始）
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - 7)
    start.setHours(0, 0, 0, 0)
    dateRange.value = [start, end]
    loadStats()
  }
})

// 加载统计数据
const loadStats = async () => {
  if (!props.group) return

  loading.value = true
  error.value = ''

  try {
    const startTime = dateRange.value?.[0] ? toLocalISOString(dateRange.value[0]) : undefined
    const endTime = dateRange.value?.[1] ? toLocalISOString(dateRange.value[1]) : undefined

    const response = await getGroupStats(props.group.id, startTime, endTime)
    stats.value = response.data
  } catch (e: any) {
    error.value = e.message || '加载统计数据失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

// 日期改变
const handleDateChange = () => {
  if (dateRange.value) {
    loadStats()
  }
}

// 刷新统计
const refreshStats = () => {
  loadStats()
  ElMessage.success('统计数据已刷新')
}

// 显示失败详情
const handleShowFailedDetails = () => {
  if (!stats.value || stats.value.failedReplies === 0) return
  // 初始化筛选条件的日期范围
  if (dateRange.value) {
    filterForm.dateRange = [new Date(dateRange.value[0]), new Date(dateRange.value[1])]
  }
  showFailedDrawer.value = true
  loadFailedLogs()
}

// 加载失败日志
const loadFailedLogs = async () => {
  if (!props.group) return

  failedLogsLoading.value = true
  try {
    // 构建查询参数
    const params: any = {
      page: failedLogsPage.value,
      size: failedLogsSize.value,
      groupId: props.group.id,
      sendStatus: SendStatus.FAILED
    }

    // 使用筛选条件的日期范围（如果有），否则使用主统计的日期范围
    if (filterForm.dateRange && filterForm.dateRange[0] && filterForm.dateRange[1]) {
      params.startTime = toLocalISOString(filterForm.dateRange[0])
      params.endTime = toLocalISOString(filterForm.dateRange[1])
    } else if (dateRange.value) {
      params.startTime = toLocalISOString(dateRange.value[0])
      params.endTime = toLocalISOString(dateRange.value[1])
    }

    // 添加用户筛选
    if (filterForm.userKeyword.trim()) {
      params.userKeyword = filterForm.userKeyword.trim()
    }

    // 添加消息内容筛选
    if (filterForm.messageKeyword.trim()) {
      params.messageKeyword = filterForm.messageKeyword.trim()
    }

    // 添加错误类型筛选
    if (filterForm.errorType) {
      params.errorType = filterForm.errorType
    }

    const response = await listLogs(params)
    failedLogs.value = response.data.records
    failedLogsTotal.value = response.data.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败日志失败')
  } finally {
    failedLogsLoading.value = false
  }
}

// 失败日志分页变化
const handleFailedLogsPageChange = (page: number) => {
  failedLogsPage.value = page
  loadFailedLogs()
}

// 失败日志每页条数变化
const handleFailedLogsSizeChange = (size: number) => {
  failedLogsSize.value = size
  failedLogsPage.value = 1
  loadFailedLogs()
}

// 筛选查询
const handleSearch = () => {
  failedLogsPage.value = 1
  loadFailedLogs()
}

// 重置筛选条件
const handleResetFilter = () => {
  filterForm.dateRange = dateRange.value ? [new Date(dateRange.value[0]), new Date(dateRange.value[1])] : null
  filterForm.userKeyword = ''
  filterForm.messageKeyword = ''
  filterForm.errorType = ''
  failedLogsPage.value = 1
  loadFailedLogs()
}

// 导出失败日志
const handleExportFailedLogs = () => {
  // 构建导出参数
  const params: any = {
    groupId: props.group?.id,
    sendStatus: SendStatus.FAILED
  }

  if (filterForm.dateRange && filterForm.dateRange[0] && filterForm.dateRange[1]) {
    params.startTime = toLocalISOString(filterForm.dateRange[0])
    params.endTime = toLocalISOString(filterForm.dateRange[1])
  }

  if (filterForm.userKeyword.trim()) {
    params.userKeyword = filterForm.userKeyword.trim()
  }

  if (filterForm.messageKeyword.trim()) {
    params.messageKeyword = filterForm.messageKeyword.trim()
  }

  if (filterForm.errorType) {
    params.errorType = filterForm.errorType
  }

  // TODO: 调用后端导出API
  ElMessage.info('导出功能开发中...')
  console.log('导出参数:', params)
}

// 获取错误类型
const getErrorType = (errorMessage: string | null) => {
  if (!errorMessage) return null

  const errorPatterns = [
    {
      pattern: /no matching rule|无匹配规则/i,
      label: '无匹配规则',
      type: 'info' as const
    },
    {
      pattern: /routing timeout|路由超时/i,
      label: '路由超时',
      type: 'warning' as const
    },
    {
      pattern: /timeout|超时/i,
      label: '发送超时',
      type: 'warning' as const
    },
    {
      pattern: /connection|connect|连接/i,
      label: '连接错误',
      type: 'danger' as const
    },
    {
      pattern: /permission|权限|forbidden/i,
      label: '权限错误',
      type: 'danger' as const
    },
    {
      pattern: /rate limit|限流|too many/i,
      label: '限流错误',
      type: 'warning' as const
    }
  ]

  for (const item of errorPatterns) {
    if (item.pattern.test(errorMessage)) {
      return { label: item.label, type: item.type }
    }
  }

  // 默认返回其他错误
  return { label: '其他错误', type: 'info' as const }
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN')
}

// 将 Date 转换为本地时区的 ISO 格式（避免 toISOString 转为 UTC 导致日期回退）
const toLocalISOString = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
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

.time-range-section {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;

  .time-label {
    color: #606266;
    font-size: 14px;
  }
}

.stats-overview {
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;

  :deep(.el-card__body) {
    padding: 16px 8px;
  }

  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #409EFF;
    margin-bottom: 4px;
  }

  .stat-label {
    font-size: 12px;
    color: #606266;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }

  &.success .stat-value {
    color: #67C23A;
  }

  &.danger .stat-value {
    color: #F56C6C;
  }

  &.info .stat-value {
    color: #909399;
  }

  &.clickable {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    &.disabled {
      cursor: not-allowed;
      opacity: 0.7;

      &:hover {
        transform: none;
        box-shadow: none;
      }
    }
  }
}

.error-text {
  color: #F56C6C;
}

.success-rate-section {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;

  .rate-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-size: 14px;
    color: #606266;

    .rate-value {
      font-size: 18px;
      font-weight: bold;
      color: #409EFF;
    }
  }
}

.detail-table {
  margin-top: 16px;
}
</style>
