// Uses an existing Playwright installation; does not install packages or modify the prototype.
// Run a local server rooted at doc, then set NODE_PATH if Playwright is not on Node's path.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const output = process.argv[2];
if (!output) throw new Error('Usage: node constructor-html-reference.cjs OUTPUT');
(async () => {
    const browser = await chromium.launch({ channel: 'chrome' });
    try {
        const page = await browser.newPage({ viewport: { width: 900, height: 1100 } });
        await page.goto('http://127.0.0.1:8767/design-system/prototypes/web/?standalone=1#recipes');
        await page.addStyleTag({ content: '* { transition: none !important; animation: none !important; }' });
        const manifest = [];
        for (const language of ['ru', 'en']) for (const theme of ['light', 'dark']) for (const scale of ['1.0', '1.5']) {
            await page.selectOption('#locale', language);
            await page.selectOption('#theme', theme);
            await page.evaluate(scale => {
                const phone = document.querySelector('#phone');
                phone.classList.toggle('large-text', scale === '1.5');
                phone.style.setProperty('--scale', scale);
            }, scale);
            await page.waitForTimeout(700); // Theme controls update asynchronously.
            const configuration = `${language}-${theme}-${scale}`;
            const folder = path.join(output, configuration);
            fs.mkdirSync(folder, { recursive: true });
            const screens = [];
            const capture = async name => {
                await page.evaluate(() => document.fonts.ready);
                await page.waitForTimeout(500);
                await page.locator('#phone').screenshot({ path: path.join(folder, name + '.png') });
                screens.push(name);
            };
            for (const [name, type, filter, panel] of [
                ['chapters', 'book', 'chapters', null], ['book-kanji', 'book', 'kanji', null],
                ['strokes', 'book', 'kanji', 'strokes'], ['radicals', 'book', 'kanji', 'radical'],
                ['jlpt', 'book', 'kanji', 'jlpt'], ['words', 'words', 'words', null],
            ]) {
                await page.evaluate(({ type, filter, panel }) => {
                    closeModal(); initTraining(null); sourceIndex = null; sourceDraft = newMaterialDraft(type);
                    resetMaterialPicker(); materialOpenFilter = filter; materialKanjiPanel = panel; go('material-filter');
                }, { type, filter, panel });
                await capture(name);
            }
            await page.evaluate(() => {
                sourceDraft = newMaterialDraft('dictionary'); beginDictionaryTagSelection('material'); go('dictionary-tags');
            });
            await capture('tags');
            await page.evaluate(() => { dictionaryTagsMode = 'manage'; render(); });
            await capture('tag-management');
            await page.evaluate(() => editDictionaryTag());
            await capture('create-tag-dialog');
            await page.evaluate(() => { closeModal(); initTraining('tobira10'); go('recipe-editor'); trainingLeave('recipes'); });
            await capture('unsaved-dialog');
            manifest.push({ configuration, screens, cssScale: scale });
            fs.writeFileSync(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
            console.log(configuration, screens.length);
        }
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
