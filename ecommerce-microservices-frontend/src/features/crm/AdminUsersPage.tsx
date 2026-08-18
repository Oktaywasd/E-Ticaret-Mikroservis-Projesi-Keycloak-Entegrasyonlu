import { useState } from 'react';
import { Search, Users, Shield, ShieldCheck, ShieldOff, ChevronDown, ChevronUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { useAdminUsers, useAssignRole, useRemoveRole } from './useCrmQueries';
import type { AdminUser } from './types';
import type { AppRole } from '@/types';

const ALL_ROLES: AppRole[] = ['ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_CUSTOMER'];

const ROLE_CONFIG: Record<AppRole, { label: string; variant: 'default' | 'warning' | 'success' | 'info' | 'secondary' }> = {
  ROLE_ADMIN: { label: 'Admin', variant: 'default' },
  ROLE_SELLER: { label: 'Satıcı', variant: 'warning' },
  ROLE_CUSTOMER: { label: 'Müşteri', variant: 'success' },
};

function RoleBadge({ role }: { role: AppRole }) {
  const config = ROLE_CONFIG[role];
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
    const hasRole = user.roles.includes(role);
    if (hasRole) {
      remove.mutate({ userId: user.id, role });
    } else {
      assign.mutate({ userId: user.id, role });
    }
  };

  const isPending = assign.isPending || remove.isPending;

  return (
    <>
      <tr className="border-b border-border/30 hover:bg-muted/10 transition-colors">
        {/* Avatar + Name */}
        <td className="px-4 py-3">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-violet-600/30 to-indigo-600/30 border border-violet-500/20 text-sm font-semibold text-violet-300">
              {(user.firstName?.[0] ?? user.username[0]).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="font-medium text-sm truncate">
                {user.firstName && user.lastName
                  ? `${user.firstName} ${user.lastName}`
                  : user.username}
              </p>
              <p className="text-xs text-muted-foreground truncate">{user.email}</p>
            </div>
          </div>
        </td>

        {/* Username */}
        <td className="px-4 py-3 hidden md:table-cell">
          <span className="font-mono text-xs text-muted-foreground">{user.username}</span>
        </td>

        {/* Status */}
        <td className="px-4 py-3 hidden sm:table-cell">
          <Badge variant={user.enabled ? 'success' : 'destructive'}>
            {user.enabled ? 'Aktif' : 'Devre Dışı'}
          </Badge>
        </td>

        {/* Roles */}
        <td className="px-4 py-3">
          <div className="flex flex-wrap gap-1">
            {user.roles.length > 0
              ? user.roles.map((r) => <RoleBadge key={r} role={r} />)
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
            id={`expand-user-${user.id}`}
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
                  const hasRole = user.roles.includes(role);
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
  const { data: users, isLoading, isError, error, refetch } = useAdminUsers();

  const filtered = users?.filter((u) => {
    const q = search.toLowerCase();
    return (
      u.username.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q) ||
      `${u.firstName} ${u.lastName}`.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">Kullanıcı Yönetimi</h1>
          <p className="text-sm text-muted-foreground">
            {filtered?.length ?? 0} kullanıcı
          </p>
        </div>
      </div>

      {/* Stats Row */}
      {!isLoading && users && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: 'Toplam', value: users.length, icon: <Users className="h-4 w-4 text-violet-400" /> },
            { label: 'Admin', value: users.filter((u) => u.roles.includes('ROLE_ADMIN')).length, icon: <Shield className="h-4 w-4 text-red-400" /> },
            { label: 'Satıcı', value: users.filter((u) => u.roles.includes('ROLE_SELLER')).length, icon: <ShieldCheck className="h-4 w-4 text-amber-400" /> },
            { label: 'Müşteri', value: users.filter((u) => u.roles.includes('ROLE_CUSTOMER')).length, icon: <ShieldCheck className="h-4 w-4 text-emerald-400" /> },
          ].map((stat) => (
            <div key={stat.label} className="flex items-center gap-3 rounded-xl border border-border/50 bg-card p-4">
              <div className="rounded-lg bg-muted p-2">{stat.icon}</div>
              <div>
                <p className="text-2xl font-bold">{stat.value}</p>
                <p className="text-xs text-muted-foreground">{stat.label}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          id="admin-users-search"
          placeholder="Kullanıcı ara…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Table */}
      {isError ? (
        <ErrorMessage error={error} onRetry={refetch} />
      ) : (
        <div className="rounded-xl border border-border/50 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="border-b border-border/50 bg-muted/30">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Kullanıcı</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground hidden md:table-cell">Kullanıcı Adı</th>
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
                : !filtered?.length
                ? (
                    <tr>
                      <td colSpan={5} className="px-4 py-12 text-center">
                        <div className="flex flex-col items-center gap-3">
                          <Users className="h-8 w-8 text-muted-foreground/40" />
                          <p className="text-muted-foreground">
                            {search ? 'Arama sonucu bulunamadı' : 'Kullanıcı bulunamadı'}
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
