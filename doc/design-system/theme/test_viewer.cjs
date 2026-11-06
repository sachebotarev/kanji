const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 const context=await browser.newContext({viewport:{width:1440,height:1250}});
 await context.addInitScript(()=>{
  window.__themeWrites=0;
  const setItem=Storage.prototype.setItem;
  Storage.prototype.setItem=function(key,value){
   if(key==='kanji-design-theme')window.__themeWrites++;
   return setItem.call(this,key,value);
  };
 });
 const errors=[];context.on('page',p=>{p.on('pageerror',e=>errors.push(e.message));p.on('response',r=>{if(r.status()>=400)errors.push(r.status()+' '+r.url())})});
 const base=process.argv[2]||'http://127.0.0.1:8767/design-system/';
 const out=process.argv[3]||'doc/verification/material-theme';fs.mkdirSync(out,{recursive:true});
 const registry=JSON.parse(fs.readFileSync('doc/design-system/theme/screens.json'));
 assert.equal(registry.screens.length,28);assert(registry.screens.every(s=>s.status==='unapproved'));
 const captures=[],checks=[];
 try{
  const web=await context.newPage(),cat=await context.newPage(),studio=await context.newPage();
  await Promise.all([web.goto(base+'prototypes/web/?standalone=1#home'),cat.goto(base+'prototypes/textbooks/?standalone=1#catalog'),studio.goto(base+'prototypes/shared/theme-preview.html?standalone=1')]);
  await web.waitForFunction(()=>window.ThemeStudio?.source&&window.ScreenReview?.registry);
  await cat.waitForFunction(()=>window.ThemeStudio?.source&&window.catalogPrototype);
  await studio.waitForFunction(()=>window.ThemeStudio?.source&&document.querySelectorAll('.role-sample').length>0);
  await studio.locator('#theme').selectOption('light');
  await web.waitForFunction(()=>document.querySelector('#phone').dataset.theme==='light');
  await cat.waitForFunction(()=>document.querySelector('#phone').dataset.theme==='light');
  const writes=await web.evaluate(()=>window.__themeWrites);
  await web.evaluate(async()=>{
   const phone=document.querySelector('#phone');phone.dataset.theme=phone.dataset.theme;
   await new Promise(resolve=>requestAnimationFrame(resolve));
  });
  assert.equal(await web.evaluate(()=>window.__themeWrites),writes,'Redraw must not echo the shared theme into storage');
  checks.push('Theme redraw does not echo storage events across tabs');
  await studio.locator('[data-theme-hex=primary]').fill('#6750A4');
  await web.waitForFunction(()=>getComputedStyle(document.querySelector('#phone')).getPropertyValue('--primary').trim()==='#6750A4');
  await cat.waitForFunction(()=>getComputedStyle(document.querySelector('#phone')).getPropertyValue('--primary').trim()==='#6750A4');
  assert.equal(await studio.evaluate(()=>ThemeStudio.keys.primary),'#6750A4');
  await studio.reload();await studio.waitForFunction(()=>window.ThemeStudio?.source);assert.equal(await studio.evaluate(()=>ThemeStudio.keys.primary),'#6750A4');
  checks.push('Live key-color edits update both viewers across tabs and survive reload');
  await studio.locator('[data-theme-hex=primary]').fill('#FFFFFF');assert(await studio.locator('#contrast-feedback').evaluate(el=>el.classList.contains('is-warning')));
  await studio.locator('[data-theme-hex=primary]').fill('broken');assert.equal(await studio.locator('[data-theme-hex=primary]').getAttribute('aria-invalid'),'true');
  const downloadPromise=studio.waitForEvent('download');await studio.locator('#export-theme').click();assert.equal((await downloadPromise).suggestedFilename(),'theme-source.json');
  await studio.locator('#reset-theme').click();await web.waitForFunction(()=>window.ThemeStudio.keys.primary===window.ThemeStudio.source.key_colors.primary);
  checks.push('Contrast warning, invalid HEX validation, export and reset work without changing approvals');
  for(const theme of ['light','dark']){
   await studio.locator('#theme').selectOption(theme);
   await web.waitForFunction(t=>document.querySelector('#phone').dataset.theme===t,theme);
   await cat.waitForFunction(t=>document.querySelector('#phone').dataset.theme===t,theme);
   await studio.screenshot({path:path.join(out,'theme-studio-'+theme+'.png'),fullPage:true});
   for(const item of registry.screens){
    const page=item.module==='catalog'?cat:web;
    if(item.module==='catalog')await page.evaluate(route=>quickNavigate(route),item.route);
    else await page.evaluate(key=>{
     if(key==='editor:create'){initDraft();go('editor')}
     else if(key==='editor:edit'){initDraft(-1);go('editor')}
     else if(key==='recipe-editor:create')initRecipe();
     else if(key==='recipe-editor:edit')initRecipe(recipes[0].id);
     else{if(key==='word')selectedId=-1;go(key)}
    },item.key);
    assert.equal(await page.locator('#view-badge').getAttribute('data-approval'),'unapproved',item.id);
    const colors=await page.evaluate(()=>{const s=getComputedStyle(document.querySelector('#phone'));return {primary:s.getPropertyValue('--primary').trim(),role:s.getPropertyValue('--md-sys-color-primary').trim(),surface:s.getPropertyValue('--bg').trim(),surfaceRole:s.getPropertyValue('--md-sys-color-surface').trim()}});
    assert.equal(colors.primary,colors.role);assert.equal(colors.surface,colors.surfaceRole);
    assert((await page.locator('#app').innerText()).trim().length>5,item.id);
    const file=item.id.toLowerCase()+'-'+theme+'.png';await page.locator('#phone').screenshot({path:path.join(out,file)});
    captures.push({id:item.id,module:item.module,key:item.key,route:item.route,theme,file,status:'unapproved'});
   }
  }
  checks.push('All 28 screen variants render with shared roles in light and dark themes; 56 captures saved');
  // Settings are now actual prototype screens, with an interactive state/model fixture.
  await web.evaluate(()=>go('settings'));await web.locator('#model-state').selectOption('error');assert(await web.locator('.model-status').evaluate(el=>el.classList.contains('is-error')));
  await web.locator('[data-settings=model]').click();assert(await web.locator('.model-status').innerText());
  await web.locator('[data-action="go:checker-settings"]').click();await web.locator('[data-setting-toggle=shape]').uncheck();assert.equal(await web.evaluate(()=>enabledChecks.shape),false);
  await web.locator('[data-parameter=match]').fill('80');await web.locator('[data-parameter=match]').blur();assert.equal(await web.evaluate(()=>parameterValues.match),80);
  checks.push('Settings/model error, retry, checker switch and numeric parameter interactions work in the prototype');
  // Representative narrow, enlarged and localized layouts in each renderer.
  await web.evaluate(()=>go('recipes'));await web.locator('#width').selectOption('360');await web.locator('#large-text').check();await web.locator('#locale').selectOption('en');
  await web.locator('#phone').screenshot({path:path.join(out,'builder-dark-en-large.png')});
  assert(await web.evaluate(()=>{const p=document.querySelector('.recipe-list-scroll');return p.scrollWidth<=p.clientWidth+1}));
  await cat.locator('#width').selectOption('360');await cat.locator('#text-scale').selectOption('1.6');await cat.locator('#locale').selectOption('de');
  await cat.evaluate(()=>quickNavigate('filter/jlpt'));await cat.locator('#phone').screenshot({path:path.join(out,'catalog-dark-fallback-large.png')});
  assert(await cat.evaluate(()=>{const p=document.querySelector('.n-content');return p.scrollWidth<=p.clientWidth+1}));
  checks.push('360px, enlarged text, English and Russian fallback checked in both viewers');
  assert.deepEqual(errors,[]);
  fs.writeFileSync(path.join(out,'validation.json'),JSON.stringify({status:'passed',configuration:JSON.parse(fs.readFileSync('doc/design-system/theme/theme-source.json','utf8')).key_colors,checks,errors,captures,limitations:['Design prototype only; Android sources unchanged','No native TalkBack, IME or device lifecycle verification']},null,2)+'\n');
  console.log(JSON.stringify({checks:checks.length,captures:captures.length,errors}));
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
