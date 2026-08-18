import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  User, Mail, Phone, Calendar, MapPin, Plus, Shield, CheckCircle2, AlertCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { AddressCard } from './AddressCard';
import { AddressFormDialog } from './AddressFormDialog';
import { profileSchema, type ProfileFormValues } from './schemas';
import {
  useProfile, useCreateProfile, useUpdateProfile, useAddresses,
} from './useCrmQueries';
import { useAppAuth } from '@/hooks/useAppAuth';
import type { Address } from './types';

type ProfileTab = 'profile' | 'addresses' | 'security';

export function ProfilePage() {
  const auth = useAppAuth();
  const [activeTab, setActiveTab] = useState<ProfileTab>('profile');
  const [addressDialogOpen, setAddressDialogOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | undefined>();
  const [profileSaved, setProfileSaved] = useState(false);

  const {
    data: profile,
    isLoading: loadingProfile,
    isError: profileError,
    error: profileErrorData,
  } = useProfile();

  const { data: addresses, isLoading: loadingAddresses } = useAddresses();
  const createProfile = useCreateProfile();
  const updateProfile = useUpdateProfile();

  // Profile not found (404) → first login
  const profileNotFound =
    !loadingProfile &&
    !profile &&
    !!(profileErrorData as { response?: { status?: number } } | null)?.response?.status === false ||
    (profileErrorData as { response?: { status?: number } } | null)?.response?.status === 404;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { firstName: '', lastName: '', phoneNumber: '' },
  });

  // Prefill form when profile is loaded
  useEffect(() => {
    if (profile) {
      reset({
        firstName: profile.firstName,
        lastName: profile.lastName,
        phoneNumber: profile.phoneNumber ?? '',
      });
    } else if (profileNotFound) {
      // First login — prefill from Keycloak token
      reset({
        firstName: auth.user?.profile?.given_name ?? '',
        lastName: auth.user?.profile?.family_name ?? '',
        phoneNumber: '',
      });
    }
  }, [profile, profileNotFound, auth.user, reset]);

  const onSubmitProfile = async (values: ProfileFormValues) => {
    const payload = {
      ...values,
      phoneNumber: values.phoneNumber || undefined,
    };

    if (profileNotFound) {
      await createProfile.mutateAsync(payload);
    } else {
      await updateProfile.mutateAsync(payload);
    }

    setProfileSaved(true);
    setTimeout(() => setProfileSaved(false), 3000);
  };

  const handleEditAddress = (address: Address) => {
    setEditingAddress(address);
    setAddressDialogOpen(true);
  };

  const TABS: { id: ProfileTab; label: string; icon: React.ReactNode }[] = [
    { id: 'profile', label: 'Profil Bilgileri', icon: <User className="h-4 w-4" /> },
    { id: 'addresses', label: 'Adreslerim', icon: <MapPin className="h-4 w-4" /> },
    { id: 'security', label: 'Güvenlik', icon: <Shield className="h-4 w-4" /> },
  ];

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-2xl font-bold mb-6">Hesabım</h1>

      {/* Profile header card */}
      <div className="mb-6 flex items-center gap-4 rounded-xl border border-border/50 bg-card p-5">
        <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-violet-600 to-indigo-600 text-2xl font-bold text-white shadow-lg shadow-violet-500/30">
          {auth.displayName.charAt(0).toUpperCase()}
        </div>
        <div className="min-w-0">
          <h2 className="font-bold text-lg truncate">{auth.displayName}</h2>
          <p className="text-sm text-muted-foreground truncate">{auth.email}</p>
          <div className="flex flex-wrap gap-1.5 mt-1.5">
            {auth.roles.map((role) => (
              <Badge key={role} variant="secondary" className="text-[10px]">
                {role.replace('ROLE_', '')}
              </Badge>
            ))}
          </div>
        </div>
      </div>

      {/* Onboarding Banner */}
      {profileNotFound && (
        <div className="mb-6 flex items-start gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4">
          <AlertCircle className="h-5 w-5 text-amber-400 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold text-amber-300 text-sm">Profilinizi tamamlayın</p>
            <p className="text-xs text-amber-400/80">
              İlk girişinizde profil bilgilerinizi doldurmanız gerekmektedir. Sipariş verebilmek için profilinizi kaydedin.
            </p>
          </div>
        </div>
      )}

      {/* Tab Navigation */}
      <div className="flex items-center gap-1 border-b border-border/50 mb-6 overflow-x-auto">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            id={`tab-${tab.id}`}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-all whitespace-nowrap ${
              activeTab === tab.id
                ? 'border-violet-500 text-violet-400'
                : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
            }`}
          >
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </div>

      {/* ─── Profile Tab ─────────────────────────────────── */}
      {activeTab === 'profile' && (
        <div className="rounded-xl border border-border/50 bg-card p-6">
          {loadingProfile ? (
            <div className="space-y-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-3/4" />
            </div>
          ) : (
            <form onSubmit={handleSubmit(onSubmitProfile)} className="space-y-5">
              {/* Success message */}
              {profileSaved && (
                <div className="flex items-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-3">
                  <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                  <p className="text-sm text-emerald-400">Profil başarıyla güncellendi.</p>
                </div>
              )}

              {(updateProfile.error || createProfile.error) && (
                <ErrorMessage error={updateProfile.error ?? createProfile.error} />
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label htmlFor="firstName">Ad *</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="firstName"
                      placeholder="Adınız"
                      className="pl-9"
                      {...register('firstName')}
                    />
                  </div>
                  {errors.firstName && (
                    <p className="text-xs text-destructive">{errors.firstName.message}</p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="lastName">Soyad *</Label>
                  <Input id="lastName" placeholder="Soyadınız" {...register('lastName')} />
                  {errors.lastName && (
                    <p className="text-xs text-destructive">{errors.lastName.message}</p>
                  )}
                </div>
              </div>

              {/* Read-only email */}
              <div className="space-y-1.5">
                <Label>E-posta</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    value={auth.email ?? ''}
                    disabled
                    className="pl-9 opacity-60 cursor-not-allowed"
                    aria-readonly="true"
                  />
                </div>
                <p className="text-xs text-muted-foreground">
                  E-posta adresiniz Keycloak hesabınızdan alınmaktadır ve buradan değiştirilemez.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label htmlFor="phoneNumber">Telefon</Label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="phoneNumber"
                      type="tel"
                      placeholder="05XX XXX XX XX"
                      className="pl-9"
                      {...register('phoneNumber')}
                    />
                  </div>
                  {errors.phoneNumber && (
                    <p className="text-xs text-destructive">{errors.phoneNumber.message}</p>
                  )}
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <Button
                  type="submit"
                  id="profile-save-button"
                  disabled={isSubmitting || (!isDirty && !profileNotFound)}
                  className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700"
                >
                  {isSubmitting
                    ? 'Kaydediliyor…'
                    : profileNotFound
                    ? 'Profil Oluştur'
                    : 'Değişiklikleri Kaydet'}
                </Button>
                {isDirty && !profileNotFound && (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => reset()}
                    disabled={isSubmitting}
                  >
                    İptal
                  </Button>
                )}
              </div>
            </form>
          )}
        </div>
      )}

      {/* ─── Addresses Tab ───────────────────────────────── */}
      {activeTab === 'addresses' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {addresses?.length ?? 0} adres kayıtlı
            </p>
            <Button
              id="add-address-button"
              size="sm"
              className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700"
              onClick={() => { setEditingAddress(undefined); setAddressDialogOpen(true); }}
            >
              <Plus className="h-4 w-4 mr-2" />
              Adres Ekle
            </Button>
          </div>

          {loadingAddresses ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-40" />
              ))}
            </div>
          ) : !addresses?.length ? (
            <div className="flex flex-col items-center justify-center gap-4 py-16 rounded-xl border border-dashed border-border/50">
              <div className="rounded-full bg-muted p-5">
                <MapPin className="h-8 w-8 text-muted-foreground/40" />
              </div>
              <div className="text-center">
                <p className="font-semibold">Kayıtlı adres yok</p>
                <p className="text-sm text-muted-foreground">
                  Hızlı sipariş için adresinizi ekleyin.
                </p>
              </div>
              <Button
                size="sm"
                onClick={() => setAddressDialogOpen(true)}
              >
                İlk Adresimi Ekle
              </Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {addresses.map((addr) => (
                  <AddressCard
                    key={addr.id}
                    address={addr}
                    onEdit={handleEditAddress}
                  />
                ))}
            </div>
          )}
        </div>
      )}

      {/* ─── Security Tab ────────────────────────────────── */}
      {activeTab === 'security' && (
        <div className="rounded-xl border border-border/50 bg-card p-6 space-y-5">
          <div className="flex items-start gap-4">
            <div className="rounded-full bg-violet-600/10 p-3 shrink-0">
              <Shield className="h-6 w-6 text-violet-400" />
            </div>
            <div>
              <h3 className="font-semibold">Şifre & Güvenlik</h3>
              <p className="text-sm text-muted-foreground mt-1">
                Hesap güvenliğiniz Keycloak kimlik sağlayıcısı tarafından yönetilmektedir.
                Şifrenizi değiştirmek, iki faktörlü kimlik doğrulamayı etkinleştirmek veya
                oturum geçmişini görüntülemek için Keycloak hesap portalını kullanabilirsiniz.
              </p>
            </div>
          </div>
          <div className="border-t border-border/50 pt-4">
            <Button
              id="keycloak-account-button"
              variant="outline"
              onClick={() =>
                window.open(
                  `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}/account`,
                  '_blank'
                )
              }
            >
              <Shield className="h-4 w-4 mr-2" />
              Hesap Güvenlik Portalını Aç
            </Button>
          </div>

          <div className="border-t border-border/50 pt-4 space-y-2">
            <p className="text-sm font-medium">Aktif Oturum</p>
            <div className="rounded-lg bg-muted/30 p-3 text-sm space-y-1">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Kullanıcı Adı</span>
                <span className="font-mono">{auth.user?.profile?.preferred_username}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">E-posta Doğrulama</span>
                <span className={auth.user?.profile?.email_verified ? 'text-emerald-400' : 'text-amber-400'}>
                  {auth.user?.profile?.email_verified ? 'Doğrulandı ✓' : 'Doğrulanmadı'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Token Bitiş</span>
                <span>
                  {auth.user?.expires_at
                    ? new Date(auth.user.expires_at * 1000).toLocaleTimeString('tr-TR')
                    : '—'}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Address Form Dialog */}
      <AddressFormDialog
        open={addressDialogOpen}
        onOpenChange={(open) => {
          setAddressDialogOpen(open);
          if (!open) setEditingAddress(undefined);
        }}
        editing={editingAddress}
      />
    </div>
  );
}
