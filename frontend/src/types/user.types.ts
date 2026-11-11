export interface AuthUser {
  email: string
  firstName: string
  lastName: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  email: string
  firstName: string
  lastName: string
}

export interface RegisterPayload {
  email: string
  password: string
  firstName: string
  lastName: string
  phone?: string
}

export interface LoginPayload {
  email: string
  password: string
}
