import { useAppAuth } from '@/hooks/useAppAuth';
import { LayoutDashboard, Package, ShoppingBag, Users, Film } from 'lucide-react';
import { Link } from 'react-router-dom';

export function AdminDashboardPage() {
  const auth = useAppAuth();

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
        <h1 className="text-2xl font-bold">Genel Bakış</h1>
        <p className="text-sm text-muted-foreground">Hoş geldiniz, {auth.displayName}</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
      <p className="text-sm text-muted-foreground">İstatistikler ilerleyen fazlarda eklenecek.</p>
    </div>
  );
}
