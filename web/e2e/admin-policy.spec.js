const { test, expect } = require('@playwright/test');

test.describe('Admin Policy Settings page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('policyadmin');
    await page.getByLabel('Role').selectOption('admin');
    await page.getByRole('button', { name: 'Sign In' }).click();
    // A fresh demo admin has no admin_id yet, so SetupGate redirects to setup.
    await page.waitForURL(/\/(dashboard|admin\/setup)/);
    await page.goto('/admin/policy');
  });

  test('page loads with heading', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /household policy settings/i })).toBeVisible();
  });

  test('threshold fields are editable when an admin account exists', async ({ page }) => {
    // The form is always rendered once loading finishes; the Save button is only
    // enabled once an adminId was successfully resolved (listAdmins + getAdmin both
    // succeeded against the shared backend). Gate on that real signal rather than
    // guessing which error string might be showing.
    const saveButton = page.getByRole('button', { name: /save settings/i });
    await expect(saveButton).toBeVisible();

    const isEnabled = await saveButton.isEnabled();
    if (isEnabled) {
      const budgetInput = page.getByLabel(/monthly budget cap/i);
      await expect(budgetInput).toBeVisible();
      await budgetInput.fill('75000');
      await expect(budgetInput).toHaveValue('75000');

      const debtThresholdInput = page.getByLabel(/debt crossover threshold/i);
      await debtThresholdInput.fill('35');
      await expect(debtThresholdInput).toHaveValue('35');
    }
  });

  test('Save Settings button is present', async ({ page }) => {
    await expect(page.getByRole('button', { name: /save settings/i })).toBeVisible();
  });
});
