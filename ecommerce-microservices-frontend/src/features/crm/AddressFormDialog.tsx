import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { MapPin } from 'lucide-react';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select-native';
import { ErrorMessage } from '@/components/ui/error-message';
import { addressSchema, type AddressFormValues } from './schemas';
import { useCreateAddress, useUpdateAddress } from './useCrmQueries';
import type { Address } from './types';

interface AddressFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing?: Address;
}

const ADDRESS_TYPE_LABELS = {
  HOME: 'Ev',
  WORK: 'İş',
  OTHER: 'Diğer',
};

const CITIES = [
  'Adana', 'Ankara', 'Antalya', 'Bursa', 'Diyarbakır', 'Eskişehir',
  'Gaziantep', 'İstanbul', 'İzmir', 'Kayseri', 'Konya', 'Mersin',
  'Samsun', 'Trabzon',
];

export function AddressFormDialog({ open, onOpenChange, editing }: AddressFormDialogProps) {
  const isEdit = !!editing;
  const createMutation = useCreateAddress();
  const updateMutation = useUpdateAddress();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
    values: editing ? {
      title: editing.addressTitle || editing.title || '',
      street: editing.addressLine || editing.street || '',
      city: editing.city,
      state: editing.district || editing.state || '',
      zipCode: editing.zipCode ?? '',
      country: editing.country,
    } : {
      title: '',
      street: '',
      city: 'İstanbul',
      state: '',
      zipCode: '',
      country: 'Türkiye',
    },
  });

  const mutationError = createMutation.error ?? updateMutation.error;

  const onSubmit = (values: AddressFormValues) => {
    if (isEdit) {
      const updatePayload = {
        title: values.title ?? "",
        street: values.street ?? "",
        state: values.state ?? "",
        city: values.city ?? "",
        zipCode: values.zipCode ?? "",
        country: values.country ?? "",
      };
      
      updateMutation.mutate(
        { id: editing.id, payload: updatePayload as any },
        { onSuccess: () => { onOpenChange(false); reset(); } }
      );
    } else {
      const formData = values as any;
      const createPayload = {
        title: formData.title || "Okul",
        street: formData.street || "Trakya Üniversitesi",
        city: formData.city || "İstanbul",
        state: formData.state || "Keşan",
        zipCode: formData.zipCode || "34700",
        country: formData.country || "Türkiye"
      };

      createMutation.mutate(createPayload as any, {
        onSuccess: () => { onOpenChange(false); reset(); },
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-violet-600/10 p-2">
              <MapPin className="h-5 w-5 text-violet-400" />
            </div>
            <DialogTitle>{isEdit ? 'Adresi Düzenle' : 'Yeni Adres Ekle'}</DialogTitle>
          </div>
        </DialogHeader>

        {mutationError && <ErrorMessage error={mutationError} className="py-2" />}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Title */}
          <div className="space-y-1.5">
            <Label htmlFor="addr-title">Adres Başlığı *</Label>
            <Input
              id="addr-title"
              placeholder="Örn: Ev, İş"
              {...register('title')}
            />
            {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
          </div>

          {/* Street */}
          <div className="space-y-1.5">
            <Label htmlFor="addr-street">Açık Adres (Sokak/Cadde/No) *</Label>
            <Input
              id="addr-street"
              placeholder="Mahalle, Sokak, No, Daire"
              {...register('street')}
            />
            {errors.street && <p className="text-xs text-destructive">{errors.street.message}</p>}
          </div>

          {/* State + City */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="addr-state">İlçe / Eyalet *</Label>
              <Input id="addr-state" placeholder="Kadıköy" {...register('state')} />
              {errors.state && <p className="text-xs text-destructive">{errors.state.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="addr-city">Şehir *</Label>
              <Select id="addr-city" {...register('city')}>
                {CITIES.map((city) => (
                  <option key={city} value={city}>{city}</option>
                ))}
              </Select>
              {errors.city && <p className="text-xs text-destructive">{errors.city.message}</p>}
            </div>
          </div>

          {/* Zip Code + Country */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="addr-zipCode">Posta Kodu</Label>
              <Input id="addr-zipCode" placeholder="34700" maxLength={5} {...register('zipCode')} />
              {errors.zipCode && <p className="text-xs text-destructive">{errors.zipCode.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="addr-country">Ülke *</Label>
              <Input id="addr-country" {...register('country')} />
              {errors.country && <p className="text-xs text-destructive">{errors.country.message}</p>}
            </div>
          </div>

          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline" disabled={createMutation.isPending || updateMutation.isPending || isSubmitting}>
                İptal
              </Button>
            </DialogClose>
            <Button
              type="submit"
              disabled={createMutation.isPending || updateMutation.isPending || isSubmitting}
              id="address-form-submit"
              className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700"
            >
              {createMutation.isPending || updateMutation.isPending || isSubmitting
                ? isEdit ? 'Güncelleniyor…' : 'Ekleniyor…'
                : isEdit ? 'Güncelle' : 'Adresi Kaydet'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
