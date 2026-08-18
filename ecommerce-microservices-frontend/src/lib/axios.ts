import axios from 'axios';

import { toast } from 'sonner';

// Token getter — set by AuthProvider after login
let _getAccessToken: (() => string | undefined) | null = null;
let _onUnauthorized: (() => void) | null = null;

export function setAxiosAuthHandlers(
  getToken: () => string | undefined,
  onUnauthorized: () => void
) {
  _getAccessToken = getToken;
  _onUnauthorized = onUnauthorized;
}

function createAxiosInstance(baseURL: string) {
  const instance = axios.create({ baseURL });

  // Request interceptor — attach Bearer token
  instance.interceptors.request.use((config) => {
    const token = _getAccessToken?.();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  });

  // Response interceptor — handle 401 & global errors
  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      // 401 Unauthorized handling
      if (error.response?.status === 401) {
        toast.error('Oturumunuz süresi doldu veya geçersiz. Lütfen tekrar giriş yapın.');
        _onUnauthorized?.();
      } else if (error.response?.status === 403) {
        toast.error('Bu işlemi gerçekleştirmek için yetkiniz bulunmamaktadır.');
      } else {
        // Extract backend ErrorResponse or fallback to generic message
        const message = error.response?.data?.message || 'Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.';
        // Prevent showing toasts for 404s (e.g. Profile 404 is a valid state for first login)
        // Adjust if needed. For now, show toast for >= 500 or 400.
        if (error.response?.status >= 500 || error.response?.status === 400) {
          toast.error(message);
        }
      }
      return Promise.reject(error);
    }
  );

  return instance;
}

export const productApi = createAxiosInstance(
  import.meta.env.VITE_PRODUCT_CATALOG_API ?? 'http://localhost:8081/api/v1'
);

export const crmApi = createAxiosInstance(
  import.meta.env.VITE_CRM_API ?? 'http://localhost:8082/api/v1'
);

export const orderApi = createAxiosInstance(
  import.meta.env.VITE_ORDER_API ?? 'http://localhost:8083/api/v1'
);
