/**
 * Policy Type Definitions
 *
 * T044: Define PolicyDTO, PolicyVO, PolicyTemplate interfaces
 */

/**
 * Scope Enum
 */
export enum Scope {
  USER = 'USER',
  GROUP = 'GROUP',
  GLOBAL = 'GLOBAL'
}

/**
 * Scope Labels
 */
export const ScopeLabels: Record<Scope, string> = {
  [Scope.USER]: '用户级别',
  [Scope.GROUP]: '群组级别',
  [Scope.GLOBAL]: '全局级别'
}

/**
 * Policy DTO (Data Transfer Object)
 */
export interface PolicyDTO {
  // Scope Policy
  scope?: Scope
  whitelist?: string[]
  blacklist?: string[]

  // Rate Limit Policy
  rateLimitEnabled?: boolean
  rateLimitMaxRequests?: number
  rateLimitWindowSeconds?: number

  // Time Window Policy
  timeWindowEnabled?: boolean
  timeWindowStart?: string  // HH:mm format
  timeWindowEnd?: string    // HH:mm format
  timeWindowWeekdays?: string | number[]  // Backend: string "1,2,3", Frontend: array [1,2,3]

  // Role Policy
  roleEnabled?: boolean
  allowedRoles?: string[]  // owner, admin, member

  // Cooldown Policy
  cooldownEnabled?: boolean
  cooldownSeconds?: number
}

/**
 * Policy VO (View Object)
 */
export interface PolicyVO extends PolicyDTO {
  id: number
  ruleId: number
  createdAt: string
  updatedAt: string
}

/**
 * Policy Template
 */
export interface PolicyTemplate {
  name: string
  displayName: string
  description: string
  policy: PolicyDTO
  category: string
  icon?: string
}

/**
 * Policy Statistics
 */
export interface PolicyStats {
  ruleId?: number
  totalChecks: number
  passedChecks: number
  blockedChecks: number
  passRate: number
  blockReasons: Record<string, number>
  startTime?: string
  endTime?: string
}

/**
 * Policy Check Result
 */
export interface PolicyCheckResult {
  passed: boolean
  failedPolicy?: string
  reason?: string
  timestamp?: string
}

/**
 * Policy Form Model
 */
export interface PolicyFormModel {
  // Scope
  scope: Scope
  whitelist: string
  blacklist: string

  // Rate Limit
  rateLimitEnabled: boolean
  rateLimitMaxRequests: number
  rateLimitWindowSeconds: number

  // Time Window
  timeWindowEnabled: boolean
  timeWindowStart: string
  timeWindowEnd: string
  timeWindowWeekdays: number[]

  // Role
  roleEnabled: boolean
  allowedRoles: string[]

  // Cooldown
  cooldownEnabled: boolean
  cooldownSeconds: number
}

/**
 * Default Policy Values
 */
export const DEFAULT_POLICY: PolicyDTO = {
  scope: Scope.USER,
  whitelist: [],
  blacklist: [],
  rateLimitEnabled: false,
  rateLimitMaxRequests: 10,
  rateLimitWindowSeconds: 60,
  timeWindowEnabled: false,
  timeWindowStart: '00:00',
  timeWindowEnd: '23:59',
  timeWindowWeekdays: [1, 2, 3, 4, 5, 6, 7],
  roleEnabled: false,
  allowedRoles: ['owner', 'admin', 'member'],
  cooldownEnabled: false,
  cooldownSeconds: 300
}

/**
 * Policy Templates
 */
export const POLICY_TEMPLATES: PolicyTemplate[] = [
  {
    name: 'no_restriction',
    displayName: '无限制',
    description: '不设置任何策略限制',
    category: 'basic',
    policy: DEFAULT_POLICY
  },
  {
    name: 'rate_limit_basic',
    displayName: '基础限流',
    description: '每分钟最多10次请求',
    category: 'rate_limit',
    policy: {
      ...DEFAULT_POLICY,
      rateLimitEnabled: true,
      rateLimitMaxRequests: 10,
      rateLimitWindowSeconds: 60
    }
  },
  {
    name: 'rate_limit_strict',
    displayName: '严格限流',
    description: '每分钟最多3次请求',
    category: 'rate_limit',
    policy: {
      ...DEFAULT_POLICY,
      rateLimitEnabled: true,
      rateLimitMaxRequests: 3,
      rateLimitWindowSeconds: 60
    }
  },
  {
    name: 'admin_only',
    displayName: '仅管理员',
    description: '仅群主和管理员可触发',
    category: 'role',
    policy: {
      ...DEFAULT_POLICY,
      roleEnabled: true,
      allowedRoles: ['owner', 'admin']
    }
  },
  {
    name: 'working_hours',
    displayName: '工作时间',
    description: '工作日 09:00-18:00',
    category: 'time_window',
    policy: {
      ...DEFAULT_POLICY,
      timeWindowEnabled: true,
      timeWindowStart: '09:00',
      timeWindowEnd: '18:00',
      timeWindowWeekdays: [1, 2, 3, 4, 5]  // Monday-Friday
    }
  },
  {
    name: 'cooldown_5min',
    displayName: '5分钟冷却',
    description: '触发后5分钟内不可重复触发',
    category: 'cooldown',
    policy: {
      ...DEFAULT_POLICY,
      cooldownEnabled: true,
      cooldownSeconds: 300
    }
  }
]

/**
 * Role Options
 */
export const ROLE_OPTIONS = [
  { label: '群主', value: 'owner' },
  { label: '管理员', value: 'admin' },
  { label: '普通成员', value: 'member' }
]

/**
 * Weekday Options
 */
export const WEEKDAY_OPTIONS = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]
