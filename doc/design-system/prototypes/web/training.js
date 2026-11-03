'use strict';
// Clean, Compact Training Builder prototype
const trainingCopy={
 ru:{
  trainings:'Конструктор тренировок',trainingEditor:'Редактор тренировки',newTraining:'Создать тренировку',editTraining:'Изменить тренировку',
  duplicate:'Дублировать',duplicateShort:'Копия',copySuffix:' (копия)',personalDictionary:'Мой словарь',
  selected:'Выбрана',selectAction:'Выбрать',menuLabel:name=>`Действия: ${name}`,moreSources:(first,n)=>`${first} + ещё ${n}`,listHint:'Что будем тренировать?',listCount:n=>`${n} ${new Intl.PluralRules('ru').select(n)==='one'?'тренировка':new Intl.PluralRules('ru').select(n)==='few'?'тренировки':'тренировок'}`,kindWords:'Слова',kindBook:'Учебник',kindKanji:'Кандзи / JLPT',kindPersonal:'Мои слова',kindMixed:'Смешанная',createFab:'Создать',planDetails:'Подробнее',newShort:'Новая тренировка',perWord:n=>`${n} с на слово`,
  tasksLabel:'Заданий',timeLabel:'Таймер',poolLabel:'Слов',
  wordsShort:'сл.',cyclesShort:'по кругу',randomShort:'случайно',
  name:'Название',nameHint:'Например, Tobira: повторение',materials:'Учебный материал',addSource:'Добавить источник',
  source:'Источник',sourceHint:'Фильтры внутри источника пересекаются. Разные источники объединяются без повторов.',
  bookFilter:'Учебник или словарь',anyBook:'Все источники',chapters:'Главы',from:'С главы',to:'По главу',anyChapter:'Все главы',
  jlpt:'Уровень кандзи JLPT',anyLevel:'Любой уровень',kanjiFilter:'Слова с кандзи',anyKanji:'Любой кандзи',
  wordFilter:'Конкретное слово',anyWord:'Все подходящие слова',searchWords:'Найти слово, чтение или перевод',
  noWords:'По этим фильтрам слов нет. Измените условия.',add:'Добавить',apply:'Применить',remove:'Убрать',
  wordsLimit:'Заданий за сессию',timeLimit:'Время на одно слово',unlimited:'Без таймера',limited:'Ограничить',
  count:'Количество заданий',seconds:'Секунды',preview:'Состав тренировки',unique:'Уникальные слова в наборе',
  tasks:n=>`${n} ${new Intl.PluralRules('ru').select(n)==='one'?'задание':new Intl.PluralRules('ru').select(n)==='few'?'задания':'заданий'}`,
  poolSize:'По объёму набора',setCount:'Задать число',allWords:'Весь набор',secondsValue:n=>`${n} с`,
  available:n=>`Доступно: ${t('wordCount')(n)}`,session:n=>`Заданий: ${n}`,
  needSource:'Добавьте хотя бы один непустой источник.',invalidTraining:'Введите название и целые положительные ограничения.',
  saveTraining:'Сохранить тренировку',selectTraining:'Выбрать тренировку',
  deleteTraining:'Удалить тренировку?',deleteHint:'Настройки тренировки будут удалены. Учебные материалы останутся.',
  emptyTrainings:'Создайте свою первую тренировку',emptyTrainingHint:'Выберите материал и удобный темп.',
  draftOnly:'Макет для согласования. Сохранение действует до перезагрузки страницы.',savedTraining:'Тренировка сохранена',
  sourceCount:n=>`Источников: ${n}`,allCatalog:'Все слова',noActive:'Тренировка не выбрана',addFirst:'Создать тренировку',
  previewNote:'Здесь показаны уникальные слова набора, а не порядок и повторы заданий.',
  timerNote:'Таймер общий для всех знаков слова. Без ограничения — можно отвечать в своём темпе.',
  limitNote:'Число заданий может превышать число слов: слова будут повторяться.',
  discardTitle:'Сохранить изменения?',unsavedTraining:'В тренировке есть несохранённые изменения.',
  sourceEmpty:'Источник пуст. Измените фильтры.',noMatch:'Нет подходящих слов',sourceEditor:'Выбор материала',
  order:'Порядок заданий',cycles:'Вперемешку по кругу',random:'Случайно, с повторами',
  orderHint:'По кругу: весь набор перед новым повтором. Случайно: независимый выбор каждого слова.',
  optional:'Дополнительные фильтры',editSource:'Изменить источник',previewWords:'Посмотреть слова',hideWords:'Скрыть слова'
 },
 en:{
  trainings:'Training builder',trainingEditor:'Training editor',newTraining:'Create training plan',editTraining:'Edit training plan',
  duplicate:'Duplicate',duplicateShort:'Copy',copySuffix:' (copy)',personalDictionary:'My dictionary',
  selected:'Selected',selectAction:'Select',menuLabel:name=>`Actions: ${name}`,moreSources:(first,n)=>`${first} + ${n} more`,listHint:'What shall we practise?',listCount:n=>`${n} ${n===1?'training plan':'training plans'}`,kindWords:'Words',kindBook:'Textbook',kindKanji:'Kanji / JLPT',kindPersonal:'My words',kindMixed:'Mixed',createFab:'Create',planDetails:'Details',newShort:'New training plan',perWord:n=>`${n} sec per word`,
  tasksLabel:'Tasks',timeLabel:'Timer',poolLabel:'Pool',
  wordsShort:'words',cyclesShort:'cycles',randomShort:'random',
  name:'Name',nameHint:'For example, Tobira review',materials:'Study material',addSource:'Add source',
  source:'Source',sourceHint:'Filters within a source intersect. Sources are combined without duplicate words.',
  bookFilter:'Textbook or dictionary',anyBook:'All sources',chapters:'Chapters',from:'From chapter',to:'To chapter',anyChapter:'All chapters',
  jlpt:'Kanji JLPT level',anyLevel:'Any level',kanjiFilter:'Words containing kanji',anyKanji:'Any kanji',
  wordFilter:'Individual word',anyWord:'All matching words',searchWords:'Find a word, reading or translation',
  noWords:'No words match these filters. Change the conditions.',add:'Add',apply:'Apply',remove:'Remove',
  wordsLimit:'Tasks per session',timeLimit:'Time per word',unlimited:'No timer',limited:'Set limit',
  count:'Task count',seconds:'Seconds',preview:'Training content',unique:'Unique words in the pool',
  tasks:n=>`${n} ${n===1?'task':'tasks'}`,poolSize:'Match pool size',setCount:'Set count',allWords:'Entire pool',
  secondsValue:n=>`${n} s`,available:n=>`Available: ${t('wordCount')(n)}`,session:n=>`Tasks: ${n}`,
  needSource:'Add at least one non-empty source.',invalidTraining:'Enter a name and positive whole-number limits.',
  saveTraining:'Save training plan',selectTraining:'Select training plan',
  deleteTraining:'Delete training plan?',deleteHint:'The plan settings will be deleted. Study materials will remain.',
  emptyTrainings:'Create your first training plan',emptyTrainingHint:'Choose study material and a comfortable pace.',
  draftOnly:'Draft for review. Changes last until this page is reloaded.',savedTraining:'Training plan saved',
  sourceCount:n=>`Sources: ${n}`,allCatalog:'All words',noActive:'No training plan selected',addFirst:'Create a training plan',
  previewNote:'These are the unique words in the pool, not the task order or repetitions.',
  timerNote:'One timer covers all characters in the word. Unlimited lets you work at your own pace.',
  limitNote:'The task count may exceed the word count: words will repeat.',
  discardTitle:'Save changes?',unsavedTraining:'This training plan has unsaved changes.',
  sourceEmpty:'This source is empty. Change the filters.',noMatch:'No matching words',sourceEditor:'Choose material',
  order:'Task order',cycles:'Shuffled cycles',random:'Random, with replacement',
  orderHint:'Cycles: finish the pool before repeating. Random: choose each word independently.',
  optional:'Additional filters',editSource:'Edit source',previewWords:'View words',hideWords:'Hide words'
 }
};
for(const lang of Object.keys(trainingCopy))Object.assign(copy[lang],trainingCopy[lang]);
const TC=window.TEXTBOOK_CATALOG;
const blankSource=()=>({book:null,from:null,to:null,jlpt:null,kanji:null,word:null});
let trainingPlans=[
 {id:'tobira10',translations:[{language_tag:'ru',name:'Tobira · глава 10'},{language_tag:'en',name:'Tobira · chapter 10'}],sources:[{...blankSource(),book:1,from:10,to:10}],selection_mode:'shuffled_cycles',task_count:20,seconds_per_word:30},
 {id:'tobira-review',translations:[{language_tag:'ru',name:'Tobira · повторение 1–10'},{language_tag:'en',name:'Tobira · review 1–10'}],sources:[{...blankSource(),book:1,from:1,to:10}],selection_mode:'shuffled_cycles',task_count:40,seconds_per_word:null},
 {id:'n5',translations:[{language_tag:'ru',name:'Слова с кандзи N5'},{language_tag:'en',name:'Words with N5 kanji'}],sources:[{...blankSource(),jlpt:5}],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:20}
];
// Ten demo recipes exercise real catalog sources and a long scrollable list.
trainingPlans.push(
 {id:'tobira-all',translations:[{language_tag:'ru',name:'Tobira целиком'},{language_tag:'en',name:'All of Tobira'}],sources:[{...blankSource(),book:1}],selection_mode:'shuffled_cycles',task_count:30,seconds_per_word:null},
 {id:'bkb-all',translations:[{language_tag:'ru',name:'Basic Kanji Book'},{language_tag:'en',name:'Basic Kanji Book'}],sources:[{...blankSource(),book:2}],selection_mode:'shuffled_cycles',task_count:25,seconds_per_word:30},
 {id:'bkb-three',translations:[{language_tag:'ru',name:'Basic Kanji Book · глава 3'},{language_tag:'en',name:'Basic Kanji Book · chapter 3'}],sources:[{...blankSource(),book:2,from:3,to:3}],selection_mode:'shuffled_cycles',task_count:10,seconds_per_word:45},
 {id:'my-words',translations:[{language_tag:'ru',name:'Мои слова без спешки'},{language_tag:'en',name:'My words, at my own pace'}],sources:[{...blankSource(),book:'personal'}],selection_mode:'shuffled_cycles',task_count:15,seconds_per_word:null},
 {id:'n4',translations:[{language_tag:'ru',name:'Следующий шаг · N4'},{language_tag:'en',name:'Next step · N4'}],sources:[{...blankSource(),jlpt:4}],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:20},
 {id:'mixed-review',translations:[{language_tag:'ru',name:'Повторение слов из двух учебников для уверенного чтения'},{language_tag:'en',name:'Review words from two textbooks to build reading confidence'}],sources:[{...blankSource(),book:1},{...blankSource(),book:2}],selection_mode:'random_with_replacement',task_count:40,seconds_per_word:30},
 {id:'sun-words',translations:[{language_tag:'ru',name:'Слова с кандзи 日'},{language_tag:'en',name:'Words containing 日'}],sources:[{...blankSource(),kanji:26085}],selection_mode:'shuffled_cycles',task_count:12,seconds_per_word:20}
);
let activeTrainingId='tobira10',trainingDraft=null,trainingOriginal='',sourceDraft=null,sourceIndex=null,sourceQuery='',trainingMessage='',showTrainingWords=false;
let openMenuId=null;

