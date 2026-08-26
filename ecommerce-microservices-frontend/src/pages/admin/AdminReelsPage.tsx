import React, { useState, useEffect } from 'react';
import { Film, Plus, Trash2, Heart, Eye, MessageCircle } from 'lucide-react';
import { reelsService } from '@/services/reelsService';
import ReelUploadModal from '@/components/reels/ReelUploadModal';
import ReelDetailModal from '@/components/reels/ReelDetailModal';
import { toast } from 'sonner';
import { useProduct } from '@/features/products/useProductQueries';

function ProductNameDisplay({ reel }: { reel: any }) {
  const { data: productData, isLoading } = useProduct(reel?.productId || '');
  if (reel.productName || reel.productTitle || reel.product?.title || reel.product?.name) {
    return <>{reel.productName || reel.productTitle || reel.product?.title || reel.product?.name}</>;
  }
  if (!reel.productId) return <>Belirtilmemiş</>;
  if (isLoading) return <>Yükleniyor...</>;
  if (productData?.name) return <>{productData.name}</>;
  return <>Ürün #{String(reel.productId).substring(0, 8)}...</>;
}

export default function AdminReelsPage() {
  const [reels, setReels] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedReel, setSelectedReel] = useState<any>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  const fetchReels = async () => {
    try {
      setLoading(true);
      const res = await reelsService.getFeed(0, 100);
      const list = res?.content || (Array.isArray(res) ? res : []);
      setReels(list);
    } catch (err) {
      console.error('Reels çekilemedi:', err);
      toast.error('Videolar yüklenirken bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReels();
  }, []);

  const handleDelete = async (id: string) => {
    if (!window.confirm('Bu videoyu silmek istediğinize emin misiniz?')) return;
    try {
      await reelsService.deleteReel(id);
      toast.success('Reel başarıyla silindi.');
      fetchReels();
    } catch (err) {
      console.error('Silme hatası:', err);
      toast.error('Reel silinirken bir hata oluştu.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Film className="text-rose-500" />
            Reels Yönetimi
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Yüklediğiniz tanıtım videolarını yönetin ve performanslarını takip edin.
          </p>
        </div>
        <button
          onClick={() => setIsUploadModalOpen(true)}
          className="flex items-center gap-2 bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg font-medium shadow-sm transition"
        >
          <Plus size={18} />
          Yeni Reel Ekle
        </button>
      </div>

      {loading ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-600"></div>
        </div>
      ) : reels.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 bg-card rounded-xl border border-border/50 text-muted-foreground">
          <Film size={48} className="mb-4 opacity-20" />
          <p>Henüz hiç video yüklenmemiş.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {reels.map((reel) => (
            <div 
              key={reel.id} 
              onClick={() => { setSelectedReel(reel); setIsDetailModalOpen(true); }}
              className="group relative bg-card border border-border/50 rounded-xl overflow-hidden shadow-sm hover:shadow-md transition cursor-pointer"
            >
              {/* Thumbnail / Video Preview */}
              <div className="aspect-[9/16] bg-black relative">
                <img
                  src={reel.thumbnailUrl || 'https://via.placeholder.com/400x700?text=Video'}
                  alt={reel.title}
                  className="w-full h-full object-cover opacity-80 group-hover:opacity-100 transition"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent pointer-events-none" />
                
                {/* Stats Overlay */}
                <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between text-white text-xs font-medium">
                  <div className="flex items-center gap-1.5"><Heart size={14} className="fill-white" /> {reel.likeCount || 0}</div>
                  <div className="flex items-center gap-1.5"><MessageCircle size={14} /> {reel.commentCount || 0}</div>
                  <div className="flex items-center gap-1.5"><Eye size={14} /> {reel.viewCount || 0}</div>
                </div>
              </div>

              {/* Info */}
              <div className="p-4">
                <h3 className="font-semibold text-sm line-clamp-1" title={reel.title}>{reel.title}</h3>
                <p className="text-xs text-muted-foreground mt-1 line-clamp-1">
                  Ürün: <ProductNameDisplay reel={reel} />
                </p>
                
                {/* Actions */}
                <div className="mt-4 flex justify-end">
                  <button
                    onClick={(e) => { e.stopPropagation(); handleDelete(reel.id); }}
                    className="text-red-500 hover:bg-red-500/10 p-2 rounded-lg transition"
                    title="Videoyu Sil"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <ReelUploadModal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        onUploadSuccess={fetchReels}
      />

      <ReelDetailModal
        reel={selectedReel}
        isOpen={isDetailModalOpen}
        onClose={() => { setIsDetailModalOpen(false); setSelectedReel(null); }}
        onDelete={handleDelete}
      />
    </div>
  );
}
