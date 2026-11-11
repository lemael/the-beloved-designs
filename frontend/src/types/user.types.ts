export interface AuthUser {
  phone: string
  firstName: string
  lastName: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  phone: string
  firstName: string
  lastName: string
}

export interface RegisterPayload {
  phone: string
  password: string
  firstName: string
  lastName: string
}

export interface LoginPayload {
  phone: string
  password: string
}
