import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface PaginationProps {
  currentPage: number;      // 0-indexed (matches Spring Pageable)
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}

export function Pagination({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  const from = currentPage * pageSize + 1;
  const to = Math.min((currentPage + 1) * pageSize, totalElements);

  // Build visible page numbers (show max 5 around current)
  const getPages = () => {
    const pages: number[] = [];
    const delta = 2;
    const left = Math.max(0, currentPage - delta);
    const right = Math.min(totalPages - 1, currentPage + delta);
    for (let i = left; i <= right; i++) pages.push(i);
    return pages;
  };

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4">
      <p className="text-sm text-muted-foreground">
        {from}–{to} / {totalElements} ürün gösteriliyor
      </p>
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(0)}
          disabled={currentPage === 0}
          aria-label="İlk sayfa"
        >
          <ChevronsLeft className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
          aria-label="Önceki sayfa"
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>

        {getPages().map((p) => (
          <Button
            key={p}
            variant={p === currentPage ? 'default' : 'outline'}
            size="icon"
            className={`h-8 w-8 text-xs ${
              p === currentPage ? 'bg-violet-600 hover:bg-violet-700 border-violet-600' : ''
            }`}
            onClick={() => onPageChange(p)}
            aria-label={`Sayfa ${p + 1}`}
            aria-current={p === currentPage ? 'page' : undefined}
          >
            {p + 1}
          </Button>
        ))}

        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          aria-label="Sonraki sayfa"
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(totalPages - 1)}
          disabled={currentPage >= totalPages - 1}
          aria-label="Son sayfa"
        >
          <ChevronsRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
