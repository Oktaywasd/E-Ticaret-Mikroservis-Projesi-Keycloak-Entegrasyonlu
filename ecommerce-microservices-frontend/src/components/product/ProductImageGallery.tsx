import { useState } from 'react';
import { Package } from 'lucide-react';
import { cn } from '@/lib/utils'; // Assuming cn exists

interface ProductImageGalleryProps {
  images?: string[];
}

export function ProductImageGallery({ images = [] }: ProductImageGalleryProps) {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [imgError, setImgError] = useState(false);

  const safeImages = images.filter(Boolean);
  const currentImage = safeImages[selectedIndex] || safeImages[0];

  const handleImageError = () => {
    setImgError(true);
  };

  const handleSelectImage = (index: number) => {
    setSelectedIndex(index);
    setImgError(false); // Reset error state when changing image
  };

  if (!safeImages.length) {
    return (
      <div className="relative aspect-square rounded-xl overflow-hidden bg-muted/30 border border-border/50">
        <div className="flex h-full w-full items-center justify-center">
          <Package className="h-24 w-24 text-muted-foreground/20" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Main Image */}
      <div className="relative aspect-square rounded-xl overflow-hidden bg-muted/30 border border-border/50">
        {!imgError && currentImage ? (
          <img
            src={currentImage}
            alt="Product visual"
            className="h-full w-full object-contain"
            onError={handleImageError}
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <Package className="h-24 w-24 text-muted-foreground/20" />
          </div>
        )}
      </div>

      {/* Thumbnail row */}
      {safeImages.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
          {safeImages.map((img, i) => (
            <button
              key={i}
              onClick={() => handleSelectImage(i)}
              className={cn(
                "flex-shrink-0 h-16 w-16 rounded-lg overflow-hidden border-2 transition-colors",
                selectedIndex === i
                  ? "border-violet-500 shadow-[0_0_0_1px_rgba(139,92,246,0.3)]" // adding slight focus glow if active
                  : "border-border/50 hover:border-border"
              )}
              aria-label={`Görsel ${i + 1}`}
            >
              <img 
                src={img} 
                alt={`Thumbnail ${i + 1}`} 
                className="h-full w-full object-cover" 
                onError={(e) => {
                  (e.target as HTMLImageElement).src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9IiMzMzMiIHN0cm9rZS13aWR0aD0iMiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIj48cGF0aCBkPSJNMyA5bTkgNWg2TTE2IDZ2NmwyIDJNMjEgMTBMMTEgMjFIM0wxMyAzbDEwIDEwWiIgLz48L3N2Zz4='; // small fallback or just transparent, but keeping it simple. Or hide it.
                  (e.target as HTMLImageElement).style.opacity = '0.1';
                }}
              />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
