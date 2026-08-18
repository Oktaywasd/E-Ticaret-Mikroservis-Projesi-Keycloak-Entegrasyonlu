import { UserManager } from "oidc-client-ts";
import type { AuthContextProps } from "react-oidc-context";

export async function redirectToRegister(auth: AuthContextProps) {
  // Aynı ayarlarla (authority, client_id, redirect_uri, scope vs.) yeni bir UserManager
  // oluşturuyoruz ki state/PKCE, AuthProvider'ın kullandığı sessionStorage ile uyumlu üretilsin.
  const userManager = new UserManager(auth.settings);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const request = await (userManager as any)._client.createSigninRequest({});
  const registerUrl = request.url.replace(
    "/protocol/openid-connect/auth",
    "/protocol/openid-connect/registrations"
  );
  
  console.log("redirectToRegister generated URL:", registerUrl);
  sessionStorage.setItem("pending_register_flow", "true");
  
  window.location.href = registerUrl;
}
