const {chromium}=require('playwright');
const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1180}}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 const base=process.argv[2]||'http://127.0.0.1:8873/design-system/prototypes/';
 const click=s=>page.locator(s).first().click();
 async function materialResults(){const result=await page.evaluate(()=>{
  const f=materialKanjiFilters,values=v=>Array.isArray(v)?v:v?[v]:[],matches=(v,id)=>!values(v).length||values(v).includes(id);
  return {expected:materialKanji(sourceDraft).filter(k=>matches(f.strokes,k.stroke_count)&&matches(f.radical,k.radical_id)&&matches(f.jlpt,k.classification?.jlpt_level||-1)).map(k=>k.id).sort((a,b)=>a-b),actual:[...document.querySelectorAll('[data-action^="select-kanji:"]')].map(el=>Number(el.dataset.id)).sort((a,b)=>a-b)};
 });assert.deepEqual(result.actual,result.expected);}
 try{
  await page.goto(base+'web/?standalone=1#recipe-source');
  await click('[data-material-filter="kanji"]');
  for(const key of ['strokes','radical','jlpt']){
   const before=await page.evaluate(k=>JSON.stringify(materialKanjiFilters[k]),key);
   const ids=await page.evaluate(k=>k==='jlpt'?[1,5,-1]:[...new Set(materialKanji(sourceDraft).map(c=>k==='strokes'?c.stroke_count:c.radical_id))].filter(Boolean).slice(0,3),key);
   const selector=id=>key==='radical'?`[data-action="select-radical:${id}"]`:`[data-action="select-reference:${key}:${id}"]`;
   await click(`[data-action="filter:${key}"]`);await click(selector(ids[0]));await click('[data-action="reference-cancel"]');
   assert.equal(await page.evaluate(k=>JSON.stringify(materialKanjiFilters[k]),key),before);
   await click(`[data-action="filter:${key}"]`);
   for(const id of ids)await click(selector(id));
   assert.equal(await page.locator('[aria-pressed="true"]').count(),ids.length);
   assert.equal(await page.evaluate(k=>JSON.stringify(materialKanjiFilters[k]),key),before);
   await click('[data-action="reference-apply"]');
   assert.deepEqual(await page.evaluate(k=>materialKanjiFilters[k],key),ids);await materialResults();
   const label=await page.locator(`[data-action="filter:${key}"]`).innerText();
   const expected=await page.evaluate(({key,ids})=>ids.slice(0,2).map(id=>key==='radical'?TC.radicals.find(r=>r.id===id).symbol:key==='jlpt'?'N'+id:String(id)),{key,ids});
   expected.forEach(value=>assert(label.includes(value),label));if(ids.length>2)assert(label.includes('+1'));
   await click(`[data-action="filter:${key}"]`);await click('[data-action="reference-view"]');assert.equal(await page.locator('[aria-pressed="true"]').count(),ids.length);await click(selector(ids[0]));await click('[data-action="reference-cancel"]');assert.deepEqual(await page.evaluate(k=>materialKanjiFilters[k],key),ids);
  }
  const other=await page.evaluate(()=>JSON.stringify(materialKanjiFilters.radical));await click('[data-action="clear-strokes"]');assert.deepEqual(await page.evaluate(()=>materialKanjiFilters.strokes),[]);assert.equal(await page.evaluate(()=>JSON.stringify(materialKanjiFilters.radical)),other);await materialResults();
  await click('[data-action="kanji-picker-cancel"]');
  const snapshot=await page.evaluate(()=>JSON.stringify(sourceDraft));await click('[data-material-filter="chapters"]');await click('[data-action^="select-chapter:"]');await click('[data-action="chapter-picker-cancel"]');assert.equal(await page.evaluate(()=>JSON.stringify(sourceDraft)),snapshot);

  await page.goto(base+'textbooks/?standalone=1#all-kanji');await page.waitForFunction(()=>window.catalogPrototype);
  async function catalogResults(){const result=await page.evaluate(()=>{const values=v=>Array.isArray(v)?v:v?[v]:[],match=(v,id)=>!values(v).length||values(v).includes(id);return {expected:DATA.kanji.filter(k=>k.radical_id&&match(current.strokes,k.stroke_count)&&match(current.radical,k.radical_id)&&match(current.jlpt,k.classification?.jlpt_level||-1)).map(k=>k.id).sort((a,b)=>a-b),actual:[...document.querySelectorAll('.n-kanji-tile')].map(e=>Number(e.dataset.id)).sort((a,b)=>a-b)};});assert.deepEqual(result.actual,result.expected);}
  for(const key of ['strokes','radical','jlpt']){
   const before=await page.evaluate(k=>JSON.stringify(current[k]),key);await click(`[data-action="filter:${key}"]`);
   const options=page.locator(`[data-action^="select-${key}:"]`),ids=(await options.evaluateAll(nodes=>nodes.slice(0,2).map(n=>Number(n.dataset.action.split(':')[1]))));assert.equal(ids.length,2);
   for(const id of ids)await click(`[data-action="select-${key}:${id}"]`);
   assert.equal(await page.locator('[aria-pressed="true"]').count(),2);await click('[data-action="back"]');await page.waitForFunction(()=>current.route.kind==='all-kanji');assert.equal(await page.evaluate(k=>JSON.stringify(current[k]),key),before);
   await click(`[data-action="filter:${key}"]`);for(const id of ids)await click(`[data-action="select-${key}:${id}"]`);await click('[data-action="facet-apply"]');await page.waitForFunction(()=>current.route.kind==='all-kanji');assert.deepEqual(await page.evaluate(k=>current[k],key),ids);await catalogResults();
   const label=await page.locator(`[data-action="filter:${key}"]`).innerText();const expected=await page.evaluate(({key,ids})=>ids.map(id=>key==='radical'?radicalById.get(id).symbol:key==='jlpt'?(id===-1?'Без JLPT':'N'+id):String(id)),{key,ids});expected.forEach(value=>assert(label.includes(value),label));
  }
  await click('[data-action="clear-jlpt"]');await catalogResults();
  await page.evaluate(()=>quickNavigate('source/2?tab=1'));await click('[data-action="filter:chapter"]');await click('[data-action="select-chapter:1"]');await click('[data-action="select-chapter:2"]');await click('[data-action="facet-apply"]');await page.waitForFunction(()=>current.route.kind==='source');assert.deepEqual(await page.evaluate(()=>current.chapter),[1,2]);assert.match(await page.locator('[data-action="filter:chapter"]').innerText(),/1, 2/);
  assert(await page.evaluate(()=>[...document.querySelectorAll('.n-kanji-tile')].every(n=>DATA.placements.some(p=>p.source_id===2&&[1,2].includes(p.chapter_number)&&p.kanji_id===Number(n.dataset.id)))));
  await page.evaluate(()=>quickNavigate('catalog'));assert.match(await page.locator('.p-catalog-intro p').innerText(),/Шаг за шагом/);await page.evaluate(()=>quickNavigate('references'));assert.match(await page.locator('.p-catalog-intro p').innerText(),/Каждый знак/);
  await page.locator('#width').selectOption('360');await page.locator('#text-scale').selectOption('1.6');assert(await page.evaluate(()=>{const el=document.querySelector('.p-catalog-intro');return el.scrollWidth<=el.clientWidth+1;}));
  assert.deepEqual(errors,[]);console.log('Multi-select filters: staged apply/cancel, OR/AND results, chips, chapters and subtitles OK');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
