const {chromium}=require('playwright'),assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({channel:'chrome',headless:true}),page=await browser.newPage({viewport:{width:1440,height:1180}}),errors=[];page.on('pageerror',e=>errors.push(e.message+' at '+page.url()+'\n'+e.stack));const base='http://127.0.0.1:8873/design-system/prototypes/';const frame=()=>page.frames().find(f=>/\/(web|textbooks)\/index.html/.test(f.url()));
try{
 await page.goto(base+'#shared');await page.waitForFunction(()=>window.DesignViewer?.feature==='shared');
 const records=await page.evaluate(()=>DesignViewer.registry.screens.map(s=>({id:s.id,module:s.module,key:s.key,feature:s.feature,legacy_ids:s.legacy_ids})));
 assert.equal(records.length,39);assert.equal(new Set(records.map(s=>s.id)).size,39);
 for(const record of records){
  await page.goto(base+'#'+record.feature+'/'+record.id);
  await page.waitForFunction(id=>window.DesignViewer?.selected?.id===id,record.id);
  await page.waitForFunction(record=>{const w=document.querySelector('#screen-frame')?.contentWindow,phone=w?.document.querySelector('#phone');return phone?.innerText.trim().length>20&&(record.module==='web'?w.webReviewKey?.()===record.key:phone.dataset.screenKey===record.key);},record).catch(error=>{throw new Error(record.id+': '+error.message);});
  assert.equal(await page.evaluate(()=>DesignViewer.feature),record.feature);
  assert((await page.locator('#viewer-title').innerText()).includes(record.id));
  for(const legacy of record.legacy_ids){
   await page.goto(base+'#home/'+legacy);
   await page.waitForURL('**/#'+record.feature+'/'+record.id);
   await page.waitForFunction(id=>DesignViewer.selected?.id===id,record.id);
  }
  assert.equal(await page.locator('#feature-screens [aria-current=page]').count(),1);
  if(record.id==='SELECT.05'){
   const help=frame().locator('.w-selector-summary-row summary'),box=await help.boundingBox(),list=await frame().locator('.w-choice-options').boundingBox(),phone=await frame().locator('#phone').boundingBox();
   assert(box.x+box.width/2>phone.x+phone.width/2);assert(box.y+box.height<=list.y+1);assert.equal(await frame().locator('.n-appbar .w-inline-help').count(),0);
   await help.click();assert(await frame().locator('.w-selector-summary-row details[open] p').isVisible());
  }
 }
 await page.goto(base+'#dictionary/DICT.01');await page.waitForURL('**/#dictionary/DICT.01');await page.waitForFunction(()=>DesignViewer.selected?.id==='DICT.01');
 await page.goto(base+'#recipe-builder/RECIPE.01.02');await page.waitForFunction(()=>DesignViewer.selected?.id==='RECIPE.01.02');
 await page.waitForFunction(()=>document.querySelector('#screen-frame')?.contentWindow?.document.querySelector('[data-material-type=dictionary]'));
 await frame().locator('[data-material-type=dictionary]').check();await frame().locator('[data-material-tags]').click();
 await page.waitForURL('**/#shared/SELECT.07');assert.equal(await frame().evaluate(()=>dictionaryTagSession.mode),'material');
 await frame().locator('[data-action="dictionary-tag-select:1"]').click();await frame().locator('[data-dictionary-tags-apply]').click();
 await page.waitForURL('**/#recipe-builder/RECIPE.01.02');assert.deepEqual(await frame().evaluate(()=>sourceDraft.tag_ids),[1]);
 await page.locator('#viewer-locale').selectOption('en');assert.equal(await page.locator('#feature-nav [href="#shared"]').innerText(),'Shared screens');
 assert.deepEqual(errors,[]);console.log('Screen hierarchy: 39 entries, canonical and legacy links, RU/EN, shared selectors, recipe context and return OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exit(1)});
