/**
 * Tuiles de la page d'accueil. Les slugs sont ceux des catégories de la boutique (AppNavbar, base de données).
 * Photo : public/images/categories/<image>.jpg — à ajouter ; sans photo, la tuile affiche un visuel de secours.
 */
export interface CategoryTile {
  label: string
  to: string
  image: string
}

export const CATEGORY_TILES: CategoryTile[] = [
  { label: 'Vêtements Femmes', to: '/categories/femmes', image: 'femmes' },
  { label: 'Vêtements Hommes', to: '/categories/hommes', image: 'hommes' },
  { label: 'Chaussures', to: '/categories/chaussures', image: 'chaussures' },
  { label: 'Sacs à main', to: '/categories/sacs-a-main', image: 'sacs-a-main' },
  { label: 'Cosmétiques', to: '/categories/cosmetiques', image: 'cosmetiques' },
  { label: 'Promotions', to: '/promotions', image: 'promotions' },
]
