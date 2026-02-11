<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <el-icon :size="48" color="#409EFF">
          <ChatDotRound />
        </el-icon>
        <h2>Chatbot Router</h2>
        <p class="subtitle">聊天机器人路由系统</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <el-alert
          v-if="showInitTip"
          title="首次使用提示"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>默认管理员账号：<strong>admin</strong></p>
            <p>默认密码：<strong>admin123</strong></p>
            <el-button
              link
              type="primary"
              size="small"
              :loading="initLoading"
              @click="handleInitAdmin"
            >
              初始化管理员账户
            </el-button>
          </template>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, ChatDotRound } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth.store'
import { initAdmin } from '@/api/modules/auth.api'
import type { LoginRequest } from '@/types/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 登录表单
const loginForm = reactive<LoginRequest>({
  username: '',
  password: ''
})

// 记住我
const rememberMe = ref(false)

// 加载状态
const loading = ref(false)
const initLoading = ref(false)

// 是否显示初始化提示
const showInitTip = ref(false)

// 表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.login(loginForm)

      // 记住我功能
      if (rememberMe.value) {
        localStorage.setItem('rememberedUsername', loginForm.username)
      } else {
        localStorage.removeItem('rememberedUsername')
      }

      ElMessage.success('登录成功')

      // 跳转到目标页面或首页
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } catch (error: any) {
      // 如果是用户不存在，显示初始化提示
      if (error.response?.data?.code === 1301) {
        showInitTip.value = true
      }
    } finally {
      loading.value = false
    }
  })
}

// 初始化管理员账户
const handleInitAdmin = async () => {
  initLoading.value = true
  try {
    await initAdmin()
    ElMessage.success('管理员账户初始化成功，请使用 admin/admin123 登录')
    showInitTip.value = false

    // 自动填充账号密码
    loginForm.username = 'admin'
    loginForm.password = 'admin123'
  } catch (error: any) {
    ElMessage.error(error.message || '初始化失败')
  } finally {
    initLoading.value = false
  }
}

// 组件挂载时恢复记住的用户名
onMounted(() => {
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  if (rememberedUsername) {
    loginForm.username = rememberedUsername
    rememberMe.value = true
  }
})
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 30px;

  .login-header {
    text-align: center;
    margin-bottom: 32px;

    .el-icon {
      margin-bottom: 16px;
    }

    h2 {
      margin: 0 0 8px 0;
      font-size: 28px;
      font-weight: 600;
      color: #303133;
    }

    .subtitle {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .el-form {
    .el-form-item {
      margin-bottom: 24px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  .login-footer {
    margin-top: 20px;

    .el-alert {
      :deep(.el-alert__content) {
        p {
          margin: 4px 0;
          font-size: 13px;

          strong {
            color: #409EFF;
          }
        }
      }
    }
  }
}
</style>
