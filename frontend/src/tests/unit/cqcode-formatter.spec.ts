/**
 * CQ Code Formatter Tests
 *
 * Unit tests for cqcode-formatter.ts utility functions.
 *
 * @author Claude Code
 * @since 2026-02-11
 */
import { describe, it, expect } from 'vitest'
import {
  formatCQCodeType,
  formatCQCodeParams,
  formatCQCode,
  formatCQCodeDetailed,
  getCQCodeIcon,
  getCQCodeColor,
  formatCQCodeStats,
  parseCQCodeType,
  hasCQCode,
  countCQCodes
} from '@/utils/cqcode-formatter'
import type { CQCode } from '@/types/cqcode'
import { CQCodeType } from '@/types/cqcode'

describe('cqcode-formatter', () => {
  describe('formatCQCodeType', () => {
    it('should_FormatFaceType_As_ChineseLabel', () => {
      // Act
      const result = formatCQCodeType('face')

      // Assert
      expect(result).toBe('表情')
    })

    it('should_FormatImageType_As_ChineseLabel', () => {
      // Act
      const result = formatCQCodeType('image')

      // Assert
      expect(result).toBe('图片')
    })

    it('should_FormatAtType_As_ChineseLabel', () => {
      // Act
      const result = formatCQCodeType('at')

      // Assert
      expect(result).toBe('@某人')
    })

    it('should_ReturnOriginalType_When_TypeIsUnknown', () => {
      // Act
      const result = formatCQCodeType('unknown_type')

      // Assert
      expect(result).toBe('unknown_type')
    })
  })

  describe('formatCQCodeParams', () => {
    it('should_FormatSingleParameter', () => {
      // Arrange
      const params = { id: '123' }

      // Act
      const result = formatCQCodeParams(params)

      // Assert
      expect(result).toBe('id=123')
    })

    it('should_FormatMultipleParameters_WithComma', () => {
      // Arrange
      const params = { file: 'test.jpg', type: 'show' }

      // Act
      const result = formatCQCodeParams(params)

      // Assert
      expect(result).toBe('file=test.jpg, type=show')
    })

    it('should_ReturnEmptyString_When_NoParameters', () => {
      // Arrange
      const params = {}

      // Act
      const result = formatCQCodeParams(params)

      // Assert
      expect(result).toBe('')
    })
  })

  describe('formatCQCode', () => {
    it('should_FormatCQCode_WithParameters', () => {
      // Arrange
      const code: CQCode = {
        type: 'face',
        params: { id: '123' },
        rawText: '[CQ:face,id=123]'
      }

      // Act
      const result = formatCQCode(code)

      // Assert
      expect(result).toBe('表情 (id=123)')
    })

    it('should_FormatCQCode_WithoutParameters', () => {
      // Arrange
      const code: CQCode = {
        type: 'face',
        params: {},
        rawText: '[CQ:face]'
      }

      // Act
      const result = formatCQCode(code)

      // Assert
      expect(result).toBe('表情')
    })

    it('should_FormatImageCQCode', () => {
      // Arrange
      const code: CQCode = {
        type: 'image',
        params: { file: 'test.jpg' },
        rawText: '[CQ:image,file=test.jpg]'
      }

      // Act
      const result = formatCQCode(code)

      // Assert
      expect(result).toBe('图片 (file=test.jpg)')
    })
  })

  describe('formatCQCodeDetailed', () => {
    it('should_FormatDetailedCQCode_WithAllInformation', () => {
      // Arrange
      const code: CQCode = {
        type: 'face',
        params: { id: '123' },
        rawText: '[CQ:face,id=123]'
      }

      // Act
      const result = formatCQCodeDetailed(code)

      // Assert
      expect(result).toBe('表情: id=123 | 原文: [CQ:face,id=123]')
    })

    it('should_IncludeRawText_InDetailedFormat', () => {
      // Arrange
      const code: CQCode = {
        type: 'image',
        params: { file: 'test.jpg', type: 'flash' },
        rawText: '[CQ:image,file=test.jpg,type=flash]'
      }

      // Act
      const result = formatCQCodeDetailed(code)

      // Assert
      expect(result).toContain('原文: [CQ:image,file=test.jpg,type=flash]')
    })
  })

  describe('getCQCodeIcon', () => {
    it('should_ReturnSunnyIcon_ForFaceType', () => {
      // Act
      const result = getCQCodeIcon('face')

      // Assert
      expect(result).toBe('Sunny')
    })

    it('should_ReturnPictureIcon_ForImageType', () => {
      // Act
      const result = getCQCodeIcon('image')

      // Assert
      expect(result).toBe('Picture')
    })

    it('should_ReturnUserIcon_ForAtType', () => {
      // Act
      const result = getCQCodeIcon('at')

      // Assert
      expect(result).toBe('User')
    })

    it('should_ReturnQuestionFilledIcon_ForUnknownType', () => {
      // Act
      const result = getCQCodeIcon('unknown')

      // Assert
      expect(result).toBe('QuestionFilled')
    })
  })

  describe('getCQCodeColor', () => {
    it('should_ReturnWarning_ForFaceType', () => {
      // Act
      const result = getCQCodeColor('face')

      // Assert
      expect(result).toBe('warning')
    })

    it('should_ReturnSuccess_ForImageType', () => {
      // Act
      const result = getCQCodeColor('image')

      // Assert
      expect(result).toBe('success')
    })

    it('should_ReturnInfo_ForReplyType', () => {
      // Act
      const result = getCQCodeColor('reply')

      // Assert
      expect(result).toBe('info')
    })

    it('should_ReturnEmpty_ForUnknownType', () => {
      // Act
      const result = getCQCodeColor('unknown')

      // Assert
      expect(result).toBe('')
    })
  })

  describe('formatCQCodeStats', () => {
    it('should_FormatStatistics_WithMultipleTypes', () => {
      // Arrange
      const countByType = {
        face: 3,
        image: 2,
        at: 1
      }

      // Act
      const result = formatCQCodeStats(countByType)

      // Assert
      expect(result).toBe('表情×3, 图片×2, @某人×1')
    })

    it('should_ExcludeZeroCounts', () => {
      // Arrange
      const countByType = {
        face: 3,
        image: 0,
        at: 1
      }

      // Act
      const result = formatCQCodeStats(countByType)

      // Assert
      expect(result).toBe('表情×3, @某人×1')
      expect(result).not.toContain('图片')
    })

    it('should_ReturnEmptyString_When_AllCountsAreZero', () => {
      // Arrange
      const countByType = {
        face: 0,
        image: 0,
        at: 0
      }

      // Act
      const result = formatCQCodeStats(countByType)

      // Assert
      expect(result).toBe('')
    })
  })

  describe('parseCQCodeType', () => {
    it('should_ParseFaceType_FromRawText', () => {
      // Arrange
      const rawText = '[CQ:face,id=123]'

      // Act
      const result = parseCQCodeType(rawText)

      // Assert
      expect(result).toBe('face')
    })

    it('should_ParseImageType_FromRawText', () => {
      // Arrange
      const rawText = '[CQ:image,file=test.jpg]'

      // Act
      const result = parseCQCodeType(rawText)

      // Assert
      expect(result).toBe('image')
    })

    it('should_ReturnOther_When_TypeCannotBeParsed', () => {
      // Arrange
      const rawText = 'Not a CQ code'

      // Act
      const result = parseCQCodeType(rawText)

      // Assert
      expect(result).toBe('other')
    })

    it('should_HandleTypeWithUnderscore', () => {
      // Arrange
      const rawText = '[CQ:custom_type,param=value]'

      // Act
      const result = parseCQCodeType(rawText)

      // Assert
      expect(result).toBe('custom_type')
    })
  })

  describe('hasCQCode', () => {
    it('should_ReturnTrue_When_TextContainsCQCode', () => {
      // Arrange
      const text = 'Hello[CQ:face,id=123]World'

      // Act
      const result = hasCQCode(text)

      // Assert
      expect(result).toBe(true)
    })

    it('should_ReturnFalse_When_TextDoesNotContainCQCode', () => {
      // Arrange
      const text = 'Hello World'

      // Act
      const result = hasCQCode(text)

      // Assert
      expect(result).toBe(false)
    })

    it('should_ReturnTrue_When_TextContainsMultipleCQCodes', () => {
      // Arrange
      const text = '[CQ:face,id=123][CQ:image,file=test.jpg]'

      // Act
      const result = hasCQCode(text)

      // Assert
      expect(result).toBe(true)
    })

    it('should_ReturnTrue_When_CQCodeHasNoParameters', () => {
      // Arrange
      const text = 'Test[CQ:face]Message'

      // Act
      const result = hasCQCode(text)

      // Assert
      expect(result).toBe(true)
    })
  })

  describe('countCQCodes', () => {
    it('should_ReturnZero_When_NoCQCodes', () => {
      // Arrange
      const text = 'Hello World'

      // Act
      const result = countCQCodes(text)

      // Assert
      expect(result).toBe(0)
    })

    it('should_CountSingleCQCode', () => {
      // Arrange
      const text = 'Hello[CQ:face,id=123]World'

      // Act
      const result = countCQCodes(text)

      // Assert
      expect(result).toBe(1)
    })

    it('should_CountMultipleCQCodes', () => {
      // Arrange
      const text = 'Hello[CQ:face,id=123]World[CQ:image,file=test.jpg]'

      // Act
      const result = countCQCodes(text)

      // Assert
      expect(result).toBe(2)
    })

    it('should_CountCQCodesWithoutParameters', () => {
      // Arrange
      const text = '[CQ:face][CQ:image][CQ:at]'

      // Act
      const result = countCQCodes(text)

      // Assert
      expect(result).toBe(3)
    })

    it('should_CountMixedCQCodes', () => {
      // Arrange
      const text = 'Start[CQ:face,id=1]Middle[CQ:image]End[CQ:at,qq=123]'

      // Act
      const result = countCQCodes(text)

      // Assert
      expect(result).toBe(3)
    })
  })
})
