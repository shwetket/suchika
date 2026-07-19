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

  test('Inventory Add Item flow creates a new item successfully', async ({ page }) => {
    await page.goto('/household/inventory');
    const profilePicker = page.getByRole('combobox').first();
    await expect(profilePicker).toBeVisible();
    const optionCount = await profilePicker.locator('option').count();
    if (optionCount > 1) {
      await profilePicker.selectOption({ index: 1 });
      await page.getByRole('button', { name: /add item/i }).click();
      await expect(page.getByRole('heading', { name: 'Add Item' })).toBeVisible();
      
      // Fill in details
      await page.getByPlaceholder('e.g. Milk').fill('E2E Test Milk');
      await page.getByLabel('Category').selectOption('GROCERY');
      await page.getByLabel('Quantity').fill('2');
      await page.getByLabel('Unit').selectOption('GALLON');

      // Submit
      await page.getByRole('button', { name: 'Save Item' }).click();
      await expect(page.getByRole('heading', { name: 'Add Item' })).toBeHidden();
    }
  });

  test('Goals page loads with profile picker', async ({ page }) => {
    await page.goto('/household/goals');
    await expect(page.getByRole('heading', { name: /financial goals/i })).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('Goals Add Goal flow creates a new goal successfully', async ({ page }) => {
    await page.goto('/household/goals');
    const profilePicker = page.getByRole('combobox').first();
    await expect(profilePicker).toBeVisible();
    const optionCount = await profilePicker.locator('option').count();
    if (optionCount > 1) {
      await profilePicker.selectOption({ index: 1 });
      await page.getByRole('button', { name: /add goal/i }).click();
      await expect(page.getByRole('heading', { name: 'Add Goal' })).toBeVisible();
      
      // Fill in details
      await page.getByPlaceholder('e.g. Emergency Fund').fill('E2E Test Goal');
      await page.getByLabel('Target Amount').fill('10000');
      await page.getByLabel('Target Date').fill('2026-12-31');

      // Submit
      await page.getByRole('button', { name: 'Save Goal' }).click();
      await expect(page.getByRole('heading', { name: 'Add Goal' })).toBeHidden();
    }
  });

  test('/household/vacation-planner page loads', async ({ page }) => {
    await page.goto('/household/vacation-planner');
    await expect(page.getByRole('heading', { name: /vacation/i })).toBeVisible();
  });
});
