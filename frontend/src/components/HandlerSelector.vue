<template>
  <div class="handler-selector">
    <!-- Handler Type Selection -->
    <el-select
      v-model="selectedHandlerType"
      placeholder="选择处理器类型（可选）"
      clearable
      filterable
      style="width: 100%"
      @change="handleHandlerTypeChange"
    >
      <el-option-group
        v-for="category in handlersByCategory"
        :key="category.category"
        :label="HandlerCategoryLabels[category.category] || category.category"
      >
        <el-option
          v-for="handler in category.handlers"
          :key="handler.handlerType"
          :label="handler.name"
          :value="handler.handlerType"
          :disabled="!handler.enabled"
        >
          <span>{{ handler.name }}</span>
          <span style="float: right; color: var(--el-text-color-secondary); font-size: 12px">
            {{ handler.handlerType }}
          </span>
        </el-option>
      </el-option-group>
    </el-select>

    <div v-if="selectedHandlerType" class="handler-info">
      <el-text size="small" type="info">
        {{ selectedHandlerMetadata?.description }}
      </el-text>
    </div>

    <!-- Handler Parameters Configuration -->
    <div v-if="selectedHandlerMetadata && selectedHandlerMetadata.params.length > 0" class="handler-params">
      <el-divider content-position="left">
        <el-text size="small">处理器参数</el-text>
      </el-divider>

      <el-form :model="handlerParamsModel" label-width="120px">
        <el-form-item
          v-for="param in selectedHandlerMetadata.params"
          :key="param.name"
          :label="param.displayName"
          :required="param.required"
        >
          <!-- String Parameter -->
          <el-input
            v-if="param.type === 'string'"
            v-model="handlerParamsModel[param.name]"
            :placeholder="param.description || `请输入${param.displayName}`"
            clearable
          />

          <!-- Number Parameter -->
          <el-input-number
            v-else-if="param.type === 'number'"
            v-model="handlerParamsModel[param.name]"
            :placeholder="param.description || `请输入${param.displayName}`"
            style="width: 100%"
          />

          <!-- Boolean Parameter -->
          <el-switch
            v-else-if="param.type === 'boolean'"
            v-model="handlerParamsModel[param.name]"
          />

          <!-- Enum Parameter -->
          <el-select
            v-else-if="param.type === 'enum'"
            v-model="handlerParamsModel[param.name]"
            :placeholder="`请选择${param.displayName}`"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="enumValue in param.enumValues"
              :key="enumValue"
              :label="enumValue"
              :value="enumValue"
            />
          </el-select>

          <div v-if="param.description" class="param-tip">
            <el-text size="small" type="info">
              {{ param.description }}
            </el-text>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listHandlers } from '@/api/handler'
import type { HandlerMetadataVO } from '@/types/handler'
import { HandlerCategoryLabels } from '@/types/handler'

/**
 * T056: HandlerSelector Component
 * Dropdown with handler list and parameter configuration form
 */

interface Props {
  handlerType?: string
  handlerParams?: Record<string, any>
}

interface Emits {
  (e: 'update:handlerType', value: string | undefined): void
  (e: 'update:handlerParams', value: Record<string, any>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Handler metadata list
const handlers = ref<HandlerMetadataVO[]>([])
const loading = ref(false)

// Selected handler
const selectedHandlerType = ref<string | undefined>(props.handlerType)
const handlerParamsModel = ref<Record<string, any>>(props.handlerParams || {})

// Group handlers by category
const handlersByCategory = computed(() => {
  const categoryMap = new Map<string, HandlerMetadataVO[]>()

  handlers.value.forEach(handler => {
    const category = handler.category || 'Custom'
    if (!categoryMap.has(category)) {
      categoryMap.set(category, [])
    }
    categoryMap.get(category)!.push(handler)
  })

  return Array.from(categoryMap.entries()).map(([category, handlers]) => ({
    category,
    handlers: handlers.sort((a, b) => a.name.localeCompare(b.name))
  }))
})

// Get selected handler metadata
const selectedHandlerMetadata = computed(() => {
  if (!selectedHandlerType.value) return null
  return handlers.value.find(h => h.handlerType === selectedHandlerType.value)
})

// Load handlers from API
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

// Handle handler type change
const handleHandlerTypeChange = (handlerType: string | undefined) => {
  selectedHandlerType.value = handlerType
  emit('update:handlerType', handlerType)

  // Reset parameters when handler type changes
  if (handlerType) {
    const handler = handlers.value.find(h => h.handlerType === handlerType)
    if (handler) {
      // Initialize parameters with default values
      const defaultParams: Record<string, any> = {}
      handler.params.forEach(param => {
        if (param.defaultValue !== undefined) {
          // Parse default value based on type
          if (param.type === 'number') {
            defaultParams[param.name] = Number(param.defaultValue)
          } else if (param.type === 'boolean') {
            defaultParams[param.name] = param.defaultValue === 'true'
          } else {
            defaultParams[param.name] = param.defaultValue
          }
        }
      })
      handlerParamsModel.value = defaultParams
      emit('update:handlerParams', defaultParams)
    }
  } else {
    handlerParamsModel.value = {}
    emit('update:handlerParams', {})
  }
}

// Watch parameter changes
watch(
  handlerParamsModel,
  (newParams) => {
    emit('update:handlerParams', newParams)
  },
  { deep: true }
)

// Watch props changes
watch(
  () => props.handlerType,
  (newType) => {
    selectedHandlerType.value = newType
  }
)

watch(
  () => props.handlerParams,
  (newParams) => {
    handlerParamsModel.value = newParams || {}
  },
  { deep: true }
)

// Load handlers on mount
onMounted(() => {
  loadHandlers()
})
</script>

<style scoped lang="scss">
.handler-selector {
  .handler-info {
    margin-top: 8px;
    padding: 8px 12px;
    background-color: var(--el-fill-color-light);
    border-radius: 4px;
  }

  .handler-params {
    margin-top: 16px;
    padding: 16px;
    background-color: var(--el-fill-color-lighter);
    border-radius: 4px;

    .param-tip {
      margin-top: 4px;
      line-height: 1.5;
    }
  }
}
</style>
