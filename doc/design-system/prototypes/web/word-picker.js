'use strict';
Object.assign(copy.ru,{selectionBack:'← Все',wordViewAll:'Все',wordViewSelected:'Выбранные',wordViewMode:'Режим просмотра слов',selectedWordsOnly:'Только выбранные',selectedWords:n=>`Выбрано: ${n}`,clearWordSelection:'Снять выбор'});
Object.assign(copy.en,{selectionBack:'← All',wordViewAll:'All',wordViewSelected:'Selected',wordViewMode:'Word list view',selectedWordsOnly:'Selected only',selectedWords:n=>`Selected: ${n}`,clearWordSelection:'Clear selection'});
let materialWordKanji=[],materialWordSelectedOnly=false,materialWordKanjiOpen=false,materialWordSearchOpen=false;
function resetMaterialWordPicker(){materialWordKanji=[];materialWordSelectedOnly=false;materialWordKanjiOpen=false;materialWordSearchOpen=false;}
function handleMaterialWordAction(action,value){
 if(materialOpenFilter!=='words')return false;
 if((action==='go'&&value==='material-filter'&&materialWordKanjiOpen)||action==='word-kanji-done'){materialWordKanjiOpen=false;materialKanjiPanel=null;render();return true;}
 if(action==='select-word'){const id=Number(value),selected=new Set(sourceDraft.word_ids||(sourceDraft.word?[sourceDraft.word]:[]));if(selected.has(id))selected.delete(id);else selected.add(id);sourceDraft.word_ids=[...selected];sourceDraft.word=null;const scroll=document.querySelector('.recipe-source-content').scrollTop;render();document.querySelector('.recipe-source-content').scrollTop=scroll;document.querySelector(`[data-action="select-word:${id}"]`)?.focus({preventScroll:true});return true;}
 if(action==='word-view'){materialWordSelectedOnly=value==='selected';render();document.querySelector(`[data-action="word-view:${value}"]`)?.focus({preventScroll:true});return true;}
 if(action==='filter'&&value==='word-kanji'){materialWordKanjiOpen=true;materialKanjiPanel=null;render();return true;}
 if(action==='word-kanji-clear'){materialWordKanji=[];materialWordKanjiOpen=false;materialKanjiPanel=null;render();return true;}
 if(action==='clear-word-kanji'){materialWordKanji=[];render();return true;}
 if(action==='select-kanji'&&materialWordKanjiOpen){const id=Number(value);materialWordKanji=materialWordKanji.includes(id)?materialWordKanji.filter(n=>n!==id):[...materialWordKanji,id];const scroll=document.querySelector('.recipe-source-content').scrollTop;render();document.querySelector('.recipe-source-content').scrollTop=scroll;document.querySelector(`[data-action="select-kanji:${id}"]`)?.focus({preventScroll:true});return true;}
 return false;
}
function wordSearchButton(){return `<button type="button" class="icon-button" data-word-search aria-label="${t(materialWordSearchOpen?'close':'search')}" aria-expanded="${materialWordSearchOpen}">${icon('search')}</button>`;}
function renderMaterialWords(){
 if(materialWordKanjiOpen)return renderMaterialKanjiPicker({type:'kanji',kanji_ids:materialWordKanji},true);
 const selected=sourceDraft.word_ids||(sourceDraft.word?[sourceDraft.word]:[]),q=sourceQuery.trim().normalize('NFKC').toLowerCase();
 const list=sourceWords({type:'kanji',kanji_ids:materialWordKanji}).filter(w=>(!materialWordSelectedOnly||selected.includes(w.id))&&[w.written,w.reading,translation(w)].join(' ').normalize('NFKC').toLowerCase().includes(q));
 const kanjiLabel=CatalogSelection.filterTitle(t('kanji'),materialWordKanji,id=>TC.kanji.find(k=>k.id===id)?.symbol||String(id));
 const kanjiChip=CatalogSelection.filterChip({title:kanjiLabel,type:'word-kanji',active:materialWordKanji.length>0,clear:'clear-word-kanji',clearLabel:t('clear'),arrow:materialFilterArrow(),close:materialFilterClose(),html:escapeHTML});
 const viewMode=materialSelectionView('word',materialWordSelectedOnly,selected.length);
 return `<div class="recipe-editor-screen material-catalog-filter">${header(t('chooseWords'),'recipe-source',wordSearchButton())}<div class="recipe-content source-picker recipe-source-content">${materialWordSearchOpen?`<label class="n-search">${icon('search')}<input id="material-word-query" type="search" aria-label="${t('searchWords')}" placeholder="${t('searchWords')}" value="${escapeHTML(sourceQuery)}"></label>`:''}<div class="w-textbook-filterbar">${kanjiChip}</div><div class="material-word-summary">${viewMode}<button type="button" class="text-button" data-word-clear ${selected.length?'':'disabled'}>${t('clearWordSelection')}</button></div><div class="n-word-list w-chapter-word-list">${list.map(w=>CatalogSelection.wordRow(w,{gloss:translation(w),html:escapeHTML,select:true,selected:selected.includes(w.id)})).join('')||`<div class="recipe-search-empty"><img src="../../assets/mascot/mascot_sticker_peek_empty_box.png" alt=""><h3>${t('materialNoWords')}</h3><p>${t('materialNoWordsHint')}</p></div>`}</div></div><div class="recipe-editor-footer"><button type="button" class="recipe-save-fab" data-action="go:recipe-source">${selected.length?t('chooseCount')(selected.length):t('apply')}</button></div></div>`;
}
document.addEventListener('click',e=>{
 if(screen!=='material-filter'||materialOpenFilter!=='words')return;
 if(e.target.closest('[data-word-search]')){materialWordSearchOpen=!materialWordSearchOpen;if(!materialWordSearchOpen)sourceQuery='';render();document.getElementById('material-word-query')?.focus();}
 if(e.target.closest('[data-word-clear]')){sourceDraft.word_ids=[];sourceDraft.word=null;render();}
});

