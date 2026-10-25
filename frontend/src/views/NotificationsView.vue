<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { useCommand } from '../composables/useCommand'
import { dateTime } from '../utils/format'
import PageHeader from '../components/PageHeader.vue'
import Pagination from '../components/Pagination.vue'
const page = ref(1)
const { data, loading, error, reload } = useResource(() => bankApi.notices(page.value))
const { pending, run } = useCommand()
watch(page, reload)
onMounted(reload)
async function read(id: number) {
  await run(async () => {
    await bankApi.readNotice(id)
    await reload()
  })
}
</script>
<template>
  <PageHeader
    title="消息中心"
    description="只显示当前员工的通知，体验事件驱动的业务协作。"
    ><button
      class="button secondary"
      :disabled="loading"
      @click="reload"
    >
      ↻ 刷新消息
    </button></PageHeader
  >
  <section class="panel">
    <div
      v-if="error"
      class="error-panel"
    >
      {{ error }}
    </div>
    <div
      v-if="!data?.items.length"
      class="empty-state"
    >
      {{ loading ? '正在读取…' : '暂无消息，完成转账、贷款或产品申购后可在此查看。' }}
    </div>
    <article
      v-for="item in data?.items"
      :key="item.id"
      class="notice-row"
      :class="{ unread: !item.readAt }"
    >
      <span class="notice-symbol">{{ item.readAt ? '◷' : '●' }}</span>
      <div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.content }}</p>
        <small class="muted">{{ dateTime(item.createdAt) }}</small>
      </div>
      <button
        v-if="!item.readAt"
        class="text-button"
        :disabled="pending"
        @click="read(item.id)"
      >
        标记已读</button
      ><span
        v-else
        class="muted"
        >已读</span
      >
    </article>
    <Pagination
      :page="page"
      :total="data?.total ?? 0"
      :loading="loading"
      @change="page = $event"
    />
  </section>
</template>
