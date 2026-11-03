'use strict';
const studioRoleGrid=document.getElementById('role-grid');
function contrastRatio(a,b){
 const luminance=hex=>{const rgb=hex.replace('#','').match(/../g).map(x=>parseInt(x,16)/255).map(x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4);return rgb.reduce((v,n,i)=>v+n*[.2126,.7152,.0722][i],0)};
 const x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);
}
function updateStudio(){
 const studio=window.ThemeStudio;if(!studio?.source)return;
 const theme=document.documentElement.dataset.theme,roles=studio.schemes[theme];
 for(const [key,color] of Object.entries(studio.keys)){
  document.querySelector(`[data-theme-key="${key}"]`).value=color;
  const hex=document.querySelector(`[data-theme-hex="${key}"]`);if(document.activeElement!==hex)hex.value=color;
 }
 studioRoleGrid.replaceChildren(...Object.entries(roles).map(([name,color])=>{
  const sample=document.createElement('div');sample.className='role-sample';const swatch=document.createElement('div');swatch.className='role-swatch';swatch.style.backgroundColor=color;swatch.setAttribute('aria-hidden','true');const label=document.createElement('code');label.textContent=name;const value=document.createElement('small');value.textContent=color;sample.append(swatch,label,value);return sample;
 }));
 const specimens=document.getElementById('type-specimens');specimens.replaceChildren(...Object.entries(studio.source.typography).map(([name,[size,line,weight,tracking]])=>{
  const row=document.createElement('div'),label=document.createElement('small'),text=document.createElement('span');label.textContent=`${name} · ${size}/${line} · ${weight}`;text.textContent='Кандзи дэс нэ · 学ぶ';Object.assign(text.style,{fontSize:size+'px',lineHeight:line+'px',fontWeight:weight,letterSpacing:tracking+'px'});row.append(label,text);return row;
 }));
 const pairs=['primary','secondary','tertiary','error'].flatMap(k=>[[k,'on-'+k],[k+'-container','on-'+k+'-container']]);
 const failures=pairs.map(([base,on])=>({base,on,ratio:contrastRatio(roles[base],roles[on])})).filter(x=>x.ratio<4.5);
 const feedback=document.getElementById('contrast-feedback');feedback.classList.toggle('is-warning',failures.length>0);
 feedback.textContent=failures.length?'Нужна проверка контраста: '+failures.map(x=>`${x.base} / ${x.on}: ${x.ratio.toFixed(2)}:1`).join('; '):'Основные пары текста и контейнеров: контраст не ниже 4.5:1.';
}
window.addEventListener('kanji-theme-updated',updateStudio);
if(window.ThemeStudio?.source)updateStudio();
document.addEventListener('input',e=>{
 const key=e.target.dataset.themeKey||e.target.dataset.themeHex;if(!key||!window.ThemeStudio?.source)return;
 const raw=e.target.value.trim();const valid=/^#[0-9a-f]{6}$/i.test(raw);e.target.setAttribute('aria-invalid',String(!valid));
 if(!valid)document.getElementById('theme-feedback').textContent='Введите цвет в формате #RRGGBB. Последний корректный цвет сохранён.';
 if(valid){window.ThemeStudio.setColors({...window.ThemeStudio.keys,[key]:raw});document.getElementById('theme-feedback').textContent='Черновик применён ко всем экранам просмотрщика.';}
});
document.getElementById('reset-theme').addEventListener('click',()=>{window.ThemeStudio.reset();document.querySelectorAll('[data-theme-hex]').forEach(el=>el.setAttribute('aria-invalid','false'));document.getElementById('theme-feedback').textContent='Восстановлена исходная палитра из конфигурации.';});
document.getElementById('export-theme').addEventListener('click',()=>{const blob=new Blob([JSON.stringify(window.ThemeStudio.export(),null,2)+'\n'],{type:'application/json'}),url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download='theme-source.json';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);document.getElementById('theme-feedback').textContent='Конфигурация выгружена. Статусы согласования не изменены.';});
fetch('../../theme/screens.json',{cache:'no-store'}).then(r=>r.json()).then(registry=>{
 document.getElementById('screen-register').replaceChildren(...registry.screens.map(screen=>{
  const row=document.createElement('div'),id=document.createElement('code'),link=document.createElement('a'),status=document.createElement('span');
  id.textContent=screen.id;link.href=`../${screen.module==='catalog'?'textbooks':'web'}/#${screen.route}`;link.textContent=screen.translations.find(t=>t.language_tag==='ru')?.title||screen.key;status.textContent=screen.status==='approved'?'Согласовано':'Не согласовано';row.append(id,link,status);return row;
 }));
}).catch(()=>{document.getElementById('screen-register').textContent='Не удалось загрузить реестр экранов.'});
