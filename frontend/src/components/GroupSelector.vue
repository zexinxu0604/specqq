<template>
  <el-select
    v-model="selectedValue"
    :placeholder="placeholder"
    :multiple="multiple"
    :loading="loading"
    filterable
    remote
    :remote-method="handleSearch"
    clearable
    style="width: 100%"
    @change="handleChange"
  >
    <el-option
      v-for="group in groups"
      :key="group.id"
      :label="`${group.groupName} (${group.groupId})`"
      :value="group.id"
    />
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { listGroups } from '@/api/modules/group.api'
import type { GroupChat } from '@/types/group'

interface Props {
  modelValue: number | number[] | null
  multiple?: boolean
  placeholder?: string
}

interface Emits {
  (e: 'update:modelValue', value: number | number[] | null): void
}

const props = withDefaults(defineProps<Props>(), {
  multiple: false,
  placeholder: '请选择群聊'
})

const emit = defineEmits<Emits>()

const selectedValue = ref(props.modelValue)
const groups = ref<GroupChat[]>([])
const loading = ref(false)

watch(() => props.modelValue, (newVal) => {
  selectedValue.value = newVal
})

const handleSearch = async (query: string) => {
  loading.value = true
  try {
    const response = await listGroups({
      keyword: query,
      page: 1,
      size: 20
    })
    groups.value = response.data.records
  } finally {
    loading.value = false
  }
}

const handleChange = (value: number | number[]) => {
  emit('update:modelValue', value)
}

// 初始加载
handleSearch('')
</script>
