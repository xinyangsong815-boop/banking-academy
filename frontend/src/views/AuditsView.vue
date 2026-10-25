<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { dateTime } from '../utils/format'
import PageHeader from '../components/PageHeader.vue'
import DataTable from '../components/DataTable.vue'
import Pagination from '../components/Pagination.vue'
const page = ref(1)
const { data, loading, error, reload } = useResource(() => bankApi.audits(page.value))
watch(page, reload)
onMounted(reload)
const names: Record<string, string> = {
  CUSTOMER_CREATE: '新建客户',
  CUSTOMER_UPDATE: '修改客户',
  ACCOUNT_OPEN: '开立账户',
  ACCOUNT_STATUS_CHANGE: '账户状态变更',
  TRANSFER_SUBMIT: '提交转账',
  LOAN_APPLY: '贷款申请',
  LOAN_REVIEW: '贷款审核',
  LOAN_DISBURSE: '贷款放款',
  PRODUCT_SUBSCRIBE: '产品申购',
}
const columns = [
  { key: 'id', label: '记录编号' },
  { key: 'actor', label: '操作人员' },
  { key: 'action', label: '业务操作' },
  { key: 'traceId', label: '请求追踪号' },
  { key: 'createdAt', label: '记录时间' },
]
</script>
<template>
  <PageHeader
    title="操作审计"
    description="从操作人员和请求追踪号，连接页面行为与后端日志。"
    ><button
      class="button secondary"
      :disabled="loading"
      @click="reload"
    >
      ↻ 刷新记录
    </button></PageHeader
  >
  <div class="info-strip">
    审计切面记录成功返回的业务调用及幂等重放，不记录密码、令牌和完整请求参数。失败审计与可靠投递留作进阶扩展。
  </div>
  <section class="panel">
    <DataTable
      :rows="data?.items ?? []"
      :columns="columns"
      :loading="loading"
      :error="error"
      @retry="reload"
      ><template #actor="{ row }"
        ><strong>{{ row.actor }}</strong></template
      ><template #action="{ row }"
        >{{ names[row.action] ?? row.action
        }}<small class="cell-sub">{{ row.action }}</small></template
      ><template #traceId="{ row }"
        ><code>{{ row.traceId }}</code></template
      ><template #createdAt="{ row }">{{ dateTime(row.createdAt) }}</template></DataTable
    ><Pagination
      :page="page"
      :total="data?.total ?? 0"
      :loading="loading"
      @change="page = $event"
    />
  </section>
</template>
