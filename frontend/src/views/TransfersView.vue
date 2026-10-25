<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { useCommand, createIdempotentExecutor } from '../composables/useCommand'
import { useAuthStore } from '../stores/auth'
import { money, dateTime } from '../utils/format'
import type { TransferCommand, Transfer } from '../types/models'
import PageHeader from '../components/PageHeader.vue'
import DataTable from '../components/DataTable.vue'
import Pagination from '../components/Pagination.vue'
import AccountSelect from '../components/AccountSelect.vue'
import MoneyInput from '../components/MoneyInput.vue'
const auth = useAuthStore()
const page = ref(1)
const { data, loading, error, reload } = useResource(() => bankApi.transfers(page.value))
const { pending, run } = useCommand()
const idempotent = createIdempotentExecutor()
const form = reactive<TransferCommand>({
  sourceId: '',
  targetId: '',
  amount: '',
  channel: 'STANDARD',
  remark: '',
})
const receipt = ref<Transfer>()
const selectorVersion = ref(0)
watch(page, reload)
onMounted(reload)
async function submit() {
  const payload = { ...form }
  const result = await run(
    () => idempotent(payload, (key) => bankApi.transfer(payload, key)),
    '转账已完成',
  )
  if (result) {
    receipt.value = result
    form.amount = ''
    selectorVersion.value++
    await reload()
  }
}
const columns = [
  { key: 'id', label: '转账业务号' },
  { key: 'accounts', label: '资金方向' },
  { key: 'amount', label: '转账金额' },
  { key: 'fee', label: '手续费' },
  { key: 'createdAt', label: '完成时间' },
]
</script>
<template>
  <PageHeader
    title="转账结算"
    description="体验一笔完整转账：校验、锁定账户、平衡记账、提交事务。"
  />
  <div class="split-grid">
    <section
      v-if="auth.can('transfer:write')"
      class="panel"
    >
      <div class="panel-heading">
        <h2>发起转账</h2>
        <span class="badge active">人民币 CNY</span>
      </div>
      <form
        class="form-grid"
        @submit.prevent="submit"
      >
        <label for="source">转出账户</label
        ><AccountSelect
          :key="`source-${selectorVersion}`"
          id="source"
          v-model="form.sourceId"
        /><label for="target">转入账户</label
        ><AccountSelect
          :key="`target-${selectorVersion}`"
          id="target"
          v-model="form.targetId"
        /><label for="transfer-amount">转账金额</label
        ><MoneyInput
          id="transfer-amount"
          v-model="form.amount"
        /><label for="channel">转账渠道</label
        ><select
          id="channel"
          v-model="form.channel"
        >
          <option value="STANDARD">标准转账 · 免手续费</option>
          <option value="INSTANT">即时转账 · 0.1%，最低 1 元，最高 20 元</option></select
        ><label for="remark">业务备注</label
        ><input
          id="remark"
          v-model="form.remark"
          maxlength="120"
          placeholder="选填，如：教学转账"
        />
        <p class="form-help">
          单笔上限 20,000 元；单账户每日累计上限 50,000 元。即时转账需额外支付手续费。
        </p>
        <button
          class="button full"
          :disabled="pending || !form.sourceId || !form.targetId"
        >
          {{ pending ? '正在提交…' : '确认转账 →' }}
        </button>
      </form>
    </section>
    <section class="panel transfer-guide">
      <p class="eyebrow">BEHIND THE TRANSACTION</p>
      <h2>一笔转账，六个步骤</h2>
      <ol class="process-list">
        <li><strong>确认身份与权限</strong><span>Bearer 会话 + 方法级授权</span></li>
        <li><strong>识别重复请求</strong><span>幂等键 + 请求指纹 + 唯一约束</span></li>
        <li><strong>锁定与校验账户</strong><span>统一锁顺序 + 状态 / 余额 / 限额规则</span></li>
        <li><strong>计算手续费</strong><span>策略注册表选择渠道算法</span></li>
        <li><strong>写入平衡分录</strong><span>转出、转入、手续费之和为零</span></li>
        <li><strong>原子提交与通知</strong><span>业务、余额、流水、通知一起提交</span></li>
      </ol>
      <div
        v-if="receipt"
        class="receipt"
        role="status"
      >
        <span class="badge active">转账成功</span>
        <h3>¥ {{ money(receipt.amount) }}</h3>
        <p>手续费 ¥ {{ money(receipt.fee) }}</p>
        <code>{{ receipt.id }}</code>
        <p>可前往账户中心核对转出和转入流水。</p>
      </div>
    </section>
  </div>
  <section class="panel">
    <div class="panel-heading">
      <h2>转账记录</h2>
      <button
        class="text-button"
        :disabled="loading"
        @click="reload"
      >
        刷新
      </button>
    </div>
    <DataTable
      :rows="data?.items ?? []"
      :columns="columns"
      :loading="loading"
      :error="error"
      @retry="reload"
      ><template #id="{ row }"
        ><code :title="row.id">{{ row.id.slice(0, 8) }}…</code
        ><small class="cell-sub">{{
          row.channel === 'STANDARD' ? '标准转账' : '即时转账'
        }}</small></template
      ><template #accounts="{ row }"
        ><code>{{ row.sourceId.slice(0, 8) }} → {{ row.targetId.slice(0, 8) }}</code></template
      ><template #amount="{ row }"
        ><strong class="amount">¥ {{ money(row.amount) }}</strong></template
      ><template #fee="{ row }">{{ money(row.fee) }}</template
      ><template #createdAt="{ row }">{{ dateTime(row.createdAt) }}</template></DataTable
    ><Pagination
      :page="page"
      :total="data?.total ?? 0"
      :loading="loading"
      @change="page = $event"
    />
  </section>
</template>
