export function isValidPhone(value: string): boolean {
  return /^\+[1-9]\d{6,14}$/.test(value)
}

export function isStrongEnoughPassword(value: string): boolean {
  return value.length >= 8
}
