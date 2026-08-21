import { useAppAuth } from '@/hooks/useAppAuth';
import { LayoutDashboard, Package, ShoppingBag, Users, Film, AlertTriangle, TrendingUp, XCircle, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAdminOrders } from './useOrderQueries';
import { useAdminUsers } from '@/features/crm/useCrmQueries';
import { useProducts } from '@/features/products/useProductQueries';
import { Skeleton } from '@/components/ui/skeleton';

export function AdminDashboardPage() {
  const auth = useAppAuth();

  // Fetch all necessary data for metrics (using large page size to get all data for simple analytics)
  const { data: ordersData, isLoading: ordersLoading } = useAdminOrders({ size: 1000 });
  const { data: usersData, isLoading: usersLoading } = useAdminUsers();
  const { data: productsData, isLoading: productsLoading } = useProducts({ size: 1000 });

  const rawOrders = ordersData?.content || (ordersData as any)?.orders || (Array.isArray(ordersData) ? ordersData : []);
  const rawProducts = productsData?.content || (productsData as any)?.products || (Array.isArray(productsData) ? productsData : []);
  const rawUsers = Array.isArray(usersData) ? usersData : [];

  // Metrics Calculation
  const totalUsers = rawUsers.length;
  const totalProducts = rawProducts.length;
  const totalOrders = rawOrders.length;

  const completedOrders = rawOrders.filter((o: any) => o.status === 'DELIVERED' || o.orderStatus === 'DELIVERED');
  const cancelledOrders = rawOrders.filter((o: any) => o.status === 'CANCELLED' || o.orderStatus === 'CANCELLED');
  const cancelRate = totalOrders > 0 ? Math.round((cancelledOrders.length / totalOrders) * 100) : 0;

  const totalRevenue = completedOrders.reduce((sum: number, o: any) => {
    const amount = typeof o?.totalAmount === 'object' ? (o?.totalAmount?.amount || 0) : Number(o?.totalAmount || o?.totalPrice || 0);
    return sum + amount;
  }, 0);

  const criticalStockProducts = rawProducts.filter((p: any) => p.stock?.currentStock <= 5);

  const isLoading = ordersLoading || usersLoading || productsLoading;

  const cards = [
    { title: 'Ürünler', icon: <Package className="h-5 w-5" />, to: '/admin/products', color: 'violet' },
    { title: 'Siparişler', icon: <ShoppingBag className="h-5 w-5" />, to: '/admin/orders', color: 'indigo' },
    { title: 'Reels Videoları', icon: <Film className="h-5 w-5" />, to: '/admin/reels', color: 'rose' },
    ...(auth.isAdmin
      ? [{ title: 'Kullanıcılar', icon: <Users className="h-5 w-5" />, to: '/admin/users', color: 'emerald' }]
      : []),
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Genel Bakış & Analitik</h1>
        <p className="text-sm text-muted-foreground">Hoş geldiniz, {auth.displayName}</p>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
           {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-32 w-full rounded-xl" />)}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="rounded-xl border border-border/50 bg-card p-5 hover:border-emerald-500/30 transition-all shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="font-medium text-sm text-muted-foreground">Toplam Gelir</span>
              <div className="rounded-md bg-emerald-500/10 p-2 text-emerald-500">
                <TrendingUp className="h-4 w-4" />
              </div>
            </div>
            <p className="text-2xl font-bold">{totalRevenue.toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}</p>
            <p className="text-xs text-muted-foreground mt-1">Sadece teslim edilen siparişler</p>
          </div>

          <div className="rounded-xl border border-border/50 bg-card p-5 hover:border-indigo-500/30 transition-all shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="font-medium text-sm text-muted-foreground">Toplam Sipariş</span>
              <div className="rounded-md bg-indigo-500/10 p-2 text-indigo-500">
                <ShoppingBag className="h-4 w-4" />
              </div>
            </div>
            <p className="text-2xl font-bold">{totalOrders}</p>
            <p className="text-xs text-muted-foreground mt-1">İptal oranı: %{cancelRate}</p>
          </div>

          <div className="rounded-xl border border-border/50 bg-card p-5 hover:border-violet-500/30 transition-all shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="font-medium text-sm text-muted-foreground">Toplam Kullanıcı</span>
              <div className="rounded-md bg-violet-500/10 p-2 text-violet-500">
                <Users className="h-4 w-4" />
              </div>
            </div>
            <p className="text-2xl font-bold">{totalUsers}</p>
            <p className="text-xs text-muted-foreground mt-1">Kayıtlı aktif üyeler</p>
          </div>

          <div className="rounded-xl border border-border/50 bg-card p-5 hover:border-rose-500/30 transition-all shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="font-medium text-sm text-muted-foreground">Kritik Stok Uyarısı</span>
              <div className="rounded-md bg-rose-500/10 p-2 text-rose-500">
                <AlertTriangle className="h-4 w-4" />
              </div>
            </div>
            <p className="text-2xl font-bold text-rose-500">{criticalStockProducts.length}</p>
            <p className="text-xs text-muted-foreground mt-1">Stoğu 5'in altında olan ürünler</p>
          </div>
        </div>
      )}

      {/* Sipariş Durum Dağılımı Bar Chart (Tailwind) */}
      {!isLoading && (
        <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm">
          <h2 className="text-lg font-semibold mb-6">Sipariş Durum Dağılımı</h2>
          
          <div className="space-y-4">
            <div className="space-y-1.5">
              <div className="flex justify-between text-sm">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                  <span>Tamamlanan</span>
                </div>
                <span className="font-medium">{completedOrders.length}</span>
              </div>
              <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                <div 
                  className="h-full bg-emerald-500 rounded-full" 
                  style={{ width: `${totalOrders > 0 ? (completedOrders.length / totalOrders) * 100 : 0}%` }}
                />
              </div>
            </div>
            
            <div className="space-y-1.5">
              <div className="flex justify-between text-sm">
                <div className="flex items-center gap-2">
                  <XCircle className="h-4 w-4 text-destructive" />
                  <span>İptal Edilen</span>
                </div>
                <span className="font-medium">{cancelledOrders.length}</span>
              </div>
              <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                <div 
                  className="h-full bg-destructive rounded-full" 
                  style={{ width: `${cancelRate}%` }}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex justify-between text-sm">
                <div className="flex items-center gap-2">
                  <LayoutDashboard className="h-4 w-4 text-blue-500" />
                  <span>Devam Eden / Bekleyen</span>
                </div>
                <span className="font-medium">{totalOrders - completedOrders.length - cancelledOrders.length}</span>
              </div>
              <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                <div 
                  className="h-full bg-blue-500 rounded-full" 
                  style={{ width: `${totalOrders > 0 ? ((totalOrders - completedOrders.length - cancelledOrders.length) / totalOrders) * 100 : 0}%` }}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      <h2 className="text-lg font-semibold pt-4 border-t border-border/50">Hızlı Erişim Menüsü</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((c) => (
          <Link
            key={c.title}
            to={c.to}
            className="flex items-center gap-4 rounded-xl border border-border/50 bg-card p-5 hover:border-violet-500/30 hover:shadow-md transition-all"
          >
            <div className="rounded-lg bg-violet-600/10 p-3 text-violet-400">
              {c.icon}
            </div>
            <span className="font-semibold">{c.title}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
