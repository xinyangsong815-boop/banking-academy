<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { useCommand, createIdempotentExecutor } from '../composables/useCommand'
import { useAuthStore } from '../stores/auth'
import { money, dateTime } from '../utils/format'
import type { Product } from '../types/models'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import DataTable from '../components/DataTable.vue'
import Pagination from '../components/Pagination.vue'
import AppModal from '../components/AppModal.vue'
import AccountSelect from '../components/AccountSelect.vue'
import MoneyInput from '../components/MoneyInput.vue'
const auth = useAuthStore()
const products = useResource(bankApi.products)
const page = ref(1)
const subscriptions = useResource(() => bankApi.subscriptions(page.value))
const { pending, run } = useCommand()
const idempotent = createIdempotentExecutor()
const selected = ref<Product>()
const form = reactive({ accountId: '', amount: '' })
function chooseProduct(product: Product) {
  selected.value = product
  form.amount = product.minimumAmount
}
watch(page, subscriptions.reload)
onMounted(() => {
  void products.reload()
  void subscriptions.reload()
})
async function subscribe() {
  if (!selected.value) return
  const payload = { productId: selected.value.id, ...form }
  const result = await run(
    () => idempotent(payload, (key) => bankApi.subscribe(payload, key)),
    '产品申购成功',
  )
  if (result) {
    selected.value = undefined
    form.amount = ''
    await Promise.all([products.reload(), subscriptions.reload()])
  }
}
const columns = [
  { key: 'id', label: '申购业务号' },
  { key: 'productId', label: '产品' },
  { key: 'accountId', label: '扣款账户' },
  { key: 'amount', label: '申购金额' },
  { key: 'createdAt', label: '申购时间' },
]
</script>
<template>
  <PageHeader
    title="产品与申购"
    description="通过风险适配、额度扣减与资金归集，学习跨模块事务。"
    ><button
      class="button secondary"
      :disabled="products.loading.value"
      @click="products.reload"
    >
      ↻ 刷新产品
    </button></PageHeader
  >
  <div class="info-strip">
    页面收益率为虚构教学参数。申购实现扣款与额度管理，到期兑付作为进阶练习。
  </div>
  <div
    v-if="products.error.value"
    class="error-panel"
  >
    {{ products.error.value }}
  </div>
  <div
    v-if="products.loading.value && !products.data.value"
    class="empty-state"
  >
    正在读取产品…
  </div>
  <section class="product-grid">
    <article
      v-for="product in products.data.value"
      :key="product.id"
      class="panel product-card"
    >
      <div class="product-top">
        <span class="product-symbol">◈</span><StatusBadge :value="product.riskLevel" />
      </div>
      <h2>{{ product.name }}</h2>
      <p class="muted">{{ product.description }}</p>
      <div class="product-rate">
        <strong>{{ (Number(product.annualRate) * 100).toFixed(2) }}<small>%</small></strong
        ><span>教学年化参数</span>
      </div>
      <dl>
        <div>
          <dt>产品期限</dt>
          <dd>{{ product.durationDays }} 天</dd>
        </div>
        <div>
          <dt>起购金额</dt>
          <dd>¥ {{ money(product.minimumAmount) }}</dd>
        </div>
        <div>
          <dt>剩余额度</dt>
          <dd>¥ {{ money(product.remainingQuota) }}</dd>
        </div>
      </dl>
      <button
        v-if="auth.can('product:write')"
        class="button full secondary"
        :disabled="!product.active"
        @click="chooseProduct(product)"
      >
        选择产品 →
      </button>
      <p
        v-else
        class="form-help"
      >
        当前角色可查看产品和申购记录。
      </p>
    </article>
  </section>
  <section class="panel">
    <div class="panel-heading">
      <h2>申购记录</h2>
      <span class="muted">{{ subscriptions.data.value?.total ?? 0 }} 笔</span>
    </div>
    <DataTable
      :rows="subscriptions.data.value?.items ?? []"
      :columns="columns"
      :loading="subscriptions.loading.value"
      :error="subscriptions.error.value"
      @retry="subscriptions.reload"
      ><template #id="{ row }"
        ><code :title="row.id">{{ row.id.slice(0, 8) }}</code></template
      ><template #productId="{ row }">{{
        products.data.value?.find((product) => product.id === row.productId)?.name ?? row.productId
      }}</template
      ><template #accountId="{ row }"
        ><code :title="row.accountId">{{ row.accountId.slice(0, 8) }}</code></template
      ><template #amount="{ row }"
        ><strong class="amount">¥ {{ money(row.amount) }}</strong></template
      ><template #createdAt="{ row }">{{ dateTime(row.createdAt) }}</template></DataTable
    ><Pagination
      :page="page"
      :total="subscriptions.data.value?.total ?? 0"
      :loading="subscriptions.loading.value"
      @change="page = $event"
    />
  </section>
  <AppModal
    v-if="selected"
    :title="`申购 ${selected.name}`"
    @close="!pending && (selected = undefined)"
    ><form
      class="form-grid"
      @submit.prevent="subscribe"
    >
      <label for="subscription-account">扣款账户</label
      ><AccountSelect
        id="subscription-account"
        v-model="form.accountId"
      /><label for="subscription-amount">申购金额</label
      ><MoneyInput
        id="subscription-amount"
        v-model="form.amount"
      />
      <p class="form-help">后端会检查客户风险承受能力、起购金额、产品剩余额度与账户余额。</p>
      <div class="form-actions">
        <button
          class="button"
          :disabled="pending || !form.accountId"
        >
          {{ pending ? '正在申购…' : '确认申购' }}
        </button>
      </div>
    </form></AppModal
  >
</template>
