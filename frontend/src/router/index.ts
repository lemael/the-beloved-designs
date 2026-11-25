import { createRouter, createWebHistory } from 'vue-router'
import { requireAdmin, requireAuth } from './guards'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('@/views/ProductListView.vue'),
    },
    {
      path: '/products/:slug',
      name: 'product-detail',
      component: () => import('@/views/ProductDetailView.vue'),
    },
    {
      path: '/categories/:slug',
      name: 'category',
      component: () => import('@/views/CategoryView.vue'),
    },
    {
      path: '/promotions',
      name: 'promotions',
      component: () => import('@/views/PromotionsView.vue'),
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('@/views/CartView.vue'),
      // Le panier est lié au compte (API /api/cart protégée)
      beforeEnter: requireAuth,
    },
    {
      path: '/checkout',
      name: 'checkout',
      component: () => import('@/views/CheckoutView.vue'),
      beforeEnter: requireAuth,
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoritesView.vue'),
      beforeEnter: requireAuth,
    },
    {
      path: '/account',
      name: 'account',
      component: () => import('@/views/AccountView.vue'),
    },
    // ---- Back-office (rôle ADMIN) ----
    // Une seule route /admin : la connexion est l'enfant '' (public, affiché sans menu latéral),
    // les autres enfants héritent de meta.requiresAdmin. Deux routes '/admin' distinctes
    // feraient correspondre /admin à la route protégée → boucle de redirection.
    {
      path: '/admin',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-login',
          component: () => import('@/views/admin/AdminLoginView.vue'),
          meta: { requiresAdmin: false, adminBare: true },
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboardView.vue'),
        },
        {
          path: 'products',
          name: 'admin-products',
          component: () => import('@/views/admin/AdminProductListView.vue'),
        },
        {
          path: 'products/new',
          name: 'admin-product-new',
          component: () => import('@/views/admin/AdminProductCreateView.vue'),
        },
        {
          path: 'stock',
          name: 'admin-stock',
          component: () => import('@/views/admin/AdminStockView.vue'),
        },
        {
          path: 'restocks',
          name: 'admin-restocks',
          component: () => import('@/views/admin/AdminRestockView.vue'),
        },
        {
          path: 'reports/yearly',
          name: 'admin-yearly-report',
          component: () => import('@/views/admin/AdminYearlyReportView.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach(requireAdmin)

export default router
