import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { SearchX } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-6 px-4 text-center">
      <div className="rounded-full bg-muted p-6">
        <SearchX className="h-12 w-12 text-muted-foreground" />
      </div>
      <div className="space-y-2">
        <h1 className="text-6xl font-bold text-muted-foreground">404</h1>
        <h2 className="text-2xl font-bold">Sayfa Bulunamadı</h2>
        <p className="text-muted-foreground max-w-sm">
          Aradığınız sayfa mevcut değil veya taşınmış olabilir.
        </p>
      </div>
      <Button asChild>
        <Link to="/">Ana Sayfaya Dön</Link>
      </Button>
    </div>
  );
}
