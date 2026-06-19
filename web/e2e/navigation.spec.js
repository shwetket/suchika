const { test, expect } = require('@playwright/test');

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill('navuser');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('nav shows Dashboard, Profiles, Wealth and Health after sign-in', async ({ page }) => {
    const nav = page.getByRole('navigation');
    await expect(nav.getByRole('link', { name: 'Dashboard', exact: true })).toBeVisible();
    // Nav Profiles link has exact text "Profiles"; scope to nav to avoid dashboard card
    await expect(nav.getByRole('link', { name: 'Profiles', exact: true })).toBeVisible();
    await expect(nav.getByRole('button', { name: /Wealth/i })).toBeVisible();
    await expect(nav.getByRole('button', { name: /Health/i })).toBeVisible();
  });

  test('Wealth dropdown shows Accounts and Transactions', async ({ page }) => {
    const nav = page.getByRole('navigation');
    await nav.getByRole('button', { name: /Wealth/i }).click();
    // Scope to nav to avoid matching page content that contains "Accounts" substring
    await expect(nav.getByRole('link', { name: 'Accounts', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Transactions', exact: true })).toBeVisible();
  });

  test('Health dropdown shows Vitals and Doctor Visits', async ({ page }) => {
    const nav = page.getByRole('navigation');
    await nav.getByRole('button', { name: /Health/i }).click();
    // Scope to nav to avoid matching dashboard card links whose accessible names contain "vitals"
    await expect(nav.getByRole('link', { name: 'Vitals', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Doctor Visits', exact: true })).toBeVisible();
  });

  test('Dashboard cards link to correct domain pages', async ({ page }) => {
    // Profiles card — first link with name 'Profiles' on the page
    await page.getByRole('link', { name: 'Profiles' }).first().click();
    await expect(page).toHaveURL('/household/profiles');
    await page.goBack();

    // Wealth card
    await page.getByRole('link', { name: 'Wealth' }).click();
    await expect(page).toHaveURL('/wealth/accounts');
    await page.goBack();

    // Health card
    await page.getByRole('link', { name: 'Health' }).click();
    await expect(page).toHaveURL('/health/vitals');
  });
});
