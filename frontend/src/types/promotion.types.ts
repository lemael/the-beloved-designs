export interface Promotion {
  id: number
  label: string
  discountPercent: number
  startsAt: string | null
  endsAt: string | null
  isActive: boolean
}
