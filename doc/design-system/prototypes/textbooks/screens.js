'use strict';
// Current screen compositions. Approval lives in theme/screens.json, never in the renderer key.

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
  const references=current.route.kind==='references';
  const content=references?`<section class="p-references"><div class="p-reference-group">${reference(text('catalog_all_kanji'),localized(referenceDescriptions.kanji),'all-kanji',kanjiIcon)}${reference(text('catalog_all_radicals'),localized(referenceDescriptions.radicals),'all-radicals',radicalIcon)}</div></section>`:`<section class="p-textbooks"><div class="p-source-list">${cards}</div></section>`;
  return `<div class="n-home catalog-proposal">${artFilter}<div class="p-catalog-intro"><p>${html(localized(catalogIntroTranslations[references?'references':'textbooks']))}</p><img class="p-reading-cat" src="../../assets/mascot/${references?'mascot_sticker_jp_inkstone_puzzle.png':'mascot_sticker_jp_bound_book.png'}" alt="" width="96" height="96"></div>${content}</div>`;

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
  const rows=select?1:Math.max(1,Math.ceil(chapter.symbols.length/capacity));
  const perRow=Math.floor(chapter.symbols.length/rows),extra=chapter.symbols.length%rows;
  let offset=0;
  const preview=Array.from({length:rows},(_,row)=>{
    const count=perRow+(row<extra?1:0),symbols=chapter.symbols.slice(offset,offset+count);offset+=count;
    return `<span class="p-book-kanji-row">${symbols.map(symbol=>`<span lang="ja">${html(symbol)}</span>`).join('')}</span>`;
  }).join('');
  return CatalogSelection.chapterCard({number:chapter.number,label,counts,preview,select,selected:CatalogSelection.filterValues(current.facetIds??current.route.selected).includes(chapter.number),html});

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
  const summary=words?workingWordsSummary(items.length,total,true):workingListSummary(items.length,total,'kanji',true);
  const content=workingFilterBar(words)+summary+(words?workingWordList(items):workingKanjiGrid(items));
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
function workingNavRow(title,subtitle,target,label=title){return CatalogSelection.navRow({title,subtitle,target,label,html,chevron:workingChevron()});}
function workingPanel(content,title=null){return title?workingSection(`<div class="n-info-panel">${content}</div>`,title):`<div class="n-info-panel">${content}</div>`;}
function workingSection(content,title){return `<section class="n-section"><h3>${html(title)}</h3>${content}</section>`;}
function catalogFacetTitle(key,value){const ru=uiLanguage()!=='en',ids=CatalogSelection.filterValues(value),titles={chapter:[ru?'Все главы':'All chapters',ru?'Главы':'Chapters'],strokes:[ru?'Все черты':'All stroke counts',ru?'Черты':'Strokes'],radical:[ru?'Все ключи':'All radicals',ru?'Ключи':'Radicals'],jlpt:['JLPT','JLPT'],kanji:[ru?'Все кандзи':'All kanji',ru?'Кандзи':'Kanji']};return CatalogSelection.filterTitle(titles[key][ids.length?1:0],ids,id=>key==='radical'?radicalById.get(id)?.symbol||String(id):key==='kanji'?kanjiById.get(id)?.symbol||String(id):key==='jlpt'?(id===-1?(ru?'Без JLPT':'No JLPT'):'N'+id):String(id));}
function workingFilterBar(words=false){
 const ru=uiLanguage()!=='en',fixed=current.route.fixedChapter||0;
 const chip=(key,value)=>CatalogSelection.filterChip({title:catalogFacetTitle(key,value),type:key,active:CatalogSelection.filterValues(value).length>0,clear:'clear-'+key,clearLabel:ru?'Сбросить фильтр':'Clear filter',arrow:workingChevron(),close:'<span aria-hidden="true">×</span>',html});
 const chapters=current.route.sourceId&&!fixed?chip('chapter',current.chapter):'';
 const strokes=!words?chip('strokes',current.strokes):'';
 if(current.route.kind==='all-radicals')return `<div class="w-textbook-filterbar">${strokes}</div>`;
 const symbols=words?chip('kanji',current.selectedKanjiIds?.length?current.selectedKanjiIds:current.selectedKanji):chip('radical',current.radical);
 return `<div class="w-textbook-filterbar">${chapters}${strokes}${symbols}${current.route.kind==='all-kanji'?chip('jlpt',current.jlpt):''}</div>`;
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
    if(!placement||!CatalogSelection.filterMatches(current.chapter,placement.chapter_number))continue;
    if(placement.kanji_number<best){best=placement.kanji_number;chapter=placement.chapter_number;}
  }
  return chapter;
}
function workingWordsSummary(count,total,always=false){
  const narrowed=count!==total||Boolean(current.query.trim())||JSON.stringify(CatalogSelection.filterValues(current.chapter))!==JSON.stringify(CatalogSelection.filterValues(current.route.fixedChapter))||Boolean(current.selectedKanji);
  if(!always&&!narrowed&&!current.route.fixedChapter)return '';
  const label=narrowed?(uiLanguage()==='en'?`Showing ${count} of ${catalogQuantity(total,'word')}`:`Показано ${count} из ${catalogQuantity(total,'word')}`):catalogQuantity(total,'word');
  return `<div class="w-kanji-summary"><p class="p-textbook-kanji-count" role="status">${html(label)}</p></div>`;
}
function workingListSummary(count,total,noun,numbered=false){
  const filtered=count!==total||Boolean(current.query)||JSON.stringify(CatalogSelection.filterValues(current.chapter))!==JSON.stringify(CatalogSelection.filterValues(current.route.fixedChapter))||[current.radical,current.selectedKanji,current.strokes,current.jlpt].some(v=>CatalogSelection.filterValues(v).length>0);
  const label=filtered?(uiLanguage()==='en'?`Showing ${count} of ${catalogQuantity(total,noun)}`:`Показано ${count} из ${catalogQuantity(total,noun)}`):catalogQuantity(total,noun);
  return `<div class="w-kanji-summary"><p class="p-textbook-kanji-count" role="status">${html(label)}</p>${numbered?placementLegend():''}</div>`;
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
  return CatalogSelection.symbolGrid(items,{select,radicals,showPlacement,sourceId:current.route.sourceId,selectedIds:[current.route.selected||0],placementByKey,uiLanguage,text,plural,meaning,radicalName,meaningVariants:workingKanjiMeaningVariants,html});
}
function workingKanjiGrid(items,select=false){return workingSymbolGrid(items,{select,showPlacement:!select});}
function workingRadicalGrid(items,select=false){return workingSymbolGrid(items,{select,radicals:true});}

