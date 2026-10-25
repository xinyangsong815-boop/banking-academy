<script setup lang="ts">
defineProps<{ modelValue: string; id: string; min?: string }>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>
<template>
  <div class="money-input">
    <span>¥</span
    ><input
      :id="id"
      :value="modelValue"
      inputmode="decimal"
      required
      pattern="[0-9]{1,12}(\.[0-9]{1,2})?"
      placeholder="0.00"
      title="请输入最多两位小数的正金额"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
  </div>
</template>
