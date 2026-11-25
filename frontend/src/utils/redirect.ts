/**
 * Destination après connexion/inscription (paramètre ?redirect= posé par requireAuth ou la fiche produit).
 * Uniquement un chemin interne : refuse les URL externes (« //site.com », « https://… ») pour éviter
 * une redirection ouverte vers un site tiers.
 */
export function safeRedirect(redirect: unknown, fallback = '/'): string {
  if (typeof redirect !== 'string') return fallback
  if (!redirect.startsWith('/') || redirect.startsWith('//') || redirect.startsWith('/\\')) return fallback
  return redirect
}
