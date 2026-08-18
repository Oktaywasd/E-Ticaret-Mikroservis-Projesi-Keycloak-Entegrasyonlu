import { useEffect } from 'react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select } from '@/components/ui/select-native';
import { ErrorMessage } from '@/components/ui/error-message';
import {
  useProduct,
  useCategories,
  useCreateProduct,
  useUpdateProduct,
} from './useProductQueries';

// ─── Validation Schema ────────────────────────────────────────────────────────
const productSchema = z.object({
  productCode: z
    .string()
    .min(1, 'Ürün kodu zorunlu')
    .max(50, 'Maks 50 karakter')
    .regex(/^[A-Z0-9ÇĞİÖŞÜ_-]+$/, 'Sadece büyük harf, rakam, _ ve - kullanılabilir'),
  name: z.string().min(2, 'Ürün adı en az 2 karakter').max(200, 'Maks 200 karakter'),
  description: z.string().max(2000, 'Maks 2000 karakter').optional(),
  price: z.coerce
    .number({ invalid_type_error: 'Sayı giriniz' })
    .positive('Fiyat 0\'dan büyük olmalı'),
  stock: z.coerce
    .number({ invalid_type_error: 'Sayı giriniz' })
    .int('Tam sayı olmalı')
    .min(0, 'Stok 0 veya üzeri olmalı'),
  categoryId: z.string().min(1, 'Kategori seçiniz'),
  brand: z.string().max(100, 'Maks 100 karakter').optional(),
  imageUrl: z.string().url('Geçerli bir URL giriniz').optional().or(z.literal('')),
});

type ProductFormValues = z.infer<typeof productSchema>;

