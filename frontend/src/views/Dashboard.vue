<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总规则数" :value="stats.totalRules">
            <template #prefix>
              <el-icon color="#409EFF"><List /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总群聊数" :value="stats.totalGroups">
            <template #prefix>
              <el-icon color="#67C23A"><ChatDotRound /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日消息数" :value="stats.todayMessages">
            <template #prefix>
              <el-icon color="#E6A23C"><ChatLineRound /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="成功率" :value="stats.successRate" suffix="%">
            <template #prefix>
              <el-icon color="#F56C6C"><SuccessFilled /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统概览</span>
            </div>
          </template>
          <el-empty description="仪表盘功能开发中..." />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { List, ChatDotRound, ChatLineRound, SuccessFilled } from '@element-plus/icons-vue'

const stats = ref({
  totalRules: 0,
  totalGroups: 0,
  todayMessages: 0,
  successRate: 0
})

onMounted(() => {
  // TODO: 加载统计数据
  stats.value = {
    totalRules: 12,
    totalGroups: 5,
    todayMessages: 156,
    successRate: 98.5
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
