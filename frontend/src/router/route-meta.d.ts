import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** Back-office : rôle ADMIN requis (guard requireAdmin). */
    requiresAdmin?: boolean
    /** Back-office : page affichée sans le menu latéral (connexion). */
    adminBare?: boolean
  }
}
