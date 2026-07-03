const { test, expect } = require('@playwright/test');

test.describe('Household pages', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('householduser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('Calendar page loads with profile picker', async ({ page }) => {
    await page.goto('/household/calendar');
    await expect(page.getByRole('heading', { name: /household calendar/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('Calendar Add Event flow opens modal when a profile is selected', async ({ page }) => {
    await page.goto('/household/calendar');
    const profilePicker = page.getByRole('combobox').first();
    await expect(profilePicker).toBeVisible();
    const optionCount = await profilePicker.locator('option').count();
    if (optionCount > 1) {
      await profilePicker.selectOption({ index: 1 });
      await page.getByRole('button', { name: /add event/i }).click();
      await expect(page.getByRole('heading', { name: 'Add Event' })).toBeVisible();
      await expect(page.getByPlaceholder('Event title')).toBeVisible();
    }
  });

  test('Inventory page loads with profile picker', async ({ page }) => {
    await page.goto('/household/inventory');
    await expect(page.getByRole('heading', { name: /household inventory/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('Inventory Add Item flow opens modal when a profile is selected', async ({ page }) => {
    await page.goto('/household/inventory');
    const profilePicker = page.getByRole('combobox').first();
    await expect(profilePicker).toBeVisible();
    const optionCount = await profilePicker.locator('option').count();
    if (optionCount > 1) {
      await profilePicker.selectOption({ index: 1 });
      await page.getByRole('button', { name: /add item/i }).click();
      await expect(page.getByRole('heading', { name: 'Add Item' })).toBeVisible();
      await expect(page.getByPlaceholder('e.g. Milk')).toBeVisible();
    }
  });

  test('Goals page loads with profile picker', async ({ page }) => {
    await page.goto('/household/goals');
    await expect(page.getByRole('heading', { name: /financial goals/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('Goals Add Goal flow opens modal when a profile is selected', async ({ page }) => {
    await page.goto('/household/goals');
    const profilePicker = page.getByRole('combobox').first();
    await expect(profilePicker).toBeVisible();
    const optionCount = await profilePicker.locator('option').count();
    if (optionCount > 1) {
      await profilePicker.selectOption({ index: 1 });
      await page.getByRole('button', { name: /add goal/i }).click();
      await expect(page.getByRole('heading', { name: 'Add Goal' })).toBeVisible();
      await expect(page.getByPlaceholder('e.g. Emergency Fund')).toBeVisible();
    }
  });

  test('/household/vacation-planner page loads', async ({ page }) => {
    await page.goto('/household/vacation-planner');
    await expect(page.getByRole('heading', { name: /vacation/i })).toBeVisible();
  });
});