export function ProductFormPage() {
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();

  const { data: existing, isLoading: loadingProduct } = useProduct(id ?? '');
  const { data: categories, isLoading: loadingCategories } = useCategories();
  const createMutation = useCreateProduct();
  const updateMutation = useUpdateProduct(id ?? '');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
    setValue,
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      productCode: '',
      name: '',
      description: '',
      price: 0,
      stock: 0,
      categoryId: '',
      brand: '',
      imageUrl: '',
    },
  });

  // Pre-fill form when editing
  useEffect(() => {
    if (existing) {
      reset({
        productCode: existing?.productCode || existing?.code || "",
        name: existing?.name || "",
        description: existing?.description || "",
        brand: existing?.brand || "",
        price: typeof existing?.price === 'object' 
          ? (existing?.price?.sellingPrice || existing?.price?.amount || 0) 
          : Number(existing?.price || 0),
        stock: typeof existing?.stock === 'object' 
          ? (existing?.stock?.currentStock || existing?.stock?.quantity || 0) 
          : Number(existing?.stock || existing?.stockQuantity || 0),
        categoryId: existing?.categoryId || existing?.category?.id || "",
        imageUrl: existing?.imageUrl || "",
      });
    }
  }, [existing, reset]);

  const onSubmit = (values: ProductFormValues) => {
    if (isEdit) {
      const patchPayload = {
        productCode: values.productCode,
        name: values.name,
        description: values.description,
        brand: values.brand,
        price: Number(values.price),
        stock: typeof values.stock === 'object' ? Number((values.stock as any).quantity) : Number(values.stock),
        categoryId: values.categoryId,
        imageUrl: values.imageUrl
      };

      updateMutation.mutate(patchPayload as any, {
        onSuccess: () => navigate('/admin/products'),
        onError: (error: any) => {
          console.error("Backend Validation Error:", error?.response?.data);
          const msg = error?.response?.data?.message || error?.response?.data?.error || error?.message || 'Ürün güncellenirken bir hata oluştu';
          toast.error(msg);
        }
      });
    } else {
      const createPayload = {
        productCode: String(values.productCode || "URUN_EKLE_1"),
        name: String(values.name),
        description: String(values.description || ""),
        brand: String(values.brand || ""),
        price: Number(values.price),
        stock: Number(values.stock),
        categoryId: String(values.categoryId),
        imageUrl: String(values.imageUrl || "")
      };

      console.log("Gönderilen Payload:", createPayload);

      createMutation.mutate(createPayload as any, {
        onSuccess: () => navigate('/admin/products'),
        onError: (error: any) => {
          console.error("Backend Validation Error:", error?.response?.data);
          const msg = error?.response?.data?.message || error?.response?.data?.error || error?.message || 'Ürün eklenirken bir hata oluştu';
          toast.error(msg);
        }
      });
    }
  };

  const isLoading = isEdit && (loadingProduct || !existing);
  const mutationError = createMutation.error ?? updateMutation.error;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-violet-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate('/admin/products')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-xl font-bold">{isEdit ? 'Ürün Düzenle' : 'Yeni Ürün Ekle'}</h1>
          {isEdit && (
            <p className="text-sm text-muted-foreground">
              Ürün Kodu: <span className="font-mono">{existing?.productCode}</span>
            </p>
          )}
        </div>
      </div>

      {mutationError && <ErrorMessage error={mutationError} />}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Product Code */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="productCode">Ürün Kodu *</Label>
            <Input
              id="productCode"
              placeholder="PRD-001"
              disabled={isEdit} // immutable after creation
              {...register('productCode', {
                onChange: (e) => {
                  const sanitizedValue = e.target.value
                    .toUpperCase()
                    .replace(/Ğ/g, 'G')
                    .replace(/Ü/g, 'U')
                    .replace(/Ş/g, 'S')
                    .replace(/İ/g, 'I')
                    .replace(/Ö/g, 'O')
                    .replace(/Ç/g, 'C');
                  e.target.value = sanitizedValue;
                  setValue('productCode', sanitizedValue, { shouldValidate: true });
                }
              })}
              aria-describedby={errors.productCode ? 'productCode-error' : undefined}
            />
            {errors.productCode && (
              <p id="productCode-error" className="text-xs text-destructive">{errors.productCode.message}</p>
            )}
            {isEdit && (
              <p className="text-xs text-muted-foreground">Ürün kodu değiştirilemez</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="brand">Marka</Label>
            <Input id="brand" placeholder="Örn: Apple" {...register('brand')} />
            {errors.brand && (
              <p className="text-xs text-destructive">{errors.brand.message}</p>
            )}
          </div>
        </div>

        {/* Name */}
        <div className="space-y-1.5">
          <Label htmlFor="name">Ürün Adı *</Label>
          <Input
            id="name"
            placeholder="Ürün adını giriniz"
            {...register('name')}
            aria-describedby={errors.name ? 'name-error' : undefined}
          />
          {errors.name && (
            <p id="name-error" className="text-xs text-destructive">{errors.name.message}</p>
          )}
        </div>

        {/* Description */}
        <div className="space-y-1.5">
          <Label htmlFor="description">Açıklama</Label>
          <Textarea
            id="description"
            placeholder="Ürün hakkında detaylı bilgi…"
            rows={4}
            {...register('description')}
          />
          {errors.description && (
            <p className="text-xs text-destructive">{errors.description.message}</p>
          )}
        </div>

        {/* Price + Stock */}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="price">Fiyat (₺) *</Label>
            <Input
              id="price"
              type="number"
              step="0.01"
              min={0.01}
              placeholder="0.00"
              {...register('price')}
              aria-describedby={errors.price ? 'price-error' : undefined}
            />
            {errors.price && (
              <p id="price-error" className="text-xs text-destructive">{errors.price.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="stock">Stok *</Label>
            <Input
              id="stock"
              type="number"
              min={0}
              step={1}
              placeholder="0"
              {...register('stock')}
              aria-describedby={errors.stock ? 'stock-error' : undefined}
            />
            {errors.stock && (
              <p id="stock-error" className="text-xs text-destructive">{errors.stock.message}</p>
            )}
          </div>
        </div>

        {/* Category */}
        <div className="space-y-1.5">
          <Label htmlFor="categoryId">Kategori *</Label>
          <Select
            id="categoryId"
            placeholder="Kategori seçiniz"
            disabled={loadingCategories}
            {...register('categoryId')}
            aria-describedby={errors.categoryId ? 'cat-error' : undefined}
          >
            {categories?.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </Select>
          {errors.categoryId && (
            <p id="cat-error" className="text-xs text-destructive">{errors.categoryId.message}</p>
          )}
        </div>

        {/* Image URL */}
        <div className="space-y-1.5">
          <Label htmlFor="imageUrl">Görsel URL</Label>
          <Input
            id="imageUrl"
            type="url"
            placeholder="https://example.com/image.jpg"
            {...register('imageUrl')}
          />
          {errors.imageUrl && (
            <p className="text-xs text-destructive">{errors.imageUrl.message}</p>
          )}
        </div>

        {/* Actions */}
        <div className="flex gap-3 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate('/admin/products')}
            disabled={isSubmitting}
          >
            İptal
          </Button>
          <Button
            type="submit"
            id="product-form-submit"
            disabled={isSubmitting}
            className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700"
          >
            {isSubmitting
              ? isEdit ? 'Güncelleniyor…' : 'Ekleniyor…'
              : isEdit ? 'Güncelle' : 'Ürün Ekle'}
          </Button>
        </div>
      </form>
    </div>
  );
}
