import { useState } from 'react';
import { Star } from 'lucide-react';
import { toast } from 'sonner';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { reviewService } from '@/services/reviewService';
import { cn } from '@/lib/utils';

interface ReviewModalProps {
  productId: string;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function ReviewModal({ productId, isOpen, onClose, onSuccess }: ReviewModalProps) {
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (rating === 0) {
      toast.error('Lütfen bir puan seçin.');
      return;
    }
    if (!comment.trim()) {
      toast.error('Lütfen bir değerlendirme metni yazın.');
      return;
    }

    try {
      setIsSubmitting(true);
      await reviewService.createReview(productId, { rating, comment });
      toast.success('Değerlendirmeniz başarıyla gönderildi.');
      onSuccess();
      onClose();
      // Reset state for future opening
      setRating(0);
      setHoverRating(0);
      setComment('');
    } catch (error: any) {
      if (error.response?.status === 403) {
        toast.error('Yalnızca ürünü sipariş etmiş olan kullanıcılar değerlendirme yapabilir.');
      }
      // other errors are handled by axios interceptor
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Ürünü Değerlendir</DialogTitle>
          <DialogDescription>
            Puan vererek ve yorumunuzu yazarak diğer kullanıcılara yardımcı olabilirsiniz.
          </DialogDescription>
        </DialogHeader>

        <div className="py-4 space-y-4">
          <div className="flex flex-col items-center justify-center gap-2">
            <span className="text-sm font-medium">Puanınız</span>
            <div className="flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  className="focus:outline-none transition-transform hover:scale-110"
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  onClick={() => setRating(star)}
                >
                  <Star
                    className={cn(
                      'h-8 w-8',
                      (hoverRating || rating) >= star
                        ? 'fill-amber-400 text-amber-400'
                        : 'text-muted-foreground/30'
                    )}
                  />
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <span className="text-sm font-medium">Değerlendirmeniz</span>
            <Textarea
              placeholder="Ürün hakkındaki düşüncelerinizi buraya yazın..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              className="min-h-[120px] resize-none"
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            İptal
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting} className="bg-violet-600 hover:bg-violet-700">
            {isSubmitting ? 'Gönderiliyor...' : 'Değerlendirmeyi Gönder'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
