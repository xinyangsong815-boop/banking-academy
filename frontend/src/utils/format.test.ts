import { describe, expect, it } from 'vitest'
import { money } from './format'

describe('金额展示', () => {
  it('保留大额金额的末位分，不经过浮点运算', () => {
    expect(money('999999999999.99')).toBe('999,999,999,999.99')
    expect(money('-1234.01')).toBe('-1,234.01')
  })
  it('兼容零、整数和加载中的值', () => {
    expect(money('0')).toBe('0.00')
    expect(money('1000.1')).toBe('1,000.10')
    expect(money(undefined)).toBe('—')
  })
})
