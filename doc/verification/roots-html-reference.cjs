// Existing Playwright/Chrome only. Serve doc on localhost:8767; do not alter the prototype.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const output = process.argv[2];
if (!output) throw new Error('Usage: node roots-html-reference.cjs OUTPUT');
(async () => {
    const browser = await chromium.launch({ channel: 'chrome' });
    const manifest = [];
    try {
        const page = await browser.newPage({ viewport: { width: 900, height: 1100 } });
        for (const language of ['ru', 'en']) for (const theme of ['light', 'dark']) for (const scale of ['1.0', '1.5']) {
            const configuration = `${language}-${theme}-${scale}`;
            const folder = path.join(output, configuration);
            fs.mkdirSync(folder, { recursive: true });
            for (const [name, route] of [
                ['CAT-01', 'textbooks/?standalone=1#catalog'],
                ['CAT-18', 'textbooks/?standalone=1#references'],
                ['APP-03', 'web/?standalone=1#dictionary'],
            ]) {
                await page.goto('http://127.0.0.1:8767/design-system/prototypes/' + route);
                await page.selectOption('#locale', language);
                await page.selectOption('#theme', theme);
                await page.evaluate(scale => {
                    const phone = document.querySelector('#phone');
                    phone.classList.toggle('large-text', scale === '1.5');
                    phone.style.setProperty('--scale', scale);
                }, scale);
                await page.waitForTimeout(900);
                await page.evaluate(() => document.fonts.ready);
                await page.locator('#phone').screenshot({ path: path.join(folder, name + '.png') });
            }
            manifest.push({ configuration, screens: ['CAT-01', 'CAT-18', 'APP-03'], cssScale: scale });
            fs.writeFileSync(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
            console.log(configuration);
        }
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
