// Requires Playwright and locally installed Chrome. Does not modify the approval registry.
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
(async()=>{
  const rawBase=process.argv[2]||'http://127.0.0.1:8767/design-system/prototypes/textbooks/';
  const base=rawBase.includes('?')?rawBase:rawBase+'?standalone=1';
  const output=process.argv[3];
  const browser=await chromium.launch({headless:true,channel:'chrome'});
  const page=await browser.newPage({viewport:{width:1440,height:1250}});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  page.on('response',r=>{if(r.status()>=400)errors.push(`${r.status()} ${r.url()}`)});
  try{
    await page.goto(base);await page.waitForFunction(()=>window.catalogPrototype);await page.evaluate(()=>document.fonts.ready);
    const registry=await page.evaluate(()=>window.catalogPrototype.review);
    assert.equal(registry.screens.length,18);const pending=registry.screens.filter(s=>s.status!=='approved').length;
    const visited=[];
    for(const screen of registry.screens){
      await page.locator(`[data-quick="${screen.route}"]`).click();
      await page.waitForFunction(key=>document.querySelector('#phone').dataset.screenKey===key,screen.key);
      assert.equal(await page.locator('#review-notice').isVisible(),screen.status!=='approved');
      assert.match(await page.locator('#view-status').innerText(),screen.status==='approved'?/^Согласовано/:/^Не согласовано/);
      assert.equal(await page.locator('#phone #review-notice').count(),0);
      assert.ok((await page.locator('#app').innerText()).length>5);
      visited.push(screen.id);
    }
    await page.evaluate(()=>quickNavigate('source/2'));
    await page.locator('[data-action="toggle-chapter-search"]').click();await page.locator('#catalog-query').fill('999');
    assert.match(await page.locator('.p-book-empty strong').innerText(),/Ничего не найдено/);
    assert.match(await page.locator('.p-book-empty p').innerText(),/1–45/);
    await page.evaluate(()=>quickNavigate('source/2?tab=2'));
    await page.locator('[data-action="toggle-module-search"]').click();await page.locator('#catalog-query').fill('no-match-xyz');
    assert.match(await page.locator('.w-empty strong').innerText(),/Ничего не найдено/);
    await page.evaluate(()=>quickNavigate('source/1?tab=1'));
    await page.locator('[data-action="filter:strokes"]').click();
    await page.locator('[data-action="select-strokes:1"]').click();
    await page.locator('[data-action="facet-apply"]').click();
    await page.waitForFunction(()=>current.route.kind==='source'&&current.strokes.includes(1));
    await page.locator('[data-action="tab:2"]').click();
    await page.locator('[data-action="filter:chapter"]').click();
    assert.equal(await page.locator('[data-action^="select-chapter:"]').count(),18); // Clear selection is a separate control.
    assert.match(await page.locator('[data-action="select-chapter:3"] small').innerText(),/118 слов/);
    await page.locator('[data-action="back"]').click();await page.waitForFunction(()=>current.route.kind==='source');
    await page.locator('[data-action="tab:1"]').click();await page.locator('[data-action="filter:chapter"]').click();
    assert.equal(await page.locator('[data-action^="select-chapter:"]').count(),1);
    assert.doesNotMatch(await page.locator('[data-action="select-chapter:3"] small').innerText(),/слов/);
    await page.evaluate(()=>quickNavigate('words/19968/1'));
    assert.match(await page.locator('.w-kanji-summary').innerText(),/13 слов/);
    assert.match(await page.locator('.w-word-chapter-heading').innerText(),/Глава 3/);
    assert.doesNotMatch(await page.locator('.w-word-chapter-heading').innerText(),/13 слов/);
    assert.doesNotMatch(await page.locator('.w-word-chapter-heading').innerText(),/из 13/);
    await page.evaluate(()=>quickNavigate('source/2?tab=2'));
    const reading=await page.locator('.n-word-reading').first().evaluate(n=>({size:getComputedStyle(n).fontSize,line:getComputedStyle(n).lineHeight}));
    assert.deepEqual(reading,{size:'16px',line:'24px'});
    await page.evaluate(()=>quickNavigate('source/2?tab=1'));
    assert.equal(await page.locator('.w-kanji-summary .w-numbering-help summary svg').count(),1);
    await page.locator('.w-numbering-help summary').click();
    assert.equal(await page.locator('.w-numbering-help p').isVisible(),true);
    assert.match(await page.locator('.w-numbering-help p').innerText(),/Номер на плитке/);
    const sizes=[];
    for(const width of ['360','411.43','480'])for(const scale of ['1','1.3','1.6']){
      await page.selectOption('#width',width);await page.selectOption('#text-scale',scale);
      await page.evaluate(()=>quickNavigate('filter/radical'));
      const clipped=await page.locator('.w-full-caption .n-kanji-meaning').evaluateAll(nodes=>nodes.filter(n=>n.scrollHeight>n.clientHeight+1).map(n=>n.textContent));
      assert.deepEqual(clipped,[]);sizes.push({width,scale});
    }
    for(const locale of ['ru','en','de']){
      await page.selectOption('#locale',locale);await page.evaluate(()=>quickNavigate('kanji/26085/2'));
      assert.match(await page.locator('.w-kanji-classification').innerText(),locale==='en'?/Elementary school/:/Начальная школа/);
    }
    await page.selectOption('#locale','ru');await page.selectOption('#width','411.43');await page.selectOption('#text-scale','1');
    for(const state of ['loading','error']){
      await page.selectOption('#load-state',state);assert.equal(await page.locator('.p-catalog-state').count(),1);
      const key=await page.evaluate(()=>reviewScreenKey());
      const approved=registry.screens.find(s=>s.key===key)?.status==='approved';
      assert((await page.locator('#view-status').innerText()).startsWith(approved?'Согласовано':'Не согласовано'));
    }
    await page.selectOption('#load-state','ready');await page.selectOption('#review-filter','unapproved');
    assert.equal(await page.locator('.nav-link[data-quick]:visible').count(),pending);
    await page.evaluate(()=>quickNavigate('source/2?tab=1'));
    await page.locator('.workbench').evaluate(n=>n.scrollTop=0);
    if(output){fs.mkdirSync(output,{recursive:true});await page.screenshot({path:path.join(output,'overview.png'),fullPage:true});}
    assert.deepEqual(errors,[]);
    const report={pending,browser:await browser.version(),playwright:require('playwright/package.json').version,url:base,revision:registry.revision,visited,sizes,locales:['ru','en','de'],states:['ready','loading','error'],errors};
    if(output)fs.writeFileSync(path.join(output,'validation.json'),JSON.stringify(report,null,2)+'\n');
    console.log(JSON.stringify(report));
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
