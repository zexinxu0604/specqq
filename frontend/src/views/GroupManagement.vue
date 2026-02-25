<template>
  <div class="group-management">
    <!-- Sync Status Card -->
    <GroupSyncStatus @sync-complete="loadGroups" />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>群聊管理</span>
          <div class="header-actions">
            <el-button type="success" :icon="Search" @click="handleDiscoverGroups">
              发现新群组
            </el-button>
            <el-button type="primary" :icon="Plus">批量导入</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="groups" stripe>
        <el-table-column prop="groupName" label="群名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="groupId" label="群ID" width="120" />
        <el-table-column prop="memberCount" label="成员数" width="80" align="center" />
        <el-table-column label="同步状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.syncStatus"
              :type="getSyncStatusType(row.syncStatus)"
              size="small"
            >
              {{ getSyncStatusText(row.syncStatus) }}
            </el-tag>
            <span v-else class="text-muted">未同步</span>
          </template>
        </el-table-column>
        <el-table-column label="最后同步" width="140">
          <template #default="{ row }">
            <span v-if="row.lastSyncTime" class="sync-time">
              {{ formatRelativeTime(row.lastSyncTime) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleSyncGroup(row)">
              同步
            </el-button>
            <el-button link type="primary" size="small" @click="handleConfigRules(row)">
              规则
            </el-button>
            <el-button link type="primary" size="small" @click="handleViewStats(row)">
              统计
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadGroups"
      />
    </el-card>

    <!-- 群组规则配置对话框 -->
    <GroupRulesDialog
      v-model="rulesDialogVisible"
      :group="selectedGroup"
      @rules-changed="loadGroups"
    />

    <!-- 群组统计对话框 -->
    <GroupStatsDialog
      v-model="statsDialogVisible"
      :group="selectedGroup"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { listGroups, toggleGroupStatus } from '@/api/modules/group.api'
import { useGroupSyncStore } from '@/stores/groupSync'
import GroupSyncStatus from '@/components/GroupSyncStatus.vue'
import GroupRulesDialog from '@/components/GroupRulesDialog.vue'
import GroupStatsDialog from '@/components/GroupStatsDialog.vue'
import type { GroupChat } from '@/types/group'

const groupSyncStore = useGroupSyncStore()

const groups = ref<GroupChat[]>([])
const loading = ref(false)
const selectedGroup = ref<GroupChat | null>(null)
const rulesDialogVisible = ref(false)
const statsDialogVisible = ref(false)

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const loadGroups = async () => {
  loading.value = true
  try {
    const response = await listGroups({
      page: pagination.page,
      size: pagination.size
    })
    groups.value = response.data.records
    pagination.total = response.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载群聊列表失败')
  } finally {
    loading.value = false
  }
}

const handleToggleStatus = async (group: GroupChat) => {
  try {
    await toggleGroupStatus(group.id, group.enabled)
    ElMessage.success(`群聊已${group.enabled ? '启用' : '禁用'}`)
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
    group.enabled = !group.enabled
  }
}

const formatDateTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const formatRelativeTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const getSyncStatusType = (status: string): 'success' | 'warning' | 'danger' | 'info' => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'PENDING': return 'warning'
    default: return 'info'
  }
}

const getSyncStatusText = (status: string): string => {
  switch (status) {
    case 'SUCCESS': return '成功'
    case 'FAILED': return '失败'
    case 'PENDING': return '同步中'
    default: return status
  }
}

const handleSyncGroup = async (group: GroupChat) => {
  try {
    await groupSyncStore.syncSingleGroup(group.id)
    ElMessage.success(`群组 "${group.groupName}" 同步成功`)
    await loadGroups()
  } catch (error: any) {
    ElMessage.error(error.message || '同步失败')
  }
}

const handleConfigRules = (group: GroupChat) => {
  selectedGroup.value = group
  rulesDialogVisible.value = true
}

const handleViewStats = (group: GroupChat) => {
  selectedGroup.value = group
  statsDialogVisible.value = true
}

const handleDiscoverGroups = async () => {
  try {
    await ElMessageBox.confirm(
      '将从NapCat获取机器人所在的所有群组，并添加尚未记录的新群组。确定继续吗？',
      '发现新群组',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    // Assuming clientId is 1 for now - in production, this should be selected by user
    const clientId = 1
    const count = await groupSyncStore.discoverNewGroups(clientId)

    if (count > 0) {
      ElMessage.success(`成功发现并添加 ${count} 个新群组`)
      await loadGroups() // Refresh the list
    } else {
      ElMessage.info('没有发现新群组')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '发现新群组失败')
    }
  }
}

// 页面加载时自动同步群聊信息
const autoSyncOnLoad = async () => {
  // 先加载已有数据，让用户看到页面
  await loadGroups()

  // 后台静默执行发现和同步
  try {
    const clientId = 1

    // 并行执行：发现新群组 + 同步已有群组
    const [discoverResult, syncResult] = await Promise.allSettled([
      groupSyncStore.discoverNewGroups(clientId),
      groupSyncStore.triggerSync()
    ])

    let hasChanges = false

    // 处理发现新群组结果
    if (discoverResult.status === 'fulfilled') {
      const newCount = discoverResult.value
      if (newCount > 0) {
        ElMessage.success(`自动发现 ${newCount} 个新群组`)
        hasChanges = true
      }
    } else {
      console.warn('自动发现新群组失败:', discoverResult.reason)
    }

    // 处理同步结果
    if (syncResult.status === 'fulfilled') {
      const result = syncResult.value
      if (result.successCount > 0 || result.failureCount > 0) {
        console.log(`自动同步完成: 成功 ${result.successCount} 个, 失败 ${result.failureCount} 个`)
        hasChanges = true
      }
    } else {
      console.warn('自动同步群组失败:', syncResult.reason)
    }

    // 如果有变化，刷新列表
    if (hasChanges) {
      await loadGroups()
    }
  } catch (error) {
    // 静默处理错误，不影响用户体验
    console.error('自动同步群聊信息失败:', error)
  }
}

onMounted(() => {
  autoSyncOnLoad()
})
</script>

<style scoped lang="scss">
.group-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.text-muted {
  color: #909399;
  font-size: 13px;
}

.sync-time {
  font-size: 13px;
  color: #606266;
}

:deep(.el-table) {
  .el-table__cell {
    padding: 8px 0;
  }
}
</style>
