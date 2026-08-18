import { Trash2 } from 'lucide-react';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle,
  DialogDescription, DialogFooter, DialogClose,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

interface DeleteConfirmModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title?: string;
  description?: string;
  onConfirm: () => void;
  isLoading?: boolean;
}

export function DeleteConfirmModal({
  open,
  onOpenChange,
  title = 'Silmeyi Onayla',
  description = 'Bu işlem geri alınamaz. Devam etmek istediğinizden emin misiniz?',
  onConfirm,
  isLoading,
}: DeleteConfirmModalProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <div className="flex items-center gap-3 mb-1">
            <div className="rounded-full bg-destructive/10 p-2.5">
              <Trash2 className="h-5 w-5 text-destructive" />
            </div>
            <DialogTitle>{title}</DialogTitle>
          </div>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" disabled={isLoading}>İptal</Button>
          </DialogClose>
          <Button
            variant="destructive"
            onClick={onConfirm}
            disabled={isLoading}
            id="confirm-delete-button"
          >
            {isLoading ? 'Siliniyor…' : 'Evet, Sil'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
