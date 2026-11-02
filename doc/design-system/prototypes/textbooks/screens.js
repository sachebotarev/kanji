'use strict';
// Current screen compositions. Approval lives in review-status.json, never in the renderer key.

function workingCatalogHomeProposal() {
  const artFilter=catalogArtFilter();
  const chevron='<svg class="p-reference-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 6 6 6-6 6"/></svg>';
  const copy=catalogCopy[uiLanguage()];
  const cards=DATA.sources.map(source=>{
    const kanjiCount=DATA.placements.filter(p=>p.source_id===source.id).length;
    const wordCount=new Set(DATA.links.filter(l=>l.source_id===source.id).map(l=>l.word_id)).size;
    const volume=catalogQuantity(kanjiCount,'kanji')+' · '+catalogQuantity(wordCount,'word');
    return `<button class="n-source-card p-source-card" data-id="${source.id}" data-action="source:${source.id}" aria-label="${html(copy.open+' '+sourceTitle(source.id)+'. '+volume)}"><span class="p-source-body"><span class="p-book-icon">${textbookIcon(source.id)}</span><span class="p-source-heading"><strong>${html(sourceTitle(source.id))}</strong><span class="p-source-details"><span class="p-source-meta">${html(volume)}</span></span></span>${chevron}</span></button>`;
  }).join('');
  const reference=(title,subtitle,target,symbol)=>`<button class="n-nav-row p-reference-row" data-action="${target}"><span class="p-reference-icon" aria-hidden="true">${symbol}</span><span class="p-reference-text"><strong>${html(title)}</strong><small>${html(subtitle)}</small></span>${chevron}</button>`;
  const kanjiIcon='<img class="p-reference-bitmap" src="../../assets/reference-icons/kanji-sage.png" alt="" width="36" height="36">';
  const radicalIcon='<img class="p-reference-bitmap" src="../../assets/reference-icons/radical-ochre.png" alt="" width="36" height="36">';
  return `<div class="n-home catalog-proposal">${artFilter}<section class="p-textbooks" aria-labelledby="textbooks-heading"><div class="p-section-intro"><h3 id="textbooks-heading">${html(text('catalog_sources'))}</h3></div><div class="p-source-list">${cards}</div></section><section class="p-references" aria-labelledby="references-heading"><h3 id="references-heading">${html(text('catalog_reference'))}</h3><div class="p-reference-group">${reference(text('catalog_all_kanji'),localized(referenceDescriptions.kanji),'all-kanji',kanjiIcon)}${reference(text('catalog_all_radicals'),localized(referenceDescriptions.radicals),'all-radicals',radicalIcon)}</div></section><div class="p-catalog-mascot" aria-hidden="true"><img class="p-reading-cat" src="assets/cat-reading.png" alt="" width="156" height="156"></div></div>`;

}

function workingCatalogStatusView(state,copy=catalogStatusCopy()) {
  const isLoading=state==='loading';
  const art=isLoading?'assets/cat-shelving-books.png?v=1':'assets/cat-catalog-error-v2.png';
  const title=isLoading?copy.loadingTitle:copy.errorTitle;
  const body=isLoading?copy.loadingBody:copy.errorBody;
  const progress=isLoading?`<div class="p-catalog-state-progress" aria-hidden="true"><i></i><i></i><i></i></div>`:`<button class="p-catalog-retry" data-action="retry">${html(copy.retry)}</button>`;
  const role=isLoading?'status':'alert';
  return `${catalogArtFilter()}<div class="p-catalog-state${isLoading?'':' is-error'}"><img class="p-catalog-state-art" src="${art}" alt="" width="156" height="156"><div class="p-catalog-state-copy" role="${role}"${isLoading?' aria-live="polite" aria-busy="true"':''}><strong tabindex="-1">${html(title)}</strong>${body?`<p>${html(body)}</p>`:''}</div>${progress}</div>`;
}

