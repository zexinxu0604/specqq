<template>
  <el-card class="sync-status-card">
    <template #header>
      <div class="card-header">
        <span>群组同步状态</span>
        <el-button
          type="primary"
          size="small"
          :loading="syncStore.syncInProgress"
          :disabled="syncStore.syncInProgress"
          @click="handleSyncNow"
        >
          <el-icon><Refresh /></el-icon>
          {{ syncStore.syncInProgress ? '同步中...' : '立即同步' }}
        </el-button>
      </div>
    </template>

    <el-descriptions :column="2" border>
      <el-descriptions-item label="最后同步时间">
        <span v-if="syncStore.lastSyncTime">
          {{ formatTime(syncStore.lastSyncTime) }}
        </span>
        <span v-else class="text-muted">从未同步</span>
      </el-descriptions-item>

      <el-descriptions-item label="成功率">
        <el-tag
          v-if="syncStore.lastSyncResult"
          :type="getSuccessRateType(syncStore.lastSyncSuccessRate)"
        >
          {{ syncStore.lastSyncSuccessRate.toFixed(1) }}%
        </el-tag>
        <span v-else class="text-muted">-</span>
      </el-descriptions-item>

      <el-descriptions-item label="同步耗时">
        <span v-if="syncStore.lastSyncResult">
          {{ syncStore.lastSyncResult.durationMs }}ms
        </span>
        <span v-else class="text-muted">-</span>
      </el-descriptions-item>

      <el-descriptions-item label="告警群组">
        <el-badge
          :value="syncStore.alertGroups.length"
          :type="syncStore.hasAlerts ? 'danger' : 'success'"
        >
          <el-button
            size="small"
            text
            @click="showAlertDialog = true"
            :disabled="!syncStore.hasAlerts"
          >
            查看详情
          </el-button>
        </el-badge>
      </el-descriptions-item>
    </el-descriptions>

    <div v-if="syncStore.lastSyncResult" class="sync-details">
      <el-divider />
      <el-row :gutter="16">
        <el-col :span="8">
          <el-statistic title="总计" :value="syncStore.lastSyncResult.totalCount" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="成功" :value="syncStore.lastSyncResult.successCount" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="失败" :value="syncStore.lastSyncResult.failureCount" />
        </el-col>
      </el-row>
    </div>

    <!-- Alert Groups Dialog -->
    <el-dialog
      v-model="showAlertDialog"
      title="需要告警的群组"
      width="600px"
    >
      <el-table :data="syncStore.alertGroups" stripe>
        <el-table-column prop="groupName" label="群组名称" />
        <el-table-column prop="consecutiveFailureCount" label="连续失败次数" width="120" />
        <el-table-column prop="failureReason" label="失败原因" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              link
              @click="handleResetFailure(row.groupId)"
            >
              重置
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="showAlertDialog = false">关闭</el-button>
        <el-button
          type="warning"
          :loading="retrying"
          @click="handleRetryFailed"
        >
          重试所有失败群组
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useGroupSyncStore } from '@/stores/groupSync'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

const syncStore = useGroupSyncStore()
const showAlertDialog = ref(false)
const retrying = ref(false)

// Initialize store
syncStore.initialize()

function formatTime(time: string) {
  return formatDistanceToNow(new Date(time), {
    addSuffix: true,
    locale: zhCN
  })
}

function getSuccessRateType(rate: number): 'success' | 'warning' | 'danger' {
  if (rate >= 95) return 'success'
  if (rate >= 80) return 'warning'
  return 'danger'
}

async function handleSyncNow() {
  try {
    await ElMessageBox.confirm(
      '确定要立即同步所有活跃群组吗？',
      '确认同步',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await syncStore.triggerSync()

    ElMessage.success(
      `同步完成！总计 ${result.totalCount} 个群组，成功 ${result.successCount} 个，失败 ${result.failureCount} 个`
    )
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '同步失败')
    }
  }
}

async function handleRetryFailed() {
  retrying.value = true
  try {
    const result = await syncStore.retryFailedGroups()

    ElMessage.success(
      `重试完成！总计 ${result.totalCount} 个群组，成功 ${result.successCount} 个，失败 ${result.failureCount} 个`
    )

    if (result.failureCount === 0) {
      showAlertDialog.value = false
    }
  } catch (error: any) {
    ElMessage.error(error.message || '重试失败')
  } finally {
    retrying.value = false
  }
}

async function handleResetFailure(groupId: number) {
  try {
    await syncStore.resetFailureCount(groupId)
    ElMessage.success('失败计数已重置')
  } catch (error: any) {
    ElMessage.error(error.message || '重置失败')
  }
}
</script>

<style scoped>
.sync-status-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sync-details {
  margin-top: 16px;
}

.text-muted {
  color: #909399;
}
</style>