const planName=p=>p?.translations.find(x=>x.language_tag===locale)?.name||p?.translations.find(x=>x.language_tag==='ru')?.name||p?.translations[0]?.name||t('translationMissing');
const activeTraining=()=>trainingPlans.find(p=>p.id===activeTrainingId);
function currentTrainingName(){return activeTraining()?planName(activeTraining()):t('noActive');}
function currentTrainingSummary(){const p=activeTraining();return p?planSummary(p):t('addFirst');}
function planSummary(p){return `${p.task_count===null?t('allWords'):t('tasks')(p.task_count)} · ${p.seconds_per_word===null?t('unlimited'):t('secondsValue')(p.seconds_per_word)}`;}

function sourceWords(s){
 const kanjiSet=new Set(TC.kanji.filter(k=>(!s.kanji||k.id===s.kanji)&&(!s.jlpt||k.classification?.jlpt_level===s.jlpt)).map(k=>k.symbol));
 const isPersonal = s.book === 'personal';
 const isBook = typeof s.book === 'number';
 const placements = isBook ? TC.placements.filter(p=>(p.source_id===s.book)&&(!s.from||p.chapter_number>=s.from)&&(!s.to||p.chapter_number<=s.to)) : [];
 const placed = new Set(placements.map(p=>`${p.source_id}:${p.kanji_id}`));
 const ids = isBook ? new Set(TC.links.filter(l=>(l.source_id===s.book)&&(!(s.from||s.to)||placed.has(`${l.source_id}:${l.kanji_id}`))).map(l=>l.word_id)) : null;

 const allWords = [...new Map([...TC.words,...words.filter(w=>w.personal)].map(w=>[w.id,w])).values()];
 return allWords.filter(w => {
  if (s.word && w.id !== s.word) return false;
  if (isPersonal && !w.personal) return false;
  if (isBook && (!ids || !ids.has(w.id))) return false;
  if ((s.kanji || s.jlpt) && ![...w.written].some(c => kanjiSet.has(c))) return false;
  return true;
 });
}
function planWords(p){return [...new Map(p.sources.flatMap(sourceWords).map(w=>[w.id,w])).values()];}

