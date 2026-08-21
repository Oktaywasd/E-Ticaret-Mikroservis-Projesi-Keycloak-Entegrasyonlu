import { useState, useMemo } from 'react';
import { Search, Users, Shield, ShieldCheck, ShieldOff, ChevronDown, ChevronUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { useAdminUsers, useAssignRole, useRemoveRole } from './useCrmQueries';
import type { AdminUser } from './types';
import type { AppRole } from '@/types';
import { useAppAuth } from '@/hooks/useAppAuth';

const ALL_ROLES: AppRole[] = ['ROLE_ADMIN', 'ROLE_CUSTOMER'];

const ROLE_CONFIG: Record<string, { label: string; variant: 'default' | 'warning' | 'success' | 'info' | 'secondary' }> = {
  ROLE_ADMIN: { label: 'Admin', variant: 'default' },
  ADMIN: { label: 'Admin', variant: 'default' },
  ROLE_CUSTOMER: { label: 'Müşteri', variant: 'success' },
  CUSTOMER: { label: 'Müşteri', variant: 'success' },
};

function RoleBadge({ role }: { role: AppRole }) {
  const normalizedRole = (role.startsWith('ROLE_') ? role : `ROLE_${role}`).toUpperCase();
  const config = ROLE_CONFIG[normalizedRole] || { label: role, variant: 'secondary' };
  return <Badge variant={config.variant} className="text-xs">{config.label}</Badge>;
}

interface UserRowProps {
  user: AdminUser;
}

function UserRow({ user }: UserRowProps) {
  const [expanded, setExpanded] = useState(false);
  const assign = useAssignRole();
  const remove = useRemoveRole();

  const handleToggleRole = (role: AppRole) => {
    const roles = Array.isArray(user?.roles) ? user.roles : [];
    const hasRole = roles.some(r => typeof r === 'string' && (r.toUpperCase() === role || r.toUpperCase() === role.replace('ROLE_', '')));
    if (hasRole) {
      remove.mutate({ userId: user.id, role });
    } else {
      assign.mutate({ userId: user.id, role });
    }
  };

  const isPending = assign.isPending || remove.isPending;
  const roles = Array.isArray(user?.roles) ? user.roles : [];

  return (
    <>
      <tr className="border-b border-border/30 hover:bg-muted/10 transition-colors">
        {/* Avatar + Name */}
        <td className="px-4 py-3">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-violet-600/30 to-indigo-600/30 border border-violet-500/20 text-sm font-semibold text-violet-300">
              {(user?.firstName?.[0] ?? user?.username?.[0] ?? '?').toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="font-medium text-sm truncate">
                {user?.firstName || user?.lastName
                  ? `${user?.firstName || ''} ${user?.lastName || ''}`.trim()
                  : (user?.username || 'İsimsiz Kullanıcı')}
              </p>
              <p className="text-xs text-muted-foreground truncate">{user?.email || '-'}</p>
              {user?.phoneNumber && <p className="text-xs text-muted-foreground truncate">{user.phoneNumber}</p>}
            </div>
          </div>
        </td>

        {/* Identity & Date */}
        <td className="px-4 py-3 hidden md:table-cell">
          <p className="font-mono text-xs text-muted-foreground" title="Keycloak ID">
            {user?.keycloakUserId || user?.id || '-'}
          </p>
          {user?.createdTimestamp && (
            <p className="text-xs text-muted-foreground mt-1">
              {new Date(user.createdTimestamp).toLocaleDateString('tr-TR')}
            </p>
          )}
        </td>

        {/* Status */}
        <td className="px-4 py-3 hidden sm:table-cell">
          <Badge variant={user?.enabled ? 'success' : 'destructive'}>
            {user?.enabled ? 'Aktif' : 'Devre Dışı'}
          </Badge>
        </td>

        {/* Roles */}
        <td className="px-4 py-3">
          <div className="flex flex-wrap gap-1">
            {roles.length > 0
              ? roles.map((r) => <RoleBadge key={r} role={r as AppRole} />)
              : <span className="text-xs text-muted-foreground">—</span>}
          </div>
        </td>

        {/* Expand toggle */}
        <td className="px-4 py-3">
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={() => setExpanded((v) => !v)}
            aria-label="Rol yönetimi"
            id={`expand-user-${user?.id}`}
          >
            {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          </Button>
        </td>
      </tr>

      {/* Expanded Role Management Row */}
      {expanded && (
        <tr className="border-b border-border/30 bg-muted/10">
          <td colSpan={5} className="px-4 py-4">
            <div className="space-y-3">
              <p className="text-sm font-medium flex items-center gap-2">
                <Shield className="h-4 w-4 text-violet-400" />
                Rol Yönetimi
                {isPending && <span className="text-xs text-muted-foreground animate-pulse">Güncelleniyor…</span>}
              </p>
              <div className="flex flex-wrap gap-2">
                {ALL_ROLES.map((role) => {
                  const hasRole = roles.some(r => typeof r === 'string' && (r.toUpperCase() === role || r.toUpperCase() === role.replace('ROLE_', '')));
                  const config = ROLE_CONFIG[role];
                  return (
                    <button
                      key={role}
                      onClick={() => handleToggleRole(role)}
                      disabled={isPending}
                      id={`toggle-role-${user.id}-${role}`}
                      className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-all ${
                        hasRole
                          ? 'border-violet-500 bg-violet-500/20 text-violet-300 hover:bg-violet-500/10 hover:border-violet-400'
                          : 'border-border/50 text-muted-foreground hover:border-violet-500/30 hover:text-foreground'
                      } disabled:opacity-50 disabled:cursor-not-allowed`}
                      aria-pressed={hasRole}
                    >
                      {hasRole ? (
                        <ShieldCheck className="h-3.5 w-3.5" />
                      ) : (
                        <ShieldOff className="h-3.5 w-3.5" />
                      )}
                      {config.label}
                      {hasRole && <span className="text-xs opacity-70">✓</span>}
                    </button>
                  );
                })}
              </div>
              <p className="text-xs text-muted-foreground">
                Rollere tıklayarak kullanıcıya atayabilir veya kaldırabilirsiniz.
                Değişiklikler anında uygulanır.
              </p>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

export function AdminUsersPage() {
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<string | null>(null);
  const { data: users, isLoading, isError, error, refetch } = useAdminUsers();
  const auth = useAppAuth();

  const enrichedUsers = useMemo(() => {
    if (!users || !Array.isArray(users)) return [];
    
    return users.map(u => {
       const roles = Array.isArray(u?.roles) ? u.roles : [];
       const hasAdminRole = roles.includes('ADMIN') || 
                            roles.includes('ROLE_ADMIN') || 
                            roles.some(r => typeof r === 'string' && (r.toUpperCase() === 'ADMIN' || r.toUpperCase() === 'ROLE_ADMIN')) || 
                            (u?.email && typeof u.email === 'string' && u.email.toLowerCase().includes('admin'));
       
       return {
         ...u,
         roles: hasAdminRole ? ['ROLE_ADMIN'] : ['ROLE_CUSTOMER']
       } as AdminUser;
    });
  }, [users]);

  const filtered = enrichedUsers.filter((u) => {
    const q = search.toLowerCase();
    const username = u?.username || '';
    const email = u?.email || '';
    const firstName = u?.firstName || '';
    const lastName = u?.lastName || '';
    
    const matchesSearch = username.toLowerCase().includes(q) ||
                          email.toLowerCase().includes(q) ||
                          `${firstName} ${lastName}`.toLowerCase().includes(q);
    
    const roles = Array.isArray(u?.roles) ? u.roles : [];
    
    if (roleFilter === 'ADMIN') {
      return matchesSearch && roles.includes('ROLE_ADMIN');
    }
    if (roleFilter === 'CUSTOMER') {
      return matchesSearch && roles.includes('ROLE_CUSTOMER');
    }
    return matchesSearch;
  });

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">Kullanıcı Yönetimi</h1>
          <p className="text-sm text-muted-foreground">
            {filtered.length} kullanıcı listeleniyor
          </p>
        </div>
        {roleFilter && (
          <Button variant="outline" size="sm" onClick={() => setRoleFilter(null)}>
            Filtreyi Temizle
          </Button>
        )}
      </div>

      {/* Stats Row */}
      {!isLoading && enrichedUsers.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {[
            { label: 'Toplam Kullanıcı', filter: null, value: enrichedUsers.length, icon: <Users className="h-4 w-4 text-violet-400" /> },
            { label: 'Admin', filter: 'ADMIN', value: enrichedUsers.filter((u) => u.roles.includes('ROLE_ADMIN')).length, icon: <Shield className="h-4 w-4 text-red-400" /> },
            { label: 'Müşteri', filter: 'CUSTOMER', value: enrichedUsers.filter((u) => u.roles.includes('ROLE_CUSTOMER')).length, icon: <ShieldCheck className="h-4 w-4 text-emerald-400" /> },
          ].map((stat) => (
            <div 
              key={stat.label} 
              onClick={() => setRoleFilter(stat.filter)}
              className={`flex items-center gap-3 rounded-xl border p-4 cursor-pointer transition-all ${
                roleFilter === stat.filter 
                  ? 'border-violet-500 bg-violet-500/10 shadow-sm' 
                  : 'border-border/50 bg-card hover:border-violet-500/30'
              }`}
            >
              <div className="rounded-lg bg-muted p-2">{stat.icon}</div>
              <div>
                <p className="text-2xl font-bold">{stat.value}</p>
                <p className="text-xs text-muted-foreground">{stat.label}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Search and Refresh */}
      <div className="flex items-center gap-3">
        <div className="relative max-w-sm flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            id="admin-users-search"
            placeholder="Kullanıcı ara…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <Button variant="outline" onClick={() => refetch()} disabled={isLoading}>
          Yenile
        </Button>
      </div>

      {/* Table */}
      {isError ? (
        <ErrorMessage error={error} onRetry={refetch} />
      ) : (
        <div className="rounded-xl border border-border/50 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="border-b border-border/50 bg-muted/30">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Kullanıcı (Ad Soyad, İletişim)</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground hidden md:table-cell">Keycloak ID & Tarih</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground hidden sm:table-cell">Durum</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Roller</th>
                <th className="px-4 py-3 w-10"></th>
              </tr>
            </thead>
            <tbody>
              {isLoading
                ? Array.from({ length: 6 }).map((_, i) => (
                    <tr key={i} className="border-b border-border/30">
                      <td className="px-4 py-3">
                         <div className="flex items-center gap-3">
                           <Skeleton className="h-9 w-9 rounded-full" />
                           <div className="space-y-1">
                             <Skeleton className="h-4 w-28" />
                             <Skeleton className="h-3 w-36" />
                           </div>
                         </div>
                      </td>
                      <td className="px-4 py-3 hidden md:table-cell"><Skeleton className="h-4 w-20" /></td>
                      <td className="px-4 py-3 hidden sm:table-cell"><Skeleton className="h-5 w-14" /></td>
                      <td className="px-4 py-3"><Skeleton className="h-5 w-32" /></td>
                      <td className="px-4 py-3"><Skeleton className="h-8 w-8" /></td>
                    </tr>
                  ))
                : !filtered.length
                ? (
                    <tr>
                      <td colSpan={5} className="px-4 py-12 text-center">
                        <div className="flex flex-col items-center gap-3">
                          <Users className="h-8 w-8 text-muted-foreground/40" />
                          <p className="text-muted-foreground">
                            {search || roleFilter ? 'Filtreye uygun kullanıcı bulunamadı' : 'Kullanıcı bulunamadı'}
                          </p>
                        </div>
                      </td>
                    </tr>
                  )
                : filtered.map((user) => (
                    <UserRow key={user.id} user={user} />
                  ))
              }
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
