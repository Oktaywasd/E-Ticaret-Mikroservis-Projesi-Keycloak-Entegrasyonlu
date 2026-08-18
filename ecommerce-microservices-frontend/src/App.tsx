import { useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import { Toaster } from 'sonner';
import { AppRouter } from '@/app/router';
import { setAxiosAuthHandlers } from '@/lib/axios';

/**
 * Inner component that has access to the auth context.
 * Configures Axios interceptors with live token getter.
 */
function AppInner() {
  const auth = useAuth();

  useEffect(() => {
    setAxiosAuthHandlers(
      () => auth.user?.access_token,
      () => auth.signinRedirect()
    );
  }, [auth]);

  return <AppRouter />;
}

export default function App() {
  return (
    <>
      <Toaster richColors position="top-right" />
      <AppInner />
    </>
  );
}
