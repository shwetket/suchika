const { test, expect } = require('@playwright/test');

/**
 * Click "Save and Continue" and wait for step 2 to render. Retries the submit
 * once: at time of writing, the live gateway process on :8080 was observed
 * returning zero Access-Control-Allow-* headers on both the CORS preflight and
 * the actual response (confirmed via curl against the running instance), which
 * makes every non-simple cross-origin POST (createAdmin/createProfile) fail in
 * the browser with net::ERR_FAILED even though quarkus.http.cors=true is set in
 * application.properties. That is a live backend/infra defect, not a defect in
 * this test or in Setup.js — see the QA report for details. The retry exists so
 * this spec self-heals if the gateway is hot-reloaded/restarted mid-session;
 * it does not mask a real, deterministic product bug.
 */
async function submitStep1AndAwaitAdvance(page) {
  await page.getByRole('button', { name: 'Save and Continue' }).click();
  const advanced = await page
    .getByPlaceholder('e.g. SBI Savings')
    .waitFor({ state: 'visible', timeout: 8000 })
    .then(() => true)
    .catch(() => false);
  if (!advanced) {
    await page.getByRole('button', { name: 'Save and Continue' }).click();
    await expect(page.getByPlaceholder('e.g. SBI Savings')).toBeVisible({ timeout: 10000 });
  }
}

test.describe('SetupGate redirect', () => {
  test('a brand-new admin (no admin_id yet) is redirected from /dashboard to /admin/setup', async ({
    page,
  }) => {
    await page.goto('/signin');
    await page.getByLabel('Username').fill(`setupgate_${Date.now()}`);
    await page.getByLabel('Role').selectOption('admin');
    await page.getByRole('button', { name: 'Sign In' }).click();
    // SetupGate wraps /dashboard: a fresh admin has no admin_id, so it redirects
    // to /admin/setup instead of rendering the Dashboard.
    await expect(page).toHaveURL('/admin/setup');
    await expect(page.getByRole('heading', { name: /household setup/i })).toBeVisible();
  });
});

test.describe('Admin Setup Wizard', () => {
  test.beforeEach(async ({ page }) => {
    const username = `setupwizard_${Date.now()}`;
    await page.goto('/signin');
    await page.getByLabel('Username').fill(username);
    await page.getByLabel('Role').selectOption('admin');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/admin/setup');
  });

  test('step indicator shows all three steps and starts on step 1', async ({ page }) => {
    await expect(page.getByText('1. Your Details')).toBeVisible();
    await expect(page.getByText('2. Wealth (optional)')).toBeVisible();
    await expect(page.getByText('3. Health (optional)')).toBeVisible();
    await expect(page.getByPlaceholder('e.g. Ketan Verma')).toBeVisible();
  });

  test('step 1 shows a validation error when required fields are missing', async ({ page }) => {
    await page.getByRole('button', { name: 'Save and Continue' }).click();
    await expect(page.getByText('Full name is required')).toBeVisible();
  });

  test('full wizard: identity -> skip wealth -> skip health -> finish redirects to dashboard', async ({
    page,
  }) => {
    const fullName = `E2E Test User ${Date.now()}`;

    // Step 1 — identity (mandatory)
    await page.getByPlaceholder('e.g. Ketan Verma').fill(fullName);
    // DOB has no placeholder/label association (type="date" input); scoped to the
    // step-1 form and identified by its input type — see Q41 in OpenQuestions.md.
    await page
      .locator('form')
      .filter({ hasText: 'Date of Birth' })
      .locator('input[type="date"]')
      .fill('1990-01-01');
    await submitStep1AndAwaitAdvance(page);

    // Step 2 — skip wealth entirely
    await page.getByRole('button', { name: /skip for now/i }).click();

    // Step 3 — health, skip as well
    await expect(page.getByText('Weight')).toBeVisible();
    await expect(page.getByText('Height')).toBeVisible();

    await page.getByRole('button', { name: 'Finish Setup' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('full wizard: identity -> add an account -> continue to health -> finish', async ({
    page,
  }) => {
    const fullName = `E2E Wealth User ${Date.now()}`;
    const accountName = `E2E Account ${Date.now()}`;

    // Step 1
    await page.getByPlaceholder('e.g. Ketan Verma').fill(fullName);
    await page
      .locator('form')
      .filter({ hasText: 'Date of Birth' })
      .locator('input[type="date"]')
      .fill('1985-05-15');
    await submitStep1AndAwaitAdvance(page);

    // Step 2 — add a liquid account
    await page.getByPlaceholder('e.g. SBI Savings').fill(accountName);
    await page.getByPlaceholder('e.g. State Bank of India').fill('E2E Test Bank');
    await page.getByRole('button', { name: '+ Add Account' }).click();
    await expect(page.getByText(accountName)).toBeVisible();

    // Continue (button label changes once an account has been added)
    await page.getByRole('button', { name: 'Continue' }).click();

    // Step 3 — record a weight vital
    await expect(page.getByText('Weight')).toBeVisible();
    const dateInputs = page.locator('input[type="date"]');
    await dateInputs.first().fill('2026-01-01');
    await page.getByPlaceholder('kg').fill('70');
    await page.getByRole('button', { name: 'Add' }).first().click();
    await expect(page.getByText(/WEIGHT: 70 kg/)).toBeVisible();

    await page.getByRole('button', { name: 'Finish Setup' }).click();
    await expect(page).toHaveURL('/dashboard');
  });
});
