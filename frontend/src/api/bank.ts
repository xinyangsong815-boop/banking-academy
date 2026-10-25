import { post, put, request } from './http'
import type {
  Account,
  Audit,
  Customer,
  Dashboard,
  Entry,
  Loan,
  LoanCommand,
  Notice,
  Page,
  Product,
  RepaymentPeriod,
  Staff,
  Subscription,
  Transfer,
  TransferCommand,
} from '../types/models'

const query = (page: number, size = 10) => `page=${page}&size=${size}`
export const authApi = {
  login: (username: string, password: string) =>
    post<{ token: string; expiresAt: string; user: Staff }>('/auth/login', { username, password }),
  me: () => request<{ user: Staff; permissions: string[] }>('/auth/me'),
  logout: () => post<void>('/auth/logout'),
}
export const bankApi = {
  dashboard: () => request<Dashboard>('/dashboard'),
  customers: (page = 1, keyword = '', size = 10) =>
    request<Page<Customer>>(
      `/customers?${query(page, size)}&keyword=${encodeURIComponent(keyword)}`,
    ),
  saveCustomer: (data: Pick<Customer, 'name' | 'phone' | 'email' | 'riskLevel'>, id?: number) =>
    id ? put<Customer>(`/customers/${id}`, data) : post<Customer>('/customers', data),
  accounts: (page = 1, customerId?: number, size = 10) =>
    request<Page<Account>>(
      `/accounts?${query(page, size)}${customerId ? `&customerId=${customerId}` : ''}`,
    ),
  openAccount: (customerId: number, name: string) =>
    post<Account>('/accounts', { customerId, name }),
  accountStatus: (id: string, status: Account['status']) =>
    put<Account>(`/accounts/${id}/status`, { status }),
  entries: (id: string, page = 1) => request<Page<Entry>>(`/accounts/${id}/entries?${query(page)}`),
  transfers: (page = 1) => request<Page<Transfer>>(`/transfers?${query(page)}`),
  transfer: (data: TransferCommand, key: string) => post<Transfer>('/transfers', data, key),
  loans: (page = 1) => request<Page<Loan>>(`/loans?${query(page)}`),
  applyLoan: (data: LoanCommand) => post<Loan>('/loans', data),
  reviewLoan: (id: string, approved: boolean, note: string) =>
    post<Loan>(`/loans/${id}/review`, { approved, note }),
  disburse: (id: string) => post<Loan>(`/loans/${id}/disburse`),
  plan: (id: string) => request<RepaymentPeriod[]>(`/loans/${id}/plan`),
  products: () => request<Product[]>('/products'),
  subscribe: (data: { productId: number; accountId: string; amount: string }, key: string) =>
    post<Subscription>('/products/subscriptions', data, key),
  subscriptions: (page = 1) =>
    request<Page<Subscription>>(`/products/subscriptions?${query(page)}`),
  notices: (page = 1) => request<Page<Notice>>(`/notifications?${query(page)}`),
  readNotice: (id: number) => put<void>(`/notifications/${id}/read`),
  audits: (page = 1) => request<Page<Audit>>(`/audits?${query(page)}`),
}
