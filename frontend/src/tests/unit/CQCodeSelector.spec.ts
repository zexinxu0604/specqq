/**
 * CQCodeSelector Component Tests
 *
 * Unit tests for CQCodeSelector.vue component using Vitest and Vue Test Utils.
 *
 * @author Claude Code
 * @since 2026-02-11
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import CQCodeSelector from '@/components/CQCodeSelector.vue'
import { useCQCodeStore } from '@/stores/cqcode.store'
import type { CQCodePattern } from '@/types/cqcode'
import { CQCodeType } from '@/types/cqcode'

// Mock Element Plus components
vi.mock('element-plus', () => ({
  ElCascader: {
    name: 'ElCascader',
    template: '<div class="el-cascader"><slot /></div>',
    props: ['modelValue', 'options', 'props', 'placeholder', 'disabled', 'clearable', 'filterable'],
    emits: ['change']
  },
  ElIcon: {
    name: 'ElIcon',
    template: '<span class="el-icon"><slot /></span>'
  },
  ElDivider: {
    name: 'ElDivider',
    template: '<div class="el-divider"><slot /></div>',
    props: ['contentPosition']
  },
  ElForm: {
    name: 'ElForm',
    template: '<form class="el-form"><slot /></form>',
    props: ['model', 'labelWidth']
  },
  ElFormItem: {
    name: 'ElFormItem',
    template: '<div class="el-form-item"><slot /></div>',
    props: ['label', 'required']
  },
  ElInput: {
    name: 'ElInput',
    template: '<input class="el-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue', 'input']
  },
  ElTag: {
    name: 'ElTag',
    template: '<span class="el-tag"><slot /></span>',
    props: ['type', 'size']
  }
}))

// Mock Element Plus icons
vi.mock('@element-plus/icons-vue', () => ({
  Sunny: { name: 'Sunny' },
  Picture: { name: 'Picture' },
  User: { name: 'User' },
  ChatDotRound: { name: 'ChatDotRound' },
  Microphone: { name: 'Microphone' },
  VideoCamera: { name: 'VideoCamera' },
  QuestionFilled: { name: 'QuestionFilled' }
}))

describe('CQCodeSelector', () => {
  let wrapper: VueWrapper<any>
  let cqCodeStore: ReturnType<typeof useCQCodeStore>

  const mockPatterns: CQCodePattern[] = [
    {
      id: 'face-pattern',
      name: 'face_pattern',
      label: '表情模式',
      pattern: '\\[CQ:face,id=(\\d+)\\]',
      type: CQCodeType.FACE,
      example: '[CQ:face,id=123]',
      description: '匹配QQ表情',
      isPredefined: true
    },
    {
      id: 'image-pattern',
      name: 'image_pattern',
      label: '图片模式',
      pattern: '\\[CQ:image,file=([^\\]]+)\\]',
      type: CQCodeType.IMAGE,
      example: '[CQ:image,file=test.jpg]',
      description: '匹配图片消息',
      isPredefined: true,
      paramFilters: [
        {
          name: 'file',
          value: '.*\\.jpg',
          isRegex: true,
          required: false
        }
      ]
    },
    {
      id: 'at-pattern',
      name: 'at_pattern',
      label: '@某人模式',
      pattern: '\\[CQ:at,qq=(\\d+)\\]',
      type: CQCodeType.AT,
      example: '[CQ:at,qq=123456]',
      description: '匹配@某人',
      isPredefined: true
    }
  ]

  beforeEach(() => {
    // Create fresh Pinia instance for each test
    setActivePinia(createPinia())
    cqCodeStore = useCQCodeStore()

    // Mock store state
    cqCodeStore.patterns = mockPatterns
    cqCodeStore.types = [CQCodeType.FACE, CQCodeType.IMAGE, CQCodeType.AT]

    // Mock store methods
    vi.spyOn(cqCodeStore, 'initialize').mockResolvedValue()
    vi.spyOn(cqCodeStore, 'getPatternById').mockImplementation((id: string) => {
      return mockPatterns.find(p => p.id === id) || null
    })
  })

  /**
   * T060: should_PopulateDropdown_When_ComponentMounted
   *
   * Verify that the cascader dropdown is populated with pattern categories
   * and patterns when the component is mounted.
   */
  it('should_PopulateDropdown_When_ComponentMounted', async () => {
    // Arrange & Act
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      }
    })

    await nextTick()

    // Assert
    expect(cqCodeStore.initialize).toHaveBeenCalled()

    // Check that cascader options are computed correctly
    const cascaderOptions = (wrapper.vm as any).cascaderOptions

    expect(cascaderOptions).toBeDefined()
    expect(cascaderOptions.length).toBeGreaterThan(0)

    // Verify structure: categories with children patterns
    const faceCategory = cascaderOptions.find((cat: any) => cat.value === CQCodeType.FACE)
    expect(faceCategory).toBeDefined()
    expect(faceCategory.label).toBe('表情')
    expect(faceCategory.children).toBeDefined()
    expect(faceCategory.children.length).toBe(1)
    expect(faceCategory.children[0].value).toBe('face-pattern')
    expect(faceCategory.children[0].label).toBe('表情模式')
  })

  /**
   * T061: should_EmitPattern_When_OptionSelected
   *
   * Verify that the component emits the selected pattern and updates
   * the model value when a user selects an option from the cascader.
   */
  it('should_EmitPattern_When_OptionSelected', async () => {
    // Arrange
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      }
    })

    await nextTick()

    // Act: Simulate pattern selection
    const cascaderValue = [CQCodeType.FACE, 'face-pattern']
    await (wrapper.vm as any).handlePatternChange(cascaderValue)
    await nextTick()

    // Assert: Check emitted events
    const emittedUpdateModelValue = wrapper.emitted('update:modelValue')
    const emittedChange = wrapper.emitted('change')

    expect(emittedUpdateModelValue).toBeDefined()
    expect(emittedUpdateModelValue![0]).toEqual(['face-pattern'])

    expect(emittedChange).toBeDefined()
    expect(emittedChange![0][0]).toEqual(mockPatterns[0])
  })

  /**
   * should_EmitNull_When_SelectionCleared
   *
   * Verify that the component emits null when the selection is cleared.
   */
  it('should_EmitNull_When_SelectionCleared', async () => {
    // Arrange
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      },
      props: {
        modelValue: 'face-pattern'
      }
    })

    await nextTick()

    // Act: Clear selection
    await (wrapper.vm as any).handlePatternChange([])
    await nextTick()

    // Assert
    const emittedUpdateModelValue = wrapper.emitted('update:modelValue')
    const emittedChange = wrapper.emitted('change')

    expect(emittedUpdateModelValue).toBeDefined()
    expect(emittedUpdateModelValue![0]).toEqual([null])

    expect(emittedChange).toBeDefined()
    expect(emittedChange![0]).toEqual([null])
  })

  /**
   * should_ShowParameterFilters_When_PatternHasFilters
   *
   * Verify that parameter filter inputs are displayed when a pattern
   * with parameter filters is selected.
   */
  it('should_ShowParameterFilters_When_PatternHasFilters', async () => {
    // Arrange
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      }
    })

    await nextTick()

    // Act: Select pattern with parameter filters (image-pattern)
    const cascaderValue = [CQCodeType.IMAGE, 'image-pattern']
    await (wrapper.vm as any).handlePatternChange(cascaderValue)
    await nextTick()

    // Assert: Check that parameter filters section is visible
    const hasParameterFilters = (wrapper.vm as any).hasParameterFilters
    expect(hasParameterFilters).toBe(true)

    // Check that parameter values are initialized
    const parameterValues = (wrapper.vm as any).parameterValues
    expect(parameterValues).toBeDefined()
    expect(parameterValues.file).toBe('.*\\.jpg')
  })

  /**
   * should_EmitParameterChange_When_ParameterValueChanged
   *
   * Verify that parameter-change event is emitted when parameter values change.
   */
  it('should_EmitParameterChange_When_ParameterValueChanged', async () => {
    // Arrange
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      }
    })

    await nextTick()

    // Select pattern with filters
    const cascaderValue = [CQCodeType.IMAGE, 'image-pattern']
    await (wrapper.vm as any).handlePatternChange(cascaderValue)
    await nextTick()

    // Act: Change parameter value
    ;(wrapper.vm as any).parameterValues.file = '.*\\.png'
    await (wrapper.vm as any).handleParameterChange()
    await nextTick()

    // Assert
    const emittedParameterChange = wrapper.emitted('parameter-change')
    expect(emittedParameterChange).toBeDefined()
    expect(emittedParameterChange![0][0]).toEqual({
      file: '.*\\.png'
    })
  })

  /**
   * should_UpdateSelection_When_ModelValueChangesExternally
   *
   * Verify that the component updates its selection when the modelValue
   * prop changes externally (v-model binding).
   */
  it('should_UpdateSelection_When_ModelValueChangesExternally', async () => {
    // Arrange
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      },
      props: {
        modelValue: null
      }
    })

    await nextTick()

    // Act: Update modelValue externally
    await wrapper.setProps({ modelValue: 'face-pattern' })
    await nextTick()

    // Assert: Check that selectedPatternId is updated
    const selectedPatternId = (wrapper.vm as any).selectedPatternId
    expect(selectedPatternId).toEqual([CQCodeType.FACE, 'face-pattern'])
  })

  /**
   * should_BeDisabled_When_DisabledPropIsTrue
   *
   * Verify that the cascader is disabled when the disabled prop is true.
   */
  it('should_BeDisabled_When_DisabledPropIsTrue', async () => {
    // Arrange & Act
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      },
      props: {
        disabled: true
      }
    })

    await nextTick()

    // Assert: Check that cascader receives disabled prop
    const cascader = wrapper.findComponent({ name: 'ElCascader' })
    expect(cascader.props('disabled')).toBe(true)
  })

  /**
   * should_ShowPlaceholder_When_NoSelection
   *
   * Verify that the placeholder text is displayed when no pattern is selected.
   */
  it('should_ShowPlaceholder_When_NoSelection', async () => {
    // Arrange & Act
    wrapper = mount(CQCodeSelector, {
      global: {
        plugins: [createPinia()]
      },
      props: {
        placeholder: '自定义占位符'
      }
    })

    await nextTick()

    // Assert
    const cascader = wrapper.findComponent({ name: 'ElCascader' })
    expect(cascader.props('placeholder')).toBe('自定义占位符')
  })
})
