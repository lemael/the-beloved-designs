/**
 * Listes de produits du menu admin. Une seule vue (AdminProductListView) filtrée par ?list=<key>.
 * "Habits" n'est pas une catégorie en base : il regroupe les catégories femmes + hommes de la boutique.
 * Les slugs doivent exister dans la table categories (voir AppNavbar pour ceux de la boutique).
 */
export interface AdminProductList {
  key: string
  label: string
  menuLabel: string
  categorySlugs: string[]
}

export const ADMIN_PRODUCT_LISTS: AdminProductList[] = [
  { key: 'sacs-a-main', label: 'Sacs à main', menuLabel: 'Liste des sacs à main', categorySlugs: ['sacs-a-main'] },
  { key: 'habits', label: 'Habits', menuLabel: 'Liste des habits', categorySlugs: ['femmes', 'hommes'] },
  {
    key: 'cosmetiques',
    label: 'Produits cosmétiques',
    menuLabel: 'Liste des produits cosmétiques',
    categorySlugs: ['cosmetiques'],
  },
  { key: 'chaussures', label: 'Chaussures', menuLabel: 'Liste des chaussures', categorySlugs: ['chaussures'] },
]

export function findProductList(key: unknown): AdminProductList | undefined {
  return ADMIN_PRODUCT_LISTS.find((list) => list.key === key)
}

/** Seuil "stock bas" par défaut (question encore ouverte, voir project-context.md). */
export const DEFAULT_LOW_STOCK_THRESHOLD = 5
