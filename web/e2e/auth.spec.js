const { test, expect } = require('@playwright/test');

test.describe('Authentication', () => {
  test('home page shows Sign In link when unauthenticated', async ({ page }) => {
    await page.goto('/');
    // Two Sign In links exist (nav + hero), use first
    await expect(page.getByRole('link', { name: 'Sign In' }).first()).toBeVisible();
  });

  test('sign in with demo credentials redirects to dashboard', async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('testuser');
    await page.getByLabel('Role').selectOption('user');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
    await expect(page.getByText('Welcome back, testuser')).toBeVisible();
  });

  test('after sign-in, home page redirects to dashboard', async ({ page }) => {
    // Sign in first
    await page.goto('/signin');
    await page.getByLabel('Username').fill('testuser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');

    // Visiting / should now redirect to /dashboard
    await page.goto('/');
    await expect(page).toHaveURL('/dashboard');
  });

  test('logout returns to home page', async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('testuser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
    await page.getByRole('button', { name: 'Logout' }).click();
    await expect(page.getByRole('link', { name: 'Sign In' }).first()).toBeVisible();
  });
});
