const assert = require('node:assert/strict');
const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage();
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  try {
    await page.goto('http://127.0.0.1:8873/design-system/prototypes/web/?standalone=1#recipes');
    await page.waitForSelector('#phone .plan-card');
    for (const language of ['ru', 'en']) {
      await page.selectOption('#locale', language);
      await page.evaluate(() => go('recipes'));
      await page.waitForFunction(expected => document.querySelector('#phone').innerText.includes(expected), language === 'ru' ? 'Рецепты' : 'Recipes');
      await page.evaluate(() => { initRecipe(null); go('recipe-editor'); });
      await page.waitForFunction(expected => document.querySelector('#phone').innerText.includes(expected), language === 'ru' ? 'Создать рецепт' : 'Create recipe');
      await page.evaluate(() => go('home'));
      const home = await page.locator('#phone').innerText();
      assert(home.includes(language === 'ru' ? 'Выбранный рецепт' : 'Selected recipe'));
      assert(home.includes(language === 'ru' ? 'Новая тренировка' : 'New training'));
    }
    assert.deepEqual(errors, []);
    console.log('OK: RU/EN Recipes → Create recipe; Selected recipe → New training, no JS errors');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
