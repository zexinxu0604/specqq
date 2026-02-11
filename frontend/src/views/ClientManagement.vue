<template>
  <div class="client-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>客户端管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建客户端
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索客户端名称或类型"
          clearable
          style="width: 300px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterEnabled"
          placeholder="启用状态"
          clearable
          style="width: 150px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option label="全部" :value="null" />
          <el-option label="已启用" :value="true" />
          <el-option label="已禁用" :value="false" />
        </el-select>
        <el-button type="primary" @click="handleSearch" style="margin-left: 10px">
          搜索
        </el-button>
      </div>

      <!-- 客户端列表表格 -->
      <el-table
        :data="clientList"
        v-loading="loading"
        style="width: 100%; margin-top: 20px"
        stripe
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="clientName" label="客户端名称" width="200" />
        <el-table-column prop="clientType" label="客户端类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getClientTypeTagType(row.clientType)">
              {{ getClientTypeName(row.clientType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="protocolType" label="协议类型" width="150">
          <template #default="{ row }">
            <el-tag
              v-for="protocol in parseProtocols(row.protocolType)"
              :key="protocol"
              size="small"
              style="margin-right: 5px"
            >
              {{ protocol }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="connectionStatus" label="连接状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.connectionStatus)">
              {{ getStatusText(row.connectionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggleStatus(row)"
              :loading="row.statusLoading"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeatTime" label="最后心跳" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastHeartbeatTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleTestConnection(row)"
              :loading="row.testLoading"
            >
              测试连接
            </el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
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
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 客户端表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <ClientForm
        ref="clientFormRef"
        :client="currentClient"
        :is-edit="isEdit"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import ClientForm from '@/components/ClientForm.vue'
import { clientApi } from '@/api/client'

// 搜索和过滤
const searchKeyword = ref('')
const filterEnabled = ref(null)

// 表格数据
const clientList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentClient = ref(null)
const clientFormRef = ref(null)
const submitLoading = ref(false)

// 获取客户端列表
const fetchClientList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value || undefined,
      enabled: filterEnabled.value
    }

    const response = await clientApi.listClients(params)
    if (response.code === 200) {
      clientList.value = response.data.records.map(item => ({
        ...item,
        testLoading: false,
        statusLoading: false
      }))
      pagination.total = response.data.total
    } else {
      ElMessage.error(response.message || '获取客户端列表失败')
    }
  } catch (error) {
    console.error('Failed to fetch client list:', error)
    ElMessage.error('获取客户端列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchClientList()
}

// 分页变化
const handleSizeChange = () => {
  fetchClientList()
}

const handlePageChange = () => {
  fetchClientList()
}

// 新建客户端
const handleCreate = () => {
  dialogTitle.value = '新建客户端'
  isEdit.value = false
  currentClient.value = null
  dialogVisible.value = true
}

// 编辑客户端
const handleEdit = (row) => {
  dialogTitle.value = '编辑客户端'
  isEdit.value = true
  currentClient.value = { ...row }
  dialogVisible.value = true
}

// 删除客户端
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除客户端 "${row.clientName}" 吗？删除后将同时删除关联的群聊配置。`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await clientApi.deleteClient(row.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      fetchClientList()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete client:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 测试连接
const handleTestConnection = async (row) => {
  row.testLoading = true
  try {
    const response = await clientApi.testConnection(row.id)
    if (response.code === 200 && response.data) {
      ElMessage.success('连接测试成功')
      fetchClientList()
    } else {
      ElMessage.error(response.message || '连接测试失败')
    }
  } catch (error) {
    console.error('Failed to test connection:', error)
    ElMessage.error('连接测试失败')
  } finally {
    row.testLoading = false
  }
}

// 切换启用状态
const handleToggleStatus = async (row) => {
  row.statusLoading = true
  try {
    const response = await clientApi.toggleStatus(row.id, row.enabled)
    if (response.code === 200) {
      ElMessage.success(row.enabled ? '已启用' : '已禁用')
    } else {
      ElMessage.error(response.message || '操作失败')
      row.enabled = !row.enabled // 回滚状态
    }
  } catch (error) {
    console.error('Failed to toggle status:', error)
    ElMessage.error('操作失败')
    row.enabled = !row.enabled // 回滚状态
  } finally {
    row.statusLoading = false
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    const formData = await clientFormRef.value.validate()
    if (!formData) return

    submitLoading.value = true

    let response
    if (isEdit.value) {
      response = await clientApi.updateClient(currentClient.value.id, formData)
    } else {
      response = await clientApi.createClient(formData)
    }

    if (response.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchClientList()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('Failed to submit form:', error)
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 关闭对话框
const handleDialogClose = () => {
  currentClient.value = null
}

// 辅助函数
const getClientTypeName = (type) => {
  const typeMap = {
    qq: 'QQ',
    wechat: '微信',
    dingtalk: '钉钉'
  }
  return typeMap[type] || type
}

const getClientTypeTagType = (type) => {
  const typeMap = {
    qq: 'primary',
    wechat: 'success',
    dingtalk: 'warning'
  }
  return typeMap[type] || 'info'
}

const parseProtocols = (protocolType) => {
  if (!protocolType) return []
  return protocolType.split(',').map(p => p.trim().toUpperCase())
}

const getStatusText = (status) => {
  const statusMap = {
    CONNECTED: '已连接',
    DISCONNECTED: '未连接',
    ERROR: '错误'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const typeMap = {
    CONNECTED: 'success',
    DISCONNECTED: 'info',
    ERROR: 'danger'
  }
  return typeMap[status] || 'info'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  fetchClientList()
})
</script>

<style scoped>
.client-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  display: flex;
  align-items: center;
}
</style>
