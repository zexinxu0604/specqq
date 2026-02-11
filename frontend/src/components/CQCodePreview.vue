<!--
  CQCodePreview Component

  Displays CQ code pattern information including description, example,
  and regex pattern. Shows visual preview of how the pattern will match.

  @author Claude Code
  @since 2026-02-11
-->
<template>
  <div v-if="pattern" class="cqcode-preview">
    <el-card class="preview-card" shadow="hover">
      <!-- Header with type icon and label -->
      <template #header>
        <div class="preview-header">
          <el-icon :size="20" class="type-icon">
            <component :is="getTypeIcon(pattern.type)" />
          </el-icon>
          <span class="type-label">{{ getTypeLabel(pattern.type) }}</span>
          <el-tag
            :type="pattern.isPredefined ? 'success' : 'info'"
            size="small"
            class="pattern-tag"
          >
            {{ pattern.isPredefined ? '预定义' : '自定义' }}
          </el-tag>
        </div>
      </template>

      <!-- Pattern Information -->
      <div class="preview-content">
        <!-- Pattern Name -->
        <div class="info-row">
          <span class="info-label">名称:</span>
          <span class="info-value">{{ pattern.label }}</span>
        </div>

        <!-- Description -->
        <div class="info-row">
          <span class="info-label">描述:</span>
          <span class="info-value">{{ pattern.description }}</span>
        </div>

        <!-- Regex Pattern -->
        <div class="info-row">
          <span class="info-label">正则表达式:</span>
          <el-input
            :model-value="pattern.pattern"
            readonly
            size="small"
            class="pattern-input"
          >
            <template #prepend>
              <el-icon><Edit /></el-icon>
            </template>
            <template #append>
              <el-button
                :icon="DocumentCopy"
                @click="copyPattern"
                size="small"
              >
                复制
              </el-button>
            </template>
          </el-input>
        </div>

        <!-- Example -->
        <div class="info-row">
          <span class="info-label">示例消息:</span>
          <el-tag type="warning" effect="plain" class="example-tag">
            {{ pattern.example }}
          </el-tag>
        </div>

        <!-- Parameter Filters (if any) -->
        <div v-if="pattern.paramFilters && pattern.paramFilters.length > 0" class="info-row">
          <span class="info-label">参数过滤:</span>
          <div class="param-filters">
            <el-tag
              v-for="filter in pattern.paramFilters"
              :key="filter.name"
              :type="filter.required ? 'danger' : 'info'"
              size="small"
              class="filter-tag"
            >
              {{ filter.name }}={{ filter.value }}
              <el-icon v-if="filter.isRegex" class="regex-icon">
                <Star />
              </el-icon>
            </el-tag>
          </div>
        </div>

        <!-- Preview Visual Matching -->
        <el-divider content-position="left">
          <el-icon><View /></el-icon>
          匹配预览
        </el-divider>

        <div class="match-preview">
          <el-input
            v-model="testMessage"
            placeholder="输入测试消息查看匹配效果"
            size="small"
            clearable
            @input="handleTestInput"
          >
            <template #prepend>
              <el-icon><ChatDotRound /></el-icon>
            </template>
          </el-input>

          <div v-if="testMessage" class="match-result">
            <el-alert
              v-if="matchResult"
              :title="matchResult.matched ? '✓ 匹配成功' : '✗ 不匹配'"
              :type="matchResult.matched ? 'success' : 'warning'"
              :closable="false"
              show-icon
            >
              <template v-if="matchResult.matched && matchResult.matches">
                <div class="match-details">
                  <div v-for="(match, index) in matchResult.matches" :key="index" class="match-item">
                    <el-tag size="small" type="success">
                      {{ match }}
                    </el-tag>
                  </div>
                </div>
              </template>
            </el-alert>
          </div>
        </div>
      </div>
    </el-card>
  </div>
  <div v-else class="cqcode-preview-empty">
    <el-empty description="请选择一个CQ码模式" :image-size="80" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { formatCQCodeType, getCQCodeIcon } from '@/utils/cqcode-formatter'
import type { CQCodePattern } from '@/types/cqcode'
import {
  Edit,
  DocumentCopy,
  View,
  ChatDotRound,
  Star,
  Sunny,
  Picture,
  User,
  ChatDotRound as ChatIcon,
  Microphone,
  VideoCamera,
  QuestionFilled
} from '@element-plus/icons-vue'

interface Props {
  pattern: CQCodePattern | null
}

interface MatchResult {
  matched: boolean
  matches?: string[]
}

const props = defineProps<Props>()

const testMessage = ref('')
const matchResult = ref<MatchResult | null>(null)

// Get icon component by type
const iconComponents: Record<string, any> = {
  Sunny,
  Picture,
  User,
  ChatDotRound: ChatIcon,
  Microphone,
  VideoCamera,
  QuestionFilled
}

function getTypeIcon(type: string): any {
  const iconName = getCQCodeIcon(type)
  return iconComponents[iconName] || QuestionFilled
}

function getTypeLabel(type: string): string {
  return formatCQCodeType(type)
}

// Copy pattern to clipboard
async function copyPattern() {
  if (!props.pattern) return

  try {
    await navigator.clipboard.writeText(props.pattern.pattern)
    ElMessage.success('正则表达式已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

// Test pattern matching
function handleTestInput() {
  if (!props.pattern || !testMessage.value) {
    matchResult.value = null
    return
  }

  try {
    const regex = new RegExp(props.pattern.pattern, 'g')
    const matches = testMessage.value.match(regex)

    matchResult.value = {
      matched: matches !== null && matches.length > 0,
      matches: matches || undefined
    }
  } catch (error) {
    matchResult.value = {
      matched: false
    }
  }
}

// Reset test when pattern changes
watch(
  () => props.pattern,
  () => {
    testMessage.value = ''
    matchResult.value = null
  }
)
</script>

<style scoped lang="scss">
.cqcode-preview {
  width: 100%;

  .preview-card {
    .preview-header {
      display: flex;
      align-items: center;
      gap: 8px;

      .type-icon {
        color: var(--el-color-primary);
      }

      .type-label {
        font-size: 16px;
        font-weight: 600;
        flex: 1;
      }

      .pattern-tag {
        margin-left: auto;
      }
    }

    .preview-content {
      .info-row {
        margin-bottom: 16px;

        &:last-child {
          margin-bottom: 0;
        }

        .info-label {
          display: inline-block;
          width: 100px;
          font-weight: 500;
          color: var(--el-text-color-secondary);
        }

        .info-value {
          color: var(--el-text-color-primary);
        }

        .pattern-input {
          width: 100%;
          margin-top: 4px;
          font-family: 'Courier New', monospace;
        }

        .example-tag {
          margin-top: 4px;
          max-width: 100%;
          white-space: pre-wrap;
          word-break: break-all;
        }

        .param-filters {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          margin-top: 4px;

          .filter-tag {
            display: inline-flex;
            align-items: center;
            gap: 4px;

            .regex-icon {
              font-size: 12px;
            }
          }
        }
      }

      .el-divider {
        margin: 20px 0 16px 0;
      }

      .match-preview {
        .match-result {
          margin-top: 12px;

          .match-details {
            margin-top: 8px;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;

            .match-item {
              .el-tag {
                font-family: 'Courier New', monospace;
              }
            }
          }
        }
      }
    }
  }
}

.cqcode-preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}
</style>
