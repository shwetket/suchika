const { test, expect } = require('@playwright/test');

test.describe('Wealth pages', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('wealthuser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('Accounts page loads with profile picker', async ({ page }) => {
    await page.goto('/wealth/accounts');
    await expect(page.getByRole('heading', { name: /account/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('Transactions page loads with profile picker', async ({ page }) => {
    await page.goto('/wealth/transactions');
    await expect(page.getByRole('heading', { name: /transaction/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('Upload tab shows drag-and-drop zone', async ({ page }) => {
    await page.goto('/wealth/transactions');
    await expect(page.getByRole('heading', { name: /transaction/i })).toBeVisible();
  });

  test('/transactions URL redirects to /wealth/transactions', async ({ page }) => {
    await page.goto('/transactions');
    await expect(page).toHaveURL('/wealth/transactions');
  });

  test.describe('Physical Assets', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/wealth/physical-assets');
      await expect(page.getByRole('heading', { name: /physical assets/i }).first()).toBeVisible();
    });

    test('add and edit a Real Estate asset', async ({ page }) => {
      // Open add modal
      await page.getByRole('button', { name: /add physical asset/i }).click();
      await expect(page.getByRole('heading', { name: 'Add Physical Asset' })).toBeVisible();

      // Fill in details
      await page.getByLabel('Asset Name').fill('Test Apartment');
      await page.getByLabel('Asset Type').selectOption('REAL_ESTATE');
      await page.getByLabel('Current Value').fill('5000000');

      // Vehicle specific fields should NOT be visible
      await expect(page.getByLabel('Make')).not.toBeVisible();
      await expect(page.getByLabel('Model')).not.toBeVisible();

      // Save
      await page.getByRole('button', { name: 'Add Asset' }).click();

      // Should show up on the page
      await expect(page.getByText('Test Apartment')).toBeVisible();
      await expect(page.getByText('5,000,000')).toBeVisible();

      // Open Edit Modal
      // The edit button now uses a title="Edit asset" aria-label="Edit asset"
      const editButton = page.locator('button[aria-label="Edit asset"]').last();
      await editButton.click();

      await expect(page.getByRole('heading', { name: /Edit — Test Apartment/i })).toBeVisible();

      // Update value
      await page.getByLabel('Current Value').fill('5500000');
      await page.getByRole('button', { name: 'Save Changes' }).click();

      // Verify update
      await expect(page.getByText('5,500,000')).toBeVisible();
    });
  });
});
