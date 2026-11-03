'use strict';

const DATA = window.TEXTBOOK_CATALOG;
const $ = selector => document.querySelector(selector);
const html = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const byId = list => new Map(list.map(item => [item.id, item]));
const sourceById = byId(DATA.sources), wordById = byId(DATA.words), kanjiById = byId(DATA.kanji), radicalById = byId(DATA.radicals);
const placementByKey = new Map(DATA.placements.map(p => [p.source_id + ':' + p.kanji_id, p]));
let language = 'ru', current, nextId = 1, loadState = 'ready';
const catalogLayout = 'wip'; // Renderer/CSS key, never an approval status.
let reviewRegistry;
const entries = new Map();
const uiLanguage = () => DATA.strings[language] ? language : 'ru';
function format(text, args) { return text.replace(/%(?:(\d+)\$)?[ds]/g, (_, n) => String(args[(Number(n) || 1) - 1] ?? '')); }
function text(key, ...args) { return format(DATA.strings[uiLanguage()][key] ?? DATA.strings.ru[key] ?? key, args); }
function plural(key, count) {
  const forms = DATA.plurals[uiLanguage()][key] ?? DATA.plurals.ru[key];
  return format(forms[new Intl.PluralRules(uiLanguage()).select(count)] ?? forms.other, [count]);
}
function localized(translations, fallback = null, all = false) {
  for (const tag of [...new Set([language.toLowerCase(), language.toLowerCase().split('-')[0], 'ru'])]) {
    const found = translations.filter(item => item.language_tag.toLowerCase() === tag);
    if (found.length) return all ? found.map(item => item.text).join('; ') : found[0].text;
  }
  return fallback;
}
const sourceTitle = id => { const source = sourceById.get(id); return source ? localized(source.translations, source.original_title) : ''; };
const meaning = kanji => localized(kanji.translations, text('catalog_missing'), true);
const radicalName = radical => localized(radical.translations, text('catalog_missing'));
function wordContent(word) {
  const rows = word.content_translations || word.translations;
  for (const tag of [...new Set([language.toLowerCase(), language.toLowerCase().split('-')[0], 'ru'])]) {
    const row = rows.find(item => item.language_tag.toLowerCase() === tag);
    if (row) return row;
  }
  return null;
}
const translation = word => wordContent(word)?.text ?? text('catalog_missing');
const normalize = value => [...String(value).toLowerCase()].map(c => {const code = c.codePointAt(0); return code >= 0x30a1 && code <= 0x30f6 ? String.fromCodePoint(code - 0x60) : c;}).join('');
const matches = (query, values) => {const needle = normalize(query.trim()); return !needle || values.some(value => normalize(value).includes(needle));};

