/**
 * Bandeaux (hero) des pages catégorie. Une catégorie absente de cette liste n'a pas de bandeau.
 * Photo : public/images/heroes/<image>.jpg (photo à gauche, texte à droite).
 * Pour l'instant les catégories femmes partagent la photo tirée de la maquette (femme.jpg).
 *
 * layout :
 *  - 'photo-left'   : photo à gauche, texte à droite (défaut) ;
 *  - 'photo-center' : texte à gauche, photo au centre, atouts + signature à droite.
 */
export interface CategoryHero {
  subtitle: string
  tagline: [string, string]
  image: string
  layout?: 'photo-left' | 'photo-center'
  /** Liste à puces cochées (colonne de droite, mise en page 'photo-center'). */
  highlights?: string[]
  /** Signature manuscrite sur deux lignes (colonne de droite, mise en page 'photo-center'). */
  signature?: [string, string]
}

export const CATEGORY_HEROES: Record<string, CategoryHero> = {
  hommes: {
    subtitle: 'Vêtements Hommes',
    tagline: ['Affirmez votre style,', 'en toute confiance !'],
    image: 'homme',
    layout: 'photo-center',
    highlights: ['Qualité premium', 'Styles tendance', 'Prix abordables'],
    signature: ['Style', 'sans limites !'],
  },
  femmes: {
    subtitle: 'Vêtements Femmes',
    tagline: ['Affirmez votre style,', 'en toute confiance !'],
    image: 'femme',
  },
  'sacs-a-main': {
    subtitle: 'Sacs à main',
    tagline: ['Affirmez votre style,', 'en toute confiance !'],
    image: 'femme',
  },
  cosmetiques: {
    subtitle: 'Cosmétiques',
    tagline: ['Affirmez votre style,', 'en toute confiance !'],
    image: 'femme',
  },
}
