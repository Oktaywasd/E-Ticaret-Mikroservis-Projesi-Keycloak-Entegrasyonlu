import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useUpdateProductStock } from './useProductQueries';

const stockSchema = z.object({
  stock: z.coerce
    .number({ invalid_type_error: 'Sayı giriniz' })
    .int('Tam sayı olmalı')
    .min(0, 'Stok 0 veya üzeri olmalıdır'),
});

type StockFormValues = z.infer<typeof stockSchema>;

interface StockUpdateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  productId: string;
  productName: string;
  currentStock: number;
}

export function StockUpdateModal({
  open,
  onOpenChange,
  productId,
  productName,
  currentStock,
}: StockUpdateModalProps) {
  const { mutate, isPending } = useUpdateProductStock(productId);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<StockFormValues>({
    resolver: zodResolver(stockSchema),
    defaultValues: { stock: currentStock },
  });

  const onSubmit = (values: StockFormValues) => {
    mutate(values, {
      onSuccess: () => {
        onOpenChange(false);
        reset();
      },
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Stok Güncelle</DialogTitle>
          <p className="text-sm text-muted-foreground truncate">{productName}</p>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="stock-input">Yeni Stok Miktarı</Label>
            <Input
              id="stock-input"
              type="number"
              min={0}
              {...register('stock')}
              aria-describedby={errors.stock ? 'stock-error' : undefined}
            />
            {errors.stock && (
              <p id="stock-error" className="text-xs text-destructive">{errors.stock.message}</p>
            )}
          </div>

          <p className="text-xs text-muted-foreground">
            Mevcut stok: <span className="font-semibold">{currentStock}</span>
          </p>

          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline" disabled={isPending}>İptal</Button>
            </DialogClose>
            <Button type="submit" disabled={isPending} id="stock-update-confirm">
              {isPending ? 'Güncelleniyor…' : 'Güncelle'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
