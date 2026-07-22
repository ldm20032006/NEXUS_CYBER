import { expect, test } from '@playwright/test'

test('serves installable PWA metadata and service worker', async ({ page, request }) => {
  await page.goto('/')
  await expect(page).toHaveTitle(/NEXUS/)

  const manifest = await request.get('/manifest.webmanifest')
  expect(manifest.ok()).toBeTruthy()
  const body = await manifest.json()
  expect(body.name).toBe('NEXUS Smart Cyber Esports')
  expect(body.display).toBe('standalone')
  expect(body.icons.length).toBeGreaterThanOrEqual(2)

  const serviceWorker = await request.get('/sw.js')
  expect(serviceWorker.ok()).toBeTruthy()
  const serviceWorkerBody = await serviceWorker.text()
  expect(serviceWorkerBody).toContain('nexus-pwa-v1')
  expect(serviceWorkerBody).toContain('/offline.html')
})

test('keeps the app shell responsive without horizontal overflow', async ({ page }) => {
  await page.goto('/')
  await page.waitForLoadState('networkidle')

  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1)
  expect(overflow).toBe(false)
  await expect(page.locator('body')).toBeVisible()
})
