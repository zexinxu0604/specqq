/**
 * Vue Router 配置
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

/**
 * 路由配置
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: {
          title: '仪表盘',
          icon: 'Odometer'
        }
      },
      {
        path: 'rules',
        name: 'RuleManagement',
        component: () => import('@/views/RuleManagement.vue'),
        meta: {
          title: '规则管理',
          icon: 'List'
        }
      },
      {
        path: 'groups',
        name: 'GroupManagement',
        component: () => import('@/views/GroupManagement.vue'),
        meta: {
          title: '群聊管理',
          icon: 'ChatDotRound'
        }
      },
      {
        path: 'logs',
        name: 'LogManagement',
        component: () => import('@/views/LogManagement.vue'),
        meta: {
          title: '日志管理',
          icon: 'Document'
        }
      },
      {
        path: 'clients',
        name: 'ClientManagement',
        component: () => import('@/views/ClientManagement.vue'),
        meta: {
          title: '客户端管理',
          icon: 'Connection'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: {
      title: '页面不存在'
    }
  }
]

/**
 * 创建路由实例
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

/**
 * 全局前置守卫 - 认证检查
 */
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - Chatbot Router`
  }

  // 检查是否需要认证
  if (to.meta.requiresAuth !== false) {
    if (!authStore.isLoggedIn) {
      // 未登录，跳转到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    } else {
      next()
    }
  } else {
    // 已登录用户访问登录页，重定向到首页
    if (to.path === '/login' && authStore.isLoggedIn) {
      next({ path: '/' })
    } else {
      next()
    }
  }
})

/**
 * 全局后置钩子
 */
router.afterEach(() => {
  // 页面切换后滚动到顶部
  window.scrollTo(0, 0)
})

export default router
