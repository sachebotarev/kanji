const {chromium}=require('playwright'),assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({channel:'chrome',headless:true}),page=await browser.newPage({viewport:{width:1440,height:1180}}),errors=[];page.on('pageerror',e=>errors.push(e.message));const base=process.argv[2]||'http://127.0.0.1:8873/design-system/prototypes/';
const app=()=>page.frames().find(f=>/\/(web|textbooks)\/index.html/.test(f.url()));
const ready=async(module,route)=>{await page.waitForFunction(({module,route})=>{const f=document.querySelector('#screen-frame');return f?.contentWindow?.location.pathname.includes('/'+module+'/')&&(!route||f.contentWindow.location.hash==='#'+route);},{module,route});await page.waitForTimeout(100);};
try{
 await page.goto(base+'index.html#references');await ready('textbooks','references');assert.equal(await app().locator('.n-appbar h2').innerText(),'Справочники');assert.equal(await app().locator('.p-source-card').count(),0);
 for(const feature of ['catalog','references']){await page.locator(`#feature-nav [href="#${feature}"]`).click();await ready('textbooks',feature);assert.equal(await app().locator('.n-appbar h2').innerText(),feature==='references'?'Справочники':'Учебники');assert.equal(await app().locator('.p-reference-row').count(),feature==='references'?2:0);}
 await page.goto(base+'index.html#home');await ready('web','home');
 assert.equal(await app().locator('[data-action="go:catalog"]').innerText(),'Учебники\n2 учебника');assert(await app().locator('[data-action="go:references"]').isVisible());
 await app().locator('[data-action="go:references"]').click();await ready('textbooks','references');assert.equal(await app().locator('.p-source-card').count(),0);assert.equal(await app().locator('.p-reference-row').count(),2);
 await page.goto(base+'index.html#dictionary/DICT.01');await ready('web','word');
 const kanjiAction=await app().locator('.w-word-kanji-grid [data-action]').first().getAttribute('data-action'),id=Number(kanjiAction.split(':')[1]);
 await app().locator('.w-word-kanji-grid [data-action]').first().click();await ready('textbooks',`kanji/${id}/-1`);
 assert.match(await app().locator('.n-source-context').innerText(),/Мой словарь/);
 await app().locator(`[data-action="related:${id}"]`).click();await ready('textbooks',`words/${id}/-1`);assert.equal(await app().locator('.w-dictionary-row').count(),1);
 const personal=await app().evaluate(id=>wordsFor({sourceId:-1,kanji:id}).map(w=>w.id),id);assert.deepEqual(personal,[-1]);
 const scopes=await app().evaluate(id=>({book:wordsFor({sourceId:2,kanji:id}).map(w=>w.id),all:wordsFor({sourceId:0,kanji:id}).map(w=>w.id)}),id);assert(!scopes.book.some(n=>n<0));assert(scopes.all.includes(-1));assert(scopes.all.length>personal.length);
 await page.goto(base+'web/?standalone=1#dictionary');await page.waitForSelector('.dictionary-screen');await page.locator('[data-action="new"]').click();await page.locator('[name="written"]').fill('木のテスト');await page.locator('[name="reading"]').fill('きのてすと');await page.locator('[data-current-translation]').fill('Личная тестовая запись');await page.locator('[form="word-form"]').click();
 await page.goto(base+`textbooks/?standalone=1#kanji/${id}/0`);await page.waitForFunction(()=>window.catalogPrototype);const actual=await page.evaluate(id=>({personal:wordsFor({sourceId:-1,kanji:id}).map(w=>w.written),book:wordsFor({sourceId:2,kanji:id}).map(w=>w.written),all:wordsFor({sourceId:0,kanji:id}).map(w=>w.written)}),id);assert(actual.personal.includes('木のテスト'));assert(actual.all.includes('木のテスト'));assert(!actual.book.includes('木のテスト'));
 assert.deepEqual(errors,[]);console.log(JSON.stringify({checks:['separate home entries','references excludes textbooks','viewer dynamic kanji scope preserved','dictionary-only related words','textbook scope excludes personal words','global scope includes personal words','created records shared across modules'],errors}));
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exit(1);});
