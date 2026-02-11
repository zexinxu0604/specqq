<template>
  <div class="group-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>群聊管理</span>
          <el-button type="primary" :icon="Plus">批量导入</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="groups" stripe>
        <el-table-column prop="groupName" label="群名称" min-width="150" />
        <el-table-column prop="groupId" label="群ID" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" size="small">配置规则</el-button>
            <el-button link type="primary" size="small">统计</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listGroups, toggleGroupStatus } from '@/api/modules/group.api'
import type { GroupChat } from '@/types/group'

const groups = ref<GroupChat[]>([])
const loading = ref(false)

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

onMounted(() => {
  loadGroups()
})
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
