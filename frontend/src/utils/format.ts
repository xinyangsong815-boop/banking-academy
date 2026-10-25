/** 展示用字符串分组，金额不经过 Number。负号和小数位均保留。 */
export function money(value: string | undefined): string {
  if (value === undefined) return '—'
  const [integer, decimal = ''] = value.split('.')
  return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}.${decimal.padEnd(2, '0')}`
}
export function dateTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}
export const riskName = { LOW: '稳健型', MEDIUM: '平衡型', HIGH: '进取型' }
export const roleName = { ADMIN: '管理员', TELLER: '业务柜员', AUDITOR: '审计员' }
