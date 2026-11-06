const {chromium}=require('playwright'),assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1180}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 const click=s=>page.locator(s).first().click();
 try{
 await page.goto('http://127.0.0.1:8873/design-system/prototypes/web/?standalone=1#recipe-source');
 await page.waitForSelector('[data-material-type=dictionary]');await page.locator('[data-material-type=dictionary]').check();
 await page.evaluate(()=>{dictionaryTagIds=[2];});
 await click('[data-material-tags]');assert.equal(await page.evaluate(()=>screen),'dictionary-tags');
 assert.equal(await page.evaluate(()=>dictionaryTagSession.mode),'material');
 assert.equal(await page.locator('.dictionary-tags-content>[data-dictionary-tags=manage]').count(),0);
 await click('[data-action="dictionary-tag-select:1"]');await click('[data-action="go:recipe-source"]');
 assert.deepEqual(await page.evaluate(()=>sourceDraft.tag_ids||[]),[]);
 await click('[data-material-tags]');await click('[data-dictionary-tag-search]');await page.locator('#dictionary-tag-query').fill('ПОВТОРИТЬ');assert.equal(await page.locator('[data-action^="dictionary-tag-select:"]').count(),1);
 await click('[data-action="dictionary-tag-select:1"]');await page.locator('#dictionary-tag-query').fill('');await click('[data-action="dictionary-tag-select:3"]');
 assert((await page.locator('[data-dictionary-tags-apply]').innerText()).includes('(2)'));
 await click('[data-action="dictionary-tags-menu"]');assert(await page.locator('#dictionary-tag-menu').isVisible());await page.keyboard.press('Escape');assert.equal(await page.locator('#dictionary-tag-menu').count(),0);
 await click('[data-action="dictionary-tags-menu"]');await click('[data-dictionary-tags=manage]');assert.equal(await page.evaluate(()=>dictionaryTagsMode),'manage');
 await click('[data-action="go:recipe-source"]');assert.equal(await page.evaluate(()=>dictionaryTagsMode),'material');assert.deepEqual(await page.evaluate(()=>dictionaryTagSession.selected),[1,3]);
 await click('[data-dictionary-tags-apply]');assert.deepEqual(await page.evaluate(()=>sourceDraft.tag_ids),[1,3]);assert.deepEqual(await page.evaluate(()=>dictionaryTagIds),[2]);
 assert((await page.locator('[data-material-tags]').innerText()).includes('Повторить'));
 const result=await page.evaluate(()=>({actual:sourceWords(sourceDraft).map(w=>w.id),expected:words.filter(w=>w.personal&&(w.tag_ids||[]).some(id=>[1,3].includes(id))).map(w=>w.id)}));assert.deepEqual(result.actual.sort(),result.expected.sort());
 await click('[data-material-filter=kanji]');assert(await page.locator('.kanji-picker-screen').isVisible());await click('[data-action^="select-kanji:"]');await click('[data-action=kanji-picker-apply]');
 assert(await page.evaluate(()=>sourceWords(sourceDraft).every(w=>(w.tag_ids||[]).some(id=>sourceDraft.tag_ids.includes(id))&&sourceDraft.kanji_ids.some(id=>w.written.includes(TC.kanji.find(k=>k.id===id).symbol)))));
 await click('[data-tr=source-apply]');assert(await page.evaluate(()=>recipeDraft.sources.some(s=>s.book==='personal'&&s.tag_ids?.includes(1)&&s.tag_ids.includes(3))));
 await page.locator('#recipe-name').fill('Тренировка по тегам');await click('[form="recipe-form"]');
 assert(await page.evaluate(()=>recipes.some(plan=>plan.translations.some(tr=>tr.name==='Тренировка по тегам')&&plan.sources.some(source=>JSON.stringify(source.tag_ids)==='[1,3]'))));
 for(const localeValue of ['ru','en'])for(const theme of ['light','dark'])for(const scale of [1,1.3,1.6]){
 await page.evaluate(({localeValue,theme,scale})=>{locale=localeValue;const phone=document.querySelector('#phone');phone.style.width='360px';phone.style.setProperty('--scale',scale);phone.classList.toggle('large-text',scale>1);phone.dataset.theme=theme;sourceDraft=newMaterialDraft('dictionary');beginDictionaryTagSelection('material');},{localeValue,theme,scale});
 await click('[data-action="dictionary-tags-menu"]');assert(!(await page.locator('#app').evaluate(el=>el.scrollWidth>el.clientWidth+1)));assert(await page.locator('[data-dictionary-tags=manage]').isVisible());await page.keyboard.press('Escape');
 }
 assert.deepEqual(errors,[]);console.log('Recipe tags: shared picker, search, staged apply/cancel, OR tags / AND kanji, management return, 360px RU/EN themes/scales OK');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
