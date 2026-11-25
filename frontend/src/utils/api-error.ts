import axios from 'axios'

/** Format des erreurs renvoyées par le backend (except/ApiError.java). */
export interface ApiError {
  status: number
  code: string
  message: string
}

/** Erreur du backend si c'en est une (corps ApiError), sinon null (réseau coupé, erreur JS…). */
export function toApiError(error: unknown): ApiError | null {
  if (!axios.isAxiosError(error)) return null
  const data = error.response?.data as Partial<ApiError> | undefined
  if (!data || typeof data.code !== 'string' || typeof data.message !== 'string') return null
  return { status: error.response?.status ?? 0, code: data.code, message: data.message }
}

/** Message à montrer au client : celui du serveur (déjà en français) ou un message de secours. */
export function errorMessage(error: unknown, fallback: string): string {
  return toApiError(error)?.message ?? fallback
}
