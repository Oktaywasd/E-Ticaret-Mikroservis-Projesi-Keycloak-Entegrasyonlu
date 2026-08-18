import { test, expect } from '@playwright/test';

test.describe('E-Ticaret E2E Tests', () => {
  
  test('Ana sayfa yükleniyor ve ürün listesi görüntüleniyor', async ({ page }) => {
    await page.goto('/');
    
    // Check main title
    await expect(page.locator('h1').first()).toContainText('Hoş Geldiniz');

    // Go to products
    await page.click('text=Alışverişe Başla');
    await expect(page).toHaveURL(/.*\/products/);
    
    // Check if products are loaded
    await expect(page.locator('text=Tüm Ürünler')).toBeVisible();
  });

  test('Giriş yapılmadan ödeme sayfasına gidilmeye çalışıldığında hata', async ({ page }) => {
    await page.goto('/checkout');
    
    // AuthProvider should intercept or ProtectedRoute should show Unauthorized
    // If not logged in, Keycloak should redirect to login or show unauthorized
    // Wait for either the unauthorized page or keycloak login redirect
    const url = page.url();
    expect(url.includes('auth') || url.includes('unauthorized')).toBeTruthy();
  });
});
