'use strict';
(()=>{
 if(!new URLSearchParams(location.search).has('embedded'))return;
 const module=document.documentElement.dataset.viewerModule;
 let queued=null,applying=false;
 const send=data=>parent.postMessage(data,location.origin);
 const selectValue=(id,value)=>{const el=document.getElementById(id);if(!el)return;if(![...el.options].some(x=>x.value===String(value))){const o=document.createElement('option');o.value=value;o.textContent=value;el.append(o)}el.value=String(value)};
 function stateControl(){
  if(module==='catalog')return document.getElementById('load-state');
  if(module!=='web')return null;
  return document.getElementById(screen==='practice'?'practice-state':screen==='dictionary'?'dictionary-state':screen==='settings'?'model-state':'');
 }
 function changed(){
  if(applying)return;
  if(module==='theme'){send({type:'kanji-viewer-size',height:document.body.scrollHeight});return}
  const state=stateControl();
  send({type:'kanji-screen-changed',module,key:module==='web'?webReviewKey():reviewScreenKey(),route:module==='web'?screen:routeHash(current.route),state:state?{id:state.id,value:state.value,options:[...state.options].map(x=>({value:x.value,label:x.textContent}))}:null});
 }
 function ready(){return module==='theme'?!!window.ThemeStudio?.source:module==='catalog'?!!window.catalogPrototype:typeof screen==='string'&&!!window.ScreenReview?.registry&&!!window.ThemeStudio?.source}
 async function configure(message){
  if(!ready()){queued=message;return}
  applying=true;
  const v=message.controls||{};
  if(v.theme){selectValue('theme',v.theme);document.documentElement.dataset.theme=v.theme;if(document.getElementById('phone'))document.getElementById('phone').dataset.theme=v.theme}
  if(module!=='theme'){
   if(v.width){selectValue('width',v.width);document.getElementById('phone').style.width=v.width+'px'}
   if(v.scale){const phone=document.getElementById('phone');phone.style.setProperty('--scale',v.scale);phone.classList.toggle('large-text',Number(v.scale)>1);if(document.getElementById('large-text'))document.getElementById('large-text').checked=Number(v.scale)>1;selectValue('text-scale',v.scale)}
   if(v.locale){selectValue('locale',v.locale);if(module==='web')locale=v.locale;else language=v.locale}
   if(message.navigate){
    if(module==='catalog')quickNavigate(message.route);
    else if(message.route==='dictionary-translations'){if(!draft)initDraft(words.find(word=>word.personal)?.id);go('dictionary-translations')}
    else if(message.route==='dictionary-kanji'){dictionaryKanjiDraft=[...dictionaryKanjiIds];materialKanjiQuery='';materialKanjiSearchOpen=false;materialKanjiSelectedOnly=false;materialKanjiPanel=null;materialKanjiFilters={jlpt:null,radical:null,strokes:null};go('dictionary-kanji')}
    else if(message.route==='dictionary-tags'){dictionaryTagSession=null;dictionaryTagsMode=message.key==='dictionary-tags:manage'?'manage':'filter';dictionaryTagQuery='';dictionaryTagSearchOpen=false;go('dictionary-tags')}
    else if(message.key==='editor:create'){initDraft();go('editor')}
    else if(message.key==='editor:edit'){initDraft(-1);go('editor')}
    else if(message.key==='recipe-editor:create')initRecipe();
    else if(message.key==='recipe-editor:edit')initRecipe(recipes[0].id);
    else if(message.route==='material-filter'){
     initRecipe(recipes[0].id,null);resetMaterialPicker();sourceIndex=0;
     sourceDraft=newMaterialDraft(message.key.includes('words')?'words':'book');
     materialOpenFilter=message.key.includes('words')?'words':message.key.endsWith('chapters')?'chapters':'kanji';
     if(materialOpenFilter==='chapters')materialChapterScope=materialChapterOptions(sourceDraft);
     go('material-filter');
    }
    else if(message.route==='recipe-source'||message.route==='recipe-words'){
     initRecipe(recipes[0].id,null);
     if(message.route==='recipe-source'){
      sourceIndex=message.key==='recipe-source:edit'?0:null;
      sourceDraft=sourceIndex===null?newMaterialDraft():structuredClone(recipeDraft.sources[sourceIndex]);
      resetMaterialPicker();
      if(sourceIndex===null)sourceDraft.type='book';
     }
     go(message.route);
    }
    else {if(message.route==='word')selectedId=-1;go(message.route)}
   }else if(module==='web')render();else render();
   if(v.state){const control=document.getElementById(v.state.id);if(control){control.value=v.state.value;control.dispatchEvent(new Event('change',{bubbles:true}))}}
  }
  applying=false;changed();
  if(module==='theme')document.getElementById('theme')?.dispatchEvent(new Event('change',{bubbles:true}));
 }
 window.addEventListener('message',event=>{if(event.origin!==location.origin||event.source!==parent||event.data?.type!=='kanji-viewer-configure')return;configure(event.data)});
 const start=()=>{
  if(!ready()){setTimeout(start,50);return}
  if(module!=='theme'){
   const original=window.render;
   window.render=function(...args){const value=original(...args);changed();return value};
  }else new ResizeObserver(changed).observe(document.body);
  send({type:'kanji-viewer-ready',module});
  if(queued){const value=queued;queued=null;configure(value)}else changed();
 };
 start();
})();
