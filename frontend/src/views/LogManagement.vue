<template>
  <div class="log-management">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="群聊">
          <GroupSelector
            v-model="searchForm.groupId"
            placeholder="全部群聊"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="用户ID">
          <el-input
            v-model="searchForm.userId"
            placeholder="用户ID"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="发送状态">
          <el-select
            v-model="searchForm.sendStatus"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="(label, value) in SendStatusLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button :icon="Download" @click="handleExport">导出CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志列表 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-table v-loading="loading" :data="logs" stripe>
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column prop="groupId" label="群聊ID" width="100" />

        <el-table-column prop="userId" label="用户ID" width="120" />

        <el-table-column prop="userNickname" label="用户昵称" width="120" />

        <el-table-column prop="messageContent" label="消息内容" min-width="200" show-overflow-tooltip />

        <el-table-column prop="responseContent" label="回复内容" min-width="200" show-overflow-tooltip />

        <el-table-column prop="processingTimeMs" label="处理时间" width="100">
          <template #default="{ row }">
            {{ row.processingTimeMs ? `${row.processingTimeMs}ms` : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="sendStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="SendStatusColors[row.sendStatus]">
              {{ SendStatusLabels[row.sendStatus] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="timestamp" label="时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.timestamp) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.sendStatus === 'FAILED'"
              link
              type="warning"
              size="small"
              @click="handleRetry(row)"
            >
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="600px"
    >
      <el-descriptions v-if="currentLog" :column="1" border>
        <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="消息ID">{{ currentLog.messageId }}</el-descriptions-item>
        <el-descriptions-item label="群聊ID">{{ currentLog.groupId }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentLog.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户昵称">{{ currentLog.userNickname }}</el-descriptions-item>
        <el-descriptions-item label="消息内容">{{ currentLog.messageContent }}</el-descriptions-item>
        <el-descriptions-item label="匹配规则ID">{{ currentLog.matchedRuleId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回复内容">{{ currentLog.responseContent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">
          {{ currentLog.processingTimeMs ? `${currentLog.processingTimeMs}ms` : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="发送状态">
          <el-tag :type="SendStatusColors[currentLog.sendStatus]">
            {{ SendStatusLabels[currentLog.sendStatus] }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ currentLog.errorMessage || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间戳">{{ formatDateTime(currentLog.timestamp) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import GroupSelector from '@/components/GroupSelector.vue'
import { listLogs, exportLogs, retryFailedMessage } from '@/api/modules/log.api'
import { SendStatus, SendStatusLabels, SendStatusColors } from '@/types/log'
import type { MessageLog } from '@/types/log'

const searchForm = reactive({
  groupId: null as number | null,
  userId: '',
  sendStatus: undefined as SendStatus | undefined,
  startTime: '',
  endTime: ''
})

const dateRange = ref<[Date, Date] | null>(null)

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const logs = ref<MessageLog[]>([])
const loading = ref(false)

// 日志详情
const detailVisible = ref(false)
const currentLog = ref<MessageLog | null>(null)

const loadLogs = async () => {
  loading.value = true
  try {
    const response = await listLogs({
      page: pagination.page,
      size: pagination.size,
      groupId: searchForm.groupId || undefined,
      userId: searchForm.userId || undefined,
      sendStatus: searchForm.sendStatus,
      startTime: searchForm.startTime || undefined,
      endTime: searchForm.endTime || undefined
    })
    logs.value = response.data.records
    pagination.total = response.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载日志列表失败')
  } finally {
    loading.value = false
  }
}

const handleDateChange = (value: [Date, Date] | null) => {
  if (value) {
    searchForm.startTime = value[0].toISOString()
    searchForm.endTime = value[1].toISOString()
  } else {
    searchForm.startTime = ''
    searchForm.endTime = ''
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadLogs()
}

const handleReset = () => {
  searchForm.groupId = null
  searchForm.userId = ''
  searchForm.sendStatus = undefined
  searchForm.startTime = ''
  searchForm.endTime = ''
  dateRange.value = null
  handleSearch()
}

const handleExport = async () => {
  try {
    await exportLogs({
      groupId: searchForm.groupId || undefined,
      userId: searchForm.userId || undefined,
      sendStatus: searchForm.sendStatus,
      startTime: searchForm.startTime || undefined,
      endTime: searchForm.endTime || undefined
    })
    ElMessage.success('导出成功')
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败')
  }
}

const handleViewDetail = (log: MessageLog) => {
  currentLog.value = log
  detailVisible.value = true
}

const handleRetry = (log: MessageLog) => {
  ElMessageBox.confirm(
    '确定要重试发送此消息吗？',
    '重试确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await retryFailedMessage(log.id)
      ElMessage.success('重试请求已提交')
      loadLogs()
    } catch (error: any) {
      ElMessage.error(error.message || '重试失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

const formatDateTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped lang="scss">
.search-card {
  :deep(.el-card__body) {
    padding: 16px;
  }

  .el-form {
    margin-bottom: 0;
  }
}
</style>
