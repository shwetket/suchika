const { test, expect } = require('@playwright/test');

test.describe('Action Center page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('actioncenteruser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
    await page.goto('/action-center');
  });

  test('page loads with heading and description', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Action Center' })).toBeVisible();
    await expect(page.getByText(/alerts across the household/i)).toBeVisible();
  });

  test('shows guidance message when signed-in demo user has no linked profile', async ({
    page,
  }) => {
    // Demo sign-in fallback does not attach a profile_id, so the page should
    // show the "no linked profile" message rather than the alert sections.
    await expect(page.getByText(/sign in with a linked profile to view alerts/i)).toBeVisible();
  });

  test('Refresh button is visible and disabled without a linked profile', async ({ page }) => {
    const refreshButton = page.getByRole('button', { name: /refresh/i });
    await expect(refreshButton).toBeVisible();
    await expect(refreshButton).toBeDisabled();
  });
});
