<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { useCommand } from '../composables/useCommand'
import { useAuthStore } from '../stores/auth'
import { money } from '../utils/format'
import type { Loan, LoanCommand, RepaymentPeriod } from '../types/models'
import PageHeader from '../components/PageHeader.vue'
import DataTable from '../components/DataTable.vue'
import Pagination from '../components/Pagination.vue'
import StatusBadge from '../components/StatusBadge.vue'
import AppModal from '../components/AppModal.vue'
import MoneyInput from '../components/MoneyInput.vue'
import CustomerSelect from '../components/CustomerSelect.vue'
import AccountSelect from '../components/AccountSelect.vue'
const auth = useAuthStore()
const page = ref(1)
const { data, loading, error, reload } = useResource(() => bankApi.loans(page.value))
const { pending, run } = useCommand()
const showApply = ref(false)
const review = ref<Loan>()
const disbursing = ref<Loan>()
const reviewForm = reactive({ approved: true, note: '' })
const selectedPlan = ref<RepaymentPeriod[]>()
const form = reactive<LoanCommand>({
  customerId: 0,
  accountId: '',
  principal: '',
  months: 12,
  repaymentMethod: 'EQUAL_PRINCIPAL',
  purpose: '',
})
watch(page, reload)
watch(
  () => form.customerId,
  () => {
    form.accountId = ''
  },
)
onMounted(reload)
async function apply() {
  const result = await run(() => bankApi.applyLoan({ ...form }), '申请已提交，请由另一位管理员复核')
  if (result) {
    showApply.value = false
    await reload()
  }
}
async function approve() {
  if (!review.value) return
  const result = await run(
    () => bankApi.reviewLoan(review.value!.id, reviewForm.approved, reviewForm.note),
    '审核结果已保存',
  )
  if (result) {
    review.value = undefined
    reviewForm.note = ''
    await reload()
  }
}
async function disburse() {
  if (!disbursing.value) return
  const result = await run(
    () => bankApi.disburse(disbursing.value!.id),
    '贷款已放款，还款计划已生成',
  )
  if (result) {
    disbursing.value = undefined
    await reload()
  }
}
async function showPlan(loan: Loan) {
  const result = await run(() => bankApi.plan(loan.id))
  if (result) selectedPlan.value = result
}
const columns = [
  { key: 'id', label: '贷款业务' },
  { key: 'principal', label: '申请本金' },
  { key: 'months', label: '期限' },
  { key: 'repaymentMethod', label: '还款方式' },
  { key: 'state', label: '当前状态' },
  { key: 'actions', label: '操作' },
]
</script>
<template>
  <PageHeader
    title="贷款工作台"
    description="学习申请、双人复核、放款与还款计划之间的状态约束。"
    ><button
      v-if="auth.can('loan:apply')"
      class="button"
      @click="showApply = true"
    >
      ＋ 提交贷款申请
    </button></PageHeader
  >
  <div class="info-strip">
    演示流程：使用 teller 申请，再切换 admin 审批和放款。教学年利率为 4.8%，上限 200,000 元。
  </div>
  <section class="panel">
    <DataTable
      :rows="data?.items ?? []"
      :columns="columns"
      :loading="loading"
      :error="error"
      @retry="reload"
      ><template #id="{ row }"
        ><strong>客户 #{{ row.customerId }}</strong
        ><code class="cell-sub">{{ row.id.slice(0, 8) }}</code
        ><small class="cell-sub">{{ row.purpose }}</small></template
      ><template #principal="{ row }"
        ><strong class="amount">¥ {{ money(row.principal) }}</strong></template
      ><template #months="{ row }">{{ row.months }} 个月</template
      ><template #repaymentMethod="{ row }">{{
        row.repaymentMethod === 'EQUAL_PRINCIPAL' ? '等额本金' : '等额本息'
      }}</template
      ><template #state="{ row }"
        ><StatusBadge :value="row.state" /><small
          v-if="row.reviewNote"
          class="cell-sub"
          >{{ row.reviewNote }}</small
        ></template
      ><template #actions="{ row }"
        ><div class="button-row">
          <button
            v-if="row.state === 'SUBMITTED' && auth.can('loan:review')"
            class="text-button"
            :disabled="row.applicantId === auth.user?.id || pending"
            @click="review = row"
          >
            审核</button
          ><button
            v-if="row.state === 'APPROVED' && auth.can('loan:disburse')"
            class="text-button"
            :disabled="pending"
            @click="disbursing = row"
          >
            放款</button
          ><button
            v-if="row.state === 'DISBURSED'"
            class="text-button"
            :disabled="pending"
            @click="showPlan(row)"
          >
            还款计划</button
          ><span
            v-if="row.state === 'REJECTED'"
            class="muted"
            >流程已结束</span
          >
        </div></template
      ></DataTable
    ><Pagination
      :page="page"
      :total="data?.total ?? 0"
      :loading="loading"
      @change="page = $event"
    />
  </section>
  <AppModal
    v-if="showApply"
    title="提交贷款申请"
    @close="!pending && (showApply = false)"
    ><form
      class="form-grid"
      @submit.prevent="apply"
    >
      <label for="loan-customer">贷款客户</label
      ><CustomerSelect
        id="loan-customer"
        v-model="form.customerId"
      /><label for="loan-account">收款账户</label
      ><AccountSelect
        v-if="form.customerId"
        id="loan-account"
        v-model="form.accountId"
        :customer-id="form.customerId"
      />
      <p
        v-else
        class="form-help"
      >
        请先选择客户。
      </p>
      <label for="loan-principal">申请本金</label
      ><MoneyInput
        id="loan-principal"
        v-model="form.principal"
      /><label for="months">贷款期限（月）</label
      ><input
        id="months"
        v-model.number="form.months"
        type="number"
        min="1"
        max="60"
        required
      /><label for="repayment">还款方式</label
      ><select
        id="repayment"
        v-model="form.repaymentMethod"
      >
        <option value="EQUAL_PRINCIPAL">等额本金</option>
        <option value="EQUAL_INSTALLMENT">等额本息</option></select
      ><label for="purpose">贷款用途</label
      ><textarea
        id="purpose"
        v-model="form.purpose"
        required
        maxlength="160"
      ></textarea>
      <div class="form-actions">
        <button
          class="button"
          :disabled="pending || !form.customerId || !form.accountId"
        >
          提交申请
        </button>
      </div>
    </form></AppModal
  >
  <AppModal
    v-if="review"
    title="贷款复核"
    @close="!pending && (review = undefined)"
    ><form
      class="form-grid"
      @submit.prevent="approve"
    >
      <p>申请本金：¥ {{ money(review.principal) }} · {{ review.months }} 个月</p>
      <label for="review-result">审核结论</label
      ><select
        id="review-result"
        v-model="reviewForm.approved"
      >
        <option :value="true">批准</option>
        <option :value="false">拒绝</option></select
      ><label for="review-note">审核意见</label
      ><textarea
        id="review-note"
        v-model="reviewForm.note"
        required
        maxlength="200"
      ></textarea>
      <div class="form-actions">
        <button
          class="button"
          :disabled="pending"
        >
          确认审核
        </button>
      </div>
    </form></AppModal
  >
  <AppModal
    v-if="disbursing"
    title="确认放款"
    @close="!pending && (disbursing = undefined)"
    ><p>
      将向收款账户发放 ¥ {{ money(disbursing.principal) }}，并生成
      {{ disbursing.months }} 期教学还款计划。
    </p>
    <div class="form-actions">
      <button
        class="button secondary"
        :disabled="pending"
        @click="disbursing = undefined"
      >
        取消</button
      ><button
        class="button"
        :disabled="pending"
        @click="disburse"
      >
        确认放款
      </button>
    </div></AppModal
  >
  <AppModal
    v-if="selectedPlan"
    title="教学还款计划"
    @close="selectedPlan = undefined"
    ><p class="form-help">本项目实现计划生成，实际还款、逾期和计息结算作为进阶练习。</p>
    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>期数</th>
            <th>还款日期</th>
            <th>本金</th>
            <th>利息</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="period in selectedPlan"
            :key="period.periodNo"
          >
            <td>{{ period.periodNo }}</td>
            <td>{{ period.dueDate }}</td>
            <td>{{ money(period.principal) }}</td>
            <td>{{ money(period.interest) }}</td>
          </tr>
        </tbody>
      </table>
    </div></AppModal
  >
</template>
