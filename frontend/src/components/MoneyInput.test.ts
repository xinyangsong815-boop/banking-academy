import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MoneyInput from './MoneyInput.vue'

describe('金额输入', () => {
  it('把原始十进制字符串传回父组件', async () => {
    const wrapper = mount(MoneyInput, { props: { modelValue: '', id: 'amount' } })
    await wrapper.find('input').setValue('999999999999.99')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['999999999999.99'])
    expect(wrapper.find('input').attributes('inputmode')).toBe('decimal')
  })
  it('使用原生表单约束拒绝三位小数和指数输入', async () => {
    const wrapper = mount(MoneyInput, { props: { modelValue: '', id: 'amount' } })
    const input = wrapper.find('input')
    for (const value of ['1.001', '1e3', '-1']) {
      await input.setValue(value)
      expect(input.element.checkValidity()).toBe(false)
    }
    await input.setValue('100.01')
    expect(input.element.checkValidity()).toBe(true)
  })
})