function isWorkingSourceWords(){return current.route.kind==='source'&&current.tab===2;}
function workingChapterWordsEmpty(){return workingEmpty();}
function workingWordRow(word){
  const gloss=translation(word);
  const openHint=uiLanguage()==='ru'?'Открыть карточку слова':'Open word details';
  const aria=[word.written,word.reading,gloss,openHint].join('. ');
  return CatalogSelection.wordRow(word,{gloss,html,aria});
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
    return `<section class="w-word-chapter-block" aria-labelledby="${id}"><header class="w-word-chapter-heading"><h3 id="${id}">${html(text('catalog_chapter',b.chapter))}${blocks.length>1?`<span class="w-word-chapter-volume">${html((catalogLayout==='wip'?'\u00a0·\u00a0':' · ')+volume)}</span>`:''}</h3></header><div class="w-chapter-word-cards">${cards}</div></section>`;
  }).join('');
  return `<div class="n-word-list w-chapter-word-list w-chapter-word-list--grouped">${sections}</div>`;
}
function workingRelatedWordsEmpty(){return workingEmpty();}
function workingWordList(items){
  const related=current.route.kind==='words';
  if((isWorkingSourceWords()||related)&&items.length){
    const complete=!current.query.trim()&&!current.selectedKanji&&JSON.stringify(CatalogSelection.filterValues(current.chapter))===JSON.stringify(CatalogSelection.filterValues(current.route.fixedChapter));
    const groupChapters=related&&!CatalogSelection.filterValues(current.chapter).length&&current.route.sourceId>0;
    const scope=related?{sourceId:current.route.sourceId,kanji:current.route.id}:null;
    const sourceId=current.route.sourceId;
    const ordered=isWorkingSourceWords()?items.slice().sort((a,b)=>(wordSourceChapter(a,sourceId)||0)-(wordSourceChapter(b,sourceId)||0)):items;
    const list=workingWordListByChapter(ordered,groupChapters,scope);
    const ending=complete&&isWorkingSourceWords()&&!current.route.fixedChapter?`<div class="w-chapter-end">${catalogArtFilter()}<img src="assets/cat-reading.png" alt="" width="120" height="120"><p>${html(current.route.fixedChapter?(uiLanguage()==='en'?'That’s the whole chapter':'Вот и вся глава'):(uiLanguage()==='en'?'That’s the whole word list':'Вот и все слова учебника'))}</p></div>`:'';
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
  return CatalogSelection.wordDetails(word,{gloss:translation(word),html,usage:wordContent(word),usageTitle:uiLanguage()==='en'?'Usage':'Употребление',partsTitle:text('word_parts'),kanjiTitle:text('word_kanji'),kanjiGrid:literal.length?workingSymbolGrid(literal,{showPlacement:false}):''});
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
        // Preserve KanjiVG's writing-cell coordinates, including intentional whitespace.
        svg.setAttribute('viewBox','0 0 109 109');
        svg.setAttribute('preserveAspectRatio','xMidYMid meet');
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
function workingHelp(label,body){return `<details class="w-inline-help w-numbering-help"><summary aria-label="${html(label)}" title="${html(label)}"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 0 1 5 .3c0 1.7-2.5 1.9-2.5 3.7"/><path d="M12 16.5h.01"/></svg></summary><p>${html(body)}</p></details>`;}
function placementLegend(){return workingHelp(uiLanguage()==='en'?'Tile numbers':'Нумерация',text('catalog_placement_legend'));}
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
  const identity=`<div class="w-kanji-identity">${workingKanjiMark(k)}<p class="w-kanji-meaning">${html(meaning(k))}</p><p class="w-stroke-count">${html(plural('catalog_strokes',k.stroke_count))}</p></div>`;
  const classificationContent=workingKanjiClassification(k);
  const classification=classificationContent?`<div class="w-kanji-classification">${classificationContent}</div>`:'';
  const reading=(title,values)=>values.length?`<section class="n-section w-reading-block"><h3>${html(title)}</h3><p class="w-reading" lang="ja">${html(values.join('、'))}</p></section>`:'';
  const readings=`<div class="w-readings">${reading(text('catalog_on'),on)}${reading(text('catalog_kun'),kun)}</div>`;
  const radicalRow=radical?workingNavRow(text('catalog_radical'),`${radical.symbol} · ${radicalName(radical)}`,`radical:${radical.id}`,`${text('catalog_radical')}. ${radical.symbol}. ${radicalName(radical)}`):'';
  const wordCount=catalogQuantity(related.length,'word');
  const words=workingNavRow(text('catalog_related_words'),wordCount,`related:${k.id}`,`${text('catalog_related_words')}. ${wordCount}`);
  return `<div class="n-detail">${identity}${on.length||kun.length?readings:''}${classification}${radicalRow}${words}</div>`;
}

function workingKanjiPicker(){
 const ru=uiLanguage()!=='en',route=current.route;
 current.pickerIds??=[...(current.parent?.selectedKanjiIds||(route.selected?[route.selected]:[]))];
 const selected=current.pickerIds;
 const items=kanjiFor({...route.selection,kanji:0,strokes:current.strokes,radical:current.radical,jlpt:current.jlpt},current.query).filter(k=>!current.pickerSelectedOnly||selected.includes(k.id));
 const chip=(key,title)=>CatalogSelection.filterChip({title:catalogFacetTitle(key,current[key]),type:key,active:CatalogSelection.filterValues(current[key]).length>0,clear:'clear-'+key,clearLabel:ru?'Сбросить':'Clear',arrow:workingChevron(),close:'<span aria-hidden="true">×</span>',html});
 const toolbar=`<div class="w-textbook-filterbar">${chip('strokes',ru?'Все черты':'All stroke counts')}${chip('radical',ru?'Все ключи':'All radicals')}${chip('jlpt','JLPT')}</div>`;
 const controls=`<div class="material-word-summary"><button type="button" class="n-text-button material-selection-view" data-action="picker-view">${current.pickerSelectedOnly?(ru?'← Все':'← All'):(ru?'Выбрано: ':'Selected: ')+selected.length}</button><button type="button" class="n-text-button" data-action="picker-clear" ${selected.length?'':'disabled'}>${ru?'Снять выбор':'Clear selection'}</button></div>`;
 const grid=CatalogSelection.symbolGrid(items,{select:true,selectedIds:selected,showPlacement:false,uiLanguage,text,plural,meaning,radicalName,meaningVariants:k=>localized(k.translations,'',true).split('; '),html});
 return {extra:'',content:CatalogSelection.kanjiPicker({title:html(text('catalog_kanji')),back:`<button class="n-back" data-action="back" aria-label="${html(text('catalog_back'))}">${icon('back')}</button>`,searchButton:`<button class="n-back" data-action="picker-search" aria-label="${ru?'Поиск':'Search'}" aria-expanded="${!!current.pickerSearchOpen}"><svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg></button>`,content:`<div class="material-kanji">${toolbar}${current.pickerSearchOpen?search('catalog_search'):''}${controls}${grid}</div>`,apply:`<button class="recipe-save-fab" data-action="picker-apply">${selected.length?(ru?'Выбрать':'Choose')+' ('+selected.length+')':(ru?'Применить':'Apply')}</button>`})};
}
function workingFilterView(){
 const route=current.route,key=route.filter,selection=route.selection||{sourceId:route.sourceId},ru=uiLanguage()!=='en';if(key==='kanji')return workingKanjiPicker();
 current.facetIds??=CatalogSelection.filterValues(route.selected??current.parent?.[key]);const ids=current.facetIds;
 const visible=id=>!current.facetSelectedOnly||ids.includes(id);let body='';
 if(key==='chapter'){const q=current.query.trim().normalize('NFKC');body=`<div class="n-chapter-list p-book-chapter-list">${chaptersFor(route.sourceId,selection).filter(c=>visible(c.number)&&(!q||String(c.number)===String(Number(q)))).map(c=>workingChapterCard(c,true)).join('')}</div>`;}
 else if(key==='radical'){body=CatalogSelection.symbolGrid(radicalsFor(selection,current.query).filter(r=>visible(r.id)),{radicals:true,select:true,selectedIds:ids,showPlacement:false,uiLanguage,text,plural,meaning,radicalName,meaningVariants:workingKanjiMeaningVariants,html});}
 else{const radicalIndex=Boolean(selection.radicalIndex),groups=new Map(),pool=radicalIndex?DATA.radicals:kanjiFor({...selection,[key]:0},'');pool.forEach(item=>{const id=key==='jlpt'?item.classification?.jlpt_level||-1:item.stroke_count;groups.set(id,(groups.get(id)||0)+1);});const options=key==='jlpt'?[5,4,3,2,1,-1]:[...new Set([...groups.keys(),...ids])].sort((a,b)=>a-b);body=`<div class="n-word-list w-choice-options">${options.filter(visible).map(id=>CatalogSelection.navRow({title:key==='jlpt'?(id===-1?(ru?'Без JLPT':'No JLPT'):'JLPT N'+id):plural('catalog_strokes',id),subtitle:catalogQuantity(groups.get(id)||0,radicalIndex?'radical':'kanji'),target:`select-${key}:${id}`,select:true,selected:ids.includes(id),html})).join('')}</div>`;}
 const levelHelp=key==='jlpt'?workingHelp(ru?'Об уровнях':'About levels',ru?'Уровни — учебный ориентир, не официальный список JLPT.':'Levels are a study guide, not an official JLPT list.'):'';
 const controls=`<div class="material-word-summary"><button type="button" class="n-text-button" data-action="facet-view">${current.facetSelectedOnly?(ru?'← Все':'← All'):(ru?'Выбрано: ':'Selected: ')+ids.length}</button><button type="button" class="n-text-button" data-action="facet-clear" ${ids.length?'':'disabled'}>${ru?'Снять выбор':'Clear selection'}</button></div>`;
 return {extra:'',content:CatalogSelection.kanjiPicker({title:html(pageTitle()),back:`<button type="button" class="n-back" data-action="back" aria-label="${html(text('catalog_back'))}">${icon('back')}</button>`,searchButton:['chapter','radical'].includes(key)?`<button type="button" class="n-back" data-action="facet-search" aria-label="${ru?'Поиск':'Search'}" aria-expanded="${!!current.facetSearchOpen}">⌕</button>`:'',content:`${current.facetSearchOpen?search(key==='chapter'?'catalog_search_chapter':'catalog_search'):''}${controls}${levelHelp?`<div class="w-selector-summary-row">${levelHelp}</div>`:''}${body}`,apply:`<button type="button" class="recipe-save-fab" data-action="facet-apply">${ids.length?(ru?'Выбрать':'Choose')+' ('+ids.length+')':(ru?'Применить':'Apply')}</button>`})};
}
function workingView() {
  const route=current.route;
  if(route.kind==='catalog'||route.kind==='references')return {extra:'',content:workingCatalogHomeProposal()};
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
    return {extra:workingSearchControl(),content:`<div class="w-radical-detail">${workingPanel(hero)}${workingSection(workingListSummary(items.length,total,'kanji',Boolean(route.sourceId&&items.length))+workingKanjiGrid(items),text('catalog_related_kanji'))}</div>`};
  }
  const base={sourceId:route.sourceId,kanji:route.id},items=wordsFor(base,current.query);
  return {extra:workingSearchControl(),content:workingWordsSummary(items.length,wordsFor(base).length,true)+workingWordList(items)};
}
