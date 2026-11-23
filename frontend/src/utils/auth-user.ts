import type { AuthResponse, AuthUser } from '@/types/user.types'

export function toAuthUser(response: AuthResponse): AuthUser {
  return {
    phone: response.phone,
    firstName: response.firstName,
    lastName: response.lastName,
    roles: response.roles ?? [],
  }
}
