import { describe, expect, it } from 'vitest'
import { safeRedirect } from '@/utils/redirect'

describe('safeRedirect', () => {
  it.each([
    ['/products/robe-satin', '/products/robe-satin'],
    ['/cart?x=1', '/cart?x=1'],
  ])('garde un chemin interne : %s', (input, expected) => {
    expect(safeRedirect(input)).toBe(expected)
  })

  it.each([
    ['https://site-malveillant.com'],
    ['//site-malveillant.com'],
    ['/\\site-malveillant.com'],
    ['javascript:alert(1)'],
    [''],
    [undefined],
    [['/a', '/b']],
  ])('refuse %s → page d\'accueil', (input) => {
    expect(safeRedirect(input)).toBe('/')
  })
})
