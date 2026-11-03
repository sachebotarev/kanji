import {resolveTheme} from './material-colors.js';
// Local design drafts are shared by every viewer tab; approval is separate configuration.
const sourceUrl=new URL('../../theme/theme-source.json',import.meta.url).href,draftKey='kanji-design-color-draft',themeKey='kanji-design-theme';
let source,keys,schemes;
const root=document.documentElement,phone=document.getElementById('phone'),select=document.getElementById('theme');
const embedded=parent!==window;
const names=['primary','secondary','tertiary','error'];
function readDraft(){try{const draft=JSON.parse(localStorage.getItem(draftKey));return draft?.base===JSON.stringify(source?.key_colors)?draft.key_colors:null}catch{return null}}
function validated(value){return value&&names.every(k=>/^#[0-9a-f]{6}$/i.test(value[k]))?Object.fromEntries(names.map(k=>[k,value[k].toUpperCase()])):null}
function notify(){window.dispatchEvent(new CustomEvent('kanji-theme-updated',{detail:{keys:{...keys},schemes,theme:root.dataset.theme}}))}
function apply(persistTheme=false){
 const theme=phone?.dataset.theme||select?.value||root.dataset.theme||'light';root.dataset.theme=theme;
 if(schemes){for(const [key,value] of Object.entries(schemes[theme])){root.style.setProperty('--md-sys-color-'+key,value);phone?.style.setProperty('--md-sys-color-'+key,value)}notify()}
 if(persistTheme===true&&!embedded)try{localStorage.setItem(themeKey,theme)}catch{}
}
function update(next,persist=true){const clean=validated(next);if(!source||!clean)return false;keys=clean;schemes=resolveTheme({...source,key_colors:keys});if(persist)try{localStorage.setItem(draftKey,JSON.stringify({base:JSON.stringify(source.key_colors),key_colors:keys}))}catch{}apply();return true}
window.ThemeStudio={
 ready:null,
 get source(){return source},get keys(){return {...keys}},get schemes(){return schemes},
 setColors:update,
 reset(){try{localStorage.removeItem(draftKey)}catch{}update(source.key_colors,false)},
 export(){return {...source,key_colors:{...keys}}}
};
window.ThemeStudio.ready=fetch(sourceUrl,{cache:'no-store'}).then(r=>{if(!r.ok)throw Error('Theme configuration HTTP '+r.status);return r.json()}).then(config=>{
 source=config;let theme='light';try{theme=embedded?(select?.value||phone?.dataset.theme||'light'):(localStorage.getItem(themeKey)||phone?.dataset.theme||'light')}catch{}
 if(!['light','dark'].includes(theme))theme='light';if(select)select.value=theme;if(phone)phone.dataset.theme=theme;root.dataset.theme=theme;
 update(validated(readDraft())||config.key_colors,false);return window.ThemeStudio;
}).catch(e=>{const note=document.createElement('p');note.className='bench-note';note.setAttribute('role','alert');note.textContent='Не удалось загрузить конфигурацию темы: '+e.message;document.querySelector('.workbench')?.append(note);throw e});
if(phone)new MutationObserver(apply).observe(phone,{attributes:true,attributeFilter:['data-theme']});
select?.addEventListener('change',()=>{if(phone)phone.dataset.theme=select.value;else root.dataset.theme=select.value;apply(true)});
window.addEventListener('storage',e=>{
 if(e.key===draftKey&&source)update(validated(readDraft())||source.key_colors,false);
 if(e.key===themeKey){
  let theme;try{theme=localStorage.getItem(themeKey)}catch{return}
  if(!['light','dark'].includes(theme))return;
  if(select)select.value=theme;
  if(phone&&phone.dataset.theme!==theme)phone.dataset.theme=theme;
  else if(!phone)root.dataset.theme=theme;
  apply();
 }
});
const link=document.createElement('a');link.className='nav-link';link.href=new URL('../index.html#theme',import.meta.url).href;link.textContent='Тема приложения';if(!document.getElementById('feature-nav')&&!location.pathname.endsWith('/theme-preview.html'))document.querySelector('.workbench nav')?.before(link);
if(window.ScreenReview)ScreenReview.ready.then(()=>{if(typeof render==='function')render()});