function sourceLabel(s){
 const parts=[];
 if(s.book === 'personal') parts.push(t('personalDictionary'));
 else if(s.book) parts.push(TC.sources.find(b=>b.id===s.book)?.original_title);
 if(s.from&&s.from===s.to) parts.push(`${t('chapter')} ${s.from}`);
 else if(s.from||s.to) parts.push(`${t('chapters')} ${s.from||1}–${s.to||'…'}`);
 if(s.jlpt) parts.push(`JLPT N${s.jlpt}`);
 if(s.kanji) parts.push(TC.kanji.find(k=>k.id===s.kanji)?.symbol);
 if(s.word) parts.push([...TC.words,...words].find(w=>w.id===s.word)?.written);
 return parts.join(' · ')||t('allCatalog');
}

function planCardView(p){
 const selected=p.id===activeTrainingId;
 const menuOpen=openMenuId===p.id;
 const materialFull=p.sources.map(sourceLabel).join(' + ');
 const material=p.sources.length>1?t('moreSources')(sourceLabel(p.sources[0]),p.sources.length-1):materialFull;
 const tasks=p.task_count===null?t('allWords'):t('tasks')(p.task_count);
 const time=p.seconds_per_word===null?t('unlimited'):t('perWord')(p.seconds_per_word);
 const book=p.sources.length===1?p.sources[0].book:null;
 const kind=p.sources.length>1?'mixed':book==='personal'?'personal':typeof book==='number'?'book':p.sources.some(s=>s.kanji||s.jlpt)?'kanji':'words';
 const kindLabel=t({book:'kindBook',kanji:'kindKanji',personal:'kindPersonal',mixed:'kindMixed',words:'kindWords'}[kind]);
 const badge=selected?icon('paw'):typeof book==='number'?`<img src="../../assets/textbook-icons/textbook-${book===1?'sage':'blue'}.png" alt="">`:p.sources.length===1&&p.sources[0].jlpt?`<span>N${p.sources[0].jlpt}</span>`:icon(kind==='mixed'?'duplicate':'word-stack');
 return `<article class="plan-card ${selected?'plan-card-selected':''}" data-id="${p.id}" data-kind="${kind}" data-timed="${p.seconds_per_word!==null}">
  <button type="button" class="plan-card-main" data-tr="select" data-id="${p.id}" aria-label="${escapeHTML(planName(p)+'. '+materialFull+'. '+tasks+'. '+time)}" aria-current="${selected?'true':'false'}">
   <span class="plan-title-row"><span class="plan-art" aria-hidden="true">${badge}</span><span class="plan-title" title="${escapeHTML(planName(p))}">${escapeHTML(planName(p))}</span></span>
   <span class="plan-material" title="${escapeHTML(materialFull)}">${selected?`<strong class="plan-active-label">${t('selected')}</strong><span aria-hidden="true"> · </span>`:''}<strong class="plan-kind">${escapeHTML(kindLabel)}</strong><span aria-hidden="true"> · </span>${escapeHTML(material)}</span>
   <span class="plan-metrics"><span>${icon('word-stack')}${escapeHTML(tasks)}</span><span class="plan-time ${p.seconds_per_word===null?'':'is-timed'}">${icon(p.seconds_per_word===null?'no-timer':'timer')}${escapeHTML(time)}</span></span>
  </button>
  <div class="plan-menu-wrap ${menuOpen?'is-open':''}">
   <button type="button" class="plan-menu-btn" data-tr="menu-toggle" data-id="${p.id}" aria-label="${escapeHTML(t('menuLabel')(planName(p)))}" aria-haspopup="menu" aria-expanded="${menuOpen}" aria-controls="plan-menu-${p.id}"><svg class="burger-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h14M5 12h14M5 17h14"/></svg></button>
   <div id="plan-menu-${p.id}" class="plan-dropdown-menu" role="menu" aria-label="${escapeHTML(t('menuLabel')(planName(p)))}">
    <button type="button" role="menuitem" class="dropdown-item" data-tr="details" data-id="${p.id}">${icon('word-stack')}<span>${t('planDetails')}</span></button>
    <button type="button" role="menuitem" class="dropdown-item" data-tr="select" data-id="${p.id}">${icon('check')}<span>${t('selectAction')}</span></button>
    <button type="button" role="menuitem" class="dropdown-item" data-tr="edit" data-id="${p.id}">${icon('edit')}<span>${t('editTraining')}</span></button>
    <button type="button" role="menuitem" class="dropdown-item" data-tr="duplicate" data-id="${p.id}">${icon('duplicate')}<span>${t('duplicate')}</span></button>
    <button type="button" role="menuitem" class="dropdown-item danger" data-tr="delete" data-id="${p.id}">${icon('trash')}<span>${t('delete')}</span></button>
   </div>
  </div>
 </article>`;
}
function trainingsView(){
 return `<div class="training-list-screen">${header(t('trainings'))}<div class="training-list-scroll" tabindex="0" role="region" aria-label="${t('trainings')}"><div class="training-content training-list-content">
  <div class="training-welcome"><div><p>${t('listHint')}</p><span>${t('listCount')(trainingPlans.length)}</span></div><img src="../../assets/mascot/cat-crafting-cards.png" alt="" aria-hidden="true"></div>
  ${trainingMessage?`<p role="status" class="training-status-msg">${escapeHTML(trainingMessage)}</p>`:''}
  <div class="plans-list">${trainingPlans.length?trainingPlans.map(planCardView).join(''):`<div class="training-empty"><img src="../textbooks/assets/cat-shelving-books.png" alt="">${emptyView(t('emptyTrainings'),t('emptyTrainingHint'),false)}</div>`}</div>
 </div></div><div class="training-create-bar"><button type="button" class="training-create" data-tr="new" aria-label="${t('newTraining')}">${icon('plus')}<span>${t('createFab')}</span></button></div>
 </div>`;
}
function setTrainingMenu(id,focusItem=false){
 const previous=openMenuId;
 openMenuId=id;
 document.querySelectorAll('.plan-menu-wrap').forEach(w=>{
  const button=w.querySelector('.plan-menu-btn');
  const open=button.dataset.id===id;
  w.classList.toggle('is-open',open);
  button.setAttribute('aria-expanded',String(open));
  w.closest('.plan-card').classList.toggle('plan-menu-open',open);
  if(open){
   const menu=w.querySelector('.plan-dropdown-menu');
   menu.style.removeProperty('max-height');
   const bounds=$('.training-list-scroll').getBoundingClientRect(),anchor=button.getBoundingClientRect();
   const fabTop=$('.training-create').getBoundingClientRect().top;
   const below=Math.min(bounds.bottom,fabTop-8)-anchor.bottom-8,above=anchor.top-bounds.top-8;
   const placement=below>=menu.offsetHeight||below>=above?'below':'above';
   w.dataset.placement=placement;
   menu.style.maxHeight=Math.max(48,Math.min(340,placement==='above'?above:below))+'px';
   if(focusItem)menu.querySelector('button')?.focus();
  }
 });
 return previous;
}
Object.assign(glyphs,{
 'word-stack':'<rect x="6" y="4" width="13" height="15" rx="3"/><path d="M3 8v10a4 4 0 0 0 4 4M10 9h5M10 13h4"/>',
 timer:'<circle cx="12" cy="14" r="8"/><path d="M12 10v4l3 2M9 2h6M12 2v4M19 6l2 2"/>',
 'no-timer':'<path d="M6 9c-4 0-5 8 0 8 4 0 8-8 12-8 5 0 4 8 0 8-4 0-8-8-12-8Z"/>',
 duplicate:'<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M5 16H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v1"/>'
});