function workingBookCopy(){return {search:text('catalog_open_search'),closeSearch:text('catalog_close_search')};}
function isWorkingTextbook(){return catalogLayout==='wip'&&current.route.kind==='source'&&!current.route.fixedChapter;}
function workingBookAppbar(){
  const copy=workingBookCopy(),isChapters=current.tab===0;
  const expanded=Boolean(current.chapterSearchOpen||current.query);
  // Official Google Material Icons Round, original paths preserved.
  const searchIcon="<svg fill=\"currentColor\" aria-hidden=\"true\" xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 0 24 24\" width=\"24\"><path d=\"M0 0h24v24H0V0z\" fill=\"none\"/><path d=\"M15.5 14h-.79l-.28-.27c1.2-1.4 1.82-3.31 1.48-5.34-.47-2.78-2.79-5-5.59-5.34-4.23-.52-7.79 3.04-7.27 7.27.34 2.8 2.56 5.12 5.34 5.59 2.03.34 3.94-.28 5.34-1.48l.27.28v.79l4.25 4.25c.41.41 1.08.41 1.49 0 .41-.41.41-1.08 0-1.49L15.5 14zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z\"/></svg>";
  const searchOffIcon="<svg fill=\"currentColor\" aria-hidden=\"true\" xmlns=\"http://www.w3.org/2000/svg\" enable-background=\"new 0 0 24 24\" height=\"24\" viewBox=\"0 0 24 24\" width=\"24\"><g><rect fill=\"none\" height=\"24\" width=\"24\"/><rect fill=\"none\" height=\"24\" width=\"24\"/></g><g><g><path d=\"M15.5,14h-0.79l-0.28-0.27c1.2-1.4,1.82-3.31,1.48-5.34c-0.47-2.78-2.79-4.99-5.58-5.34C6.54,2.58,3.3,5.38,3.03,9h2.02 c0.24-2.12,1.92-3.8,4.06-3.98C11.65,4.8,14,6.95,14,9.5c0,2.49-2.01,4.5-4.5,4.5c-0.17,0-0.33-0.03-0.5-0.05l0,2.02 c0,0,0,0,0.01,0.01c1.8,0.13,3.47-0.47,4.72-1.55L14,14.71v0.79l4.25,4.25c0.41,0.41,1.08,0.41,1.49,0l0,0 c0.41-0.41,0.41-1.08,0-1.49L15.5,14z\"/><path d=\"M6.12,11.17L4,13.29l-2.12-2.12c-0.2-0.2-0.51-0.2-0.71,0l0,0c-0.2,0.2-0.2,0.51,0,0.71L3.29,14l-2.12,2.12 c-0.2,0.2-0.2,0.51,0,0.71l0,0c0.2,0.2,0.51,0.2,0.71,0L4,14.71l2.12,2.12c0.2,0.2,0.51,0.2,0.71,0l0,0c0.2-0.2,0.2-0.51,0-0.71 L4.71,14l2.12-2.12c0.2-0.2,0.2-0.51,0-0.71l0,0C6.63,10.98,6.32,10.98,6.12,11.17z\"/></g></g></svg>";
  return `<header class="n-appbar p-book-appbar"><button class="n-back" data-action="back" aria-label="${html(text('catalog_back'))}">${icon('back')}</button><h2>${html(pageTitle())}</h2>${isChapters&&loadState==='ready'?`<button class="p-book-search-toggle" data-action="toggle-chapter-search" aria-label="${html(expanded?copy.closeSearch:copy.search)}" aria-expanded="${expanded}"${expanded?' aria-controls="book-chapter-search"':''}>${expanded?searchOffIcon:searchIcon}</button>`:''}</header>`;

}
function workingChapterCard(chapter,select=false){
  const counts=catalogQuantity(chapter.kanjiCount,'kanji')+(!select||current.route.forWords?' · '+catalogQuantity(chapter.wordCount,'word'):'');
  const label=text('catalog_chapter',chapter.number);
  const chevron='<svg class="p-book-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 6 6 6-6 6"/></svg>';
  const scale=Number($('#text-scale').value);
  const available=Math.min(Number($('#width').value),$('#phone').clientWidth)-72;
  const capacity=Math.max(1,Math.floor((available+12)/(26*scale+12)));
  const rows=Math.max(1,Math.ceil(chapter.symbols.length/capacity));
  const perRow=Math.floor(chapter.symbols.length/rows),extra=chapter.symbols.length%rows;
  let offset=0;
  const preview=Array.from({length:rows},(_,row)=>{
    const count=perRow+(row<extra?1:0),symbols=chapter.symbols.slice(offset,offset+count);offset+=count;
    return `<span class="p-book-kanji-row">${symbols.map(symbol=>`<span lang="ja">${html(symbol)}</span>`).join('')}</span>`;
  }).join('');
  return `<button class="n-chapter-card p-book-chapter-card" data-id="${chapter.number}" data-action="${select?'select-chapter':'chapter'}:${chapter.number}" ${select?`aria-pressed="${chapter.number===(current.route.selected||0)}"`:''} aria-label="${html(label+'. '+counts)}"><span class="p-book-card-heading"><span class="p-book-card-labels"><strong>${html(label)}</strong><small>${html(counts)}</small>${select&&chapter.number===current.route.selected?`<span class="w-selection-check" aria-hidden="true">✓</span>`:''}</span>${chevron}</span><span class="p-book-chapter-preview" aria-hidden="true">${preview}</span></button>`;

}
function workingChapterList(chapters,select=false){
  if(!chapters.length)return workingEmpty();
  const query=current.query.trim().normalize('NFKC');
  if(!query)return `<div class="n-chapter-list p-book-chapter-list">${chapters.map(chapter=>workingChapterCard(chapter,select)).join('')}</div>`;
  const numeric=/^[0-9]+$/.test(query);
  const number=numeric?Number(query):null;
  const matches=numeric?chapters.filter(chapter=>chapter.number===number):[];
  if(!matches.length){
    return `<div class="p-book-empty p-book-empty--search" role="status">${workingSearchMissArt()}<strong>${html(text('catalog_empty'))}</strong><p>${html(text('catalog_chapter_range',chapters[0].number,chapters.at(-1).number))}</p></div>`;
  }
  return `<div class="n-chapter-list p-book-chapter-list">${matches.map(chapter=>workingChapterCard(chapter,select)).join('')}</div>`;
}
function workingSourceView() {
  const fixed=current.route.fixedChapter||0,selection=sourceSelection();
  const tabs=(fixed?[1,2]:[0,1,2]).map(tab=>`<button class="n-tab" role="tab" aria-selected="${current.tab===tab}" data-action="tab:${tab}">${html(text(['catalog_chapters','catalog_kanji','catalog_words'][tab]))}</button>`).join('');
  if(!fixed&&current.tab===0){
    const open=current.chapterSearchOpen||current.query;
    const controls=open?`<div id="book-chapter-search">${search('catalog_search_chapter')}</div>`:'';
    return {extra:`<div class="n-tabs" role="tablist">${tabs}</div>${controls}`,content:workingChapterList(chaptersFor(current.route.sourceId))};
  }
  const words=current.tab===2;
  const items=words?wordsFor({...selection,radical:0},current.query):kanjiFor(selection,current.query);
  const base={sourceId:current.route.sourceId,chapter:fixed};
  const total=words?wordsFor(base).length:kanjiFor(base).length;
  const summary=words?workingWordsSummary(items.length,total,true):workingListSummary(items.length,total,'kanji');
  const content=workingFilterBar(words)+summary+(!words?placementLegend():'')+(words?workingWordList(items):workingKanjiGrid(items));
  const wordGap=words?'<div class="w-word-tab-gap" aria-hidden="true"></div>':'';
  return {extra:`<div class="n-tabs" role="tablist">${tabs}</div>${workingSearchControl()}${wordGap}`,content};
}

