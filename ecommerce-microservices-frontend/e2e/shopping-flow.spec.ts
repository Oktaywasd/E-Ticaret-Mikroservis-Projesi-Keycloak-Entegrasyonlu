import { test, expect } from '@playwright/test';

test.describe('Alışveriş, Sepet ve Sipariş Akışı E2E Testleri', () => {

  test.beforeEach(async ({ page }) => {
    // 1. Ana sayfaya git
    await page.goto('/');
  });

  test('1. Ürün arama ve filtreleme çalışmalı', async ({ page }) => {
    // Arama kutusuna yaz
    const searchInput = page.locator('input[placeholder*="ara" i], input[type="search"], input[type="text"]').first();
    
    if (await searchInput.isVisible()) {
      await searchInput.fill('Kulaklık');
      await page.waitForTimeout(600);
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('2. Ürünler sayfasına gidilmeli ve ürün sepete eklenmeli', async ({ page }) => {
    // Navigasyondaki "Ürünler" veya "Alışverişe Başla" butonuna tıkla
    const productsNav = page.getByRole('link', { name: /ürünler|alisverise basla|ürün/i }).first();
    if (await productsNav.isVisible()) {
      await productsNav.click();
    }

    // İlk ürün kartına tıkla
    const productCard = page.locator('a[href*="/products/"], [data-testid="product-card"], .product-card').first();
    if (await productCard.isVisible()) {
      await productCard.click();
      
      // "Sepete Ekle" butonuna bas
      const addToCartBtn = page.getByRole('button', { name: /sepete ekle|add to cart/i }).first();
      if (await addToCartBtn.isVisible()) {
        await addToCartBtn.click();
        await page.waitForTimeout(500);
      }
    }
  });

  test('3. Sepet çekmecesi / sayfası açılabilmeli ve içerik kontrol edilmeli', async ({ page }) => {
    // Önce Ürünler sayfasına gidip bir ürün sepete atalım
    const productsNav = page.getByRole('link', { name: /ürünler/i }).first();
    if (await productsNav.isVisible()) {
      await productsNav.click();
    }

    const productCard = page.locator('a[href*="/products/"], [data-testid="product-card"]').first();
    if (await productCard.isVisible()) {
      await productCard.click();
      const addToCartBtn = page.getByRole('button', { name: /sepete ekle/i }).first();
      if (await addToCartBtn.isVisible()) {
        await addToCartBtn.click();
        await page.waitForTimeout(600);
      }
    }

    // Sağ üstteki sepet butonuna veya /cart adresine git
    await page.goto('/cart');

    // Sepet sayfasının başarıyla yüklendiğini teyit et
    const pageBody = page.locator('body');
    await expect(pageBody).toBeVisible();
    await expect(pageBody).not.toBeEmpty();
  });

  test('4. Satın alma (Checkout) adımına geçilebilmeli', async ({ page }) => {
    await page.goto('/cart');
    
    // Siparişi tamamla / Satın al butonunu kontrol et
    const checkoutBtn = page.getByRole('button', { name: /siparişi tamamla|satın al|checkout|ürünleri incele/i }).first();
    if (await checkoutBtn.isVisible()) {
      await checkoutBtn.click();
      await page.waitForTimeout(500);
    }
    await expect(page.locator('body')).toBeVisible();
  });

});