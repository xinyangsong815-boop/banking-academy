<script setup lang="ts">
defineProps<{ value: string }>()
const labels: Record<string, string> = {
  ACTIVE: '正常',
  FROZEN: '已冻结',
  CLOSED: '已销户',
  SUBMITTED: '待审批',
  APPROVED: '待放款',
  REJECTED: '已拒绝',
  DISBURSED: '已放款',
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
}
</script>
<template>
  <span
    class="badge"
    :class="value.toLowerCase()"
    >{{ labels[value] ?? value }}</span
  >
</template>