const workingModuleIcons={"search":"<svg fill=\"currentColor\" aria-hidden=\"true\" xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 0 24 24\" width=\"24\"><path d=\"M0 0h24v24H0V0z\" fill=\"none\"/><path d=\"M15.5 14h-.79l-.28-.27c1.2-1.4 1.82-3.31 1.48-5.34-.47-2.78-2.79-5-5.59-5.34-4.23-.52-7.79 3.04-7.27 7.27.34 2.8 2.56 5.12 5.34 5.59 2.03.34 3.94-.28 5.34-1.48l.27.28v.79l4.25 4.25c.41.41 1.08.41 1.49 0 .41-.41.41-1.08 0-1.49L15.5 14zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z\"/></svg>","off":"<svg fill=\"currentColor\" aria-hidden=\"true\" xmlns=\"http://www.w3.org/2000/svg\" enable-background=\"new 0 0 24 24\" height=\"24\" viewBox=\"0 0 24 24\" width=\"24\"><g><rect fill=\"none\" height=\"24\" width=\"24\"/><rect fill=\"none\" height=\"24\" width=\"24\"/></g><g><g><path d=\"M15.5,14h-0.79l-0.28-0.27c1.2-1.4,1.82-3.31,1.48-5.34c-0.47-2.78-2.79-4.99-5.58-5.34C6.54,2.58,3.3,5.38,3.03,9h2.02 c0.24-2.12,1.92-3.8,4.06-3.98C11.65,4.8,14,6.95,14,9.5c0,2.49-2.01,4.5-4.5,4.5c-0.17,0-0.33-0.03-0.5-0.05l0,2.02 c0,0,0,0,0.01,0.01c1.8,0.13,3.47-0.47,4.72-1.55L14,14.71v0.79l4.25,4.25c0.41,0.41,1.08,0.41,1.49,0l0,0 c0.41-0.41,0.41-1.08,0-1.49L15.5,14z\"/><path d=\"M6.12,11.17L4,13.29l-2.12-2.12c-0.2-0.2-0.51-0.2-0.71,0l0,0c-0.2,0.2-0.2,0.51,0,0.71L3.29,14l-2.12,2.12 c-0.2,0.2-0.2,0.51,0,0.71l0,0c0.2,0.2,0.51,0.2,0.71,0L4,14.71l2.12,2.12c0.2,0.2,0.51,0.2,0.71,0l0,0c0.2-0.2,0.2-0.51,0-0.71 L4.71,14l2.12-2.12c0.2-0.2,0.2-0.51,0-0.71l0,0C6.63,10.98,6.32,10.98,6.12,11.17z\"/></g></g></svg>"};
function workingModuleCopy(){return {search:text('catalog_open_search'),hideSearch:text('catalog_close_search')};}
function isWorkingRemainingScreen(){return catalogLayout==='wip'&&current.route.kind!=='catalog'&&!isTextbookChapters();}
function workingSearchLabel(){
  const route=current.route;
  if(route.kind==='source')return current.tab===2?'catalog_search_words_short':current.tab===0?'catalog_search_chapter':'catalog_search';
  if(route.kind==='filter')return {jlpt:null,chapter:'catalog_search_chapter',kanji:'catalog_search',radical:'catalog_search_radical',strokes:null}[route.filter];
  if(route.kind==='words')return 'catalog_search_words_short';
  if(route.kind==='all-radicals')return 'catalog_search_radical';
  if(route.kind==='all-kanji'||route.kind==='radical')return 'catalog_search';
  return null;
}
function workingSearchOpen(){return Boolean(current.searchOpenTabs?.[current.tab]||current.query);}
function workingModuleAppbar(){
  const label=workingSearchLabel(),open=workingSearchOpen(),copy=workingModuleCopy();
  const contextText=detailContext();
  const context=contextText?`<p class="n-source-context">${html(contextText)}</p>`:'';
  const title=catalogLayout==='wip'&&current.route.kind==='words'?(uiLanguage()==='en'?'Words with ':'Слова с ')+(kanjiById.get(current.route.id)?.symbol||''):pageTitle();
  return `<header class="n-appbar p-book-appbar"><button class="n-back" data-action="back" aria-label="${html(text('catalog_back'))}">${icon('back')}</button><h2>${html(title)}</h2>${label&&loadState==='ready'?`<button class="p-book-search-toggle" data-action="toggle-module-search" aria-label="${html(open?copy.hideSearch:text(label))}" aria-expanded="${open}"${open?' aria-controls="module-search"':''}>${open?workingModuleIcons.off:workingModuleIcons.search}</button>`:''}</header>${context}`;
}
function workingSearchControl(){const label=workingSearchLabel();return label&&workingSearchOpen()?`<div id="module-search">${search(label)}</div>`:'';}
function workingSearchMissArt(){
  return `${catalogArtFilter()}<img class="w-empty-mascot" src="assets/cat-magnify.png" alt="" width="156" height="156">`;
}
function workingEmpty(notFound=false){
  const query=Boolean(current.query.trim());
  const title=text(notFound?'catalog_not_found':query?'catalog_empty':'catalog_no_materials');
  const hint=notFound?'':text(query?'catalog_empty_hint':'catalog_no_materials_hint');
  return `<div class="w-empty${query?' w-empty--search':''}" role="status">${query?workingSearchMissArt():''}<strong>${html(title)}</strong>${hint?`<p>${html(hint)}</p>`:''}</div>`;
}
function workingChevron(){return '<svg class="w-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 6 6 6-6 6"/></svg>';}
function workingNavRow(title,subtitle,target,label=title){return `<button class="n-nav-row w-nav-row" data-action="${target}" aria-label="${html(label)}"><span><strong>${html(title)}</strong>${subtitle!==null&&subtitle!==undefined?`<small>${html(subtitle)}</small>`:''}</span>${workingChevron()}</button>`;}
function workingPanel(content,title=null){return title?workingSection(`<div class="n-info-panel">${content}</div>`,title):`<div class="n-info-panel">${content}</div>`;}
function workingSection(content,title){return `<section class="n-section"><h3>${html(title)}</h3>${content}</section>`;}
function workingJlptFilterView(){
  const en=uiLanguage()==='en',picked=current.route.selected||0;
  const pool=kanjiFor({...current.route.selection,jlpt:0},current.query);
  const count=code=>pool.filter(k=>code===-1?!k.classification?.jlpt_level:k.classification?.jlpt_level===code).length;
  const label=code=>code===-1?(en?'No JLPT data':'Нет данных JLPT'):`JLPT N${code}`;
  const rows=[5,4,3,2,1,-1].filter(code=>count(code)>0||code===picked).map(code=>`<button class="n-nav-row w-nav-row" data-action="select-jlpt:${code}" aria-pressed="${code===picked}"><span><strong>${html(label(code))}</strong><small>${html(catalogQuantity(count(code),'kanji'))}</small></span>${code===picked?'<span class="w-selection-check" aria-hidden="true">✓</span>':''}</button>`).join('');
  const note=en?'Levels are a study guide, not an official JLPT list.':'Уровни — учебный ориентир, не официальный список JLPT.';
  return {extra:'',content:`<div class="w-selector-controls"><button class="n-text-button n-selector-all" data-action="select-jlpt:0">${html(en?'All levels':'Все уровни')}</button></div><p class="w-filter-note">${html(note)}</p><div class="n-word-list">${rows||workingEmpty()}</div>`};
}
function workingFilterBar(words=false){
  const ru=uiLanguage()==='ru',fixed=current.route.fixedChapter||0;
  const chapterLabel=current.chapter?text('catalog_chapter',current.chapter):(ru?'Все главы':'All chapters');
  const selected=words?kanjiById.get(current.selectedKanji):radicalById.get(current.radical);
  const idleKanji=isWorkingSourceWords()?(ru?'Слова с кандзи':'Words with kanji'):(ru?'Все кандзи':'All kanji');
  const label=selected?(words?(ru?'Кандзи ':'Kanji '):(ru?'Ключ ':'Radical '))+selected.symbol:(words?idleKanji:(ru?'Все ключи':'All radicals'));
  const arrow='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>';
  const close='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><path d="m7 7 10 10M17 7 7 17"/></svg>';
  const chip=(title,type,active,clear,clearLabel)=>`<span class="w-textbook-chip${active?' is-active':''}" role="group" aria-label="${html(title)}"><button class="w-chip-open" data-action="filter:${type}"><span>${html(title)}</span>${active&&catalogLayout==='wip'?'':arrow}</button>${active?`<button class="w-chip-reset" data-action="${clear}" aria-label="${html(clearLabel)}">${close}</button>`:''}</span>`;
  const chapter=current.route.sourceId&&!fixed?chip(chapterLabel,'chapter',Boolean(current.chapter),'clear-chapter',text('catalog_remove_chapter')):'';
  const strokeLabel=current.strokes?plural('catalog_strokes',current.strokes):(ru?'Все черты':'All stroke counts');
  const strokes=!words?chip(strokeLabel,'strokes',Boolean(current.strokes),'clear-strokes',ru?'Сбросить число черт':'Clear stroke count'):'';
  const reference=catalogLayout==='wip'&&current.route.kind==='all-kanji';
  const jlpt=reference?chip(current.jlpt?(current.jlpt===-1?(ru?'Без JLPT':'No JLPT'):`JLPT N${current.jlpt}`):'JLPT','jlpt',Boolean(current.jlpt),'clear-jlpt',ru?'Сбросить JLPT':'Clear JLPT'):'';
  if(current.route.kind==='all-radicals')return `<div class="w-textbook-filterbar">${strokes}</div>`;
  return `<div class="w-textbook-filterbar">${chapter}${strokes}${chip(label,words?'kanji':'radical',Boolean(selected),words?'clear-kanji':'clear-radical',text(words?'catalog_remove_kanji':'catalog_remove_radical'))}${jlpt}</div>`;
}
function wordSourceChapter(word,sourceId){
  const prefer=current.route.kind==='words'?current.route.id:(current.selectedKanji||0);
  if(prefer&&DATA.links.some(l=>l.word_id===word.id&&l.source_id===sourceId&&l.kanji_id===prefer)){
    return placementByKey.get(sourceId+':'+prefer)?.chapter_number||0;
  }
  let best=Infinity,chapter=0;
  for(const link of DATA.links){
    if(link.word_id!==word.id||link.source_id!==sourceId)continue;
    const placement=placementByKey.get(sourceId+':'+link.kanji_id);
    if(!placement||current.chapter&&placement.chapter_number!==current.chapter)continue;
    if(placement.kanji_number<best){best=placement.kanji_number;chapter=placement.chapter_number;}
  }
  return chapter;
}
function workingWordsSummary(count,total,always=false){
  const narrowed=count!==total||Boolean(current.query.trim())||current.chapter!==(current.route.fixedChapter||0)||Boolean(current.selectedKanji);
  if(!always&&!narrowed&&!current.route.fixedChapter)return '';
  const label=narrowed?(uiLanguage()==='en'?`Showing ${count} of ${catalogQuantity(total,'word')}`:`Показано ${count} из ${catalogQuantity(total,'word')}`):catalogQuantity(total,'word');
  return `<div class="w-kanji-summary"><p class="p-textbook-kanji-count" role="status">${html(label)}</p></div>`;
}
function workingListSummary(count,total,noun){
  const filtered=count!==total||Boolean(current.query)||current.chapter!==(current.route.fixedChapter||0)||Boolean(current.radical||current.selectedKanji||current.strokes||current.jlpt);
  const label=filtered?(uiLanguage()==='en'?`Showing ${count} of ${catalogQuantity(total,noun)}`:`Показано ${count} из ${catalogQuantity(total,noun)}`):catalogQuantity(total,noun);
  return `<div class="w-kanji-summary"><p class="p-textbook-kanji-count" role="status">${html(label)}</p></div>`;
}
function workingKanjiMeaningVariants(kanji){
  const tags=[...new Set([language.toLowerCase(),language.toLowerCase().split('-')[0],'ru'])];
  for(const tag of tags){const found=kanji.translations.filter(item=>item.language_tag.toLowerCase()===tag).map(item=>item.text);if(found.length)return found;}
  return [text('catalog_missing')];
}
function fitWorkingKanjiPreviews(){
  if(!showsWorkingModule()||loadState!=='ready')return;
  for(const caption of document.querySelectorAll('.w-compact-symbol .n-kanji-meaning')){
    const variants=JSON.parse(caption.dataset.meaningVariants||'[]');
    if(variants.length<2)continue;
    caption.textContent=variants.join('; ');
    while(variants.length>1&&caption.scrollHeight>caption.clientHeight+1){variants.pop();caption.textContent=variants.join('; ');}
  }
}
function workingSymbolGrid(items,{select=false,radicals=false,showPlacement=true}={}){
  if(!items.length)return workingEmpty();
  const sourceId=current.route.sourceId,ru=uiLanguage()==='ru';
  return `<div class="${radicals?'n-radical-grid':'n-kanji-grid'} w-compact-grid">${items.map(item=>{
    const placement=showPlacement&&!radicals&&sourceId?placementByKey.get(sourceId+':'+item.id):null;
    const number=radicals?item.id:placement?.kanji_number,chapter=placement?.chapter_number;
    const ordinal=chapter&&number?chapter+'–'+number:number;
    const full=radicals?radicalName(item):meaning(item);
    const variants=radicals?[full]:workingKanjiMeaningVariants(item);
    const strokesLabel=!radicals&&item.stroke_count?plural('catalog_strokes',item.stroke_count):'';
    const label=(chapter?text('catalog_chapter',chapter)+'. ':'')+(number?(radicals?text('catalog_radical_number',number):(ru?'Кандзи № ':'Kanji no. ')+number)+'. ':'')+(strokesLabel?strokesLabel+'. ':'')+item.symbol+' · '+full;
    const selected=select&&item.id===(current.route.selected||0);
    const mark=selected?'<span class="w-choice-check" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m5 12 4 4L19 6"/></svg></span>':'';
    const target=radicals?'radical':'kanji';
    return `<button class="${radicals?'n-radical-tile':'n-kanji-tile'} w-numbered-kanji w-compact-symbol${select&&radicals?' w-full-caption':''}" data-id="${item.id}" data-action="${select?'select-'+target:target}:${item.id}" aria-label="${html(label)}"${select?` aria-pressed="${selected}"`:''}>${number?`<span class="w-kanji-ordinal" aria-hidden="true">${html(ordinal)}</span>`:''}${mark}<span class="${radicals?'n-radical-symbol':'n-kanji-symbol'}" lang="ja" aria-hidden="true">${html(item.symbol)}</span><span class="w-kanji-caption-slot" aria-hidden="true"><span class="n-kanji-meaning" data-meaning-variants="${html(JSON.stringify(variants))}">${html(full)}</span></span>${strokesLabel?`<span class="w-kanji-strokes">${html(strokesLabel)}</span>`:''}</button>`;
  }).join('')}</div>`;
}
function workingKanjiGrid(items,select=false){return workingSymbolGrid(items,{select});}
function workingRadicalGrid(items,select=false){return workingSymbolGrid(items,{select,radicals:true});}

