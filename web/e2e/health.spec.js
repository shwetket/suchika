const { test, expect } = require('@playwright/test');

test.describe('Health pages', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('healthuser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('Vitals page loads with profile picker', async ({ page }) => {
    await page.goto('/health/vitals');
    await expect(page.getByRole('heading', { name: /vital/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('Doctor Visits page loads with profile picker', async ({ page }) => {
    await page.goto('/health/doctors');
    await expect(page.getByRole('heading', { name: /doctor/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('/health URL redirects to /health/vitals', async ({ page }) => {
    await page.goto('/health');
    await expect(page).toHaveURL('/health/vitals');
  });
});
