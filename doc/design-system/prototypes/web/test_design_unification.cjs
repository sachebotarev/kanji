const {chromium}=require('playwright');
const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1180}}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto((process.argv[2]||'http://127.0.0.1:8873/design-system/prototypes/web/')+'?standalone=1#home');
 await page.waitForSelector('.home');
 const routes=['home','dictionary','word','editor','dictionary-translations','dictionary-tags','dictionary-kanji','settings','checker-settings','practice','recipes','recipe-detail','recipe-editor','recipe-source','recipe-words','material-filter'];
 let checks=0;
 for(const width of [360,412,480])for(const scale of [1,1.3,1.6])for(const theme of ['light','dark'])for(const language of ['ru','en'])for(const route of routes){
  await page.evaluate(({width,scale,theme,language,route})=>{
   const phone=document.querySelector('#phone');phone.style.width=width+'px';phone.style.setProperty('--scale',scale);phone.dataset.theme=theme;phone.classList.toggle('large-text',scale>1);locale=language;
   selectedId=words.find(w=>w.personal).id;initDraft(selectedId);initRecipe(recipes[0].id,null);
   if(route==='dictionary-tags')beginDictionaryTagSelection('filter');
   else if(route==='dictionary-kanji')(dictionaryKanjiDraft=[...dictionaryKanjiIds],go(route));
   else if(route==='material-filter'){sourceDraft=structuredClone(recipeDraft.sources[0]);materialOpenFilter='kanji';go(route);}
   else if(route==='recipe-source'){sourceDraft=structuredClone(recipeDraft.sources[0]);go(route);}
   else go(route);
  },{width,scale,theme,language,route});
  const overflow=await page.locator('#app').evaluate(el=>el.scrollWidth>el.clientWidth+1);
  assert(!overflow,`Horizontal overflow ${route} ${width}/${scale}/${theme}/${language}`);checks++;
 }
 await page.evaluate(()=>{go('checker-settings');});
 assert.equal(await page.locator('.settings-algorithms').getAttribute('open'),null);
 await page.locator('[data-strictness][value=strict]').check();
 assert.equal(await page.evaluate(()=>parameterValues.match),84);
 await page.locator('.settings-algorithms summary').click();
 assert.equal(await page.locator('[data-parameter=match]').evaluate(el=>getComputedStyle(el).borderRadius),'8px');
 await page.locator('[data-parameter=match]').fill('80');await page.locator('[data-parameter=match]').dispatchEvent('change');
 assert.equal(await page.evaluate(()=>strictnessPreset),'custom');
 assert.equal(await page.locator('[data-parameter-slider=match]').inputValue(),'80');
 await page.evaluate(()=>{initDraft();go('editor');});
 const field=page.locator('[name=written]');const before=await field.evaluate(el=>el.clientHeight);
 const sentence='これは長い例文です。'.repeat(30);await field.fill(sentence);assert((await field.evaluate(el=>el.clientHeight))>before);assert.equal(await field.inputValue(),sentence);
 await page.evaluate(()=>go('home'));assert.equal(await page.locator('.settings-art').count(),0);assert(await page.locator('.stat-unit').isVisible());
 assert.deepEqual(errors,[]);console.log(JSON.stringify({checks,errors}));await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