function trainingDirty(){return trainingDraft&&JSON.stringify(trainingDraft)!==trainingOriginal;}
function trainingLeave(next){showModal(t('discardTitle'),`<p>${t('unsavedTraining')}</p>`,[{label:t('keepEditing'),fn:closeModal},{label:t('discard'),fn:()=>{trainingDraft=null;go(next)}},{label:t('save'),fn:()=>saveTraining(next)}]);}
function initTraining(id){const p=trainingPlans.find(p=>p.id===id);trainingDraft=p?structuredClone(p):{id:null,translations:[{language_tag:locale,name:''}],sources:[],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:null};trainingOriginal=JSON.stringify(trainingDraft);showTrainingWords=false;openMenuId=null;go('training-editor');}

function trainingEditorView(){if(!trainingDraft){trainingDraft={id:null,translations:[{language_tag:locale,name:''}],sources:[],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:null};trainingOriginal=JSON.stringify(trainingDraft);}const p=trainingDraft,ws=planWords(p);const localName=p.translations.find(x=>x.language_tag===locale)?.name||'';return header(t(p.id?'editTraining':'newTraining'),'trainings')+`<form id="training-form" class="training-content" novalidate><label class="field">${t('name')}<input id="training-name" maxlength="120" value="${escapeHTML(localName)}" placeholder="${t('nameHint')}"></label><section><h3>${t('materials')}</h3><p class="training-note">${t('sourceHint')}</p><div class="source-list">${p.sources.map((s,i)=>`<div class="source-row"><button type="button" data-tr="source-edit" data-index="${i}"><strong>${s.book===1||s.book===2?`<img class="training-book-icon" src="../../assets/textbook-icons/textbook-${s.book===1?'sage':'blue'}.png" alt="">`:''}${escapeHTML(sourceLabel(s))}</strong><small>${t('available')(sourceWords(s).length)}</small></button><button type="button" data-tr="source-remove" data-index="${i}" aria-label="${t('remove')} ${escapeHTML(sourceLabel(s))}">×</button></div>`).join('')}</div><button type="button" class="tonal-button full-width" data-tr="source-add">${icon('plus')}${t('addSource')}</button></section>${limitControl('task_count','wordsLimit','count',p.task_count)}<p class="training-note">${t('limitNote')}</p>${limitControl('seconds_per_word','timeLimit','seconds',p.seconds_per_word)}<p class="training-note">${t('timerNote')}</p>${p.task_count!==null?`<details><summary>${t('order')}</summary><label class="field">${t('order')}<select id="training-order"><option value="shuffled_cycles" ${p.selection_mode==='shuffled_cycles'?'selected':''}>${t('cycles')}</option><option value="random_with_replacement" ${p.selection_mode==='random_with_replacement'?'selected':''}>${t('random')}</option></select></label><p class="training-note">${t('orderHint')}</p></details>`:''}<section class="training-preview"><h3>${t('preview')}</h3><p>${t('available')(ws.length)}</p><p>${t('session')(p.task_count??ws.length)}</p><small>${t('unique')}</small><p class="training-note">${t('previewNote')}</p><button type="button" class="text-button" data-tr="preview">${t(showTrainingWords?'hideWords':'previewWords')}</button>${showTrainingWords?`<div class="training-word-preview">${ws.map(w=>`<p><strong lang="ja">${escapeHTML(w.written)}</strong> <span lang="ja">${escapeHTML(w.reading)}</span><br>${escapeHTML(translation(w))}</p>`).join('')||t('noMatch')}</div>`:''}</section><p id="training-error" class="form-error" role="alert"></p><button class="primary-button full-width" type="submit">${t('saveTraining')}</button></form>`;}
function limitControl(key,title,label,value){return `<section><h3>${t(title)}</h3><div class="limit-choices" role="group" aria-label="${t(title)}">${[false,true].map(limited=>`<button type="button" aria-pressed="${limited===(value!==null)}" data-tr="limit" data-key="${key}" data-limited="${limited}">${t(key==='task_count'?(limited?'setCount':'poolSize'):(limited?'limited':'unlimited'))}</button>`).join('')}</div>${value!==null?`<label class="field">${t(label)}<input type="number" min="1" step="1" inputmode="numeric" data-limit="${key}" value="${value}"></label>`:''}</section>`;}
function sourceOptions(items,current,empty){return `<option value="">${escapeHTML(empty)}</option>`+items.map(([id,label])=>`<option value="${id}" ${String(current)===String(id)?'selected':''}>${escapeHTML(label)}</option>`).join('');}
function sourcePicker(){
 const s=sourceDraft;
 const isBook = typeof s.book === 'number';
 const matches=sourceWords({...s,word:null}).filter(w=>[w.written,w.reading,...w.translations.map(x=>x.text)].join(' ').toLowerCase().includes(sourceQuery.toLowerCase()));
 const bookItems=[...TC.sources.map(b=>[b.id,b.original_title]),['personal',t('personalDictionary')]];
 showModal(t('sourceEditor'),`<div class="source-picker"><label class="field">${t('bookFilter')}<select data-source="book">${sourceOptions(bookItems,s.book,t('anyBook'))}</select></label><div class="chapter-range"><label class="field">${t('from')}<input type="number" min="1" step="1" data-source="from" value="${s.from??''}"></label><label class="field">${t('to')}<input type="number" min="1" step="1" data-source="to" value="${s.to??''}"></label></div><details ${(s.jlpt||s.kanji||s.word||sourceQuery)?'open':''}><summary>${t('optional')}</summary><label class="field">${t('jlpt')}<select data-source="jlpt">${sourceOptions([5,4,3,2,1].map(n=>[n,'N'+n]),s.jlpt,t('anyLevel'))}</select></label><label class="field">${t('kanjiFilter')}<select data-source="kanji">${sourceOptions(TC.kanji.map(k=>[k.id,k.symbol]),s.kanji,t('anyKanji'))}</select></label><label class="field">${t('searchWords')}<input id="source-query" value="${escapeHTML(sourceQuery)}" type="search"></label><label class="field">${t('wordFilter')}<select data-source="word">${sourceOptions(matches.map(w=>[w.id,`${w.written} · ${w.reading} · ${translation(w)}`]),s.word,t('anyWord'))}</select></label></details><p role="status">${t('available')(sourceWords(s).length)}</p><p id="source-error" role="alert" class="form-error"></p></div>`,[{label:t('cancel'),fn:closeModal},{label:t(sourceIndex===null?'add':'apply'),fn:()=>{if(((s.from||s.to)&&typeof s.book!=='number')||['from','to'].some(k=>s[k]!==null&&(!Number.isSafeInteger(s[k])||s[k]<1))||(s.from&&s.to&&s.from>s.to)||!sourceWords(s).length){$('#source-error').textContent=t('sourceEmpty');return;}if(sourceIndex===null)trainingDraft.sources.push(structuredClone(s));else trainingDraft.sources[sourceIndex]=structuredClone(s);closeModal();render();}}]);
}
function saveTraining(next='trainings'){const p=trainingDraft;const error=!p.translations.some(x=>x.name.trim())||['task_count','seconds_per_word'].some(k=>p[k]!==null&&(!Number.isSafeInteger(p[k])||p[k]<1||p[k]>2147483647))?t('invalidTraining'):!p.sources.length||p.sources.some(s=>!sourceWords(s).length)?t('needSource'):null;if(error){closeModal();$('#training-error').textContent=error;$('#training-error').scrollIntoView({block:'nearest'});return;}p.translations=p.translations.filter(x=>x.name.trim()).map(x=>({...x,name:x.name.trim()}));if(!p.id)p.id=crypto.randomUUID();const saved=structuredClone(p);trainingPlans=trainingPlans.filter(x=>x.id!==p.id);trainingPlans.push(saved);if(!activeTrainingId)activeTrainingId=p.id;trainingDraft=null;trainingMessage=t('savedTraining');openMenuId=null;go(next);}

