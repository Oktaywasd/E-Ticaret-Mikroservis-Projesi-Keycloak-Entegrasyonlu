import { useEffect, useRef } from 'react';
import { useAuth } from 'react-oidc-context';
import { useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';

export function CallbackPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const hasRetried = useRef(false);

  useEffect(() => {
    console.log("CallbackPage auth state:", auth.user?.state);
    
    if (!auth.isLoading && auth.isAuthenticated) {
      const isRegisterFlow = sessionStorage.getItem("pending_register_flow") === "true";
      
      console.log("CallbackPage - isRegisterFlow:", isRegisterFlow);

      if (isRegisterFlow) {
        sessionStorage.removeItem("pending_register_flow");
        console.log("Register flow detected. Removing user and redirecting to login prompt.");
        auth.removeUser().then(() => {
          auth.signinRedirect({ prompt: "login" });
        });
        return;
      }

      console.log("Normal login flow detected. Redirecting to app.");
      const from = (auth.user?.state as { from?: string })?.from ?? "/";
      navigate(from, { replace: true });
    }
  }, [auth.isLoading, auth.isAuthenticated, auth.user, navigate, auth]);

  useEffect(() => {
    if (auth.error && !hasRetried.current) {
      hasRetried.current = true;
      auth.signinRedirect();
    }
  }, [auth.error, auth]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <Loader2 className="h-10 w-10 animate-spin text-violet-500" />
      <p className="text-sm text-muted-foreground">Giriş yapılıyor…</p>
    </div>
  );
}
