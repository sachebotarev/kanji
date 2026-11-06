// Existing Playwright/Chrome only. Start an HTTP server rooted at doc on port 8767.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const radius = JSON.parse(fs.readFileSync(path.join(__dirname, '../design-system/theme/theme-source.json'))).component_shape.text_field;
const output = process.argv[2];
if (!output) throw Error('Usage: node input-shapes.cjs OUTPUT');
(async () => {
    const browser = await chromium.launch({ channel: 'chrome' });
    const manifest = [];
    try {
        const page = await browser.newPage({ viewport: { width: 900, height: 1100 } });
        await page.goto('http://127.0.0.1:8767/design-system/prototypes/web/?standalone=1#home');
        for (const language of ['ru', 'en']) for (const theme of ['light', 'dark']) for (const scale of [1, 1.5]) {
            await page.selectOption('#locale', language);
            await page.selectOption('#theme', theme);
            await page.evaluate(scale => {
                document.querySelector('#phone').style.setProperty('--scale', scale);
                document.querySelector('#phone').classList.toggle('large-text', scale === 1.5);
            }, scale);
            await page.waitForTimeout(700);
            const configuration = `${language}-${theme}-${scale}`;
            for (const view of ['word', 'recipe', 'tag', 'search', 'settings']) {
                await page.evaluate(view => {
                    closeModal();
                    if (view === 'word') { initDraft(); go('editor'); }
                    if (view === 'recipe') { initTraining('tobira10'); go('recipe-editor'); }
                    if (view === 'tag') { sourceDraft = newMaterialDraft('dictionary'); beginDictionaryTagSelection('material'); go('dictionary-tags'); editDictionaryTag(); }
                    if (view === 'search') { trainingSearchOpen = true; go('recipes'); }
                    if (view === 'settings') go('checker-settings');
                }, view);
                await page.evaluate(() => document.fonts.ready);
                await page.waitForTimeout(100);
                const checked = await page.evaluate(radius => {
                    const fields = [...document.querySelectorAll('#phone input:not([type=radio],[type=checkbox],[type=range]),#phone textarea,#phone select')].filter(e => e.getClientRects().length);
                    if (!fields.length) throw Error('No visible input');
                    for (const field of fields) {
                        const style = getComputedStyle(field);
                        for (const corner of ['borderTopLeftRadius', 'borderTopRightRadius', 'borderBottomLeftRadius', 'borderBottomRightRadius']) {
                            if (style[corner] !== `${radius}px`) throw Error(`${field.outerHTML}: ${corner}=${style[corner]}`);
                        }
                    }
                    return fields.length;
                }, radius);
                const folder = path.join(output, configuration);
                fs.mkdirSync(folder, { recursive: true });
                await page.locator('#phone').screenshot({ path: path.join(folder, view + '.png') });
                manifest.push({ configuration, view, checked, radius });
            }
        }
        // A theme override changes all text-like inputs, but never checkboxes, radios or sliders.
        await page.evaluate(() => {
            closeModal(); initDraft(); go('editor');
            const probe = document.createElement('div'); probe.id = 'input-shape-probe';
            probe.innerHTML = ['text', 'search', 'number', 'email', 'password', 'tel', 'url', 'date', 'time', 'checkbox', 'radio', 'range']
                .map(type => `<input type="${type}">`).join('') + '<textarea></textarea><select><option>probe</option></select>';
            document.querySelector('#phone').append(probe);
            const controls = [...probe.querySelectorAll('[type=checkbox],[type=radio],[type=range]')];
            const before = controls.map(e => getComputedStyle(e).borderRadius);
            document.querySelector('#phone').style.setProperty('--app-input-corner-radius', '23px');
            for (const e of probe.querySelectorAll('input:not([type=checkbox],[type=radio],[type=range]),textarea,select')) {
                if (getComputedStyle(e).borderRadius !== '23px') throw Error('Theme override not propagated');
            }
            if (controls.some((e, i) => getComputedStyle(e).borderRadius !== before[i])) throw Error('Non-text control changed');
            document.querySelector('#phone').style.removeProperty('--app-input-corner-radius'); probe.remove();
        });
        for (const language of ['ru', 'en']) for (const theme of ['light', 'dark']) for (const scale of [1, 1.5]) {
            await page.goto('http://127.0.0.1:8767/design-system/prototypes/textbooks/?standalone=1#all-kanji');
            await page.selectOption('#locale', language); await page.selectOption('#theme', theme);
            await page.evaluate(scale => document.querySelector('#phone').style.setProperty('--scale', scale), scale);
            await page.locator('[data-action="toggle-module-search"]').click();
            await page.locator('#catalog-query').waitFor({ state: 'visible' });
            await page.evaluate(radius => {
                for (const e of document.querySelectorAll('#phone .n-search,#catalog-query')) {
                    if (getComputedStyle(e).borderRadius !== `${radius}px`) throw Error('Catalogue search overrides input theme');
                }
            }, radius);
            const configuration = `${language}-${theme}-${scale}`;
            await page.locator('#phone').screenshot({ path: path.join(output, configuration, 'catalogue-search.png') });
            manifest.push({ configuration, view: 'catalogue-search', checked: 1, radius });
        }
        fs.writeFileSync(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
        console.log(`OK: ${manifest.length} HTML states; all four corners ${radius}px; live token override and non-text exclusions verified`);
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
