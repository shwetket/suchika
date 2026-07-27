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

  test('logs a new vital reading', async ({ page }) => {
    await page.goto('/health/vitals');
    const profilePicker = page.getByRole('combobox').first();
    await expect(profilePicker).toBeVisible();
    const optionCount = await profilePicker.locator('option').count();
    if (optionCount > 1) {
      await profilePicker.selectOption({ index: 1 });
      await page.getByRole('button', { name: /log reading/i }).click();
      await expect(page.getByRole('heading', { name: 'Log Vital Reading' })).toBeVisible();

      // Fill form
      await page.getByLabel('Weight').fill('70');
      await page.getByLabel('Blood Pressure (Sys)').fill('120');
      await page.getByLabel('Blood Pressure (Dia)').fill('80');
      await page.getByLabel('Heart Rate').fill('75');
      await page.getByLabel('Body Temperature').fill('98.6');

      await page.getByRole('button', { name: 'Save Reading' }).click();
      await expect(page.getByRole('heading', { name: 'Log Vital Reading' })).toBeHidden();
    }
  });

  test('adds a new doctor visit', async ({ page }) => {
    await page.goto('/health/doctors');
    const profilePicker = page.getByRole('combobox').first();
    await expect(profilePicker).toBeVisible();
    const optionCount = await profilePicker.locator('option').count();
    if (optionCount > 1) {
      await profilePicker.selectOption({ index: 1 });
      await page.getByRole('button', { name: /add visit/i }).click();
      await expect(page.getByRole('heading', { name: 'Add Doctor Visit' })).toBeVisible();

      // Fill form
      await page.getByLabel('Doctor Name').fill('Dr. Smith');
      await page.getByLabel('Specialization').fill('Cardiologist');
      await page.getByLabel('Visit Date').fill('2026-07-20');
      await page.getByLabel('Notes').fill('Routine checkup');

      await page.getByRole('button', { name: 'Save Visit' }).click();
      await expect(page.getByRole('heading', { name: 'Add Doctor Visit' })).toBeHidden();
    }
  });
});
