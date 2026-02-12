import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import GroupSyncStatus from '@/components/GroupSyncStatus.vue'
import { useGroupSyncStore } from '@/stores/groupSync'
import { ElMessage, ElMessageBox } from 'element-plus'

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn()
    }
  }
})

describe('GroupSyncStatus.vue', () => {
  let wrapper: any
  let syncStore: any

  beforeEach(() => {
    // Create fresh Pinia instance for each test
    setActivePinia(createPinia())
    syncStore = useGroupSyncStore()

    // Mock store methods
    vi.spyOn(syncStore, 'initialize').mockResolvedValue(undefined)
    vi.spyOn(syncStore, 'triggerSync').mockResolvedValue({
      totalCount: 10,
      successCount: 9,
      failureCount: 1,
      durationMs: 1500
    })
    vi.spyOn(syncStore, 'retryFailedGroups').mockResolvedValue({
      totalCount: 1,
      successCount: 1,
      failureCount: 0,
      durationMs: 500
    })
    vi.spyOn(syncStore, 'resetFailureCount').mockResolvedValue(undefined)

    // Set initial store state
    syncStore.lastSyncTime = '2026-02-12T10:00:00Z'
    syncStore.lastSyncSuccessRate = 95.5
    syncStore.lastSyncResult = {
      totalCount: 10,
      successCount: 9,
      failureCount: 1,
      durationMs: 1500
    }
    syncStore.alertGroups = [
      {
        groupId: 1,
        groupName: '测试群组1',
        consecutiveFailureCount: 3,
        failureReason: '连接超时'
      }
    ]
    syncStore.hasAlerts = true
    syncStore.syncInProgress = false
  })

  it('renders properly with sync status', () => {
    wrapper = mount(GroupSyncStatus)

    // Check card title
    expect(wrapper.text()).toContain('群组同步状态')

    // Check sync button
    expect(wrapper.find('button').text()).toContain('立即同步')

    // Check sync statistics
    expect(wrapper.text()).toContain('95.5%')
    expect(wrapper.text()).toContain('1500ms')
    expect(wrapper.text()).toContain('总计')
    expect(wrapper.text()).toContain('成功')
    expect(wrapper.text()).toContain('失败')
  })

  it('displays "从未同步" when no sync time', () => {
    syncStore.lastSyncTime = null
    wrapper = mount(GroupSyncStatus)

    expect(wrapper.text()).toContain('从未同步')
  })

  it('displays alert badge with correct count', () => {
    wrapper = mount(GroupSyncStatus)

    expect(wrapper.text()).toContain('查看详情')
    // Badge should show 1 alert group
    const badge = wrapper.find('.el-badge')
    expect(badge.exists()).toBe(true)
  })

  it('disables sync button when sync is in progress', async () => {
    syncStore.syncInProgress = true
    wrapper = mount(GroupSyncStatus)

    const syncButton = wrapper.find('button')
    expect(syncButton.attributes('disabled')).toBeDefined()
    expect(syncButton.text()).toContain('同步中...')
  })

  it('shows correct success rate tag type', () => {
    // Test success rate >= 95 (should be success type)
    syncStore.lastSyncSuccessRate = 96.0
    wrapper = mount(GroupSyncStatus)
    expect(wrapper.html()).toContain('el-tag--success')

    // Test success rate 80-95 (should be warning type)
    syncStore.lastSyncSuccessRate = 85.0
    wrapper = mount(GroupSyncStatus)
    expect(wrapper.html()).toContain('el-tag--warning')

    // Test success rate < 80 (should be danger type)
    syncStore.lastSyncSuccessRate = 70.0
    wrapper = mount(GroupSyncStatus)
    expect(wrapper.html()).toContain('el-tag--danger')
  })

  it('triggers sync when sync button is clicked and confirmed', async () => {
    // Mock confirmation dialog to resolve
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as any)

    wrapper = mount(GroupSyncStatus)

    // Click sync button
    await wrapper.find('button').trigger('click')

    // Wait for async operations
    await wrapper.vm.$nextTick()

    // Verify confirmation dialog was shown
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确定要立即同步所有活跃群组吗？',
      '确认同步',
      expect.any(Object)
    )

    // Verify sync was triggered
    expect(syncStore.triggerSync).toHaveBeenCalled()

    // Verify success message
    expect(ElMessage.success).toHaveBeenCalledWith(
      expect.stringContaining('同步完成')
    )
  })

  it('does not trigger sync when confirmation is cancelled', async () => {
    // Mock confirmation dialog to reject
    vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

    wrapper = mount(GroupSyncStatus)

    // Click sync button
    await wrapper.find('button').trigger('click')

    // Wait for async operations
    await wrapper.vm.$nextTick()

    // Verify sync was not triggered
    expect(syncStore.triggerSync).not.toHaveBeenCalled()

    // Verify no error message for cancel
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('shows error message when sync fails', async () => {
    // Mock confirmation to resolve
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as any)

    // Mock sync to fail
    const error = new Error('网络错误')
    vi.spyOn(syncStore, 'triggerSync').mockRejectedValue(error)

    wrapper = mount(GroupSyncStatus)

    // Click sync button
    await wrapper.find('button').trigger('click')

    // Wait for async operations
    await wrapper.vm.$nextTick()

    // Verify error message
    expect(ElMessage.error).toHaveBeenCalledWith('网络错误')
  })

  it('opens alert dialog when "查看详情" is clicked', async () => {
    wrapper = mount(GroupSyncStatus)

    // Initially dialog should not be visible
    expect(wrapper.vm.showAlertDialog).toBe(false)

    // Click "查看详情" button
    const detailButton = wrapper.find('button[text]')
    await detailButton.trigger('click')

    // Dialog should now be visible
    expect(wrapper.vm.showAlertDialog).toBe(true)
  })

  it('retries failed groups when retry button is clicked', async () => {
    wrapper = mount(GroupSyncStatus)

    // Open alert dialog
    wrapper.vm.showAlertDialog = true
    await wrapper.vm.$nextTick()

    // Find and click retry button
    const retryButton = wrapper.findAll('button').find((btn: any) =>
      btn.text().includes('重试所有失败群组')
    )
    expect(retryButton).toBeDefined()

    await retryButton!.trigger('click')
    await wrapper.vm.$nextTick()

    // Verify retry was called
    expect(syncStore.retryFailedGroups).toHaveBeenCalled()

    // Verify success message
    expect(ElMessage.success).toHaveBeenCalledWith(
      expect.stringContaining('重试完成')
    )
  })

  it('closes alert dialog when all retries succeed', async () => {
    // Mock retry to succeed with 0 failures
    vi.spyOn(syncStore, 'retryFailedGroups').mockResolvedValue({
      totalCount: 1,
      successCount: 1,
      failureCount: 0,
      durationMs: 500
    })

    wrapper = mount(GroupSyncStatus)

    // Open alert dialog
    wrapper.vm.showAlertDialog = true
    await wrapper.vm.$nextTick()

    // Trigger retry
    await wrapper.vm.handleRetryFailed()
    await wrapper.vm.$nextTick()

    // Dialog should be closed
    expect(wrapper.vm.showAlertDialog).toBe(false)
  })

  it('keeps alert dialog open when some retries fail', async () => {
    // Mock retry with failures
    vi.spyOn(syncStore, 'retryFailedGroups').mockResolvedValue({
      totalCount: 2,
      successCount: 1,
      failureCount: 1,
      durationMs: 500
    })

    wrapper = mount(GroupSyncStatus)

    // Open alert dialog
    wrapper.vm.showAlertDialog = true
    await wrapper.vm.$nextTick()

    // Trigger retry
    await wrapper.vm.handleRetryFailed()
    await wrapper.vm.$nextTick()

    // Dialog should remain open
    expect(wrapper.vm.showAlertDialog).toBe(true)
  })

  it('resets failure count when reset button is clicked', async () => {
    wrapper = mount(GroupSyncStatus)

    // Call reset handler directly
    await wrapper.vm.handleResetFailure(1)
    await wrapper.vm.$nextTick()

    // Verify reset was called
    expect(syncStore.resetFailureCount).toHaveBeenCalledWith(1)

    // Verify success message
    expect(ElMessage.success).toHaveBeenCalledWith('失败计数已重置')
  })

  it('shows error when reset failure count fails', async () => {
    // Mock reset to fail
    const error = new Error('重置失败')
    vi.spyOn(syncStore, 'resetFailureCount').mockRejectedValue(error)

    wrapper = mount(GroupSyncStatus)

    // Call reset handler
    await wrapper.vm.handleResetFailure(1)
    await wrapper.vm.$nextTick()

    // Verify error message
    expect(ElMessage.error).toHaveBeenCalledWith('重置失败')
  })

  it('disables alert detail button when no alerts', () => {
    syncStore.hasAlerts = false
    wrapper = mount(GroupSyncStatus)

    const detailButton = wrapper.find('button[text]')
    expect(detailButton.attributes('disabled')).toBeDefined()
  })

  it('formats time correctly using formatDistanceToNow', () => {
    wrapper = mount(GroupSyncStatus)

    // The formatTime function should format the time
    const formattedTime = wrapper.vm.formatTime('2026-02-12T10:00:00Z')
    expect(formattedTime).toBeTruthy()
    expect(typeof formattedTime).toBe('string')
  })

  it('initializes store on mount', () => {
    wrapper = mount(GroupSyncStatus)

    expect(syncStore.initialize).toHaveBeenCalled()
  })
})
