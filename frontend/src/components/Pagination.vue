<script setup lang="ts">
defineProps<{ page: number; total: number; size?: number; loading?: boolean }>()
defineEmits<{ change: [page: number] }>()
</script>
<template>
  <div class="pagination">
    <span class="muted">共 {{ total }} 条 · 第 {{ page }} 页</span>
    <div class="button-row">
      <button
        class="button secondary small"
        :disabled="loading || page <= 1"
        @click="$emit('change', page - 1)"
      >
        上一页
      </button>
      <button
        class="button secondary small"
        :disabled="loading || page * (size ?? 10) >= total"
        @click="$emit('change', page + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>
