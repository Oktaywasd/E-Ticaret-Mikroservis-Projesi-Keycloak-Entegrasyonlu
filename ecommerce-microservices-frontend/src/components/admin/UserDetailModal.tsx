import React from 'react';
import { X, User, Mail, Shield, CheckCircle2, XCircle, Calendar, Hash, Copy, MapPin, Phone } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { toast } from 'sonner';

interface UserDetailModalProps {
  user: any;
  isOpen: boolean;
  onClose: () => void;
}

export default function UserDetailModal({ user, isOpen, onClose }: UserDetailModalProps) {
  if (!isOpen || !user) return null;

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success('Kopyalandı: ' + text);
  };

  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.fullName;
  const username = user.username || user.preferred_username || user.userName;
  const createdDate = user.createdTimestamp ? new Date(user.createdTimestamp).toLocaleDateString('tr-TR', { day: '2-digit', month: 'long', year: 'numeric' }) : (user.createdAt ? new Date(user.createdAt).toLocaleDateString('tr-TR') : 'Bilinmiyor');
  
  const hasRole = (role: string) => {
    return user.roles?.includes(role) || user.realmRoles?.includes(role) || user.clientRoles?.includes(role);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      <div className="relative w-full max-w-2xl bg-neutral-900 border border-neutral-800 rounded-2xl overflow-hidden shadow-2xl text-white">
        
        {/* Header (Top section) */}
        <div className="relative p-6 sm:p-8 border-b border-neutral-800 bg-neutral-900/50 flex flex-col items-center sm:flex-row sm:items-start gap-6">
          <button
            onClick={onClose}
            className="absolute top-4 right-4 p-2 text-neutral-400 hover:text-white rounded-full hover:bg-neutral-800 transition"
          >
            <X size={20} />
          </button>
          
          <div className="w-24 h-24 sm:w-20 sm:h-20 rounded-full bg-neutral-800 border-2 border-neutral-700 flex items-center justify-center flex-shrink-0">
            <User size={40} className="text-neutral-400" />
          </div>
          
          <div className="flex flex-col items-center sm:items-start flex-1 text-center sm:text-left">
            <h2 className="text-2xl font-bold text-white mb-1">{fullName || username || 'İsimsiz Kullanıcı'}</h2>
            <p className="text-neutral-400 text-sm flex items-center gap-1.5 mb-3">
              <Mail size={14} /> {user.email || 'E-posta yok'}
            </p>
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
              {hasRole('ADMIN') && <Badge className="bg-rose-500/20 text-rose-400 hover:bg-rose-500/30 border-rose-500/50"><Shield size={12} className="mr-1"/> Admin</Badge>}
              {hasRole('SELLER') && <Badge className="bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 border-blue-500/50">Satıcı</Badge>}
              {(!hasRole('ADMIN') && !hasRole('SELLER')) && <Badge className="bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700">Müşteri</Badge>}
            </div>
          </div>
        </div>

        {/* Content (Grid details) */}
        <div className="p-6 sm:p-8 max-h-[60vh] overflow-y-auto">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            {/* Username */}
            <div className="bg-neutral-800/40 p-4 rounded-xl border border-neutral-800/60">
              <p className="text-xs text-neutral-500 font-semibold mb-1 uppercase tracking-wider">Kullanıcı Adı</p>
              <p className="font-medium text-neutral-200">{username || '-'}</p>
            </div>

            {/* Verification Status */}
            <div className="bg-neutral-800/40 p-4 rounded-xl border border-neutral-800/60 flex flex-col justify-center">
              <p className="text-xs text-neutral-500 font-semibold mb-2 uppercase tracking-wider">E-posta Doğrulaması</p>
              <div className="flex items-center gap-2">
                {user.emailVerified ? (
                  <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/50 gap-1 rounded-md">
                    <CheckCircle2 size={12} /> Doğrulandı
                  </Badge>
                ) : (
                  <Badge className="bg-rose-500/20 text-rose-400 border-rose-500/50 gap-1 rounded-md">
                    <XCircle size={12} /> Doğrulanmadı
                  </Badge>
                )}
              </div>
            </div>

            {/* IDs */}
            <div className="bg-neutral-800/40 p-4 rounded-xl border border-neutral-800/60 sm:col-span-2">
              <p className="text-xs text-neutral-500 font-semibold mb-2 uppercase tracking-wider">Sistem & Keycloak ID</p>
              <div className="space-y-2">
                <div className="flex items-center justify-between bg-black/40 px-3 py-2 rounded-lg border border-neutral-800">
                  <div className="flex items-center gap-2 text-sm text-neutral-300 font-mono overflow-hidden">
                    <Hash size={14} className="text-neutral-500 flex-shrink-0" />
                    <span className="truncate">{user.id || user.keycloakId || '-'}</span>
                  </div>
                  <button 
                    onClick={() => handleCopy(user.id || user.keycloakId)}
                    className="p-1.5 text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-md transition ml-2 flex-shrink-0"
                    title="Kopyala"
                  >
                    <Copy size={14} />
                  </button>
                </div>
              </div>
            </div>

            {/* Date */}
            <div className="bg-neutral-800/40 p-4 rounded-xl border border-neutral-800/60">
              <p className="text-xs text-neutral-500 font-semibold mb-1 uppercase tracking-wider">Hesap Oluşturulma</p>
              <p className="font-medium text-neutral-200 flex items-center gap-2">
                <Calendar size={14} className="text-neutral-400" /> {createdDate}
              </p>
            </div>

            {/* Status (Enabled/Disabled) */}
            <div className="bg-neutral-800/40 p-4 rounded-xl border border-neutral-800/60">
              <p className="text-xs text-neutral-500 font-semibold mb-1 uppercase tracking-wider">Hesap Durumu</p>
              <p className="font-medium text-neutral-200 flex items-center gap-2">
                {user.enabled !== false ? (
                  <span className="flex items-center gap-1.5 text-emerald-400"><span className="w-2 h-2 rounded-full bg-emerald-500"></span> Aktif</span>
                ) : (
                  <span className="flex items-center gap-1.5 text-rose-400"><span className="w-2 h-2 rounded-full bg-rose-500"></span> Pasif / Engelli</span>
                )}
              </p>
            </div>

            {/* Optional Phone / Address */}
            {(user.phone || user.address || user.phoneNumber) && (
              <div className="bg-neutral-800/40 p-4 rounded-xl border border-neutral-800/60 sm:col-span-2 space-y-3">
                <p className="text-xs text-neutral-500 font-semibold uppercase tracking-wider mb-2">İletişim & Adres Bilgileri</p>
                {(user.phone || user.phoneNumber) && (
                  <p className="text-sm text-neutral-300 flex items-start gap-2">
                    <Phone size={14} className="text-neutral-500 mt-0.5 flex-shrink-0" />
                    {user.phone || user.phoneNumber}
                  </p>
                )}
                {user.address && (
                  <p className="text-sm text-neutral-300 flex items-start gap-2">
                    <MapPin size={14} className="text-neutral-500 mt-0.5 flex-shrink-0" />
                    {user.address}
                  </p>
                )}
              </div>
            )}
            
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-neutral-800 bg-neutral-900/50 flex justify-end">
          <button 
            onClick={onClose}
            className="px-4 py-2 bg-neutral-800 hover:bg-neutral-700 text-white rounded-lg text-sm font-medium transition"
          >
            Kapat
          </button>
        </div>

      </div>
    </div>
  );
}
