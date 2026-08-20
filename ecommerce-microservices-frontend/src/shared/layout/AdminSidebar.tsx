import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Package,
  ShoppingBag,
  Users,
  Tag,
  BarChart2,
  Settings,
  ChevronRight,
  Film,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAppAuth } from '@/hooks/useAppAuth';

interface SidebarLinkProps {
  to: string;
  icon: React.ReactNode;
  label: string;
}

function SidebarLink({ to, icon, label }: SidebarLinkProps) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-150',
          isActive
            ? 'bg-violet-600/20 text-violet-400 shadow-sm'
            : 'text-muted-foreground hover:bg-accent hover:text-foreground'
        )
      }
    >
      <span className="h-4 w-4 shrink-0">{icon}</span>
      <span>{label}</span>
      <ChevronRight className="ml-auto h-3 w-3 opacity-50" />
    </NavLink>
  );
}

export function AdminSidebar() {
  const { isAdmin, isSeller } = useAppAuth();

  return (
    <aside className="hidden lg:flex w-64 shrink-0 flex-col border-r border-border/40 bg-card/50 min-h-screen py-6 px-3 gap-1">
      <div className="px-3 pb-4">
        <h2 className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          Yönetim Paneli
        </h2>
      </div>

      <SidebarLink
        to="/admin"
        icon={<LayoutDashboard />}
        label="Genel Bakış"
      />

      <div className="px-3 pt-4 pb-1">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground/60">
          Katalog
        </p>
      </div>

      <SidebarLink to="/admin/products" icon={<Package />} label="Ürünler" />
      <SidebarLink to="/admin/categories" icon={<Tag />} label="Kategoriler" />

      <div className="px-3 pt-4 pb-1">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground/60">
          Medya & Pazarlama
        </p>
      </div>
      
      <SidebarLink to="/admin/reels" icon={<Film />} label="Reels Yönetimi" />

      <div className="px-3 pt-4 pb-1">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground/60">
          Satış
        </p>
      </div>

      <SidebarLink to="/admin/orders" icon={<ShoppingBag />} label="Siparişler" />

      {isAdmin && (
        <>
          <div className="px-3 pt-4 pb-1">
            <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground/60">
              Yönetim
            </p>
          </div>
          <SidebarLink to="/admin/users" icon={<Users />} label="Kullanıcılar" />
          <SidebarLink to="/admin/analytics" icon={<BarChart2 />} label="Analitik" />
        </>
      )}

      <div className="mt-auto border-t border-border/40 pt-4 px-3">
        <SidebarLink to="/admin/settings" icon={<Settings />} label="Ayarlar" />
      </div>
    </aside>
  );
}
