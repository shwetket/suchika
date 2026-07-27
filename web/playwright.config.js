const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 1,
  timeout: 60000,
  expect: {
    timeout: 15000,
  },
  use: {
    baseURL: 'http://127.0.0.1:3000',
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // Do NOT start the dev server automatically — tests assume it's already running
  webServer: undefined,
});
