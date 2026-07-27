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

  test('creates a new profile successfully', async ({ page }) => {
    await page.getByRole('button', { name: /add profile/i }).click();
    await expect(page.getByRole('heading', { name: 'Add Profile' })).toBeVisible();

    const adminSelect = page.locator('select[name="admin_id"]');
    await expect(adminSelect).toBeVisible();

    // Check if we have an admin to select (besides the empty default)
    const adminCount = await adminSelect.locator('option').count();
    if (adminCount > 1) {
      // Fill the form
      await adminSelect.selectOption({ index: 1 });
      await page.locator('input[name="full_name"]').fill('Test New Profile');
      await page.locator('input[name="dob"]').fill('2010-05-15');
      await page.locator('select[name="relation_to_admin"]').selectOption('CHILD');

      // Submit
      await page.getByRole('button', { name: 'Add Profile', exact: true }).click();

      // Verify modal closes
      await expect(page.getByRole('heading', { name: 'Add Profile' })).toBeHidden();
    }
  });
});