// The selections below mirror CatalogSnapshot; words retain their exact identities and source links.
function kanjiFor(selection = {}, query = '') {
  const scoped = selection.sourceId ? new Map(DATA.placements.filter(p => p.source_id === selection.sourceId && (!selection.chapter || p.chapter_number === selection.chapter)).map(p => [p.kanji_id, p.kanji_number])) : null;
  return DATA.kanji.filter(k => (query.trim() || k.radical_id || scoped) && (!scoped || scoped.has(k.id)) && (!selection.radical || k.radical_id === selection.radical) && (!selection.strokes || k.stroke_count === selection.strokes)
    && (!selection.jlpt || (selection.jlpt===-1 ? !k.classification?.jlpt_level : k.classification?.jlpt_level===selection.jlpt))
    && matches(query, [k.symbol, meaning(k), ...k.readings.map(r => r.text)]))
    .sort((a, b) => (query.trim() === a.symbol ? 0 : 1) - (query.trim() === b.symbol ? 0 : 1)
      || (scoped?.get(a.id) ?? a.id) - (scoped?.get(b.id) ?? b.id));
}
function wordsFor(selection = {}, query = '') {
  const first = new Map();
  for (const link of DATA.links) {
    const placement = placementByKey.get(link.source_id + ':' + link.kanji_id);
    if ((selection.sourceId && link.source_id !== selection.sourceId)
      || (selection.kanji && link.kanji_id !== selection.kanji)
      || (selection.chapter && placement?.chapter_number !== selection.chapter)
      || (selection.radical && kanjiById.get(link.kanji_id)?.radical_id !== selection.radical)) continue;
    first.set(link.word_id, Math.min(first.get(link.word_id) ?? Infinity, placement?.kanji_number ?? Number.MAX_SAFE_INTEGER));
  }
  return DATA.words.filter(w => first.has(w.id) && matches(query, [w.written, w.reading, translation(w)]))
    .sort((a, b) => first.get(a.id) - first.get(b.id) || a.id - b.id);
}
function chaptersFor(sourceId, selection = null) {
  const eligible = selection ? new Set(kanjiFor({...selection, chapter: 0, radical: selection.radical || 0}).filter(k => !selection.kanji || k.id === selection.kanji).map(k => k.id)) : null;
  const numbers = [...new Set(DATA.placements.filter(p => p.source_id === sourceId).map(p => p.chapter_number))].sort((a,b) => a-b);
  return numbers.map(number => {
    const placements = DATA.placements.filter(p => p.source_id === sourceId && p.chapter_number === number && (!eligible || eligible.has(p.kanji_id)));
    return {number, placements, kanjiCount: placements.length,
      wordCount: wordsFor({...selection, sourceId, chapter: number}).length,
      symbols: placements.sort((a,b) => a.kanji_number-b.kanji_number).map(p => kanjiById.get(p.kanji_id)?.symbol || '')};
  }).filter(chapter => chapter.kanjiCount > 0);
}
function radicalsFor(selection = null, query = '', ownStrokes = 0) {
  const ids = selection ? new Set(kanjiFor({...selection, radical: 0}, '').filter(k => !selection.kanji || k.id === selection.kanji).map(k => k.radical_id)) : null;
  return DATA.radicals.filter(r => (!ids || ids.has(r.id)) && (!ownStrokes || r.stroke_count === ownStrokes) && matches(query, [r.symbol, radicalName(r), r.id]))
    .sort((a,b) => a.stroke_count-b.stroke_count || a.id-b.id);
}

