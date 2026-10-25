/** 金额在接口中使用十进制字符串，避免 JavaScript Number 参与资金计算。 */
export type Money = string
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export interface Page<T> {
  items: T[]
  total: number
  page: number
  size: number
}
export interface Staff {
  id: number
  username: string
  displayName: string
  role: 'ADMIN' | 'TELLER' | 'AUDITOR'
}
export interface Customer {
  id: number
  name: string
  phone: string
  email: string
  riskLevel: RiskLevel
  createdAt: string
}
export interface Account {
  id: string
  accountNo: string
  customerId: number
  name: string
  balance: Money
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED'
  internalAccount: boolean
  createdAt: string
}
export interface Entry {
  id: number
  bookingId: string
  accountId: string
  amount: Money
  balanceAfter: Money
  kind: string
  description: string
  createdAt: string
}
export interface Transfer {
  id: string
  sourceId: string
  targetId: string
  amount: Money
  fee: Money
  channel: 'STANDARD' | 'INSTANT'
  remark: string
  actorId: number
  createdAt: string
}
export interface TransferCommand {
  sourceId: string
  targetId: string
  amount: Money
  channel: Transfer['channel']
  remark: string
}
export interface Loan {
  id: string
  customerId: number
  accountId: string
  principal: Money
  months: number
  annualRate: string
  repaymentMethod: 'EQUAL_PRINCIPAL' | 'EQUAL_INSTALLMENT'
  purpose: string
  state: 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'DISBURSED'
  applicantId: number
  reviewerId: number | null
  reviewNote: string | null
  createdAt: string
}
export interface LoanCommand {
  customerId: number
  accountId: string
  principal: Money
  months: number
  repaymentMethod: Loan['repaymentMethod']
  purpose: string
}
export interface RepaymentPeriod {
  periodNo: number
  dueDate: string
  principal: Money
  interest: Money
}
export interface Product {
  id: number
  name: string
  description: string
  riskLevel: RiskLevel
  annualRate: string
  minimumAmount: Money
  remainingQuota: Money
  durationDays: number
  active: boolean
}
export interface Subscription {
  id: string
  productId: number
  accountId: string
  amount: Money
  createdAt: string
}
export interface Notice {
  id: number
  userId: number
  title: string
  content: string
  readAt: string | null
  createdAt: string
}
export interface Audit {
  id: number
  actor: string
  action: string
  traceId: string
  createdAt: string
}
export interface Dashboard {
  customers: number
  accounts: number
  customerBalance: Money
  pendingLoans: number
  transfers: number
  transferredAmount: Money
  dailyVolumes: { date: string; count: number; amount: Money }[]
}
