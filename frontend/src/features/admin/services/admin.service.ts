import { api } from '@/services/api'
import type { Page } from '@/types/product.types'
import type {
  AdminDashboard,
  AdminProduct,
  CreateProductPayload,
  CreateRestockPayload,
  Restock,
  RestockStatus,
  StockSummary,
  YearlyReport,
} from '@/types/admin.types'

/** Tous les appels /api/admin/** passent par ici (intercepteur JWT + refresh de services/api.ts). */
export const adminService = {
  async dashboard(threshold?: number): Promise<AdminDashboard> {
    const { data } = await api.get<AdminDashboard>('/admin/dashboard', { params: { threshold } })
    return data
  },

  async listProducts(params: { categories?: string[]; page?: number; size?: number } = {}): Promise<Page<AdminProduct>> {
    const { data } = await api.get<Page<AdminProduct>>('/admin/products', {
      params: { category: params.categories, page: params.page, size: params.size },
      // ?category=a&category=b (format attendu par Spring), pas ?category[]=a
      paramsSerializer: { indexes: null },
    })
    return data
  },

  async createProduct(payload: CreateProductPayload): Promise<AdminProduct> {
    const { data } = await api.post<AdminProduct>('/admin/products', payload)
    return data
  },

  async stockSummary(threshold: number): Promise<StockSummary> {
    const { data } = await api.get<StockSummary>('/admin/stock/summary', { params: { threshold } })
    return data
  },

  async listRestocks(params: { status?: RestockStatus; page?: number; size?: number } = {}): Promise<Page<Restock>> {
    const { data } = await api.get<Page<Restock>>('/admin/restocks', { params })
    return data
  },

  async createRestock(payload: CreateRestockPayload): Promise<Restock> {
    const { data } = await api.post<Restock>('/admin/restocks', payload)
    return data
  },

  async receiveRestock(id: number): Promise<Restock> {
    const { data } = await api.post<Restock>(`/admin/restocks/${id}/receive`)
    return data
  },

  async cancelRestock(id: number): Promise<Restock> {
    const { data } = await api.post<Restock>(`/admin/restocks/${id}/cancel`)
    return data
  },

  async yearlyReport(year: number): Promise<YearlyReport> {
    const { data } = await api.get<YearlyReport>('/admin/reports/yearly', { params: { year } })
    return data
  },
}

/** Message d'erreur lisible à partir d'une réponse ApiError du backend. */
export function apiErrorMessage(error: unknown, fallback = 'Une erreur est survenue.'): string {
  const data = (error as { response?: { data?: { message?: string; details?: string[] | null } } })?.response?.data
  if (!data?.message) return fallback
  return data.details?.length ? `${data.message} : ${data.details.join(' · ')}` : data.message
}
