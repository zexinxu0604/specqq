<template>
  <div class="handler-list">
    <!-- Search and Filter -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索处理器名称或类型"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="searchForm.category"
            placeholder="全部"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option
              v-for="(label, value) in HandlerCategoryLabels"
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
      </el-form>
    </el-card>

    <!-- Handler Grid -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-row :gutter="16">
        <el-col
          v-for="handler in filteredHandlers"
          :key="handler.handlerType"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <el-card
            class="handler-card"
            :class="{ disabled: !handler.enabled }"
            shadow="hover"
          >
            <template #header>
              <div class="card-header">
                <el-text class="handler-name" truncated>
                  {{ handler.name }}
                </el-text>
                <el-tag
                  :type="handler.enabled ? 'success' : 'info'"
                  size="small"
                >
                  {{ handler.enabled ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </template>

            <div class="handler-content">
              <div class="handler-type">
                <el-text size="small" type="info">
                  {{ handler.handlerType }}
                </el-text>
              </div>

              <div class="handler-category">
                <el-tag size="small" effect="plain">
                  {{ HandlerCategoryLabels[handler.category] || handler.category }}
                </el-tag>
              </div>

              <div class="handler-description">
                <el-text size="small">
                  {{ handler.description }}
                </el-text>
              </div>

              <el-divider />

              <div class="handler-params">
                <el-text size="small" type="info">
                  参数: {{ handler.params.length }} 个
                </el-text>
                <div v-if="handler.params.length > 0" class="params-list">
                  <el-tag
                    v-for="param in handler.params.slice(0, 3)"
                    :key="param.name"
                    size="small"
                    effect="plain"
                    type="info"
                  >
                    {{ param.displayName }}
                    <span v-if="param.required" style="color: var(--el-color-danger)">*</span>
                  </el-tag>
                  <el-text v-if="handler.params.length > 3" size="small" type="info">
                    +{{ handler.params.length - 3 }} 更多
                  </el-text>
                </div>
              </div>
            </div>

            <template #footer>
              <el-button
                type="primary"
                size="small"
                :icon="View"
                @click="handleViewDetails(handler)"
              >
                查看详情
              </el-button>
            </template>
          </el-card>
        </el-col>
      </el-row>

      <!-- Empty State -->
      <el-empty
        v-if="filteredHandlers.length === 0"
        description="没有找到匹配的处理器"
      />
    </el-card>

    <!-- Handler Details Dialog -->
    <el-dialog
      v-model="detailsDialogVisible"
      :title="selectedHandler?.name"
      width="700px"
    >
      <div v-if="selectedHandler" class="handler-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="处理器类型">
            {{ selectedHandler.handlerType }}
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag size="small">
              {{ HandlerCategoryLabels[selectedHandler.category] || selectedHandler.category }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedHandler.enabled ? 'success' : 'info'" size="small">
              {{ selectedHandler.enabled ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="参数数量">
            {{ selectedHandler.params.length }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ selectedHandler.description }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">参数列表</el-divider>

        <el-table
          v-if="selectedHandler.params.length > 0"
          :data="selectedHandler.params"
          stripe
          border
        >
          <el-table-column prop="displayName" label="参数名称" width="150" />
          <el-table-column prop="name" label="参数键" width="150" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info">
                {{ row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="必填" width="80">
            <template #default="{ row }">
              <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                {{ row.required ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="defaultValue" label="默认值" width="120" />
          <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        </el-table>

        <el-empty
          v-else
          description="该处理器无需参数"
          :image-size="80"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import { listHandlers, getHandlersByCategory, searchHandlers } from '@/api/handler'
import type { HandlerMetadataVO } from '@/types/handler'
import { HandlerCategoryLabels } from '@/types/handler'

/**
 * T058: HandlerList View
 * Display all registered handlers with metadata
 */

// Search form
const searchForm = reactive({
  keyword: '',
  category: '',
  enabled: undefined as boolean | undefined
})

// Handlers list
const handlers = ref<HandlerMetadataVO[]>([])
const loading = ref(false)

// Details dialog
const detailsDialogVisible = ref(false)
const selectedHandler = ref<HandlerMetadataVO | null>(null)

// Filtered handlers
const filteredHandlers = computed(() => {
  let result = handlers.value

  // Filter by keyword
  if (searchForm.keyword) {
    const keyword = searchForm.keyword.toLowerCase()
    result = result.filter(
      h =>
        h.name.toLowerCase().includes(keyword) ||
        h.handlerType.toLowerCase().includes(keyword) ||
        h.description.toLowerCase().includes(keyword)
    )
  }

  // Filter by category
  if (searchForm.category) {
    result = result.filter(h => h.category === searchForm.category)
  }

  // Filter by enabled status
  if (searchForm.enabled !== undefined) {
    result = result.filter(h => h.enabled === searchForm.enabled)
  }

  return result
})

// Load handlers
const loadHandlers = async () => {
  loading.value = true
  try {
    const response = await listHandlers()
    handlers.value = response.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载处理器列表失败')
  } finally {
    loading.value = false
  }
}

// Search handlers
const handleSearch = () => {
  // Filtering is done by computed property
}

// Reset search
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.enabled = undefined
}

// View handler details
const handleViewDetails = (handler: HandlerMetadataVO) => {
  selectedHandler.value = handler
  detailsDialogVisible.value = true
}

// Load handlers on mount
onMounted(() => {
  loadHandlers()
})
</script>

<style scoped lang="scss">
.handler-list {
  .search-card {
    :deep(.el-card__body) {
      padding: 16px;
    }

    .el-form {
      margin-bottom: 0;
    }
  }

  .handler-card {
    margin-bottom: 16px;
    transition: all 0.3s;

    &.disabled {
      opacity: 0.6;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .handler-name {
        font-weight: 500;
        font-size: 14px;
      }
    }

    .handler-content {
      .handler-type {
        margin-bottom: 8px;
      }

      .handler-category {
        margin-bottom: 12px;
      }

      .handler-description {
        margin-bottom: 12px;
        min-height: 60px;
        line-height: 1.5;
      }

      .handler-params {
        .params-list {
          margin-top: 8px;
          display: flex;
          flex-wrap: wrap;
          gap: 4px;
        }
      }
    }
  }

  .handler-details {
    .el-descriptions {
      margin-bottom: 16px;
    }
  }
}
</style>
