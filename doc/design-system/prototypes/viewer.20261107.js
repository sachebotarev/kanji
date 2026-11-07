'use strict';
const q=selector=>document.querySelector(selector),frame=q('#screen-frame');
let viewerRegistry,selectedScreen,selectedFeature,childModule=null,pendingKey=null,activeState=null,launchRoute=null;
const localizedFeature=f=>f.translations.find(t=>t.language_tag===q('#viewer-locale').value)||f.translations.find(t=>t.language_tag==='ru');
const localizedScreen=s=>s.translations.find(t=>t.language_tag===q('#viewer-locale').value)||s.translations.find(t=>t.language_tag==='ru');
const controls=()=>({theme:q('#theme').value,width:q('#viewer-width').value,scale:q('#viewer-scale').value,locale:q('#viewer-locale').value});
function sendConfiguration(navigate=false){if(!frame.contentWindow||!selectedFeature)return;frame.contentWindow.postMessage({type:'kanji-viewer-configure',navigate,key:selectedScreen?.key,route:launchRoute||selectedScreen?.route,controls:controls()},location.origin)}
function routeFor(feature,id){return '#'+feature+(id?'/'+id:'')}
function screenByKey(module,key){return viewerRegistry.screens.find(s=>s.module===module&&s.key===key)||viewerRegistry.screens.find(s=>(s.alias_keys||[]).includes(key))}
function updateChrome(){
 const feature=viewerRegistry.features.find(f=>f.id===selectedFeature),tr=localizedFeature(feature),row=selectedScreen?localizedScreen(selectedScreen):null;
 q('#viewer-feature').textContent=tr.title;q('#viewer-title').textContent=selectedScreen?selectedScreen.id+' · '+row.title:tr.title;q('#viewer-description').textContent=tr.description;
 const status=selectedScreen?.status;q('#viewer-status').textContent=status==='approved'?'Согласовано':status?'Не согласовано':'Инструмент дизайна';q('#viewer-status').dataset.approval=status||'tool';
 q('#feature-nav').replaceChildren(...viewerRegistry.features.map(f=>{const a=document.createElement('a');a.className='nav-link'+(f.id===selectedFeature?' active':'');a.href=routeFor(f.id);a.dataset.feature=f.id;if(f.id===selectedFeature)a.setAttribute('aria-current','page');a.textContent=localizedFeature(f).title;return a}));
 const screens=viewerRegistry.screens.filter(s=>s.feature===selectedFeature&&s.status!=='retired');
 q('#screen-list-label').hidden=!screens.length;
 q('#screen-list-label').textContent=screens.length?`Экраны фичи · ${screens.length}`:'Экраны фичи';
 q('#feature-screens').replaceChildren(...screens.map(s=>{const a=document.createElement('a');a.className='nav-link screen-link'+(s.id===selectedScreen?.id?' active':'');a.href=routeFor(selectedFeature,s.id);a.dataset.screen=s.id;if(s.id===selectedScreen?.id)a.setAttribute('aria-current','page');const flag=document.createElement('span');flag.className='screen-status-mark';flag.textContent=s.status==='approved'?'✓':'○';flag.setAttribute('aria-label',s.status==='approved'?'Согласовано':'Не согласовано');a.style.paddingInlineStart=(12+Math.min(3,s.id.split('.').length-1)*12)+'px';a.title=(s.legacy_ids||[]).join(', ');a.append(flag,document.createTextNode(s.id+' · '+localizedScreen(s).title));return a}));
 if(q('#viewer-registry-rev'))q('#viewer-registry-rev').textContent=viewerRegistry.revision||'';
 q('#feature-launchers').hidden=selectedFeature!=='home';q('#viewer-layout').classList.toggle('is-home',selectedFeature==='home');q('#viewer-layout').classList.toggle('is-theme',selectedFeature==='theme');
 q('#feature-cards').replaceChildren(...viewerRegistry.features.filter(f=>f.id!=='home').map(f=>{const a=document.createElement('a');a.className='feature-launcher';a.href=routeFor(f.id);a.dataset.feature=f.id;const title=document.createElement('strong'),desc=document.createElement('span');title.textContent=localizedFeature(f).title;desc.textContent=localizedFeature(f).description;a.append(title,desc);return a}));
 frame.title=row?.title||tr.title;
 for(const id of ['viewer-width','viewer-scale','viewer-locale'])q('#'+id).disabled=selectedFeature==='theme';
}
function choose(feature,id){
 const requested=id&&viewerRegistry.screens.find(s=>s.id===id||(s.legacy_ids||[]).includes(id));
 if(requested&&(requested.feature!==feature||requested.id!==id)){feature=requested.feature;id=requested.id;history.replaceState(null,'',routeFor(feature,id));}
 if(!viewerRegistry.features.some(f=>f.id===feature))feature='home';
 selectedFeature=feature;selectedScreen=feature==='theme'?null:viewerRegistry.screens.find(s=>s.feature===feature&&s.id===id)||viewerRegistry.screens.find(s=>s.feature===feature);
 pendingKey=selectedScreen?.key;activeState=null;showState(null);updateChrome();
 const module=feature==='theme'?'theme':selectedScreen.module;
 const frameRoute=launchRoute||selectedScreen?.route||'home';
 if(module!==childModule){childModule=module;frame.style.height=module==='theme'?'1200px':'962px';const target=module==='theme'?'shared/theme-preview.html':module==='catalog'?'textbooks/index.html':'web/index.html';frame.src=target+'?embedded=1'+(selectedScreen?'#'+frameRoute:'')}
 else sendConfiguration(true);
}
function showState(state){
 const select=q('#viewer-state');activeState=state;select.replaceChildren();select.disabled=!state;
 for(const option of state?.options||[{value:'ready',label:'Основное'}]){const el=document.createElement('option');el.value=option.value;el.textContent=option.label;select.append(el)}
 if(state)select.value=state.value;
}
function navigateFromHash(){const [feature='home',id]=location.hash.slice(1).split('/');choose(feature||'home',id)}
frame.addEventListener('load',()=>sendConfiguration(true));
window.addEventListener('message',event=>{
 if(event.origin!==location.origin||event.source!==frame.contentWindow||!viewerRegistry)return;const data=event.data;
 if(data?.type==='kanji-viewer-ready'){if(data.module===childModule)sendConfiguration(true);return}
 if(data?.type==='kanji-viewer-size'&&selectedFeature==='theme'){frame.style.height=Math.max(900,Math.min(12000,data.height+20))+'px';return}
 if(data?.type==='kanji-viewer-navigate'){
  const kind=data.route?.split('/')[0];  const item=viewerRegistry.screens.find(s=>s.module===data.module&&s.route===data.route)||screenByKey(data.module,kind);if(item){launchRoute=data.route;const hash=routeFor(item.feature,item.id);if(location.hash===hash)choose(item.feature,item.id);else location.hash=hash;}return;
 }
 if(data?.type!=='kanji-screen-changed'||data.module!==childModule)return;
 // An old render notification must not overwrite a feature selection awaiting hashchange.
 if(!pendingKey&&location.hash!==routeFor(selectedFeature,selectedScreen?.id))return;
 if(pendingKey&&data.key!==pendingKey&&!(selectedScreen?.alias_keys||[]).includes(data.key)&&data.key!==selectedScreen?.key)return;
 const item=screenByKey(data.module,data.key);if(!item)return;
 pendingKey=null;launchRoute=null;selectedScreen=item;selectedFeature=item.feature;showState(data.state);updateChrome();
 const canonical=routeFor(item.feature,item.id);if(location.hash!==canonical)history.replaceState(null,'',canonical);
});
q('.viewer-controls').addEventListener('change',event=>{
 if(event.target.id==='viewer-state'&&activeState){frame.contentWindow.postMessage({type:'kanji-viewer-configure',controls:{...controls(),state:{id:activeState.id,value:event.target.value}}},location.origin);return}
 if(event.target.id==='viewer-locale')updateChrome();sendConfiguration(false);
});
window.addEventListener('hashchange',navigateFromHash);
window.addEventListener('kanji-theme-updated',()=>{if(viewerRegistry)sendConfiguration(false)});
function ensurePracticeCompleteScreen(registry){
 if((registry.screens||[]).some(s=>s.id==='PRACTICE.01'))return registry;
 registry.screens=registry.screens||[];
 registry.screens.push({
  id:'PRACTICE.01',module:'web',key:'practice-complete',route:'practice-complete',status:'unapproved',
  feature:'practice',parent_id:'PRACTICE',legacy_ids:['APP-02.01'],
  translations:[
   {language_tag:'ru',title:'Итог тренировки',changes:'Краткая статистика, оценка и маскот в трёх вариантах. Требует согласования.'},
   {language_tag:'en',title:'Training summary',changes:'Brief stats, grade and three mascot variants. Approval required.'},
  ],
 });
 registry.revision=(registry.revision||'')+'+practice-complete-fallback';
 return registry;
}
fetch(`../theme/screens.json?rev=home-split-2026-11-07&t=${Date.now()}`,{cache:'reload'}).then(r=>{if(!r.ok)throw Error('Registry HTTP '+r.status);return r.json()}).then(registry=>{
 viewerRegistry=ensurePracticeCompleteScreen(registry);
 const legacy=new URLSearchParams(location.search),legacyItem=viewerRegistry.screens.find(s=>s.module===legacy.get('module')&&s.route===legacy.get('route'));
 if(legacyItem){history.replaceState(null,'',location.pathname+routeFor(legacyItem.feature,legacyItem.id))}
 else if(!location.hash)history.replaceState(null,'','#home');
 navigateFromHash();window.DesignViewer={get registry(){return viewerRegistry},get selected(){return selectedScreen},get feature(){return selectedFeature}};
}).catch(error=>{q('#viewer-error').hidden=false;q('#viewer-error').textContent='Не удалось загрузить просмотрщик: '+error.message});
