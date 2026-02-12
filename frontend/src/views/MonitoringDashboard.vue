<template>
  <div class="monitoring-dashboard">
    <!-- Statistics Cards -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic title="总消息数" :value="systemStats.totalMessages">
            <template #prefix>
              <el-icon><ChatDotRound /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic title="处理成功率" :value="systemStats.successRate" suffix="%">
            <template #prefix>
              <el-icon><CircleCheck /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic
            title="平均处理时间"
            :value="metrics.avgHandlerExecutionTime"
            suffix="ms"
          >
            <template #prefix>
              <el-icon><Timer /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic title="规则匹配率" :value="metrics.ruleMatchRate" suffix="%">
            <template #prefix>
              <el-icon><Histogram /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card" style="margin-top: 16px">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="群组">
          <el-input
            v-model="filterForm.groupId"
            placeholder="群组ID"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="规则">
          <el-input
            v-model="filterForm.ruleId"
            placeholder="规则ID"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="处理器">
          <el-input
            v-model="filterForm.handlerType"
            placeholder="处理器类型"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="filterForm.success"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option label="成功" :value="true" />
            <el-option label="失败" :value="false" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-select
            v-model="filterForm.timeRange"
            placeholder="选择时间范围"
            style="width: 150px"
            @change="handleTimeRangeChange"
          >
            <el-option
              v-for="preset in TIME_RANGE_PRESETS"
              :key="preset.value"
              :label="preset.label"
              :value="preset.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Execution Logs Table -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>执行日志</span>
          <el-button
            type="primary"
            size="small"
            :icon="Refresh"
            :loading="loading"
            @click="loadLogs"
          >
            刷新
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="logs"
        stripe
        border
      >
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column prop="timestamp" label="时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.timestamp) }}
          </template>
        </el-table-column>

        <el-table-column prop="groupId" label="群组ID" width="120" />

        <el-table-column prop="userId" label="用户ID" width="120" />

        <el-table-column prop="userNickname" label="昵称" width="120" show-overflow-tooltip />

        <el-table-column prop="messageContent" label="消息内容" min-width="200" show-overflow-tooltip />

        <el-table-column prop="matchedRuleId" label="匹配规则" width="100" />

        <el-table-column prop="processingTimeMs" label="处理时间" width="100">
          <template #default="{ row }">
            <el-tag :type="getProcessingTimeColor(row.processingTimeMs)" size="small">
              {{ row.processingTimeMs }}ms
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="sendStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="SendStatusColors[row.sendStatus]" size="small">
              {{ SendStatusLabels[row.sendStatus] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :icon="View"
              @click="handleViewDetails(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadLogs"
        @current-change="loadLogs"
      />
    </el-card>

    <!-- Trend Charts -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>消息趋势</span>
          </template>
          <div ref="messagesTrendChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>成功率趋势</span>
          </template>
          <div ref="successRateTrendChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Log Details Dialog -->
    <el-dialog
      v-model="detailsDialogVisible"
      title="日志详情"
      width="700px"
    >
      <el-descriptions v-if="selectedLog" :column="2" border>
        <el-descriptions-item label="ID">
          {{ selectedLog.id }}
        </el-descriptions-item>
        <el-descriptions-item label="消息ID">
          {{ selectedLog.messageId }}
        </el-descriptions-item>
        <el-descriptions-item label="群组ID">
          {{ selectedLog.groupId }}
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">
          {{ selectedLog.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="用户昵称">
          {{ selectedLog.userNickname || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="匹配规则">
          {{ selectedLog.matchedRuleId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间">
          {{ selectedLog.processingTimeMs }}ms
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="SendStatusColors[selectedLog.sendStatus]" size="small">
            {{ SendStatusLabels[selectedLog.sendStatus] }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="时间戳" :span="2">
          {{ formatDateTime(selectedLog.timestamp) }}
        </el-descriptions-item>
        <el-descriptions-item label="消息内容" :span="2">
          {{ selectedLog.messageContent }}
        </el-descriptions-item>
        <el-descriptions-item label="回复内容" :span="2">
          {{ selectedLog.responseContent || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedLog.errorMessage" label="错误信息" :span="2">
          <el-text type="danger">
            {{ selectedLog.errorMessage }}
          </el-text>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  Download,
  View,
  ChatDotRound,
  CircleCheck,
  Timer,
  Histogram
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getExecutionLogs,
  getSystemStats,
  getMetrics,
  getTrends,
  exportLogs
} from '@/api/monitoring'
import type {
  ExecutionLog,
  ExecutionLogQuery,
  SystemStats,
  MetricsSummary,
  LogFilterModel
} from '@/types/monitoring'
import {
  SendStatus,
  SendStatusLabels,
  SendStatusColors,
  TIME_RANGE_PRESETS,
  MetricType
} from '@/types/monitoring'

/**
 * T085: MonitoringDashboard View
 * Execution logs table, statistics cards, trend charts
 */

// Filter form
const filterForm = reactive<LogFilterModel>({
  groupId: '',
  ruleId: undefined,
  handlerType: '',
  success: undefined,
  timeRange: 'last_24h'
})

// Pagination
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// Logs
const logs = ref<ExecutionLog[]>([])
const loading = ref(false)

// Statistics
const systemStats = ref<SystemStats>({
  totalMessages: 0,
  processedMessages: 0,
  unmatchedMessages: 0,
  successCount: 0,
  failureCount: 0,
  processingRate: 0,
  successRate: 0
})

const metrics = ref<MetricsSummary>({
  ruleMatchRate: 0,
  policyPassRate: 0,
  handlerSuccessRate: 0,
  avgHandlerExecutionTime: 0,
  avgMessageRoutingTime: 0
})

// Details dialog
const detailsDialogVisible = ref(false)
const selectedLog = ref<ExecutionLog | null>(null)

// Chart refs
const messagesTrendChart = ref<HTMLElement>()
const successRateTrendChart = ref<HTMLElement>()

// Load execution logs
const loadLogs = async () => {
  loading.value = true
  try {
    const query: ExecutionLogQuery = {
      pageNum: pagination.page,
      pageSize: pagination.size,
      groupId: filterForm.groupId || undefined,
      ruleId: filterForm.ruleId,
      handlerType: filterForm.handlerType || undefined,
      success: filterForm.success,
      startTime: filterForm.startTime,
      endTime: filterForm.endTime
    }

    const response = await getExecutionLogs(query)
    logs.value = response.data.records || []
    pagination.total = response.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载日志失败')
  } finally {
    loading.value = false
  }
}

// Load system statistics
const loadSystemStats = async () => {
  try {
    const response = await getSystemStats({
      startTime: filterForm.startTime,
      endTime: filterForm.endTime
    })
    systemStats.value = response.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载统计数据失败')
  }
}

// Load metrics
const loadMetrics = async () => {
  try {
    const response = await getMetrics()
    metrics.value = response.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载指标数据失败')
  }
}

// Load trend data and render charts
const loadTrends = async () => {
  try {
    // Load messages trend
    const messagesTrendResponse = await getTrends({
      metric: MetricType.MESSAGES,
      startTime: filterForm.startTime,
      endTime: filterForm.endTime,
      interval: 3600 // 1 hour
    })

    // Load success rate trend
    const successRateTrendResponse = await getTrends({
      metric: MetricType.SUCCESS_RATE,
      startTime: filterForm.startTime,
      endTime: filterForm.endTime,
      interval: 3600
    })

    // Render charts
    await nextTick()
    renderMessagesTrendChart(messagesTrendResponse.data)
    renderSuccessRateTrendChart(successRateTrendResponse.data)
  } catch (error: any) {
    ElMessage.error(error.message || '加载趋势数据失败')
  }
}

// Render messages trend chart
const renderMessagesTrendChart = (trendData: any) => {
  if (!messagesTrendChart.value) return

  const chart = echarts.init(messagesTrendChart.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: trendData.dataPoints.map((p: any) => p.label || p.timestamp)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '消息数量',
        type: 'line',
        data: trendData.dataPoints.map((p: any) => p.value),
        smooth: true,
        areaStyle: {}
      }
    ]
  }
  chart.setOption(option)
}

// Render success rate trend chart
const renderSuccessRateTrendChart = (trendData: any) => {
  if (!successRateTrendChart.value) return

  const chart = echarts.init(successRateTrendChart.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c}%'
    },
    xAxis: {
      type: 'category',
      data: trendData.dataPoints.map((p: any) => p.label || p.timestamp)
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [
      {
        name: '成功率',
        type: 'line',
        data: trendData.dataPoints.map((p: any) => p.value),
        smooth: true,
        itemStyle: {
          color: '#67C23A'
        }
      }
    ]
  }
  chart.setOption(option)
}

// Handle time range change
const handleTimeRangeChange = (value: string) => {
  const preset = TIME_RANGE_PRESETS.find(p => p.value === value)
  if (preset) {
    filterForm.startTime = preset.startTime()
    filterForm.endTime = preset.endTime()
  }
}

// Handle search
const handleSearch = () => {
  pagination.page = 1
  loadLogs()
  loadSystemStats()
  loadTrends()
}

// Handle reset
const handleReset = () => {
  filterForm.groupId = ''
  filterForm.ruleId = undefined
  filterForm.handlerType = ''
  filterForm.success = undefined
  filterForm.timeRange = 'last_24h'
  handleTimeRangeChange('last_24h')
  handleSearch()
}

// Handle export
const handleExport = async () => {
  try {
    const query: ExecutionLogQuery = {
      groupId: filterForm.groupId || undefined,
      ruleId: filterForm.ruleId,
      handlerType: filterForm.handlerType || undefined,
      success: filterForm.success,
      startTime: filterForm.startTime,
      endTime: filterForm.endTime
    }

    const response = await exportLogs(query)

    // Create download link
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `execution-logs-${Date.now()}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败')
  }
}

// View log details
const handleViewDetails = (log: ExecutionLog) => {
  selectedLog.value = log
  detailsDialogVisible.value = true
}

// Get processing time color
const getProcessingTimeColor = (time: number): string => {
  if (time < 100) return 'success'
  if (time < 500) return 'warning'
  return 'danger'
}

// Format date time
const formatDateTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// Initialize
onMounted(() => {
  // Set default time range
  handleTimeRangeChange('last_24h')

  // Load data
  loadLogs()
  loadSystemStats()
  loadMetrics()
  loadTrends()

  // Auto refresh every 30 seconds
  setInterval(() => {
    loadMetrics()
  }, 30000)
})
</script>

<style scoped lang="scss">
.monitoring-dashboard {
  .stats-cards {
    .el-card {
      :deep(.el-statistic__head) {
        font-size: 14px;
        color: var(--el-text-color-secondary);
      }

      :deep(.el-statistic__content) {
        font-size: 24px;
        font-weight: 600;
      }
    }
  }

  .filter-card {
    :deep(.el-card__body) {
      padding: 16px;
    }

    .el-form {
      margin-bottom: 0;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
