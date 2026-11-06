const {chromium}=require('playwright'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true}),context=await browser.newContext({viewport:{width:1440,height:1250}});
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('response',r=>{if(r.status()>=400)errors.push(`${r.status()} ${r.url()}`)});
 const base=process.argv[2]||'http://127.0.0.1:8767/design-system/prototypes/',out=process.argv[3]||'doc/verification/material-theme';fs.mkdirSync(out,{recursive:true});
 const registry=JSON.parse(fs.readFileSync('doc/design-system/theme/screens.json','utf8')),checks=[],shots=[];
 const child=()=>page.frames().find(f=>f.parentFrame());
 async function waitScreen(item){
  try{await page.waitForFunction(id=>window.DesignViewer?.selected?.id===id,item.id,{timeout:10000})}catch(e){console.log('DEBUG',await page.evaluate(()=>({hash:location.hash,selected:window.DesignViewer?.selected,pending:typeof pendingKey!=='undefined'?pendingKey:null,frame:document.querySelector('#screen-frame').src})));throw e}
  await page.waitForFunction(()=>document.querySelector('#screen-frame').contentDocument?.documentElement?.classList.contains('viewer-embedded'));
  await child().waitForFunction(t=>document.querySelector('#phone')?.dataset.theme===t,await page.locator('#theme').inputValue());
  await child().waitForFunction(key=>{try{return document.documentElement.dataset.viewerModule==='web'?webReviewKey()===key:reviewScreenKey()===key}catch{return false}},item.key);
 }
 try{
  await page.goto(base);const home=registry.screens.find(s=>s.key==='home');await waitScreen(home);
  assert.equal(await page.locator('#feature-nav [data-feature]').count(),7);assert.equal(await page.locator('#feature-cards [data-feature]').count(),6);
  assert((await child().locator('#app').innerText()).includes('Кандзи дэс нэ'));assert.equal(await child().locator('.workbench').isVisible(),false);
  await page.screenshot({path:path.join(out,'viewer-home.png')});checks.push('Home is the first application screen with six feature entry points; nested viewer chrome is hidden');
  for(const theme of ['light','dark']){
   await page.locator('#theme').selectOption(theme);
   for(const item of registry.screens){
    await page.evaluate(hash=>{location.hash=hash},'#'+item.feature+'/'+item.id);await waitScreen(item);
    assert.equal(await page.locator('#viewer-status').getAttribute('data-approval'),'unapproved');
    assert.equal(await child().locator('#phone').getAttribute('data-theme'),theme);
    assert.equal(await page.locator('.viewer-controls').count(),1);
    shots.push({id:item.id,theme,feature:item.feature});
   }
  }
  checks.push('All 28 screen variants work in the single shell in both themes with common controls and unapproved status');
  await page.locator('#feature-nav [data-feature=practice]').click();await waitScreen(registry.screens.find(s=>s.key==='practice'));
  await page.locator('#viewer-state').selectOption('correct');await child().waitForFunction(()=>practiceState==='correct');
  await page.locator('#viewer-width').selectOption('360');await page.locator('#viewer-scale').selectOption('1.6');await page.locator('#viewer-locale').selectOption('en');
  await child().waitForFunction(()=>document.querySelector('#phone').style.width==='360px'&&locale==='en');
  assert.equal(await child().locator('#phone').evaluate(el=>getComputedStyle(el).getPropertyValue('--scale').trim()),'1.6');
  await page.screenshot({path:path.join(out,'viewer-practice-dark-large.png')});
  await page.locator('#feature-nav [data-feature=catalog]').click();await waitScreen(registry.screens.find(s=>s.key==='catalog'));
  await page.locator('#viewer-state').selectOption('error');assert(await child().locator('.p-catalog-state.is-error').isVisible());
  await page.locator('#viewer-state').selectOption('ready');
  await page.locator('#viewer-width').selectOption('412');await page.locator('#viewer-scale').selectOption('1');await page.locator('#viewer-locale').selectOption('ru');
  await page.screenshot({path:path.join(out,'viewer-catalog.png')});checks.push('Width, text scale, locale and context-specific state are managed through the same toolbar');
  // Product navigation updates the shell instead of loading another viewer inside the iframe.
  await page.locator('#feature-nav [data-feature=home]').click();await waitScreen(home);
  await child().locator('[data-action="go:catalog"]').click();await waitScreen(registry.screens.find(s=>s.key==='catalog'));
  assert.equal(await child().locator('.workbench').isVisible(),false);
  await child().locator('.n-back').click();await waitScreen(home);
  await child().locator('[data-action=choose]').click();await waitScreen(registry.screens.find(s=>s.key==='recipes'));
  await child().locator('[data-tr=new]').click();await waitScreen(registry.screens.find(s=>s.key==='recipe-editor:create'));
  checks.push('Home/catalog/back and builder/editor flows synchronize the feature navigation without nested standalone viewers');
  await page.locator('#feature-nav [data-feature=theme]').click();await page.waitForFunction(()=>DesignViewer.feature==='theme');await child().waitForFunction(()=>window.ThemeStudio?.source);
  await child().locator('[data-theme-hex=primary]').fill('#6750A4');await page.waitForFunction(()=>window.ThemeStudio.keys.primary==='#6750A4');
  await page.screenshot({path:path.join(out,'viewer-theme-editor.png')});
  await child().locator('#reset-theme').click();await page.waitForFunction(()=>window.ThemeStudio.keys.primary===window.ThemeStudio.source.key_colors.primary);
  await page.locator('#feature-nav [data-feature=home]').click();await waitScreen(home);
  assert.equal(await child().evaluate(()=>ThemeStudio.keys.primary),await child().evaluate(()=>ThemeStudio.source.key_colors.primary));checks.push('Theme editor is a viewer feature and updates the whole shell and screen renderers');
  for(const legacy of ['web/#home','textbooks/#catalog','shared/theme-preview.html']){
   await page.goto(base+legacy);await page.waitForFunction(()=>window.DesignViewer);assert(!page.url().includes('standalone'));assert(page.url().includes('/prototypes/'));
  }
  checks.push('Old public links redirect to the same viewer entry point');
  assert.deepEqual(errors,[]);fs.writeFileSync(path.join(out,'shell-validation.json'),JSON.stringify({status:'passed',configuration:JSON.parse(fs.readFileSync('doc/design-system/theme/theme-source.json','utf8')).key_colors,checks,screen_checks:shots,errors},null,2)+'\n');console.log(JSON.stringify({checks:checks.length,screenChecks:shots.length,errors}));
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