document.addEventListener('click',e=>{
 const b=e.target.closest('[data-tr]');
 if(!b) {
  if(openMenuId !== null && !e.target.closest('.plan-menu-wrap')) {
   setTrainingMenu(null);
  }
  return;
 }
 e.preventDefault();
 const a=b.dataset.tr;
 if(a==='new')initTraining();
 if(a==='menu-toggle'){
  setTrainingMenu(openMenuId===b.dataset.id?null:b.dataset.id,true);
 }
 if(a==='details'){
  const p=trainingPlans.find(x=>x.id===b.dataset.id);
  setTrainingMenu(null);
  document.querySelector(`.plan-menu-btn[data-id="${b.dataset.id}"]`)?.focus();
  showModal(planName(p),`<div class="training-plan-details"><h4>${t('materials')}</h4><ul>${p.sources.map(s=>`<li>${escapeHTML(sourceLabel(s))}</li>`).join('')}</ul><p>${escapeHTML(planSummary(p))}</p><p>${t('available')(planWords(p).length)}</p><p>${t('order')}: ${t(p.task_count===null||p.selection_mode==='shuffled_cycles'?'cycles':'random')}</p></div>`,[{label:t('close'),fn:closeModal},{label:t('editTraining'),fn:()=>{closeModal();initTraining(p.id)}}]);
 }
 if(a==='edit'){
  openMenuId = null;
  initTraining(b.dataset.id);
 }
 if(a==='duplicate'){
  openMenuId = null;
  const original=trainingPlans.find(x=>x.id===b.dataset.id);
  if(original){
   const copyPlan=structuredClone(original);
   copyPlan.id=null;
   copyPlan.translations=copyPlan.translations.map(tr=>({...tr,name:(tr.name+t('copySuffix')).slice(0,120)}));
   trainingDraft=copyPlan;
   trainingOriginal=JSON.stringify(trainingDraft);
   showTrainingWords=false;
   go('training-editor');
  }
 }
 if(a==='select'){
  openMenuId = null;
  activeTrainingId=b.dataset.id;
  trainingMessage='';
  go('home');
 }
 if(a==='delete'){
  setTrainingMenu(null);
  document.querySelector(`.plan-menu-btn[data-id="${b.dataset.id}"]`)?.focus();
  const id=b.dataset.id,p=trainingPlans.find(x=>x.id===id);
  showModal(t('deleteTraining'),`<p>${escapeHTML(planName(p))}</p><p>${t('deleteHint')}</p>`,[{label:t('cancel'),fn:closeModal},{label:t('delete'),danger:true,fn:()=>{trainingPlans=trainingPlans.filter(x=>x.id!==id);if(activeTrainingId===id)activeTrainingId=null;trainingMessage='';closeModal();render();}}]);
 }
 if(a==='source-add'||a==='source-edit'){sourceIndex=a==='source-edit'?Number(b.dataset.index):null;sourceDraft=sourceIndex===null?blankSource():structuredClone(trainingDraft.sources[sourceIndex]);sourceQuery='';sourcePicker();}
 if(a==='source-remove'){trainingDraft.sources.splice(Number(b.dataset.index),1);render();}
 if(a==='limit'){trainingDraft[b.dataset.key]=b.dataset.limited==='true'?(b.dataset.key==='task_count'?20:30):null;render();}
 if(a==='preview'){showTrainingWords=!showTrainingWords;const scroll=$('#app').scrollTop;render();$('#app').scrollTop=scroll;}
});

