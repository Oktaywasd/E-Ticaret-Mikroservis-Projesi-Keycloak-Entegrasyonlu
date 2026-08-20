import { Routes, Route } from 'react-router-dom';

// Layouts
import { PublicLayout } from '@/shared/layout/PublicLayout';
import { AdminLayout } from '@/shared/layout/AdminLayout';

// Guards
import { ProtectedRoute } from '@/shared/guards/ProtectedRoute';
import { RoleBasedRoute } from '@/shared/guards/RoleBasedRoute';

// Pages — Public
import { HomePage } from '@/features/products/HomePage';
import { ProductsPage } from '@/features/products/ProductsPage';
import { ProductDetailPage } from '@/features/products/ProductDetailPage';
import { CategoriesPage } from '@/features/products/CategoriesPage';
import { CartPage } from '@/features/cart/CartPage';
import ReelsFeedPage from '@/pages/ReelsFeedPage';

// Pages — Auth
import { CallbackPage } from '@/features/auth/CallbackPage';
import { UnauthorizedPage } from '@/features/auth/UnauthorizedPage';
import { NotFoundPage } from '@/features/auth/NotFoundPage';

// Pages — Protected Customer (FAZ 2 & FAZ 3)
import { ProfilePage } from '@/features/crm/ProfilePage';
import { CheckoutPage } from '@/features/orders/CheckoutPage';
import { CustomerOrdersPage } from '@/features/orders/CustomerOrdersPage';
import { OrderDetailPage } from '@/features/orders/OrderDetailPage';

// Pages — Admin / Seller (FAZ 1, 2, 3)
import { AdminDashboardPage } from '@/features/orders/AdminDashboardPage';
import { AdminProductsPage } from '@/features/products/AdminProductsPage';
import { ProductFormPage } from '@/features/products/ProductFormPage';
import { AdminCategoriesPage } from '@/features/products/AdminCategoriesPage';
import { AdminUsersPage } from '@/features/crm/AdminUsersPage';
import { AdminOrdersPage } from '@/features/orders/AdminOrdersPage';
import AdminReelsPage from '@/pages/admin/AdminReelsPage';

function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-2">{title}</h1>
      <p className="text-muted-foreground">Bu sayfa ilerleyen bir fazda tamamlanacak.</p>
    </div>
  );
}

export function AppRouter() {
  return (
    <Routes>
      {/* ─── OIDC Callback ─────────────────────────────── */}
      <Route path="/callback" element={<CallbackPage />} />

      {/* ─── Public Routes ──────────────────────────────── */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/categories" element={<CategoriesPage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/reels" element={<ReelsFeedPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* ─── Protected Customer Routes ──────────────────── */}
      <Route
        element={
          <ProtectedRoute>
            <PublicLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/profile" element={<ProfilePage />} />
        
        {/* FAZ 3 — Orders */}
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/orders" element={<CustomerOrdersPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
      </Route>

      {/* ─── Admin / Seller Routes ──────────────────────── */}
      <Route
        element={
          <ProtectedRoute requiredRoles={['ROLE_ADMIN', 'ADMIN', 'SELLER'] as any}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/admin" element={<AdminDashboardPage />} />

        {/* FAZ 1 — Products */}
        <Route path="/admin/products" element={<AdminProductsPage />} />
        <Route path="/admin/products/new" element={<ProductFormPage />} />
        <Route path="/admin/products/:id/edit" element={<ProductFormPage />} />
        <Route path="/admin/categories" element={<AdminCategoriesPage />} />

        {/* FAZ 3 — Admin Orders */}
        <Route path="/admin/orders" element={<AdminOrdersPage />} />

        {/* Medya & Pazarlama */}
        <Route path="/admin/reels" element={<AdminReelsPage />} />

        {/* FAZ 2 — Admin-only */}
        <Route
          path="/admin/users"
          element={
            <RoleBasedRoute roles={['ROLE_ADMIN']}>
              <AdminUsersPage />
            </RoleBasedRoute>
          }
        />

        <Route
          path="/admin/analytics"
          element={
            <RoleBasedRoute roles={['ROLE_ADMIN']}>
              <PlaceholderPage title="Analitik" />
            </RoleBasedRoute>
          }
        />
        <Route path="/admin/settings" element={<PlaceholderPage title="Ayarlar" />} />
      </Route>
    </Routes>
  );
}
