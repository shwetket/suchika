const { test, expect } = require('@playwright/test');

test.describe('Profiles page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('profileuser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
    await page.goto('/household/profiles');
  });

  test('profiles page loads with a heading', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /profile/i })).toBeVisible();
  });

  test('Add Profile button is visible', async ({ page }) => {
    await expect(page.getByRole('button', { name: /add profile/i })).toBeVisible();
  });
});
