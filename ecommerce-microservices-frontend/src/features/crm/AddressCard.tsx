import { MapPin, Home, Briefcase, MoreHorizontal, Star, Pencil, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useDeleteAddress } from './useCrmQueries';
import { DeleteConfirmModal } from '@/components/ui/delete-confirm-modal';
import { useState } from 'react';
import type { Address } from './types';

interface AddressCardProps {
  address: Address;
  onEdit: (address: Address) => void;
}

export function AddressCard({ address, onEdit }: AddressCardProps) {
  const [deleteOpen, setDeleteOpen] = useState(false);
  const { mutate: deleteAddr, isPending: deleting } = useDeleteAddress();

  return (
    <div
      className="relative flex flex-col rounded-xl border border-border/50 bg-card hover:border-border p-5 transition-all duration-200"
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="flex items-center gap-2">
          <div className="rounded-lg p-2 bg-muted text-muted-foreground">
            <MapPin className="h-4 w-4" />
          </div>
          <div>
            <p className="font-semibold text-sm">{address.addressTitle || address.title}</p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={() => onEdit(address)}
            aria-label="Düzenle"
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-destructive hover:text-destructive hover:bg-destructive/10"
            onClick={() => setDeleteOpen(true)}
            aria-label="Sil"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>

      {/* Address Content */}
      <div className="space-y-0.5 text-sm text-muted-foreground">
        <p>{address.addressLine || address.street}</p>
        <p>
          {address.district || address.state}, {address.city}
          {address.zipCode && ` ${address.zipCode}`}
        </p>
        <p>{address.country}</p>
      </div>

      <DeleteConfirmModal
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Adresi Sil"
        description={`"${address.title}" adresi kalıcı olarak silinecek.`}
        onConfirm={() => deleteAddr(address.id, { onSuccess: () => setDeleteOpen(false) })}
        isLoading={deleting}
      />
    </div>
  );
}
