<!--
  CQCodeSelector Component

  Hierarchical CQ code pattern selector using Element Plus Cascader.
  Groups patterns by type (face, image, at, etc.) with Chinese labels.

  @author Claude Code
  @since 2026-02-11
-->
<template>
  <div class="cqcode-selector">
    <el-cascader
      v-model="selectedPatternId"
      :options="cascaderOptions"
      :props="cascaderProps"
      :placeholder="placeholder"
      :disabled="disabled"
      :clearable="clearable"
      :filterable="filterable"
      @change="handlePatternChange"
      class="cqcode-cascader"
    >
      <template #default="{ node, data }">
        <span>
          <el-icon v-if="!node.isLeaf" class="category-icon">
            <component :is="getCategoryIcon(data.value)" />
          </el-icon>
          {{ data.label }}
        </span>
      </template>
    </el-cascader>

    <!-- Parameter Filters (shown when pattern with filters is selected) -->
    <div v-if="selectedPattern && hasParameterFilters" class="parameter-filters">
      <el-divider content-position="left">参数过滤</el-divider>
      <el-form :model="parameterValues" label-width="100px" size="small">
        <el-form-item
          v-for="filter in selectedPattern.paramFilters"
          :key="filter.name"
          :label="filter.name"
          :required="filter.required"
        >
          <el-input
            v-model="parameterValues[filter.name]"
            :placeholder="filter.isRegex ? '正则表达式' : '参数值'"
            @input="handleParameterChange"
          >
            <template #prepend>
              <el-tag v-if="filter.isRegex" type="warning" size="small">Regex</el-tag>
              <el-tag v-else type="info" size="small">Exact</el-tag>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useCQCodeStore } from '@/stores/cqcode.store'
import { getCQCodeIcon } from '@/utils/cqcode-formatter'
import type { CQCodePattern } from '@/types/cqcode'
import {
  Sunny,
  Picture,
  User,
  ChatDotRound,
  Microphone,
  VideoCamera,
  QuestionFilled
} from '@element-plus/icons-vue'

interface Props {
  modelValue?: string | null
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
  filterable?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string | null): void
  (e: 'change', pattern: CQCodePattern | null): void
  (e: 'parameter-change', params: Record<string, string>): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  placeholder: '请选择CQ码类型',
  disabled: false,
  clearable: true,
  filterable: true
})

const emit = defineEmits<Emits>()

const cqCodeStore = useCQCodeStore()
const selectedPatternId = ref<string[]>([])
const parameterValues = ref<Record<string, string>>({})

// Get icon component by name
const iconComponents: Record<string, any> = {
  Sunny,
  Picture,
  User,
  ChatDotRound,
  Microphone,
  VideoCamera,
  QuestionFilled
}

function getCategoryIcon(type: string): any {
  const iconName = getCQCodeIcon(type)
  return iconComponents[iconName] || QuestionFilled
}

// Cascader configuration
const cascaderProps = {
  expandTrigger: 'hover' as const,
  value: 'value',
  label: 'label',
  children: 'children'
}

// Build cascader options from pattern categories
const cascaderOptions = computed(() => {
  return cqCodeStore.patternCategories.map(category => ({
    value: category.id,
    label: category.label,
    children: category.patterns.map(pattern => ({
      value: pattern.id || pattern.name,
      label: pattern.label,
      pattern: pattern
    }))
  }))
})

// Get selected pattern object
const selectedPattern = computed<CQCodePattern | null>(() => {
  if (!selectedPatternId.value || selectedPatternId.value.length < 2) {
    return null
  }

  const patternId = selectedPatternId.value[1]
  return cqCodeStore.getPatternById(patternId) || null
})

// Check if selected pattern has parameter filters
const hasParameterFilters = computed(() => {
  return selectedPattern.value?.paramFilters && selectedPattern.value.paramFilters.length > 0
})

// Handle pattern selection change
function handlePatternChange(value: string[]) {
  if (!value || value.length < 2) {
    emit('update:modelValue', null)
    emit('change', null)
    parameterValues.value = {}
    return
  }

  const patternId = value[1]
  const pattern = cqCodeStore.getPatternById(patternId)

  if (pattern) {
    emit('update:modelValue', patternId)
    emit('change', pattern)

    // Initialize parameter values
    if (pattern.paramFilters) {
      const initialParams: Record<string, string> = {}
      pattern.paramFilters.forEach(filter => {
        initialParams[filter.name] = filter.value || ''
      })
      parameterValues.value = initialParams
    } else {
      parameterValues.value = {}
    }
  }
}

// Handle parameter value change
function handleParameterChange() {
  emit('parameter-change', parameterValues.value)
}

// Watch for external model value changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (!newValue) {
      selectedPatternId.value = []
      parameterValues.value = {}
      return
    }

    // Find pattern and set cascader value
    const pattern = cqCodeStore.getPatternById(newValue)
    if (pattern) {
      selectedPatternId.value = [pattern.type, newValue]

      // Initialize parameter values from pattern
      if (pattern.paramFilters) {
        const initialParams: Record<string, string> = {}
        pattern.paramFilters.forEach(filter => {
          initialParams[filter.name] = filter.value || ''
        })
        parameterValues.value = initialParams
      }
    }
  },
  { immediate: true }
)

// Initialize store on mount
onMounted(async () => {
  if (cqCodeStore.patterns.length === 0) {
    await cqCodeStore.initialize()
  }
})
</script>

<style scoped lang="scss">
.cqcode-selector {
  width: 100%;

  .cqcode-cascader {
    width: 100%;
  }

  .category-icon {
    margin-right: 4px;
    vertical-align: middle;
  }

  .parameter-filters {
    margin-top: 16px;
    padding: 12px;
    background-color: var(--el-fill-color-light);
    border-radius: 4px;

    .el-divider {
      margin: 0 0 16px 0;
    }

    .el-form {
      margin: 0;
    }

    .el-form-item {
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
</style>
