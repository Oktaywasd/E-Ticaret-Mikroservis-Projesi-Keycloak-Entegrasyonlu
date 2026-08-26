import { Star } from 'lucide-react';
import { cn } from '@/lib/utils'; // Assuming cn exists, else I'll use standard classes

interface StarRatingProps {
  rating?: number;
  reviewCount?: number;
  showText?: boolean;
  size?: number | string;
  className?: string;
}

export function StarRating({
  rating = 0.0,
  reviewCount,
  showText = true,
  size = 16,
  className,
}: StarRatingProps) {
  const fullStars = Math.floor(rating);
  const hasHalfStar = rating % 1 >= 0.5;
  const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

  return (
    <div className={cn("flex items-center gap-1", className)}>
      <div className="flex items-center">
        {[...Array(fullStars)].map((_, i) => (
          <Star
            key={`full-${i}`}
            size={size}
            className="fill-amber-400 text-amber-400"
          />
        ))}
        {hasHalfStar && (
          <div className="relative">
            <Star size={size} className="text-amber-400" />
            <div className="absolute inset-0 overflow-hidden w-1/2">
              <Star size={size} className="fill-amber-400 text-amber-400" />
            </div>
          </div>
        )}
        {[...Array(emptyStars)].map((_, i) => (
          <Star
            key={`empty-${i}`}
            size={size}
            className="text-amber-400"
          />
        ))}
      </div>
      {showText && (
        <span className="text-xs text-muted-foreground ml-1">
          {!reviewCount ? (
            rating > 0 ? rating.toFixed(1) : "(Henüz değerlendirilmedi)"
          ) : (
            `${rating.toFixed(1)} (${reviewCount} değerlendirme)`
          )}
        </span>
      )}
    </div>
  );
}
