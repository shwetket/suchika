const { test, expect } = require('@playwright/test');

test.describe('Dashboard page', () => {
  test.beforeEach(async ({ page }) => {
    // Log in before each test
    await page.goto('/signin');
    await page.getByLabel('Username').fill('testuser');
    await page.getByLabel('Role').selectOption('user');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('loads Dashboard successfully', async ({ page }) => {
    // Look for the dashboard heading
    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible();

    // Look for the refresh button
    await expect(page.getByRole('button', { name: /refresh/i })).toBeVisible();
  });

  test('manual refresh works', async ({ page }) => {
    // Wait for auto-refresh to complete first
    // Then click "Refresh Live Data"
    await expect(page.getByRole('button', { name: /refresh live data/i })).toBeVisible({
      timeout: 10000,
    });

    await page.getByRole('button', { name: /refresh live data/i }).click();
    await expect(page.getByRole('button', { name: /refreshing/i })).toBeVisible();

    // Wait for it to return back to normal
    await expect(page.getByRole('button', { name: /refresh live data/i })).toBeVisible({
      timeout: 10000,
    });
  });

  test('navigate to other sections from sidebar', async ({ page }) => {
    // Check if sidebar navigation works
    await page.getByRole('link', { name: /physical assets/i }).click();
    await expect(page).toHaveURL(/.*\/wealth\/physical-assets/);
  });
});
