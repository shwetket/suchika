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
    // AuthContext.js only attaches profile_id on a role:'admin' login (see
    // ADR-021) — the shared beforeEach above signs in as role:'user', which
    // intentionally leaves the Refresh button disabled (see
    // action-center.spec.js's "no linked profile" coverage of that same
    // state). Sign in again as admin so this test exercises the enabled path.
    await page.goto('/signin');
    await page.getByLabel('Username').fill('testuser');
    await page.getByLabel('Role').selectOption('admin');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');

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
    // Physical Assets lives inside the collapsible "Wealth" nav dropdown
    // (Navigation.js's NavDropdown) — it isn't rendered until that button is
    // expanded, same as navigation.spec.js already accounts for.
    const nav = page.getByRole('navigation');
    await nav.getByRole('button', { name: /wealth/i }).click();
    await nav.getByRole('link', { name: /physical assets/i }).click();
    await expect(page).toHaveURL(/.*\/wealth\/physical-assets/);
  });
});