function isWorkingSourceWords(){return current.route.kind==='source'&&current.tab===2;}
function workingChapterWordsEmpty(){return workingEmpty();}
function workingWordRow(word){
  const gloss=translation(word);
  const openHint=uiLanguage()==='ru'?'Открыть карточку слова':'Open word details';
  const aria=[word.written,word.reading,gloss,openHint].join('. ');
  return `<button class="n-word-row w-word-row w-dictionary-row w-dictionary-row--open${[...word.written].length>8||[...word.reading].length>16?' is-long':''}" data-id="${word.id}" data-action="word:${word.id}" aria-label="${html(aria)}"><span class="w-dictionary-japanese"><span class="n-word-written" lang="ja">${html(word.written)}</span><span class="n-word-reading" lang="ja">${html(word.reading)}</span></span><span class="n-word-translation">${html(gloss)}</span></button>`;
}
function workingWordListByChapter(items,groupChapters,scope=null){
  if(!groupChapters)return `<div class="n-word-list w-chapter-word-list">${items.map(workingWordRow).join('')}</div>`;
  const sourceId=current.route.sourceId,blocks=[];

  let block=null;
  for(const word of items){
    const chapter=wordSourceChapter(word,sourceId)||0;
    if(!block||block.chapter!==chapter){block={chapter,words:[]};blocks.push(block);}
    block.words.push(word);
  }
  const sections=blocks.map(b=>{
    const cards=b.words.map(workingWordRow).join('');
    if(!b.chapter)return `<section class="w-word-chapter-block w-word-chapter-block--plain"><div class="w-chapter-word-cards">${cards}</div></section>`;
    const id=`w-word-ch-${sourceId}-${b.chapter}`;
    const volume=catalogQuantity(b.words.length,'word');
    return `<section class="w-word-chapter-block" aria-labelledby="${id}"><header class="w-word-chapter-heading"><h3 id="${id}">${html(text('catalog_chapter',b.chapter))}<span class="w-word-chapter-volume">${html((catalogLayout==='wip'?'\u00a0·\u00a0':' · ')+volume)}</span></h3></header><div class="w-chapter-word-cards">${cards}</div></section>`;
  }).join('');
  return `<div class="n-word-list w-chapter-word-list w-chapter-word-list--grouped">${sections}</div>`;
}
function workingRelatedWordsEmpty(){return workingEmpty();}
function workingWordList(items){
  const related=current.route.kind==='words';
  if((isWorkingSourceWords()||related)&&items.length){
    const complete=!current.query.trim()&&!current.selectedKanji&&current.chapter===(current.route.fixedChapter||0);
    const groupChapters=related&&!current.chapter&&Boolean(current.route.sourceId);
    const scope=related?{sourceId:current.route.sourceId,kanji:current.route.id}:null;
    const sourceId=current.route.sourceId;
    const ordered=isWorkingSourceWords()?items.slice().sort((a,b)=>(wordSourceChapter(a,sourceId)||0)-(wordSourceChapter(b,sourceId)||0)):items;
    const list=workingWordListByChapter(ordered,groupChapters,scope);
    const ending=complete&&isWorkingSourceWords()?`<div class="w-chapter-end">${catalogArtFilter()}<img src="assets/cat-reading.png" alt="" width="120" height="120"><p>${html(current.route.fixedChapter?(uiLanguage()==='en'?'That’s the whole chapter':'Вот и вся глава'):(uiLanguage()==='en'?'That’s the whole word list':'Вот и все слова учебника'))}</p></div>`:'';
    return `<div class="w-word-card-stack${groupChapters?' w-word-card-stack--grouped':''}">${list}</div>${ending}`;
  }
  if(!items.length&&related)return workingRelatedWordsEmpty();
  return items.length?`<div class="n-word-list">${items.map(workingWordRow).join('')}</div>`:isWorkingSourceWords()?workingChapterWordsEmpty():workingEmpty();
}
function workingDetailSection(title,content){return workingSection(content,title);}
function workingWordDetails() {
  const word=wordById.get(current.route.id);
  if(!word)return workingEmpty(true);
  const literal=[...new Set([...word.written].map(c=>kanjiById.get(c.codePointAt(0))).filter(Boolean))];
  const infoBlock=(title,body)=>`<section class="n-info-panel w-word-info"><h3>${html(title)}</h3>${body}</section>`;
  const longWord=[...word.written].length>6||[...word.reading].length>12;
  const identity=workingPanel(`<div class="n-word-hero${longWord?' is-long':''}"><span class="n-word-large ${[...word.written].length>8?'long':''}" lang="ja">${html(word.written)}</span><span class="w-word-detail-reading" lang="ja">${html(word.reading)}</span><p class="w-word-detail-translation">${html(translation(word))}</p></div>`);
  const usage=wordContent(word)?.usage_note?infoBlock(uiLanguage()==='en'?'Usage':'Употребление',`<p class="w-usage-note" lang="${html(wordContent(word).language_tag)}">${html(wordContent(word).usage_note)}</p>`):'';
  const parts=word.parts.length?infoBlock(text('word_parts'),word.parts.map(p=>`<p class="w-word-reading-part"><span lang="ja">${html(p.written_part)}</span><span lang="ja">${html(p.reading_part)}</span></p>`).join('')):'';
  const kanji=literal.length?workingDetailSection(text('word_kanji'),`<div class="w-word-kanji-grid">${workingSymbolGrid(literal,{showPlacement:false})}</div>`):'';
  return `<div class="n-detail w-word-detail">${identity}${usage}${parts}${kanji}</div>`;
}