document.addEventListener('keydown',e=>{
 const wrap=e.target.closest('.plan-menu-wrap');
 if(!wrap)return;
 const toggle=wrap.querySelector('.plan-menu-btn');
 if(e.key==='Escape'){
  if(openMenuId){e.preventDefault();setTrainingMenu(null);toggle.focus();}
  return;
 }
 if(e.target===toggle&&['ArrowDown','ArrowUp'].includes(e.key)){
  e.preventDefault();setTrainingMenu(toggle.dataset.id,true);
  if(e.key==='ArrowUp')wrap.querySelector('.dropdown-item:last-child').focus();
  return;
 }
 if(!openMenuId)return;
 const items=[...wrap.querySelectorAll('[role=menuitem]')];
 const index=items.indexOf(document.activeElement);
 if(['ArrowDown','ArrowUp','Home','End'].includes(e.key)){
  e.preventDefault();
  const next=e.key==='Home'?0:e.key==='End'?items.length-1:(index+(e.key==='ArrowDown'?1:-1)+items.length)%items.length;
  items[next].focus();
 }
 if(e.key==='Tab'){setTrainingMenu(null);toggle.focus();}
});

document.addEventListener('input',e=>{const el=e.target;if(el.id==='training-name'){let tr=trainingDraft.translations.find(x=>x.language_tag===locale);if(!tr){tr={language_tag:locale,name:''};trainingDraft.translations.push(tr);}tr.name=el.value;}if(el.dataset.limit){trainingDraft[el.dataset.limit]=el.value===''?0:Number(el.value);const p=trainingDraft;$('.training-preview p:nth-of-type(2)').textContent=t('session')(p.task_count??planWords(p).length);}});
document.addEventListener('change',e=>{
 const el=e.target;
 if(el.id==='training-order')trainingDraft.selection_mode=el.value;
 if(el.dataset.source){
  let val=el.value===''?null:(el.dataset.source==='book'&&el.value==='personal'?'personal':Number(el.value));
  sourceDraft[el.dataset.source]=val;
  if(el.dataset.source==='book'&&val!=='personal'&&typeof val!=='number'){sourceDraft.from=null;sourceDraft.to=null;}
  if(el.dataset.source==='book'&&val==='personal'){sourceDraft.from=null;sourceDraft.to=null;}
  if(el.dataset.source!=='word')sourceDraft.word=null;
  sourcePicker();
 }
 if(el.id==='source-query'){sourceQuery=el.value;sourceDraft.word=null;sourcePicker();$('#source-query').focus();}
});
document.addEventListener('submit',e=>{if(e.target.id==='training-form'){e.preventDefault();saveTraining();}});
captions.trainings='Черновик для согласования. Компактные карточки, кот из каталога и меню действий. Сохранение — до перезагрузки страницы; запуск собранных сессий не реализован.';
captions['training-editor']='Конструктор тренировок. Объединение источников, параметры сессии и таймер.';
screen=screens.includes(location.hash.slice(1))?location.hash.slice(1):'home';if(['catalog','book','chapter'].includes(screen))go(screen);else render();