const icons = {
  back: '<path d="M12 5 5 12 12 19M5 12h16"/>',
  filter: '<path d="M3 4h18l-7 8v7l-4 2v-9Z"/>'
};
const icon = name => `<svg viewBox="0 0 24 24" aria-hidden="true">${icons[name]}</svg>`;
let quickRoutes = [];
function parseRoute(value) {
  const [path, query = ''] = value.replace(/^#/, '').split('?');
  const [kind, a, b] = path.split('/');
  const tab = Number(new URLSearchParams(query).get('tab')) || 0;
  if (kind === 'book') return {kind:'source', sourceId:2, tab};
  if (kind === 'chapter' && !a) return {kind:'source', sourceId:2, fixedChapter:1, tab:1};
  if (kind === 'source') return {kind:'source', sourceId:Number(a) || 2, tab};
  if (kind === 'chapter') return {kind:'source', sourceId:Number(a) || 2, fixedChapter:Number(b) || 1, tab:tab || 1};
  if (['word','kanji','radical','words'].includes(kind)) return {kind, id:Number(a), sourceId:Number(b) || 0};
  if (kind === 'filter') return a==='jlpt'?{kind:'filter',filter:a,sourceId:0,selection:{sourceId:0}}:{kind:'filter', filter:a || 'chapter', sourceId:2, selection:{sourceId:2}};
  return {kind:['all-kanji','all-radicals'].includes(kind) ? kind : 'catalog', sourceId:0};
}
function routeHash(route) {
  if (route.kind === 'source') return (route.fixedChapter ? `chapter/${route.sourceId}/${route.fixedChapter}` : `source/${route.sourceId}`) + (route.tab ? '?tab='+route.tab : '');
  if (['word','kanji','radical','words'].includes(route.kind)) return `${route.kind}/${route.id}/${route.sourceId || 0}`;
  if (route.kind === 'filter') return `filter/${route.filter}`;
  return route.kind;
}
function createEntry(route, parent = null) {
  const entry = {key:nextId++, route, parent, query:'', queries:{}, tab:route.tab || 0,
    chapter:route.fixedChapter || 0, radical:0, selectedKanji:0, strokes:0, jlpt:0, scrolls:{}, hidden:0};
  entries.set(entry.key, entry); return entry;
}
function rememberScroll() {const content = $('.n-content'); if(content && current) current.scrolls[current.tab] = content.scrollTop;}
function navigate(route) {
  rememberScroll();current = createEntry(route, current);
  history.pushState({catalogEntry:current.key}, '', '#'+routeHash(route)); render();
}
function quickNavigate(hash) {
  rememberScroll();
  const route=parseRoute(hash);
  if(route.kind==='filter') {
    current=createEntry({kind:'source',sourceId:route.sourceId,tab:route.filter==='kanji'?2:1});
    history.pushState({catalogEntry:current.key},'', '#'+routeHash(current.route));
    navigate(route);return;
  }
  current=createEntry(route);
  history.pushState({catalogEntry:current.key}, '', '#'+hash);render();
}
function back() {
  rememberScroll();
  if(current.parent) { history.back(); }
  else if(current.route.kind !== 'catalog') {quickNavigate('catalog');}
  else {if(parent!==window)parent.postMessage({type:'kanji-viewer-navigate',module:'web',route:'home'},location.origin);else location.href='../web/#home';}
}
window.addEventListener('popstate', event => {
  const saved=entries.get(event.state?.catalogEntry);
  current=saved||createEntry(parseRoute(location.hash));
  if(!saved)history.replaceState({catalogEntry:current.key},'',location.href);
  render();
});

function pageTitle(route = current.route) {
  const keys={catalog:'catalog_title',words:'catalog_related_words','all-kanji':'catalog_kanji','all-radicals':'catalog_all_radicals'};
  if(route.kind==='source')return route.fixedChapter?text('catalog_chapter',route.fixedChapter):sourceTitle(route.sourceId);
  if(route.kind==='word')return DATA.words.find(word=>word.id===route.id)?.written||text('catalog_word');
  if(route.kind==='kanji')return kanjiById.get(route.id)?.symbol||text('catalog_kanji');
  if(route.kind==='radical'){const radical=radicalById.get(route.id);return radical?radicalName(radical):text('catalog_radical');}
  if(route.kind==='filter'&&route.filter==='jlpt')return 'JLPT';
  if(route.kind==='filter')return route.filter==='strokes'?(uiLanguage()==='en'?'Stroke count':'Число черт'):text({chapter:'catalog_choose_chapter',kanji:'catalog_choose_kanji',radical:'catalog_choose_key'}[route.filter]);
  return text(keys[route.kind]||'catalog_title');
}
function detailContext(route=current.route){
  if(['all-kanji','all-radicals'].includes(route.kind))return text('catalog_global_reference');
  const kind={word:text('catalog_word'),kanji:text('catalog_kanji'),radical:text('catalog_radical')}[route.kind]||'';
  const book=(['word','kanji','radical','words'].includes(route.kind)||(route.kind==='source'&&route.fixedChapter))&&route.sourceId?sourceTitle(route.sourceId):'';
  return [kind,book].filter(Boolean).join(' · ');
}
function appbar() {
  const context=detailContext();
  return `<header class="n-appbar"><button class="n-back" data-action="back" aria-label="${html(text('catalog_back'))}">${icon('back')}</button><h2>${html(pageTitle())}</h2></header>${context?`<p class="n-source-context">${html(context)}</p>`:''}`;
}
function search(label) {
  return `<label class="n-search"><span class="n-search-icon" aria-hidden="true">⌕</span><input id="catalog-query" type="search" value="${html(current.query)}" placeholder="${html(text(label))}" aria-label="${html(text(label))}" autocomplete="off"><button class="n-clear-query" data-action="clear-query" ${current.query?'':'hidden'}>${html(text('word_clear'))}</button></label>`;
}
function sourceSelection() {return {sourceId:current.route.sourceId||0, chapter:current.chapter, radical:current.radical, kanji:current.selectedKanji, strokes:current.strokes||0,...(catalogLayout==='wip'&&current.route.kind==='all-kanji'?{jlpt:current.jlpt||0}:{})};}
const catalogCopy = {
  ru: {chapter:{one:'глава',few:'главы',many:'глав',other:'главы'},word:{one:'слово',few:'слова',many:'слов',other:'слова'},radical:{one:'ключ',few:'ключа',many:'ключей',other:'ключа'},kanji:'кандзи',open:'Открыть учебник'},
  en: {chapter:{one:'chapter',other:'chapters'},word:{one:'word',other:'words'},radical:{one:'radical',other:'radicals'},kanji:'kanji',open:'Open textbook'}
};
function catalogQuantity(count, noun) {
  const copy=catalogCopy[uiLanguage()];
  const label=noun==='kanji'?copy.kanji:copy[noun][new Intl.PluralRules(uiLanguage()).select(count)]||copy[noun].other;
  return new Intl.NumberFormat(uiLanguage(),{useGrouping:false}).format(count)+' '+label;
}
const textbookArtwork = Object.freeze({1:'textbook-sage.png',2:'textbook-blue.png'});
function textbookIcon(sourceId) {
  const artwork=textbookArtwork[sourceId];
  if(artwork)return `<img class="p-textbook-art" src="../../assets/textbook-icons/${artwork}" alt="" width="64" height="64">`;
  return `<svg class="p-textbook-icon" viewBox="0 0 48 52" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path class="p-book-cover" d="M10 12c0-4 3-6 7-6h23v33H17c-4 0-7 2-7 6V12Z"/><path class="p-book-pages" d="M17 39h23v7H17c-4 0-7-1-7-4s3-5 7-5"/><path d="M15 11v22M19 42h17"/><path class="p-book-ribbon" d="M29 6h6v15l-3-3-3 3V6Z"/><path d="M21 27h9M21 31h6"/></svg>`;
}
const referenceDescriptions = Object.freeze({
  kanji:[{language_tag:'ru',text:'Чтения, значения и слова'},{language_tag:'en',text:'Readings, meanings and words'}],
  radicals:[{language_tag:'ru',text:'Части кандзи и их значения'},{language_tag:'en',text:'Kanji components and their meanings'}]
});
function catalogArtFilter() {
  return '<svg class="p-art-filter" width="0" height="0" aria-hidden="true"><defs><filter id="catalog-dark-art" color-interpolation-filters="sRGB"><feColorMatrix in="SourceGraphic" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -3 -4 -1 2 0" result="inkMask"/><feFlood flood-color="var(--md-ref-artwork-dark-outline)" result="ink"/><feComposite in="ink" in2="inkMask" operator="in" result="adaptedInk"/><feComposite in="adaptedInk" in2="SourceGraphic" operator="atop"/></filter></defs></svg>';
}
function catalogStatusCopy(){return {loadingTitle:text('catalog_loading'),loadingBody:'',errorTitle:text('catalog_error'),errorBody:'',retry:text('catalog_retry')};}
function setLoadState(value,{focus=false}={}) {
  loadState=value;
  $('#load-state').value=value;
  saveBench();
  current.hidden=0;
  current.scrolls[current.tab]=0;
  render();
  if(focus) {
    const target=$('.p-catalog-state-copy strong')||$('.n-appbar h2');
    if(target){target.tabIndex=-1;target.focus({preventScroll:true});}
  }
}

function reviewScreenKey(entry=current) {
  const route=entry.route;
  if(route.kind==='source')return (route.fixedChapter?'chapter':'textbook')+':'+entry.tab;
  if(route.kind==='filter')return 'filter:'+route.filter;
  return route.kind;
}
function readBench(){
  try{const value=JSON.parse(sessionStorage.getItem('textbooks-bench'));if(value&&typeof value==='object')return value;}catch{}
  return {};
}
function saveBench(){
  try{sessionStorage.setItem('textbooks-bench',JSON.stringify({
    theme:$('#theme').value,
    width:$('#width').value,
    scale:$('#text-scale').value,
    locale:language,
    load:loadState,
    collapse:$('#collapse-header').checked
  }));}catch{}
}
function applyBench(){
  const bench=readBench();
  const theme=bench.theme==='light'?'light':'dark';
  const width=['360','411.43','480'].includes(String(bench.width))?String(bench.width):'411.43';
  const scale=['1','1.3','1.6'].includes(String(bench.scale))?String(bench.scale):'1';
  const locale=['ru','en','de'].includes(bench.locale)?bench.locale:'ru';
  loadState=['ready','loading','error'].includes(bench.load)?bench.load:'ready';
  language=locale;
  $('#theme').value=theme;$('#phone').dataset.theme=theme;
  $('#width').value=width;$('#phone').style.width=width+'px';
  $('#text-scale').value=scale;$('#phone').style.setProperty('--scale',scale);
  $('#locale').value=locale;
  $('#load-state').value=loadState;
  $('#collapse-header').checked=bench.collapse!==false;
}
function isTextbookChapters(entry=current){return entry.route.kind==='source'&&!entry.route.fixedChapter&&entry.tab===0;}
function showsWorkingModule(){return isWorkingRemainingScreen();}
function isDesignedTextbook(){return isWorkingTextbook();}
function reviewStatus(entry=current){
  return reviewRegistry?.screens.find(screen=>screen.key===reviewScreenKey(entry))||{status:'unapproved',translations:[]};
}
function reviewText(screen,field){
  for(const tag of [...new Set([language.toLowerCase(),language.toLowerCase().split('-')[0],'ru'])]){const row=screen.translations.find(row=>row.language_tag.toLowerCase()===tag);if(row)return row[field]||'';}
  return '';
}
function reviewLabel(approved){return approved?(uiLanguage()==='en'?'Approved':'Согласовано'):(uiLanguage()==='en'?'Not approved':'Не согласовано');}
function render({keepFocus=false}={}) {
  const reviewKey=reviewScreenKey(),input=$('#catalog-query'),position=keepFocus?input?.selectionStart:null;
  const contentScroll=$('.n-content')?.scrollTop,initial=current.hidden;
  const phone=$('#phone');
  phone.hidden=false;
  phone.dataset.designedTextbook=String(isDesignedTextbook());
  phone.dataset.workingCatalogScreen=String(showsWorkingModule());
  phone.dataset.catalogVariant=current.route.kind==='catalog'?catalogLayout:'baseline';
  phone.dataset.screenVariant=catalogLayout;
  phone.dataset.screenKey=reviewKey;
  const output=loadState==='ready'?workingView():{extra:'',content:workingCatalogStatusView(loadState)};
  const chrome=showsWorkingModule()?workingModuleAppbar():isWorkingTextbook()?workingBookAppbar():appbar();
  $('#app').innerHTML=`<div class="n-page"><div class="n-chrome"><div class="n-chrome-inner">${chrome}${output.extra}</div></div><div class="n-content" id="catalog-content">${output.content}</div></div>`;
  document.documentElement.lang=uiLanguage();$('#app').lang=uiLanguage();$('#screen-title').textContent=pageTitle();
  const status=reviewStatus(),approved=status.status==='approved',en=uiLanguage()==='en';
  $('#view-status').textContent=reviewLabel(approved)+(loadState==='ready'?'':en?' · '+loadState:' · '+(loadState==='loading'?'ЗАГРУЗКА':'ОШИБКА'));
  $('#view-badge').textContent=approved?(en?'Unchanged':'Без изменений'):(en?'Needs approval':'На согласование');
  $('#view-badge').dataset.approval=status.status;
  $('#review-notice').hidden=approved;
  $('#review-notice').innerHTML=approved?'':`<h2>${html(en?'Updated screen — not approved':'Обновлённый экран — не согласован')}</h2><p>${html(reviewText(status,'changes'))}</p>`;
  const onlyPending=$('#review-filter').value==='unapproved';
  $('#screen-nav').innerHTML=quickRoutes.map(([hash,ru,name],index)=>{
    const route=parseRoute(hash),entry={route,tab:route.tab||0},status=reviewStatus(entry),ok=status.status==='approved';
    const mark=ok?'<path d="M2.2 6.1 4.6 8.5 9.8 3.3"/>':'<circle cx="6" cy="6" r="3.1"/>';
    return `<a class="nav-link ${routeHash(current.route)===routeHash(route)?'active':''}" href="#${hash}" data-quick="${hash}" data-approval="${status.status}" ${onlyPending&&ok?'hidden':''}><span class="nav-number">${String(index+1).padStart(2,'0')}</span><span class="nav-label">${html(en?name:ru)}</span><span class="nav-flag ${ok?'is-approved':'is-open'}" role="img" aria-label="${html(reviewLabel(ok))}" title="${html(reviewLabel(ok))}"><svg viewBox="0 0 12 12" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">${mark}</svg></span></a>`;
  }).join('');
  $('#screen-caption').textContent=en?'Material 3 design prototype. Android code is unchanged. Approval is recorded per screen.':'Макет Material 3. Android-код не менялся. Статус согласования указан для каждого экрана отдельно.';
  $('#collapse-header').disabled=isDesignedTextbook()||showsWorkingModule();
  configureChrome(initial);
  $('.n-content').scrollTop=keepFocus?contentScroll||0:current.scrolls[current.tab]||0;
  fitWorkingKanjiPreviews();mountKanjiVg($('#app'));lockFittedContent();
  requestAnimationFrame(()=>lockFittedContent());
  if((isWorkingRemainingScreen()&&workingSearchLabel()==='catalog_search_chapter'||isDesignedTextbook()&&current.tab===0)&&$('#catalog-query'))$('#catalog-query').inputMode='numeric';
  if(keepFocus&&$('#catalog-query')){const field=$('#catalog-query');field.focus({preventScroll:true});if(position!==null)try{field.setSelectionRange(position,position)}catch{}}
}
function configureChrome(hidden=0) {
  const chrome=$('.n-chrome'),inner=$('.n-chrome-inner'),content=$('.n-content');
  const max=inner.getBoundingClientRect().height;
  const pinned=showsWorkingModule()||isDesignedTextbook()||(current.route.kind==='catalog'&&loadState!=='ready');
  current.hidden=!pinned&&$('#collapse-header').checked?Math.min(hidden,max):0;
  chrome.style.height=(max-current.hidden)+'px';
  inner.style.transform=`translateY(${-current.hidden}px)`;
  function consume(delta){
    if(pinned||!$('#collapse-header').checked||!delta)return false;
    const previous=current.hidden;current.hidden=Math.max(0,Math.min(max,previous+delta));
    const used=current.hidden-previous;
    if(!used)return false;
    chrome.style.height=(max-current.hidden)+'px';inner.style.transform=`translateY(${-current.hidden}px)`;
    content.scrollTop+=delta-used;return true;
  }
  content.addEventListener('wheel',event=>{if(consume(event.deltaY))event.preventDefault();},{passive:false});
  let lastY=null;
  content.addEventListener('touchstart',event=>{lastY=event.touches[0]?.clientY??null;},{passive:true});
  content.addEventListener('touchmove',event=>{const next=event.touches[0]?.clientY;if(lastY!==null&&next!==undefined&&consume(lastY-next))event.preventDefault();lastY=next;},{passive:false});
  bindFilterReveal(content);
}
function contentFits(content){return content.scrollHeight<=content.clientHeight+1;}
function lockFittedContent(content=document.querySelector('.n-content')){
  if(!content)return;
  const fits=contentFits(content);
  content.style.overflowY=fits?'hidden':'';
  if(!fits)return;
  content.scrollTop=0;
  const bar=content.querySelector('.w-textbook-filterbar');
  if(!bar)return;
  bar.style.transform='';
  bar.classList.remove('is-concealed');
  content.style.setProperty('--w-filter-stick',bar.offsetHeight+'px');
}
function bindFilterReveal(content){
  const bar=content.querySelector('.w-textbook-filterbar');
  if(!bar)return;
  const max=bar.offsetHeight;
  let last=content.scrollTop,offset=Math.min(max,Math.max(0,last));
  const paint=value=>{
    offset=Math.max(0,Math.min(max,value));
    bar.style.transform=offset?`translateY(${-offset}px)`:'';
    bar.classList.toggle('is-concealed',offset>=max-0.5);
    content.style.setProperty('--w-filter-stick',(max-offset)+'px');
  };
  paint(contentFits(content)?0:offset);
  content.addEventListener('scroll',()=>{
    if(contentFits(content)){paint(0);return;}
    const top=content.scrollTop,delta=top-last;
    last=top;
    if(top<=0){paint(0);return;}
    paint(offset+delta);
  },{passive:true});
}
function restoreFilter(type,id) {
  const parent=current.parent;
  if(!parent){navigate({kind:'source',sourceId:current.route.sourceId||2});return;}
  if(type==='chapter')parent.chapter=id;
  else if(type==='kanji')parent.selectedKanji=id;
  else if(type==='strokes')parent.strokes=id;
  else if(type==='jlpt'){parent[type]=id;}
  else parent.radical=id;
  parent.scrolls[parent.tab]=0;
  history.back();
}

document.addEventListener('click',event=>{
  const brand=event.target.closest('.brand');
  if(brand){event.preventDefault();quickNavigate('catalog');return;}
  const quick=event.target.closest('[data-quick]');
  if(quick){event.preventDefault();quickNavigate(quick.dataset.quick);return;}
  const button=event.target.closest('[data-action]');
  if(!button)return;
  const [name,raw='']=button.dataset.action.split(':'),id=Number(raw);
  const sourceId=current.route.sourceId||0;
  if(name==='back')back();
  else if(name==='toggle-module-search'){
    const open=workingSearchOpen();
    current.searchOpenTabs??={};current.searchOpenTabs[current.tab]=!open;
    if(open){current.query='';current.queries[current.tab]='';current.scrolls[current.tab]=0;}
    current.hidden=0;render();
    (open?$('[data-action="toggle-module-search"]'):$('#catalog-query'))?.focus();
  }
  else if(name==='toggle-chapter-search'){
    const wasOpen=Boolean(current.chapterSearchOpen||current.query);
    current.chapterSearchOpen=!wasOpen;
    if(wasOpen){current.query='';current.queries[0]='';current.scrolls[0]=0;}
    current.hidden=0;render();
    (wasOpen?$('.p-book-search-toggle'):$('#catalog-query'))?.focus();
  }
  else if(name==='source')navigate({kind:'source',sourceId:id,tab:0});
  else if(name==='chapter')navigate({kind:'source',sourceId,fixedChapter:id,tab:1});
  else if(name==='word')navigate({kind:'word',id,sourceId});
  else if(name==='kanji'||name==='radical')navigate({kind:name,id,sourceId});
  else if(name==='study-kanji'){const [kanji,source]=raw.split('/').map(Number);navigate({kind:'kanji',id:kanji,sourceId:source});}
  else if(name==='related')navigate({kind:'words',id,sourceId});
  else if(name==='all-kanji'||name==='all-radicals')navigate({kind:name,sourceId:0});
  else if(name==='tab'){
    rememberScroll();current.queries[current.tab]=current.query;
    current.chaptersByTab??={};current.chaptersByTab[current.tab]=current.chapter;
    current.tab=id;current.chapter=current.chaptersByTab[id]??current.route.fixedChapter??0;current.query=current.queries[id]||'';
    current.route.tab=id;current.hidden=0;history.replaceState({catalogEntry:current.key},'', '#'+routeHash(current.route));render();
  }
  else if(name==='filter'){
    const selection=sourceSelection();
    // Word filters ignore the radical, kanji selection ignores itself, other facets retain it.
    const forWords=current.route.kind==='source'&&current.tab===2;
    if(forWords){selection.radical=0;selection.strokes=0;delete selection.jlpt;}
    if(raw==='kanji')selection.kanji=0;
    if(current.route.kind==='all-radicals')selection.radicalIndex=true;
    navigate({kind:'filter',filter:raw,sourceId,selection,forWords,selected:raw==='chapter'?current.chapter:raw==='kanji'?current.selectedKanji:raw==='strokes'?current.strokes:raw==='jlpt'?current.jlpt:current.radical});
  }
  else if(name.startsWith('select-'))restoreFilter(name.slice(7),id);
  else if(name==='clear-query'){current.query='';current.scrolls[current.tab]=0;render();$('#catalog-query')?.focus();}
  else if(name==='clear-chapter'){current.chapter=current.route.fixedChapter||0;current.scrolls[current.tab]=0;render();}
  else if(name==='clear-kanji'){current.selectedKanji=0;current.scrolls[current.tab]=0;render();}
  else if(name==='clear-radical'){current.radical=0;current.scrolls[current.tab]=0;render();}
  else if(name==='clear-jlpt'){current.jlpt=0;current.scrolls[current.tab]=0;render();}
  else if(name==='clear-strokes'){current.strokes=0;current.scrolls[current.tab]=0;render();}
  else if(name==='reset-words'){current.chapter=current.route.fixedChapter||0;current.selectedKanji=0;current.scrolls[current.tab]=0;render();}
  else if(name==='retry'){setLoadState((current.route.kind==='catalog')||isDesignedTextbook()||showsWorkingModule()?'loading':'ready',{focus:true});}
});
document.addEventListener('keydown',event=>{
  if(event.key==='Escape'&&event.target.id==='catalog-query'&&showsWorkingModule()){event.preventDefault();$('[data-action="toggle-module-search"]')?.click();return;}
  if(event.key==='Escape'&&event.target.id==='catalog-query'&&isDesignedTextbook()&&current.tab===0){event.preventDefault(); $('[data-action="toggle-chapter-search"]')?.click();}
});
document.addEventListener('input',event=>{
  if(event.target.id==='catalog-query'){
    current.query=event.target.value;current.scrolls[current.tab]=0;current.hidden=0;render({keepFocus:true});
  }
});
document.addEventListener('change',event=>{
  const {id,value}=event.target;
  rememberScroll();
  if(id==='theme')$('#phone').dataset.theme=value;
  else if(id==='review-filter'){render();return;}
  else if(id==='width')$('#phone').style.width=value+'px';
  else if(id==='text-scale')$('#phone').style.setProperty('--scale',value);
  else if(id==='locale')language=value;
  else if(id==='load-state'){setLoadState(value);return;}
  else if(id==='collapse-header')current.hidden=0;
  else return;
  saveBench();render();
});
window.addEventListener('resize',()=>{rememberScroll();render();});

// Browser initialization. Unit tests load declarations without bootstrapping DOM/history.
async function startPrototype(){
  try{
    const response=await fetch('../../theme/screens.json',{cache:'no-store'});
    if(!response.ok)throw new Error('Review registry unavailable');
    reviewRegistry=await response.json();reviewRegistry={...reviewRegistry,screens:reviewRegistry.screens.filter(s=>s.module==='catalog')};
    quickRoutes=reviewRegistry.screens.map(screen=>[screen.route,screen.translations.find(t=>t.language_tag==='ru').title,screen.translations.find(t=>t.language_tag==='en').title]);
    current=createEntry(parseRoute(location.hash));applyBench();
    history.replaceState({catalogEntry:current.key},'', '#'+routeHash(current.route));render();
    document.fonts.ready.then(()=>fitWorkingKanjiPreviews());
    window.catalogPrototype={data:DATA,kanjiFor,wordsFor,chaptersFor,radicalsFor,get review(){return reviewRegistry;}};
  }catch(error){
    $('#view-status').textContent='Статус не загружен';$('#review-notice').hidden=false;
    $('#review-notice').innerHTML='<h2>Не удалось открыть макет</h2><p>Запустите локальный HTTP-сервер по инструкции в README и обновите страницу.</p>';
    console.error(error);
  }
}
startPrototype();