const kanjiVgCache=new Map();
function kanjiVgFile(id){return `assets/kanjivg/${id.toString(16).padStart(5,'0')}.svg`;}
function kanjiVgMarkup(svgText){
  const start=svgText.indexOf('<svg');
  if(start<0)return '';
  const doc=new DOMParser().parseFromString(svgText.slice(start),'image/svg+xml');
  const svg=doc.querySelector('svg');
  if(!svg||doc.querySelector('parsererror')||!svg.querySelector('[id^="kvg:StrokePaths_"]'))return '';
  svg.querySelectorAll('script').forEach(node=>node.remove());
  svg.removeAttribute('width');
  svg.removeAttribute('height');
  svg.setAttribute('role','img');
  return svg.outerHTML;
}
function mountKanjiVg(root=document){
  root.querySelectorAll('[data-kanji-vg]').forEach(slot=>{
    const id=Number(slot.dataset.kanjiVg);
    const apply=markup=>{
      if(!slot.isConnected)return;
      const label=slot.querySelector('.n-kanji-hero')?.textContent||slot.getAttribute('aria-label')||'';
      if(!markup)return;
      slot.innerHTML=markup;
      const svg=slot.querySelector('svg');
      if(svg){
        svg.setAttribute('aria-label',label);
        const nodes=[...svg.querySelectorAll('[id^="kvg:StrokePaths"],[id^="kvg:StrokeNumbers"]')];
        let x0=Infinity,y0=Infinity,x1=-Infinity,y1=-Infinity;
        for(const node of nodes){
          if(!node.getBBox)continue;
          const box=node.getBBox();
          if(!box.width&&!box.height)continue;
          x0=Math.min(x0,box.x);y0=Math.min(y0,box.y);
          x1=Math.max(x1,box.x+box.width);y1=Math.max(y1,box.y+box.height);
        }
        if(x1>x0&&y1>y0){
          const pad=4,width=x1-x0+pad*2,height=y1-y0+pad*2;
          svg.setAttribute('viewBox',`${x0-pad} ${y0-pad} ${width} ${height}`);
          const scale=188/109;
          slot.style.width=`min(100%, ${width*scale}px)`;
        }
        if(typeof lockFittedContent==='function')lockFittedContent();
      }
    };
    if(kanjiVgCache.has(id)){apply(kanjiVgCache.get(id));return;}
    fetch(kanjiVgFile(id)).then(response=>response.ok?response.text():'').then(text=>{
      const markup=text?kanjiVgMarkup(text):'';
      kanjiVgCache.set(id,markup);
      apply(markup);
    }).catch(()=>kanjiVgCache.set(id,''));
  });
}
function workingKanjiMark(k){
  return `<div class="w-kanji-vg" data-kanji-vg="${k.id}" aria-label="${html(k.symbol)}"><div class="n-kanji-hero" lang="ja">${html(k.symbol)}</div></div>`;
}
function placementLegend(){return `<p class="w-placement-legend">${html(text('catalog_placement_legend'))}</p>`;}
function workingKanjiClassification(k){
  const c=k.classification||{},parts=[];
  if(c.school_stage){
    const key=c.school_stage==='elementary'?(c.school_year?'catalog_school_elementary':'catalog_school_elementary_unknown'):c.school_stage==='junior_high'?'catalog_school_junior':'catalog_school_high';
    parts.push(`<span class="w-school-classification">${html(text(key,c.school_year))}</span>`);
  }
  if(c.jlpt_level)parts.push(`<span>JLPT N${c.jlpt_level}</span>`);
  for(const [flag,key] of [['is_joyo','catalog_joyo'],['is_kyoiku','catalog_kyoiku'],['is_jinmeiyo','catalog_jinmeiyo']])if(k[flag])parts.push(`<span>${html(text(key))}</span>`);
  return parts.length?`<div class="w-classification-line">${parts.join('')}</div>`:'';
}
function workingKanjiDetails() {
  const k=kanjiById.get(current.route.id);
  if(!k)return workingEmpty(true);
  const related=wordsFor({sourceId:current.route.sourceId,kanji:k.id});
  const on=k.readings.filter(r=>r.reading_type==='on').map(r=>r.text),kun=k.readings.filter(r=>r.reading_type==='kun').map(r=>r.text);
  const radical=radicalById.get(k.radical_id);
  const identity=`<div class="n-info-panel w-kanji-identity">${workingKanjiMark(k)}<p class="w-kanji-meaning">${html(meaning(k))}</p><p class="w-stroke-count">${html(plural('catalog_strokes',k.stroke_count))}</p></div>`;
  const classificationContent=workingKanjiClassification(k);
  const classification=classificationContent?`<div class="n-info-panel w-kanji-classification">${classificationContent}</div>`:'';
  const reading=(title,values)=>values.length?`<section class="n-section w-reading-block"><h3>${html(title)}</h3><p class="w-reading" lang="ja">${html(values.join(' · '))}</p></section>`:'';
  const readings=`<div class="w-readings">${reading(text('catalog_on'),on)}${reading(text('catalog_kun'),kun)}<p class="w-reading-hint">${html(text('catalog_reading_hint'))}</p></div>`;
  const radicalRow=radical?workingNavRow(text('catalog_radical'),`${radical.symbol} · ${radicalName(radical)}`,`radical:${radical.id}`,`${text('catalog_radical')}. ${radical.symbol}. ${radicalName(radical)}`):'';
  const wordCount=catalogQuantity(related.length,'word');
  const words=workingNavRow(text('catalog_related_words'),wordCount,`related:${k.id}`,`${text('catalog_related_words')}. ${wordCount}`);
  return `<div class="n-detail">${identity}${on.length||kun.length?readings:`<p>${html(text('catalog_no_reading'))}</p>`}${classification}${radicalRow}${words}</div>`;
}

