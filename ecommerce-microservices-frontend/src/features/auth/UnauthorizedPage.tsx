import { useAppAuth } from '@/hooks/useAppAuth';
import { Button } from '@/components/ui/button';
import { Link } from 'react-router-dom';
import { ShieldAlert, LogIn } from 'lucide-react';

export function UnauthorizedPage() {
  const auth = useAppAuth();
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-6 px-4 text-center">
      <div className="rounded-full bg-destructive/10 p-6">
        <ShieldAlert className="h-12 w-12 text-destructive" />
      </div>
      <div className="space-y-2">
        <h1 className="text-2xl font-bold">Erişim Reddedildi</h1>
        <p className="text-muted-foreground max-w-sm">
          Bu sayfayı görüntüleme yetkiniz bulunmuyor. Farklı bir hesapla giriş yapmayı deneyebilirsiniz.
        </p>
      </div>
      <div className="flex gap-3">
        <Button variant="outline" asChild>
          <Link to="/">Ana Sayfaya Dön</Link>
        </Button>
        {!auth.isAuthenticated && (
          <Button onClick={() => auth.signinRedirect()} id="unauthorized-login-button">
            <LogIn className="h-4 w-4 mr-2" />
            Giriş Yap
          </Button>
        )}
      </div>
    </div>
  );
}
