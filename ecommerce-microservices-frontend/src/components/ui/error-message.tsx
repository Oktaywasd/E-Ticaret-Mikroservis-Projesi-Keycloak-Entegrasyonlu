import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { AxiosError } from 'axios';

interface ErrorMessageProps {
  error: Error | AxiosError | null | unknown;
  onRetry?: () => void;
  className?: string;
}

function getErrorMessage(error: unknown): string {
  if (!error) return 'Bilinmeyen hata';
  const e = error as { response?: { data?: { message?: string } }; message?: string };
  return e?.response?.data?.message ?? e?.message ?? 'Beklenmeyen bir hata oluştu';
}

export function ErrorMessage({ error, onRetry, className }: ErrorMessageProps) {
  return (
    <div className={`flex flex-col items-center justify-center gap-4 py-12 text-center ${className ?? ''}`}>
      <div className="rounded-full bg-destructive/10 p-4">
        <AlertTriangle className="h-8 w-8 text-destructive" />
      </div>
      <div className="space-y-1">
        <p className="font-semibold">Bir hata oluştu</p>
        <p className="text-sm text-muted-foreground max-w-xs">{getErrorMessage(error)}</p>
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCw className="h-4 w-4 mr-2" />
          Tekrar Dene
        </Button>
      )}
    </div>
  );
}