function workingFilterView() {
  const route=current.route,selection=route.selection||{sourceId:route.sourceId};
  if(route.filter==='jlpt')return workingJlptFilterView();
  const allLabel=route.filter==='chapter'?(uiLanguage()==='en'?'All chapters':'Все главы'):route.filter==='kanji'?(uiLanguage()==='en'?'All kanji':'Все кандзи'):route.filter==='strokes'?(uiLanguage()==='en'?'All stroke counts':'Все черты'):(uiLanguage()==='en'?'All radicals':'Все ключи');
  const allAction={chapter:'select-chapter:0',kanji:'select-kanji:0',radical:'select-radical:0',strokes:'select-strokes:0'}[route.filter];
  const all=`<div class="w-selector-controls"><button class="n-text-button n-selector-all" data-action="${allAction}" aria-pressed="${!current.route.selected}">${html(allLabel)}</button></div>`;
  if(route.filter==='chapter'){
    const chapters=chaptersFor(route.sourceId,selection),query=current.query.trim().normalize('NFKC');
    const shown=query?chapters.filter(chapter=>String(chapter.number)===String(Number(query))).length:chapters.length;
    return {extra:workingSearchControl(),content:all+workingListSummary(shown,chapters.length,'chapter')+workingChapterList(chapters,true)};
  }
  if(route.filter==='strokes'){
    const radicalIndex=Boolean(selection.radicalIndex);
    const groups=new Map();
    const pool=radicalIndex?DATA.radicals:kanjiFor({...selection,strokes:0},'');
    pool.forEach(item=>groups.set(item.stroke_count,(groups.get(item.stroke_count)||0)+1));
    const picked=current.route.selected||0;
    const noun=radicalIndex?'radical':'kanji';
    const rows=[...groups.keys()].sort((a,b)=>a-b).map(n=>`<button class="n-nav-row w-nav-row" data-action="select-strokes:${n}" aria-pressed="${n===picked}"><span><strong>${html(plural('catalog_strokes',n))}</strong><small>${html(catalogQuantity(groups.get(n),noun))}</small></span>${n===picked?'<span class="w-selection-check" aria-hidden="true">✓</span>':''}</button>`).join('');
    return {extra:'',content:all+`<div class="n-word-list">${rows}</div>`};
  }
  const radicals=route.filter==='radical';
  const items=radicals?radicalsFor(selection,current.query):kanjiFor({...selection,radical:0},current.query);
  const total=radicals?radicalsFor(selection).length:kanjiFor({...selection,radical:0}).length;
  return {extra:workingSearchControl(),content:all+workingListSummary(items.length,total,radicals?'radical':'kanji')+(!radicals?`<p class="w-selection-hint">${html(text('catalog_choose_kanji_hint'))}</p>`+placementLegend():'')+(radicals?workingRadicalGrid(items,true):workingKanjiGrid(items,true))};
}
function workingView() {
  const route=current.route;
  if(route.kind==='catalog')return {extra:'',content:workingCatalogHomeProposal()};
  if(route.kind==='source')return workingSourceView();
  if(route.kind==='word')return {extra:'',content:workingWordDetails()};
  if(route.kind==='kanji')return {extra:'',content:workingKanjiDetails()};
  if(route.kind==='filter')return workingFilterView();
  if(route.kind==='all-kanji'){
    const items=kanjiFor(sourceSelection(),current.query);
    const index=DATA.kanji.filter(kanji=>kanji.radical_id).length;
    return {extra:workingSearchControl(),content:workingFilterBar()+workingListSummary(items.length,index,'kanji')+workingKanjiGrid(items)};
  }
  if(route.kind==='all-radicals'){
    const items=radicalsFor(null,current.query,current.strokes||0);
    return {extra:workingSearchControl(),content:workingFilterBar()+workingListSummary(items.length,DATA.radicals.length,'radical')+workingRadicalGrid(items)};
  }
  if(route.kind==='radical'){
    const radical=radicalById.get(route.id);
    if(!radical)return {extra:'',content:workingEmpty(true)};
    const base={sourceId:route.sourceId,radical:route.id},items=kanjiFor(base,current.query),total=kanjiFor(base).length;
    const hero=`<div class="w-radical-summary"><span class="n-radical-symbol" lang="ja">${html(radical.symbol)}</span><div><strong>${html(radicalName(radical))}</strong><p class="w-detail-meta">${html(text('catalog_radical_number',radical.id))} · ${html(plural('catalog_strokes',radical.stroke_count))}</p></div></div>`;
    return {extra:workingSearchControl(),content:`<div class="w-radical-detail">${workingPanel(hero)}${workingSection(workingListSummary(items.length,total,'kanji')+(route.sourceId&&items.length?placementLegend():'')+workingKanjiGrid(items),text('catalog_related_kanji'))}</div>`};
  }
  const base={sourceId:route.sourceId,kanji:route.id},items=wordsFor(base,current.query);
  return {extra:workingSearchControl(),content:workingWordsSummary(items.length,wordsFor(base).length,true)+workingWordList(items)};
}
