import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  /* Test dosyalarını paralel çalıştır */
  fullyParallel: true,
  /* CI ortamında kazara test.only kalırsa build'i kır */
  forbidOnly: !!process.env.CI,
  /* CI üzerinde 2 kere tekrar dene, yerelde hemen sonucu ver */
  retries: process.env.CI ? 2 : 0,
  /* Paralel worker sayısı */
  workers: process.env.CI ? 1 : undefined,
  /* HTML raporlama */
  reporter: 'html',

  use: {
    /* React uygulamanın çalıştığı port */
    baseURL: 'http://localhost:3000',

    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  /* İlk aşamada hızlı sonuç almak için Chromium'u ana tarayıcı yapıyoruz */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    // İleride çoklu tarayıcı testi istersen burayı açabilirsin:
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },
    // {
    //   name: 'webkit',
    //   use: { ...devices['Desktop Safari'] },
    // },
  ],
});