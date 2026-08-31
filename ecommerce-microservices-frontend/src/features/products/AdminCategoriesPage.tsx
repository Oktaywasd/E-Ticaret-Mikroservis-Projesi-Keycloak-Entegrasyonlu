import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Edit2, Trash2, Tag } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { DeleteConfirmModal } from '@/components/ui/delete-confirm-modal';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from '@/components/ui/dialog';
import { useCreateCategory, useUpdateCategory, useDeleteCategory } from './useProductQueries';
import { useCategories } from '@/hooks/useCacheQueries';
import type { Category } from './types';

const categorySchema = z.object({
  name: z.string().min(2, 'En az 2 karakter').max(100, 'Maks 100 karakter'),
  description: z.string().max(500, 'Maks 500 karakter').optional(),
});
type CategoryFormValues = z.infer<typeof categorySchema>;

interface CategoryFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing?: Category;
}

function CategoryFormDialog({ open, onOpenChange, editing }: CategoryFormDialogProps) {
  const createMutation = useCreateCategory();
  const updateMutation = useUpdateCategory();
  const isEdit = !!editing;

  const {
    register, handleSubmit, formState: { errors }, reset,
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    values: editing ? { name: editing.name, description: editing.description ?? '' } : { name: '', description: '' },
  });

  const onSubmit = (values: CategoryFormValues) => {
    const payload = { ...values, description: values.description || undefined };
    if (isEdit) {
      updateMutation.mutate(
        { id: editing.id, payload },
        { onSuccess: () => { onOpenChange(false); reset(); } }
      );
    } else {
      createMutation.mutate(payload, {
        onSuccess: () => { onOpenChange(false); reset(); },
      });
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;
  const mutationError = createMutation.error ?? updateMutation.error;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Kategori Düzenle' : 'Yeni Kategori'}</DialogTitle>
        </DialogHeader>
        {mutationError && <ErrorMessage error={mutationError} className="py-4" />}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="cat-name">Kategori Adı *</Label>
            <Input
              id="cat-name"
              placeholder="Örn: Elektronik"
              {...register('name')}
            />
            {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cat-desc">Açıklama</Label>
            <Input id="cat-desc" placeholder="Opsiyonel açıklama" {...register('description')} />
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline" disabled={isPending}>İptal</Button>
            </DialogClose>
            <Button type="submit" disabled={isPending} id="category-form-submit">
              {isPending ? (isEdit ? 'Güncelleniyor…' : 'Ekleniyor…') : (isEdit ? 'Güncelle' : 'Ekle')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function AdminCategoriesPage() {
  const navigate = useNavigate();
  const [formOpen, setFormOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Category | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);

  const { data: categories, isLoading, isError, error, refetch } = useCategories();
  const { mutate: deleteCategory, isPending: deleting } = useDeleteCategory();

  const handleEdit = (cat: Category) => {
    setEditTarget(cat);
    setFormOpen(true);
  };

  const handleDelete = () => {
    if (!deleteTarget) return;
    deleteCategory(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Kategori Yönetimi</h1>
          <p className="text-sm text-muted-foreground">{categories?.length ?? 0} kategori</p>
        </div>
        <Button
          id="add-category-button"
          className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700"
          onClick={() => { setEditTarget(undefined); setFormOpen(true); }}
        >
          <Plus className="h-4 w-4 mr-2" />
          Yeni Kategori
        </Button>
      </div>

      {isError ? (
        <ErrorMessage error={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-20" />
          ))}
        </div>
      ) : !categories?.length ? (
        <div className="flex flex-col items-center justify-center gap-3 py-20 text-center">
          <Tag className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-muted-foreground">Henüz kategori eklenmemiş.</p>
          <Button size="sm" onClick={() => setFormOpen(true)}>İlk Kategoriyi Ekle</Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {categories.map((cat) => (
            <div
              key={cat.id}
              onClick={() => navigate(`/admin/products?categoryId=${cat.id}&categoryName=${encodeURIComponent(cat.name)}`)}
              className="flex items-center justify-between rounded-xl border border-border/50 bg-card p-4 hover:border-violet-500/50 cursor-pointer transition-colors"
            >
              <div className="flex items-center gap-3 min-w-0">
                <div className="rounded-lg bg-violet-600/10 p-2 shrink-0">
                  <Tag className="h-4 w-4 text-violet-400" />
                </div>
                <div className="min-w-0">
                  <p className="font-semibold truncate">{cat.name}</p>
                  {cat.description && (
                    <p className="text-xs text-muted-foreground truncate">{cat.description}</p>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-1 shrink-0 ml-2">
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleEdit(cat);
                  }}
                  aria-label="Düzenle"
                >
                  <Edit2 className="h-3.5 w-3.5" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-destructive hover:text-destructive hover:bg-destructive/10"
                  onClick={(e) => {
                    e.stopPropagation();
                    setDeleteTarget(cat);
                  }}
                  aria-label="Sil"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <CategoryFormDialog
        open={formOpen}
        onOpenChange={(o) => { setFormOpen(o); if (!o) setEditTarget(undefined); }}
        editing={editTarget}
      />

      <DeleteConfirmModal
        open={!!deleteTarget}
        onOpenChange={(o) => !o && setDeleteTarget(null)}
        title="Kategoriyi Sil"
        description={`"${deleteTarget?.name}" kategorisi silinecek. Bu kategoriye ait ürünler etkilenebilir.`}
        onConfirm={handleDelete}
        isLoading={deleting}
      />
    </div>
  );
}
