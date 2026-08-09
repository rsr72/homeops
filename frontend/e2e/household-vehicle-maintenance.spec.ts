import { expect, test } from '@playwright/test';

function uniqueName(prefix: string) {
  return `${prefix} ${Date.now()}-${Math.floor(Math.random() * 10000)}`;
}

test('Household -> Vehicle -> Maintenance Event core workflow persists through refresh', async ({ page, request }) => {
  const householdName = uniqueName('E2E Household');
  const vehicleMake = 'Honda';
  const vehicleModel = 'Accord';
  const vehicleYear = '2023';
  const initialDescription = uniqueName('Oil change');
  const updatedDescription = `${initialDescription} updated`;
  let householdId: string | null = null;

  try {
    await page.goto('/');

    await page.getByLabel(/household name/i).fill(householdName);
    await page.getByRole('button', { name: /create household/i }).click();

    await expect(page.getByRole('button', { name: new RegExp(householdName, 'i') })).toBeVisible();
    householdId = new URL(page.url()).searchParams.get('householdId');
    expect(householdId).not.toBeNull();

    await page.getByRole('button', { name: /add vehicle/i }).first().click();
    await page.getByLabel(/^make$/i).fill(vehicleMake);
    await page.getByLabel(/^model$/i).fill(vehicleModel);
    await page.getByLabel(/^year$/i).fill(vehicleYear);
    await page.getByRole('button', { name: /create vehicle/i }).click();

    const vehicleCard = page.getByRole('article', { name: new RegExp(`${vehicleMake} ${vehicleModel}`, 'i') });
    await expect(vehicleCard).toBeVisible();

    await vehicleCard.getByRole('button', { name: /maintenance/i }).click();
    await expect(page.getByRole('heading', { name: new RegExp(`${vehicleMake} ${vehicleModel} maintenance`, 'i') })).toBeVisible();

    await page.getByRole('button', { name: /add event/i }).first().click();
    await page.getByLabel(/service date/i).fill('2026-08-09');
    await page.getByLabel(/description/i).fill(initialDescription);
    await page.getByLabel(/mileage/i).fill('15000');
    await page.getByLabel(/cost/i).fill('99.50');
    await page.locator('#maintenance-notes').fill('Created by Playwright E2E');
    await page.getByRole('button', { name: /create event/i }).click();

    const eventCard = page.getByRole('article', { name: new RegExp(initialDescription, 'i') });
    await expect(eventCard).toBeVisible();

    await page.reload();

    await expect(page.getByRole('button', { name: new RegExp(householdName, 'i') })).toBeVisible();
    await expect(page.getByRole('article', { name: new RegExp(`${vehicleMake} ${vehicleModel}`, 'i') })).toBeVisible();
    const persistedEventCard = page.getByRole('article', { name: new RegExp(initialDescription, 'i') });
    await expect(persistedEventCard).toBeVisible();

    await persistedEventCard.getByRole('button', { name: /^edit$/i }).click();
    await page.getByLabel(/description/i).fill(updatedDescription);
    await page.getByRole('button', { name: /save event/i }).click();

    const updatedEventCard = page.getByRole('article', { name: new RegExp(updatedDescription, 'i') });
    await expect(updatedEventCard).toBeVisible();

    await updatedEventCard.getByRole('button', { name: /^delete$/i }).click();
    await page.getByRole('button', { name: /confirm delete/i }).click();

    await expect(page.getByRole('heading', { name: 'No maintenance events yet' })).toBeVisible();
  } finally {
    if (householdId) {
      await request.delete(`/api/households/${householdId}`);
    }
  }
});
