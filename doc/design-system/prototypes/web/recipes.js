'use strict';
// Recipe Builder prototype
const recipeCopy={
 ru:{
  recipes:'Рецепты',recipeEditor:'Редактор рецепта',newRecipe:'Создать рецепт',editRecipe:'Изменить рецепт',recipeDetail:'Рецепт',viewRecipe:'Открыть рецепт',
  reorderPlans:'Изменить порядок',finishReorder:'Готово',reorderTitle:'Порядок рецептов',screenMenu:'Действия со списком',dragPlan:'Перетащить рецепт',moveUp:'Переместить выше',moveDown:'Переместить ниже',movedPlan:(name,n)=>`${name}: позиция ${n}`,
  personalDictionary:'Мой словарь',
  selected:'Выбран',allWordsCount:n=>`Все ${t('wordCount')(n)}`,selectAction:'Выбрать',menuLabel:name=>`Действия: ${name}`,moreSources:(first,n)=>`${first} + ещё ${n}`,listHint:'Создайте или откройте',listCount:n=>`${n} ${new Intl.PluralRules('ru').select(n)==='one'?'рецепт':new Intl.PluralRules('ru').select(n)==='few'?'рецепта':'рецептов'}`,kindWords:'Слова',kindBook:'Учебник',kindKanji:'Кандзи / JLPT',kindPersonal:'Мои слова',kindMixed:'Смешанная',createFab:'Создать',editFab:'Изменить',planDetails:'Подробнее',newShort:'Новый рецепт',perWord:n=>`${n} с на слово`,
  tasksLabel:'Заданий',timeLabel:'Таймер',poolLabel:'Слов',
  tagJlpt:n=>`Кандзи JLPT N${n}`,tagKanji:symbol=>`Кандзи ${symbol}`,tagEntry:form=>`Запись: ${form}`,tagUnion:'или',tagSourceNumber:n=>`Источник ${n}`,tagFrom:n=>`С главы ${n}`,tagTo:n=>`По главу ${n}`,
  moreRecipeTags:n=>`Ещё ${n}`,collapseRecipeSources:'Свернуть состав',
  findRecipe:'Найти рецепт',hideRecipeSearch:'Скрыть поиск',recipeSearchHint:'Название или материал',clearRecipeSearch:'Очистить',recipeNotFound:'Рецепты не найдены',recipeSearchHelp:'Попробуйте другое название, учебник или уровень JLPT.',recipeResults:(n,total)=>`Найдено: ${n} из ${total}`,
  wordsShort:'сл.',cyclesShort:'по кругу',randomShort:'случайно',
  name:'Название',nameHint:'Например, Tobira: повторение',clearName:'Очистить название',materials:'Учебный материал',addSource:'Добавить материал',
  source:'Источник',sourceHint:'Фильтры внутри источника пересекаются. Разные источники объединяются без повторов.',
  bookFilter:'Учебник или словарь',anyBook:'Все источники',chapters:'Главы',from:'С главы',to:'По главу',anyChapter:'Все главы',
  jlpt:'Уровень кандзи JLPT',anyLevel:'Любой уровень',kanjiFilter:'Слова с кандзи',anyKanji:'Любой кандзи',
  wordFilter:'Конкретное слово',anyWord:'Все подходящие слова',searchWords:'Найти слово, чтение или перевод',
  noWords:'По этим фильтрам слов нет. Измените условия.',add:'Добавить',apply:'Применить',remove:'Убрать',
  trainingSettings:'Параметры тренировки',
  wordsLimit:'Заданий за рецепт',timeLimit:'Время на одно слово',unlimited:'Без таймера',limited:'Ограничить',
  count:'Количество заданий',seconds:'Секунд на слово',preview:'Итого',unique:'Уникальные слова в наборе',
  tasks:n=>`${n} ${new Intl.PluralRules('ru').select(n)==='one'?'задание':new Intl.PluralRules('ru').select(n)==='few'?'задания':'заданий'}`,
  poolSize:'По числу слов',setCount:'Задать число',allWords:'Весь набор',secondsValue:n=>`${n} с`,
  available:n=>`Доступно: ${t('wordCount')(n)}`,tasksCount:n=>`Заданий: ${n}`,
  moreMaterials:n=>`Ещё материалов: ${n}`,lessMaterials:'Свернуть материалы',missingName:'Введите название',invalidLimits:'Укажите целое число больше нуля',needSource:'Добавьте материал',invalidRecipe:'Введите название и целые положительные ограничения.',
  saveRecipeShort:'Сохранить',materialRemoved:'Материал удалён',wordPool:'Слова в рецепте',saveRecipe:'Сохранить рецепт',selectRecipe:'Выбрать рецепт',
  deleteRecipe:'Удалить рецепт?',deleteHint:'Настройки рецепта будут удалены. Учебные материалы останутся.',
  emptyRecipes:'Создайте свой первый рецепт',emptyRecipeHint:'Выберите материал и удобный темп.',
  draftOnly:'Макет для согласования. Сохранение действует до перезагрузки страницы.',savedRecipe:'Рецепт сохранён',
  sourceCount:n=>`Источников: ${n}`,allCatalog:'Все слова',noActive:'Рецепт не выбран',addFirst:'Создать рецепт',
  previewNote:'Здесь показаны уникальные слова набора, а не порядок и повторы заданий.',
  timerNote:'Таймер общий для всех знаков слова. Без ограничения — можно отвечать в своём темпе.',
  limitNote:'Число заданий может превышать число слов: слова будут повторяться.',
  discardTitle:'Сбросить изменения?',unsavedRecipe:'Все внесённые правки будут потеряны.',
  sourceEmpty:'Источник пуст. Измените фильтры.',noMatch:'Нет подходящих слов',sourceEditor:'Выбор материала',
  order:'Как показывать слова',cycles:'Вперемешку по кругу',random:'Случайно, с повторами',
  orderHint:'По кругу: весь набор перед новым повтором. Случайно: независимый выбор каждого слова.',
  optional:'Дополнительные фильтры',editSource:'Изменить материал',viewWordCount:n=>`Посмотреть ${t('wordCount')(n)}`,previewWords:'Посмотреть слова',hideWords:'Скрыть слова',
  materialWordsNoneSelected:'Слова ещё не выбраны',materialDictionaryEmpty:'В «Моём словаре» пока нет слов',materialDictionaryNoMatch:'Нет подходящих слов. Измените теги или кандзи'
 },
 en:{
  recipes:'Recipes',recipeEditor:'Recipe editor',newRecipe:'Create recipe',editRecipe:'Edit recipe',recipeDetail:'Recipe',viewRecipe:'Open recipe',
  reorderPlans:'Change order',finishReorder:'Done',reorderTitle:'Recipe order',screenMenu:'List actions',dragPlan:'Drag recipe',moveUp:'Move up',moveDown:'Move down',movedPlan:(name,n)=>`${name}: position ${n}`,
  personalDictionary:'My dictionary',
  selected:'Selected',allWordsCount:n=>`All ${t('wordCount')(n)}`,selectAction:'Select',menuLabel:name=>`Actions: ${name}`,moreSources:(first,n)=>`${first} + ${n} more`,listHint:'Create or open',listCount:n=>`${n} ${n===1?'recipe':'recipes'}`,kindWords:'Words',kindBook:'Textbook',kindKanji:'Kanji / JLPT',kindPersonal:'My words',kindMixed:'Mixed',createFab:'Create',editFab:'Edit',planDetails:'Details',newShort:'New recipe',perWord:n=>`${n} sec per word`,
  tasksLabel:'Tasks',timeLabel:'Timer',poolLabel:'Pool',
  tagJlpt:n=>`Kanji JLPT N${n}`,tagKanji:symbol=>`Kanji ${symbol}`,tagEntry:form=>`Entry: ${form}`,tagUnion:'or',tagSourceNumber:n=>`Source ${n}`,tagFrom:n=>`From chapter ${n}`,tagTo:n=>`Through chapter ${n}`,
  moreRecipeTags:n=>`${n} more`,collapseRecipeSources:'Collapse content',
  findRecipe:'Find a recipe',hideRecipeSearch:'Hide search',recipeSearchHint:'Name or study material',clearRecipeSearch:'Clear',recipeNotFound:'No recipes found',recipeSearchHelp:'Try another name, textbook or JLPT level.',recipeResults:(n,total)=>`Found: ${n} of ${total}`,
  wordsShort:'words',cyclesShort:'cycles',randomShort:'random',
  name:'Name',nameHint:'For example, Tobira review',clearName:'Clear name',materials:'Study material',addSource:'Add material',
  source:'Source',sourceHint:'Filters within a source intersect. Sources are combined without duplicate words.',
  bookFilter:'Textbook or dictionary',anyBook:'All sources',chapters:'Chapters',from:'From chapter',to:'To chapter',anyChapter:'All chapters',
  jlpt:'Kanji JLPT level',anyLevel:'Any level',kanjiFilter:'Words containing kanji',anyKanji:'Any kanji',
  wordFilter:'Individual word',anyWord:'All matching words',searchWords:'Find a word, reading or translation',
  noWords:'No words match these filters. Change the conditions.',add:'Add',apply:'Apply',remove:'Remove',
  trainingSettings:'Training settings',
  wordsLimit:'Tasks per recipe',timeLimit:'Time per word',unlimited:'No timer',limited:'Set limit',
  count:'Task count',seconds:'Seconds per word',preview:'Summary',unique:'Unique words in the pool',
  tasks:n=>`${n} ${n===1?'task':'tasks'}`,poolSize:'Match word count',setCount:'Set count',allWords:'Entire pool',
  secondsValue:n=>`${n} s`,available:n=>`Available: ${t('wordCount')(n)}`,tasksCount:n=>`Tasks: ${n}`,
  moreMaterials:n=>`${n} more materials`,lessMaterials:'Collapse materials',missingName:'Enter a name',invalidLimits:'Enter a whole number greater than zero',needSource:'Add material',invalidRecipe:'Enter a name and positive whole-number limits.',
  saveRecipeShort:'Save',materialRemoved:'Material removed',wordPool:'Recipe words',saveRecipe:'Save recipe',selectRecipe:'Select recipe',
  deleteRecipe:'Delete recipe?',deleteHint:'The plan settings will be deleted. Study materials will remain.',
  emptyRecipes:'Create your first recipe',emptyRecipeHint:'Choose study material and a comfortable pace.',
  draftOnly:'Draft for review. Changes last until this page is reloaded.',savedRecipe:'Recipe saved',
  sourceCount:n=>`Sources: ${n}`,allCatalog:'All words',noActive:'No recipe selected',addFirst:'Create a recipe',
  previewNote:'These are the unique words in the pool, not the task order or repetitions.',
  timerNote:'One timer covers all characters in the word. Unlimited lets you work at your own pace.',
  limitNote:'The task count may exceed the word count: words will repeat.',
  discardTitle:'Discard changes?',unsavedRecipe:'All edits will be lost.',
  sourceEmpty:'This source is empty. Change the filters.',noMatch:'No matching words',sourceEditor:'Choose material',
  order:'How to show words',cycles:'Shuffled cycles',random:'Random, with replacement',
  orderHint:'Cycles: finish the pool before repeating. Random: choose each word independently.',
  optional:'Additional filters',editSource:'Edit material',viewWordCount:n=>`View ${t('wordCount')(n)}`,previewWords:'View words',hideWords:'Hide words',
  materialWordsNoneSelected:'No words selected yet',materialDictionaryEmpty:'My dictionary has no words yet',materialDictionaryNoMatch:'No matching words. Change tags or kanji'
 }
};
for(const lang of Object.keys(recipeCopy))Object.assign(copy[lang],recipeCopy[lang]);
const TC=window.TEXTBOOK_CATALOG;
const blankSource=()=>({book:null,from:null,to:null,jlpt:null,kanji:null,word:null});
let recipes=[
 {id:'tobira10',translations:[{language_tag:'ru',name:'Tobira · глава 10'},{language_tag:'en',name:'Tobira · chapter 10'}],sources:[{...blankSource(),book:1,from:10,to:10}],selection_mode:'shuffled_cycles',task_count:20,seconds_per_word:30},
 {id:'tobira-review',translations:[{language_tag:'ru',name:'Tobira · повторение 1–10'},{language_tag:'en',name:'Tobira · review 1–10'}],sources:[{...blankSource(),book:1,from:1,to:10}],selection_mode:'shuffled_cycles',task_count:40,seconds_per_word:null},
 {id:'n5',translations:[{language_tag:'ru',name:'Слова с кандзи N5'},{language_tag:'en',name:'Words with N5 kanji'}],sources:[{...blankSource(),jlpt:5}],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:20}
];
// Demo recipes exercise real catalog sources and a long scrollable list.
recipes.push(
 {id:'tobira-all',translations:[{language_tag:'ru',name:'Tobira целиком'},{language_tag:'en',name:'All of Tobira'}],sources:[{...blankSource(),book:1}],selection_mode:'shuffled_cycles',task_count:30,seconds_per_word:null},
 {id:'bkb-all',translations:[{language_tag:'ru',name:'Basic Kanji Book'},{language_tag:'en',name:'Basic Kanji Book'}],sources:[{...blankSource(),book:2}],selection_mode:'shuffled_cycles',task_count:25,seconds_per_word:30},
 {id:'bkb-three',translations:[{language_tag:'ru',name:'Basic Kanji Book · глава 3'},{language_tag:'en',name:'Basic Kanji Book · chapter 3'}],sources:[{...blankSource(),book:2,from:3,to:3}],selection_mode:'shuffled_cycles',task_count:10,seconds_per_word:45},
 {id:'my-words',translations:[{language_tag:'ru',name:'Мои слова без спешки'},{language_tag:'en',name:'My words, at my own pace'}],sources:[{...blankSource(),book:'personal'}],selection_mode:'shuffled_cycles',task_count:15,seconds_per_word:null},
 {id:'n4',translations:[{language_tag:'ru',name:'Следующий шаг · N4'},{language_tag:'en',name:'Next step · N4'}],sources:[{...blankSource(),jlpt:4}],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:20},
 {id:'mixed-review',translations:[{language_tag:'ru',name:'Повторение для чтения'},{language_tag:'en',name:'Reading review'}],sources:[{...blankSource(),book:1},{...blankSource(),book:2}],selection_mode:'random_with_replacement',task_count:40,seconds_per_word:30},
 {id:'sun-words',translations:[{language_tag:'ru',name:'Слова с кандзи 日'},{language_tag:'en',name:'Words containing 日'}],sources:[{...blankSource(),kanji:26085}],selection_mode:'shuffled_cycles',task_count:12,seconds_per_word:20}
);
recipes.splice(1,0,{
 id:'many-sources',translations:[{language_tag:'ru',name:'Большое повторение'},{language_tag:'en',name:'Extended review'}],
 sources:[
  {...blankSource(),book:1,from:3,to:3}, {...blankSource(),book:1,from:4,to:5},
  {...blankSource(),book:1,from:7,to:8}, {...blankSource(),book:1,from:10,to:10},
  {...blankSource(),book:2,from:1,to:3}, {...blankSource(),book:2,from:6,to:8},
  {...blankSource(),book:2,from:12,to:15}, {...blankSource(),book:2,from:20,to:25},
  {...blankSource(),book:2,from:30,to:35}, {...blankSource(),book:'personal'}
 ],selection_mode:'shuffled_cycles',task_count:60,seconds_per_word:30
});
let expandedEditorMaterials=false;
let removedRecipeSource=null;
let selectedRecipeId='tobira10',recipeDraft=null,recipeOriginal='',sourceDraft=null,sourceIndex=null,sourceQuery='',recipeMessage='',showRecipeWords=false;
let recipeSearchOpen=false,recipeSearchQuery='';
let recipeWordsOrigin='recipe-editor';let viewingId=null,viewingSourceIndex=null;

const expandedRecipeSources=new Set();

const planName=p=>p?.translations.find(x=>x.language_tag===locale)?.name||p?.translations.find(x=>x.language_tag==='ru')?.name||p?.translations[0]?.name||t('translationMissing');
const selectedRecipe=()=>recipes.find(p=>p.id===selectedRecipeId);
function selectedRecipeName(){return selectedRecipe()?planName(selectedRecipe()):t('noActive');}
function selectedRecipeSummary(){const p=selectedRecipe();return p?planSummary(p):t('addFirst');}
function planSummary(p){return `${p.task_count===null?t('allWords'):t('tasks')(p.task_count)} · ${p.seconds_per_word===null?t('unlimited'):t('secondsValue')(p.seconds_per_word)}`;}

function sourceWords(s){
 if(s.type==='kanji')s={type:'kanji',kanji:s.kanji,kanji_ids:s.kanji_ids,kanji_match:'any'};
 if(s.type==='book'&&typeof s.book!=='number')return [];
 if(s.type==='words'&&!(s.word_ids?.length||s.word))return [];
 const selectedKanji=(s.kanji_ids||[]).map(id=>TC.kanji.find(k=>k.id===id)?.symbol).filter(Boolean);
 const kanjiSet=new Set(TC.kanji.filter(k=>(!s.kanji||k.id===s.kanji)&&(!s.jlpt||k.classification?.jlpt_level===s.jlpt)).map(k=>k.symbol));
 const isPersonal = s.book === 'personal';
 const isBook = typeof s.book === 'number';
 const placements = isBook ? TC.placements.filter(p=>(p.source_id===s.book)&&(!s.chapters?.length||s.chapters.includes(p.chapter_number))&&(!s.from||p.chapter_number>=s.from)&&(!s.to||p.chapter_number<=s.to)) : [];
 const placed = new Set(placements.map(p=>`${p.source_id}:${p.kanji_id}`));
 const ids = isBook ? new Set(TC.links.filter(l=>(l.source_id===s.book)&&(!(s.from||s.to||s.chapters?.length)||placed.has(`${l.source_id}:${l.kanji_id}`))).map(l=>l.word_id)) : null;

 const allWords = [...new Map([...TC.words,...words.filter(w=>w.personal)].map(w=>[w.id,w])).values()];
 return allWords.filter(w => {
  if (s.word && w.id !== s.word) return false;
  if(s.word_ids?.length&&!s.word_ids.includes(w.id))return false;
  if(selectedKanji.length&&!(s.kanji_match==='all'?selectedKanji.every(c=>w.written.includes(c)):selectedKanji.some(c=>w.written.includes(c))))return false;
  if (isPersonal && !w.personal) return false;
  if (isPersonal && s.tag_ids?.length && !s.tag_ids.some(id=>(w.tag_ids||[]).includes(id))) return false;
  if (isBook && (!ids || !ids.has(w.id))) return false;
  if ((s.kanji || s.jlpt) && ![...w.written].some(c => kanjiSet.has(c))) return false;
  return true;
 });
}
function planWords(p){return [...new Map(p.sources.flatMap(sourceWords).map(w=>[w.id,w])).values()];}

function sourceLabel(s){
 const parts=[];
 if(s.book === 'personal') parts.push(t('personalDictionary'));
 else if(s.book) parts.push(recipeSourceName(s.book));
 if(s.chapters?.length)parts.push(`${t('chapters')} ${s.chapters.join(', ')}`);
 if(s.from&&s.from===s.to) parts.push(`${t('chapter')} ${s.from}`);
 else if(s.from||s.to) parts.push(`${t('chapters')} ${s.from||1}–${s.to||'…'}`);
 if(s.jlpt) parts.push(`JLPT N${s.jlpt}`);
 if(s.kanji) parts.push(TC.kanji.find(k=>k.id===s.kanji)?.symbol);
 if(s.word) parts.push([...TC.words,...words].find(w=>w.id===s.word)?.written);
 if(s.kanji_ids?.length)parts.push(s.kanji_ids.map(id=>TC.kanji.find(k=>k.id===id)?.symbol).filter(Boolean).join(s.kanji_match==='all'?' ∧ ':' ∨ '));
 if(s.word_ids?.length)parts.push(t('wordCount')(s.word_ids.length));
 if(s.book==='personal'&&s.tag_ids?.length)parts.push(recipeDictionaryTagPreview(s));
 return parts.join(' · ')||t('allCatalog');
}

// Tags project existing recipe_materials fields; they are not a separate taxonomy.
function recipeSourceName(id){
 const source=TC.sources.find(item=>item.id===id);
 if(!source)return t('translationMissing');
 const tags=[locale,locale.split('-')[0],'ru'];
 return tags.map(tag=>source.translations?.find(tr=>tr.language_tag===tag)?.title).find(Boolean)||source.original_title||t('translationMissing');
}
function recipeMaterialType(source){
 if(source.book==='personal')return 'dictionary';
 if(typeof source.book==='number')return 'book';
 if(source.word_ids?.length||source.word)return 'words';
 if(source.kanji_ids?.length||source.kanji||source.jlpt)return 'kanji';
 return 'book';
}
function recipeMaterialLeading(source){
 if(source.book===1||source.book===2)return `<img class="recipe-book-icon" src="../../assets/textbook-icons/textbook-${source.book===1?'sage':'blue'}.png" alt="">`;
 const file={book:'textbook',dictionary:'notebook',kanji:'inkstone',words:'notebook'}[recipeMaterialType(source)]||'textbook';
 return `<img class="recipe-book-icon recipe-material-icon" src="../../assets/stationery/${file}.png" alt="">`;
}
function recipeMaterialTitle(source){
 if(source.book==='personal')return t('personalDictionary');
 if(source.book)return recipeSourceName(source.book);
 return {dictionary:t('personalDictionary'),kanji:t('typeKanji'),words:t('typeWords'),book:t('allCatalog')}[recipeMaterialType(source)]||t('allCatalog');
}
function recipeWordsLink(count,source=false){
 if(count)return `<button type="button" class="recipe-words-link" data-tr="preview" data-scope="${source?'source':'plan'}"><span>${t('viewWordCount')(count)}</span></button>`;
 if(!source)return '';
 const kind=sourceDraft?recipeMaterialType(sourceDraft):'';
 if(kind==='words')return `<p class="material-empty-note" role="status">${t('materialWordsNoneSelected')}</p>`;
 if(kind==='dictionary'){
  const emptyDict=!sourceWords({book:'personal'}).length;
  return `<p class="material-empty-note" role="status">${t(emptyDict?'materialDictionaryEmpty':'materialDictionaryNoMatch')}</p>`;
 }
 return `<div class="material-empty-words" role="status"><img src="../../assets/mascot/mascot_sticker_peek_empty_box.png" alt=""><h3>${t('materialNoWords')}</h3><p>${t('materialNoWordsHint')}</p></div>`;
}
function recipeTrainingSettings(body){return `<section class="recipe-training-settings"><hr class="recipe-section-divider" aria-hidden="true"><h3>${t('trainingSettings')}</h3><div class="recipe-training-settings-body">${body}</div></section>`;}
function recipeChapterPreview(source){const chapters=[...new Set(source.chapters||[])].sort((a,b)=>a-b);return chapters.slice(0,3).join(', ')+(chapters.length>3?' '+t('moreChapterNumbers')(chapters.length-3):'');}
function recipeDictionaryTagPreview(source){const names=(source.tag_ids||[]).map(id=>dictionaryTags.find(tag=>tag.id===id)).filter(Boolean).map(dictionaryTagName);return names.slice(0,2).join(', ')+(names.length>2?' · '+t('moreRecipeTags')(names.length-2):'');}
function recipeKanjiPreview(source){const ids=[...new Set(source.kanji_ids?.length?source.kanji_ids:source.kanji?[source.kanji]:[])];const symbols=ids.map(id=>TC.kanji.find(k=>k.id===id)?.symbol).filter(Boolean);return symbols.slice(0,3).join('、')+(symbols.length>3?' · '+t('moreRecipeTags')(symbols.length-3):'');}
function recipeSourceTags(source,{countKanji=false}={}){
 const tags=[];
 const add=(kind,fields,label)=>tags.push({kind,fields,label});
 if(source.book==='personal')add('personal',{source_id:'personal'},t('personalDictionary'));
 else if(typeof source.book==='number')add('source',{source_id:source.book},recipeSourceName(source.book));
 if(source.chapters?.length)add('chapters',{chapters:source.chapters},`${t('chapters')} ${recipeChapterPreview(source)}`);
 if(source.from||source.to){
  const label=source.from&&source.from===source.to?`${t('chapter')} ${source.from}`:source.from&&source.to?`${t('chapters')} ${source.from}–${source.to}`:source.from?t('tagFrom')(source.from):t('tagTo')(source.to);
  add('chapters',{chapter_from:source.from,chapter_to:source.to},label);
 }
 if(source.jlpt)add('jlpt',{jlpt_level:source.jlpt},t('tagJlpt')(source.jlpt));
 if(source.kanji&&!source.kanji_ids?.length)add('kanji',{kanji_id:source.kanji},countKanji?`${t('kanji')}: 1`:t('tagKanji')(recipeKanjiPreview(source)));
 if(source.word)add('word',{word_id:source.word},t('tagEntry')([...TC.words,...words].find(w=>w.id===source.word)?.written||t('translationMissing')));
 if(source.kanji_ids?.length)add('kanji',{kanji_ids:source.kanji_ids,kanji_match:source.kanji_match||'any'},countKanji?`${t('kanji')}: ${new Set(source.kanji_ids).size}`:recipeKanjiPreview(source));
 if(source.word_ids?.length)add('word',{word_ids:source.word_ids},t('wordCount')(source.word_ids.length));
 if(source.book==='personal'&&source.tag_ids?.length)add('dictionary-tags',{tag_ids:source.tag_ids},recipeDictionaryTagPreview(source));
 if(!tags.length)add('scope',{source_id:null},t('anyBook'));
 return tags;
}
function recipeSourceTagsView(plan){
 const chips=plan.sources.map((source,index)=>{
  const tags=recipeSourceTags(source,{countKanji:true});
  const kind=tags[0]?.kind||'scope';
  const fields=Object.assign({},...tags.map(tag=>tag.fields||{}));
  return {index,kind,fields,label:tags.map(tag=>tag.label).join(' · ')};
 });
 const expanded=expandedRecipeSources.has(plan.id),total=chips.length;
 const content=chips.map((chip,index)=>{
  const hidden=!expanded&&index>=2;
  return `${index?`<span class="plan-source-union"${hidden?' hidden':''}>${t('tagUnion')}</span>`:''}<div class="plan-source-group" role="group" aria-label="${t('tagSourceNumber')(chip.index+1)}" data-source-index="${chip.index}"${hidden?' hidden':''}><span class="plan-source-tag" data-tag-kind="${chip.kind}" data-fields="${escapeHTML(JSON.stringify(chip.fields))}"${hidden?' hidden':''}>${escapeHTML(chip.label)}</span></div>`;
 }).join('');
 return `<div class="plan-source-preview${expanded?' is-expanded':''}" data-plan-id="${plan.id}"><div id="plan-sources-${plan.id}" class="plan-source-tags is-simple">${content}</div>${total>2?`<button type="button" class="plan-sources-toggle" data-tr="sources-toggle" data-id="${plan.id}" aria-controls="plan-sources-${plan.id}" aria-expanded="${expanded}">${expanded?t('collapseRecipeSources'):t('moreRecipeTags')(total-2)}</button>`:''}</div>`;
}

function planCardView(p,mode='library'){
 const pick=mode==='pick';
 const tasks=p.task_count===null?t('allWordsCount')(planWords(p).length):t('tasks')(p.task_count);
 const time=p.seconds_per_word===null?t('unlimited'):t('perWord')(p.seconds_per_word);
 const book=p.sources.length===1?p.sources[0].book:null;
 const kind=p.sources.length>1?'mixed':book==='personal'?'personal':typeof book==='number'?'book':p.sources.some(s=>s.kanji||s.jlpt)?'kanji':'words';
 const cardAction=pick?'start':'view';
 const label=pick?t('launch'):t('viewRecipe');
 return `<article class="plan-card plan-card-plain ${pick?'plan-card-pick':''}" data-id="${p.id}" data-kind="${kind}" data-timed="${p.seconds_per_word!==null}" data-mode="${mode}">
  <div class="plan-card-main" data-tr="${cardAction}" data-id="${p.id}" role="button" tabindex="0" aria-label="${escapeHTML(label+': '+planName(p))}">
   <span class="plan-title-row"><span class="plan-title" title="${escapeHTML(planName(p))}">${escapeHTML(planName(p))}</span></span>
   <div class="plan-material">${recipeSourceTagsView(p)}</div>
   <span class="plan-metrics"><span>${icon('word-stack')}${escapeHTML(tasks)}</span><span class="plan-time ${p.seconds_per_word===null?'':'is-timed'}">${icon(p.seconds_per_word===null?'no-timer':'timer')}${escapeHTML(time)}</span></span>
  </div>
  ${pick?'':`<div class="plan-accessible-actions">${['move-up','move-down'].map((moveAction,index)=>`<button type="button" data-tr="${moveAction}" data-id="${p.id}" ${recipes.indexOf(p)===(index?recipes.length-1:0)?'disabled':''} aria-label="${escapeHTML(t(index?'moveDown':'moveUp')+': '+planName(p))}">${t(index?'moveDown':'moveUp')}</button>`).join('')}</div>`}
 </article>`;
}
const normalizeRecipeSearch=value=>value.normalize('NFKC').toLocaleLowerCase(locale).replaceAll('ё','е');
function filteredRecipePlans(){
 const terms=normalizeRecipeSearch(recipeSearchQuery).trim().split(/\s+/).filter(Boolean);
 return recipes.filter(p=>{
  const searchable=normalizeRecipeSearch([...p.translations.map(tr=>tr.name),...p.sources.map(sourceLabel)].join(' '));
  return terms.every(term=>searchable.includes(term));
 });
}
function recipeSearchCount(plans){
 const number=new Intl.NumberFormat(locale);
 return recipeSearchQuery.trim()?t('recipeResults')(number.format(plans.length),number.format(recipes.length)):t('listCount')(recipes.length);
}
function recipeListItems(plans,mode='library'){
 if(!recipes.length){
  if(mode==='pick')return `<div class="recipe-empty"><img src="../../assets/mascot/cat-crafting-cards.png" alt="">${emptyView(t('emptyRecipes'),t('pickEmptyHint'),false)}<button type="button" class="tonal-button" data-action="open-recipe-book">${t('openRecipeBook')}</button></div>`;
  return `<div class="recipe-empty"><img src="../../assets/mascot/cat-crafting-cards.png" alt="">${emptyView(t('emptyRecipes'),t('emptyRecipeHint'),false)}</div>`;
 }
 if(!plans.length)return `<div class="recipe-search-empty"><h3>${t('recipeNotFound')}</h3></div>`;
 return plans.map(p=>planCardView(p,mode)).join('');
}
// Reuse the catalog Material search and search_off artwork.
const recipeSearchIcons={"search": "<svg fill=\"currentColor\" aria-hidden=\"true\" xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 0 24 24\" width=\"24\"><path d=\"M0 0h24v24H0V0z\" fill=\"none\"/><path d=\"M15.5 14h-.79l-.28-.27c1.2-1.4 1.82-3.31 1.48-5.34-.47-2.78-2.79-5-5.59-5.34-4.23-.52-7.79 3.04-7.27 7.27.34 2.8 2.56 5.12 5.34 5.59 2.03.34 3.94-.28 5.34-1.48l.27.28v.79l4.25 4.25c.41.41 1.08.41 1.49 0 .41-.41.41-1.08 0-1.49L15.5 14zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z\"/></svg>", "off": "<svg fill=\"currentColor\" aria-hidden=\"true\" xmlns=\"http://www.w3.org/2000/svg\" enable-background=\"new 0 0 24 24\" height=\"24\" viewBox=\"0 0 24 24\" width=\"24\"><g><rect fill=\"none\" height=\"24\" width=\"24\"/><rect fill=\"none\" height=\"24\" width=\"24\"/></g><g><g><path d=\"M15.5,14h-0.79l-0.28-0.27c1.2-1.4,1.82-3.31,1.48-5.34c-0.47-2.78-2.79-4.99-5.58-5.34C6.54,2.58,3.3,5.38,3.03,9h2.02 c0.24-2.12,1.92-3.8,4.06-3.98C11.65,4.8,14,6.95,14,9.5c0,2.49-2.01,4.5-4.5,4.5c-0.17,0-0.33-0.03-0.5-0.05l0,2.02 c0,0,0,0,0.01,0.01c1.8,0.13,3.47-0.47,4.72-1.55L14,14.71v0.79l4.25,4.25c0.41,0.41,1.08,0.41,1.49,0l0,0 c0.41-0.41,0.41-1.08,0-1.49L15.5,14z\"/><path d=\"M6.12,11.17L4,13.29l-2.12-2.12c-0.2-0.2-0.51-0.2-0.71,0l0,0c-0.2,0.2-0.2,0.51,0,0.71L3.29,14l-2.12,2.12 c-0.2,0.2-0.2,0.51,0,0.71l0,0c0.2,0.2,0.51,0.2,0.71,0L4,14.71l2.12,2.12c0.2,0.2,0.51,0.2,0.71,0l0,0c0.2-0.2,0.2-0.51,0-0.71 L4.71,14l2.12-2.12c0.2-0.2,0.2-0.51,0-0.71l0,0C6.63,10.98,6.32,10.98,6.12,11.17z\"/></g></g></svg>"};
function recipeSearchControl(){
 if(!recipeSearchOpen)return '';
 return `<div id="recipe-search"><label class="n-search"><span class="n-search-icon" aria-hidden="true">${icon('search')}</span><input id="recipe-query" type="search" value="${escapeHTML(recipeSearchQuery)}" placeholder="${t('recipeSearchHint')}" aria-label="${t('findRecipe')}" autocomplete="off" aria-controls="recipe-results"><button type="button" class="n-clear-query" data-tr="search-clear" ${recipeSearchQuery?'':'hidden'}>${t('clearRecipeSearch')}</button></label></div>`;
}
function updateRecipeSearch(){
 const mode=screen==='training-pick'?'pick':'library';
 const plans=filteredRecipePlans();
 $('#recipe-results').innerHTML=recipeListItems(plans,mode);
 const count=$('#recipe-list-count'); if(count) count.textContent=recipeSearchCount(plans);
 $('#recipe-search .n-clear-query').hidden=!recipeSearchQuery;
 $('.recipe-list-scroll').scrollTop=0;
}
function recipesView(){
 const plans=filteredRecipePlans();
 const searchToggle=`<button type="button" class="recipe-search-toggle" data-tr="search-toggle" aria-label="${t(recipeSearchOpen?'hideRecipeSearch':'findRecipe')}" aria-expanded="${recipeSearchOpen}"${recipeSearchOpen?' aria-controls="recipe-search"':''}>${recipeSearchIcons[recipeSearchOpen?'off':'search']}</button>`;
 const welcome=recipes.length?`<div class="recipe-welcome"><div><p>${t('listHint')}</p><span id="recipe-list-count" role="status" aria-live="polite" aria-atomic="true">${recipeSearchCount(plans)}</span></div><img src="../../assets/mascot/cat-crafting-cards.png" alt="" aria-hidden="true"></div>`:'';
 return `<div class="recipe-list-screen">${header(t('recipes'),'home',searchToggle)}<div class="recipe-list-scroll" tabindex="0" role="region" aria-label="${t('recipes')}">${recipeSearchControl()}<div class="recipe-content recipe-list-content">
  ${welcome}
  ${recipeMessage?`<p role="status" class="recipe-status-msg">${escapeHTML(recipeMessage)}</p>`:''}
  <span class="plan-announcement" role="status" aria-live="polite" aria-atomic="true"></span>
  <div class="plans-list" id="recipe-results" role="list" aria-label="${t('recipes')}">${recipeListItems(plans,'library')}</div>
 </div></div><div class="recipe-create-bar"><button type="button" class="recipe-create" data-tr="new" aria-label="${t('newRecipe')}">${icon('plus')}<span>${t('createFab')}</span></button></div>
 </div>`;
}
function trainingPickView(){
 const plans=filteredRecipePlans();
 const searchToggle=`<button type="button" class="recipe-search-toggle" data-tr="search-toggle" aria-label="${t(recipeSearchOpen?'hideRecipeSearch':'findRecipe')}" aria-expanded="${recipeSearchOpen}"${recipeSearchOpen?' aria-controls="recipe-search"':''}>${recipeSearchIcons[recipeSearchOpen?'off':'search']}</button>`;
 const welcome=recipes.length?`<div class="recipe-welcome"><div><p>${t('pickRecipeHint')}</p><span id="recipe-list-count" role="status" aria-live="polite" aria-atomic="true">${recipeSearchCount(plans)}</span></div><img src="../../assets/mascot/cat-crafting-cards.png" alt="" aria-hidden="true"></div>`:'';
 return `<div class="recipe-list-screen training-pick-screen">${header(t('pickRecipe'),'home',searchToggle)}<div class="recipe-list-scroll" tabindex="0" role="region" aria-label="${t('pickRecipe')}">${recipeSearchControl()}<div class="recipe-content recipe-list-content">
  ${welcome}
  <span class="plan-announcement" role="status" aria-live="polite" aria-atomic="true"></span>
  <div class="plans-list" id="recipe-results" role="list" aria-label="${t('pickRecipe')}">${recipeListItems(plans,'pick')}</div>
 </div></div></div>`;
}
function recipeDirty(){return recipeDraft&&JSON.stringify(recipeDraft)!==recipeOriginal;}
function recipeLeave(next){showModal(t('unsaved'),`<p>${t('unsavedText')}</p>`,[{label:t('cancel'),fn:closeModal},{label:t('leaveUnsaved'),danger:true,fn:()=>{recipeDraft=null;go(next)}}]);}
function initRecipe(id,next='recipe-editor'){recipeWordsOrigin='recipe-editor';expandedEditorMaterials=false;removedRecipeSource=null;const p=recipes.find(p=>p.id===id);recipeDraft=p?structuredClone(p):{id:null,translations:[{language_tag:locale,name:''}],sources:[],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:null};recipeOriginal=JSON.stringify(recipeDraft);showRecipeWords=false;if(next)go(next);}


function openRecipeDetail(id){viewingId=id;recipeDraft=null;go('recipe-detail');}
function limitView(key,title,value,pool=0){
 const text=key==='task_count'?(value===null?t('allWordsCount')(pool):t('tasks')(value)):(value===null?t('unlimited'):t('perWord')(value));
 return `<div data-limit-view="${key}" class="recipe-detail-limit"><h4>${t(title)}</h4><p data-tr="detail-${key}">${escapeHTML(text)}</p></div>`;
}
function recipeMaterialView(){
 const p=recipes.find(x=>x.id===viewingId);const source=p?.sources?.[viewingSourceIndex];
 if(!source)return header(t('materialType'),'recipe-detail')+emptyView(t('recipeNotFound'),t('recipeSearchHelp'));
 const tags=recipeSourceTags(source,{countKanji:true});
 const type=recipeMaterialType(source);
 const typeLabel={book:t('typeBook'),dictionary:t('typeDictionary'),kanji:t('typeKanji'),words:t('typeWords')}[type]||t('materials');
 const rows=tags.filter(tag=>!['source','personal','scope'].includes(tag.kind)).map(tag=>`<div class="w-nav-row"><strong>${escapeHTML(tag.label)}</strong></div>`).join('');
 const book=tags.find(tag=>['source','personal'].includes(tag.kind));
 return `<div class="recipe-editor-screen">${header(t('materialType'),'recipe-detail')}<div class="recipe-content recipe-source-content" data-tr="material-viewer"><section><h3>${t('materialType')}</h3><p>${escapeHTML(typeLabel)}</p></section>${book?`<section><h3>${t('bookFilter')}</h3><p>${escapeHTML(book.label)}</p></section>`:''}${rows?`<section><h3>${t('materials')}</h3>${rows}</section>`:''}${recipeWordsLink(sourceWords(source).length,true)}</div></div>`;
}
function recipeDetailView(){
 const p=recipes.find(x=>x.id===viewingId);
 if(!p)return header(t('recipeDetail'),'recipes')+emptyView(t('recipeNotFound'),t('recipeSearchHelp'));
 const ws=planWords(p);
 const menu=`<span class="dictionary-word-menu-wrap"><button type="button" class="icon-button" data-tr="detail-menu" data-id="${p.id}" aria-label="${escapeHTML(t('menuLabel')(planName(p)))}" aria-haspopup="menu">${icon('more')}</button></span>`;
 const materials=p.sources.map((s,i)=>`<div class="source-row"><div class="source-row-body"><strong>${recipeMaterialLeading(s)}<span class="recipe-material-copy"><span class="recipe-material-title">${escapeHTML(recipeMaterialTitle(s))}</span><span class="editor-source-tags">${recipeSourceTags(s).filter(tag=>!['source','personal','scope'].includes(tag.kind)).map(tag=>`<span class="plan-source-tag" data-tag-kind="${tag.kind}">${escapeHTML(tag.label)}</span>`).join('')}</span></span></strong></div></div>`).join('');
 return `<div class="recipe-editor-screen recipe-detail-screen">${header(t('recipeDetail'),'recipes',menu)}<div class="recipe-content recipe-source-content"><label class="field recipe-name-field"><span>${t('name')}</span><div class="recipe-detail-title" data-tr="detail-name">${escapeHTML(planName(p))}</div></label><section><h3>${t('materials')}</h3><div class="source-list">${materials}</div>${recipeWordsLink(ws.length)}</section>${recipeTrainingSettings(limitView('task_count','wordsLimit',p.task_count,ws.length)+limitView('seconds_per_word','timeLimit',p.seconds_per_word))}</div><div class="recipe-editor-footer"><button type="button" class="recipe-save-fab" data-tr="edit" data-id="${p.id}">${icon('edit')}${t('editFab')}</button></div></div>`;
}
function recipeEditorView(){if(!recipeDraft){recipeDraft={id:null,translations:[{language_tag:locale,name:''}],sources:[],selection_mode:'shuffled_cycles',task_count:null,seconds_per_word:null};recipeOriginal=JSON.stringify(recipeDraft);}const p=recipeDraft,ws=planWords(p);const localName=p.translations.find(x=>x.language_tag===locale)?.name||'';return `<div class="recipe-editor-screen">`+header(t(p.id?'editRecipe':'newRecipe'),'recipes')+`<form id="recipe-form" class="recipe-content" novalidate><label class="field recipe-name-field"><span>${t('name')}</span><span class="recipe-name-input"><input id="recipe-name" maxlength="120" value="${escapeHTML(localName)}" placeholder="${t('nameHint')}"><button type="button" class="recipe-name-clear" data-tr="name-clear" aria-label="${t('clearName')}" ${localName?'':'hidden'}>${icon('close')}</button></span></label><section><h3>${t('materials')}</h3>${removedRecipeSource?`<div class="recipe-remove-notice"><span role="status">${t('materialRemoved')}</span><button type="button" class="text-button" data-tr="source-undo">${t('undo')}</button></div>`:''}<div class="source-list">${p.sources.map((s,i)=>`<div class="source-row" ${i>=3&&!expandedEditorMaterials?'hidden':''}><button type="button" data-tr="source-edit" data-index="${i}"><strong>${recipeMaterialLeading(s)}<span class="recipe-material-copy"><span class="recipe-material-title">${escapeHTML(recipeMaterialTitle(s))}</span><span class="editor-source-tags">${recipeSourceTags(s).filter(tag=>!['source','personal','scope'].includes(tag.kind)).map(tag=>`<span class="plan-source-tag" data-tag-kind="${tag.kind}">${escapeHTML(tag.label)}</span>`).join('')}</span></span></strong></button><button type="button" data-tr="source-remove" data-index="${i}" aria-label="${t('remove')} ${escapeHTML(sourceLabel(s))}">${icon('trash')}</button></div>`).join('')}</div>${p.sources.length>3?`<button type="button" class="text-button" data-tr="materials-toggle" aria-expanded="${expandedEditorMaterials}">${expandedEditorMaterials?t('lessMaterials'):t('moreMaterials')(p.sources.length-3)}</button>`:''}<button type="button" class="tonal-button full-width" data-tr="source-add">${icon('plus')}${t('addSource')}</button>${recipeWordsLink(ws.length)}</section>${recipeTrainingSettings(limitControl('task_count','wordsLimit','count',p.task_count)+limitControl('seconds_per_word','timeLimit','seconds',p.seconds_per_word))}</form><div class="recipe-editor-footer"><button class="recipe-save-fab" type="submit" form="recipe-form" aria-label="${t('saveRecipe')}"><svg class="svg-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M17 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V7l-4-4ZM7 5h9v4H7V5Zm10 14H7v-6h10v6Z"/></svg>${t('saveRecipeShort')}</button></div></div>`;}
function recipeWordsView(){
 const viewedSource=recipes.find(x=>x.id===viewingId)?.sources?.[viewingSourceIndex];
 const ws=recipeWordsOrigin==='recipe-source'&&sourceDraft?sourceWords(sourceDraft)
  :recipeWordsOrigin==='recipe-material-view'&&viewedSource?sourceWords(viewedSource)
  :recipeWordsOrigin==='recipe-detail'&&viewingId?planWords(recipes.find(x=>x.id===viewingId)||{sources:[]})
  :recipeDraft?planWords(recipeDraft):[];
 return `<div class="recipe-editor-screen">${header(t('wordPool'),recipeWordsOrigin)}<div class="recipe-content recipe-source-content recipe-catalog-words"><p class="recipe-note">${t('wordCount')(ws.length)}</p><div class="n-word-list w-chapter-word-list">${ws.map(w=>CatalogSelection.wordRow(w,{gloss:translation(w),html:escapeHTML,interactive:false})).join('')}</div></div></div>`;
}
function limitControl(key,title,label,value){return `<div class="recipe-limit"><h4>${t(title)}</h4><div class="limit-choices" role="group" aria-label="${t(title)}">${[false,true].map(limited=>`<button type="button" aria-pressed="${limited===(value!==null)}" data-tr="limit" data-key="${key}" data-limited="${limited}">${t(key==='task_count'?(limited?'setCount':'poolSize'):(limited?'limited':'unlimited'))}</button>`).join('')}</div>${value!==null?`<label class="field">${t(label)}<input type="number" min="1" step="1" inputmode="numeric" data-limit="${key}" value="${value}"></label>`:''}</div>`;}
function sourceOptions(items,current,empty){return `<option value="">${escapeHTML(empty)}</option>`+items.map(([id,label])=>`<option value="${id}" ${String(current)===String(id)?'selected':''}>${escapeHTML(label)}</option>`).join('');}
function sourcePicker(){return renderMaterialPicker();}
function applyRecipeSource(){
 const s=sourceDraft;
 clearRecipeErrors();
 if((s.from||s.to)&&typeof s.book!=='number')showRecipeFieldError('[data-source="book"]',t('sourceEmpty'));
 for(const key of ['from','to'])if(s[key]!==null&&(!Number.isSafeInteger(s[key])||s[key]<1))showRecipeFieldError(`[data-source="${key}"]`,t('invalidLimits'));
 if(s.from&&s.to&&s.from>s.to)showRecipeFieldError('[data-source="to"]',t('sourceEmpty'));
 if(!document.querySelector('[data-recipe-invalid]')&&!sourceWords(s).length)showRecipeFieldError('[data-material-type]:checked',t('sourceEmpty'));
 const invalid=document.querySelector('[data-recipe-invalid]');if(invalid){invalid.focus({preventScroll:true});(invalid.closest('.field')||invalid).scrollIntoView({block:'nearest'});return;}
 if(sourceIndex===null)recipeDraft.sources.push(structuredClone(s));else recipeDraft.sources[sourceIndex]=structuredClone(s);
 go('recipe-editor');
}
function clearRecipeErrors(){document.querySelectorAll('.recipe-inline-error').forEach(el=>el.remove());document.querySelectorAll('[data-recipe-invalid]').forEach(el=>{el.removeAttribute('aria-invalid');el.removeAttribute('aria-describedby');delete el.dataset.recipeInvalid;});}
function showRecipeFieldError(control,message){
 const el=document.querySelector(control);if(!el)return;
 const note=document.createElement('span');note.className='recipe-inline-error';note.id='recipe-field-error-'+document.querySelectorAll('.recipe-inline-error').length;note.setAttribute('role','alert');
 const mark=document.createElement('span');mark.textContent='!';mark.setAttribute('aria-hidden','true');note.append(mark,document.createTextNode(message));
 el.setAttribute('aria-invalid','true');el.setAttribute('aria-describedby',note.id);el.dataset.recipeInvalid='true';
 const field=el.closest('.field');if(field)field.append(note);else el.after(note);
}
function saveRecipe(){const p=recipeDraft;clearRecipeErrors();
 if(!p.translations.some(x=>x.name.trim()))showRecipeFieldError('#recipe-name',t('missingName'));
 for(const key of ['task_count','seconds_per_word'])if(p[key]!==null&&(!Number.isSafeInteger(p[key])||p[key]<1||p[key]>2147483647))showRecipeFieldError(`[data-limit="${key}"]`,t('invalidLimits'));
 if(!p.sources.length||p.sources.some(s=>!sourceWords(s).length))showRecipeFieldError('[data-tr="source-add"]',t('needSource'));
 const invalid=document.querySelector('[data-recipe-invalid]');if(invalid){closeModal();invalid.focus({preventScroll:true});(invalid.closest('.field')||invalid).scrollIntoView({block:'nearest'});return;}p.selection_mode='shuffled_cycles';p.translations=p.translations.filter(x=>x.name.trim()).map(x=>({...x,name:x.name.trim()}));if(!p.id)p.id=crypto.randomUUID();const saved=structuredClone(p);recipes=recipes.filter(x=>x.id!==p.id);recipes.push(saved);if(!selectedRecipeId)selectedRecipeId=p.id;recipeDraft=null;recipeMessage=t('savedRecipe');openRecipeDetail(saved.id);}

document.addEventListener('click',e=>{
 const b=e.target.closest('[data-tr]');
 if(!b)return;
 const a=b.dataset.tr;
 e.preventDefault();
 if(a==='sources-toggle'){
  if(expandedRecipeSources.has(b.dataset.id))expandedRecipeSources.delete(b.dataset.id);else expandedRecipeSources.add(b.dataset.id);
  const plan=recipes.find(p=>p.id===b.dataset.id),card=b.closest('.plan-card');
  card.querySelector('.plan-material').innerHTML=recipeSourceTagsView(plan);
  const toggle=card.querySelector('.plan-sources-toggle');
  toggle.focus({preventScroll:true});
  if(!expandedRecipeSources.has(plan.id))toggle.scrollIntoView({block:'nearest'});
 }
 if(a==='name-clear'){const input=$('#recipe-name');if(input){input.value='';let tr=recipeDraft.translations.find(x=>x.language_tag===locale);if(!tr){tr={language_tag:locale,name:''};recipeDraft.translations.push(tr);}tr.name='';$('.recipe-name-clear').hidden=true;input.focus();}return;}
 if(a==='new')initRecipe();
 if(a==='search-toggle'){
  recipeSearchOpen=!recipeSearchOpen;
  if(!recipeSearchOpen)recipeSearchQuery='';
  
  render();
  (recipeSearchOpen?$('#recipe-query'):$('.recipe-search-toggle')).focus();
 }
 if(a==='search-clear'){
  recipeSearchQuery='';
  $('#recipe-query').value='';
  updateRecipeSearch();
  $('#recipe-query').focus();
 }
 if(a==='move-up'||a==='move-down'){
  const visible=recipes,index=visible.findIndex(p=>p.id===b.dataset.id);
  const neighbor=visible[index+(a==='move-up'?-1:1)];
  if(!neighbor)return;
  const from=recipes.findIndex(p=>p.id===b.dataset.id),to=recipes.indexOf(neighbor);
  const scroll=$('.recipe-list-scroll').scrollTop;
   [recipes[from],recipes[to]]=[recipes[to],recipes[from]];
  recipeMessage='';
  render();
  $('.recipe-list-scroll').scrollTop=scroll;
  const card=document.querySelector(`.plan-card[data-id="${b.dataset.id}"]`);
  (card.querySelector(`[data-tr="${a}"]:not(:disabled)`)||card.querySelector('.plan-accessible-actions button:not(:disabled)'))?.focus({preventScroll:true});
  $('.plan-announcement').textContent=t('movedPlan')(planName(recipes[to]),to+1);
 }
 if(a==='view'){
  if(Date.now()<suppressPlanClickUntil)return;
   openRecipeDetail(b.dataset.id);
 }
 if(a==='start'){
  if(Date.now()<suppressPlanClickUntil)return;
  selectedRecipeId=b.dataset.id;
  go('practice');
 }
 if(a==='edit'){
   initRecipe(b.dataset.id);
 }
 if(a==='source-view'){viewingSourceIndex=Number(b.dataset.index);go('recipe-material-view');}
 if(a==='detail-menu'){
  const id=b.dataset.id,p=recipes.find(x=>x.id===id);
  showModal(t('deleteRecipe'),`<p>${escapeHTML(planName(p))}</p><p>${t('deleteHint')}</p>`,[{label:t('cancel'),fn:closeModal},{label:t('delete'),danger:true,fn:()=>{recipes=recipes.filter(x=>x.id!==id);if(selectedRecipeId===id)selectedRecipeId=null;viewingId=null;closeModal();go('recipes');}}]);
 }
 if(a==='source-add'||a==='source-edit'){sourceIndex=a==='source-edit'?Number(b.dataset.index):null;sourceDraft=sourceIndex===null?newMaterialDraft():structuredClone(recipeDraft.sources[sourceIndex]);resetMaterialPicker();if(sourceIndex===null)sourceDraft.type='book';go('recipe-source');}
 if(a==='source-apply')applyRecipeSource();
 if(a==='materials-toggle'){expandedEditorMaterials=!expandedEditorMaterials;render();}
 if(a==='source-remove'){const index=Number(b.dataset.index);removedRecipeSource={index,source:recipeDraft.sources.splice(index,1)[0]};render();}
 if(a==='source-undo'&&removedRecipeSource){recipeDraft.sources.splice(removedRecipeSource.index,0,removedRecipeSource.source);removedRecipeSource=null;render();}
 if(a==='limit'){recipeDraft[b.dataset.key]=b.dataset.limited==='true'?(b.dataset.key==='task_count'?20:30):null;render();}
 if(a==='preview'){recipeWordsOrigin=b.dataset.scope==='source'?(screen==='recipe-material-view'?'recipe-material-view':'recipe-source'):(screen==='recipe-detail'?'recipe-detail':'recipe-editor');go('recipe-words');}
});

// Hold the card body to drag. Before the hold, vertical motion remains scrolling.
let planDrag=null,suppressPlanClickUntil=0;
function beginPlanHold(target,x,y,pointer,touch=false){
 const card=target.closest('.plan-card');
 if(!card||card.classList.contains('plan-card-pick')||target.closest('button,input,a,select,textarea')||recipeSearchQuery.trim())return;
 if(planDrag)finishPlanDrag(false);
 const drag={card,id:card.dataset.id,pointer,touch,startX:x,startY:y,active:false,timer:null};
 planDrag=drag;
 drag.timer=setTimeout(()=>{
  if(planDrag!==drag||!card.isConnected)return;
  drag.active=true;
  card.classList.add('is-dragging');
  document.querySelector('.recipe-list-screen')?.classList.add('has-plan-drag');
  window.getSelection()?.removeAllRanges();
  if(!touch)card.setPointerCapture(pointer);
 },450);
}
function movePlanDrag(x,y){
 const drag=planDrag;
 if(!drag)return;
 if(!drag.active){
  if(Math.hypot(x-drag.startX,y-drag.startY)>8)finishPlanDrag(false);
  return;
 }
 const scroller=$('.recipe-list-scroll'),bounds=scroller.getBoundingClientRect();
 if(y<bounds.top+56)scroller.scrollTop-=16;
 if(y>bounds.bottom-56)scroller.scrollTop+=16;
 const siblings=[...document.querySelectorAll('#recipe-results .plan-card')].filter(card=>card!==drag.card);
 const next=siblings.find(card=>y<card.getBoundingClientRect().top+card.offsetHeight/2);
 if(next)drag.card.parentElement.insertBefore(drag.card,next);else drag.card.parentElement.append(drag.card);
 if(!drag.touch)drag.card.setPointerCapture(drag.pointer);
}
function finishPlanDrag(commit){
 const drag=planDrag;
 if(!drag)return;
 planDrag=null;
 clearTimeout(drag.timer);
 drag.card.classList.remove('is-dragging');
 document.querySelector('.has-plan-drag')?.classList.remove('has-plan-drag');
 if(!drag.touch&&drag.card.hasPointerCapture(drag.pointer))drag.card.releasePointerCapture(drag.pointer);
 if(!drag.active)return;
 suppressPlanClickUntil=Date.now()+500;
 if(commit){
  const ids=[...document.querySelectorAll('#recipe-results .plan-card')].map(card=>card.dataset.id);
  recipes=ids.map(id=>recipes.find(plan=>plan.id===id));
 }
 const scroll=$('.recipe-list-scroll').scrollTop;
 recipeMessage='';
 render();
 $('.recipe-list-scroll').scrollTop=scroll;
}
document.addEventListener('pointerdown',e=>{
 if(e.pointerType!=='touch'&&e.isPrimary&&e.button===0)beginPlanHold(e.target,e.clientX,e.clientY,e.pointerId);
});
document.addEventListener('pointermove',e=>{if(planDrag&&!planDrag.touch&&planDrag.pointer===e.pointerId)movePlanDrag(e.clientX,e.clientY)});
document.addEventListener('pointerup',e=>{if(planDrag&&!planDrag.touch&&planDrag.pointer===e.pointerId)finishPlanDrag(true)});
document.addEventListener('pointercancel',e=>{if(planDrag&&!planDrag.touch&&planDrag.pointer===e.pointerId)finishPlanDrag(false)});
document.addEventListener('touchstart',e=>{
 if(e.touches.length!==1){finishPlanDrag(false);return;}
 const touch=e.touches[0];
 beginPlanHold(e.target,touch.clientX,touch.clientY,touch.identifier,true);
},{passive:true});
document.addEventListener('touchmove',e=>{
 if(!planDrag?.touch)return;
 const touch=[...e.touches].find(touch=>touch.identifier===planDrag.pointer);
 if(!touch)return;
 if(planDrag.active)e.preventDefault();
 movePlanDrag(touch.clientX,touch.clientY);
},{passive:false});
document.addEventListener('touchend',()=>{if(planDrag?.touch)finishPlanDrag(true)});
document.addEventListener('touchcancel',()=>{if(planDrag?.touch)finishPlanDrag(false)});
document.addEventListener('contextmenu',e=>{if(planDrag?.active){e.preventDefault();}});
document.addEventListener('click',e=>{
 if(Date.now()<suppressPlanClickUntil){e.preventDefault();e.stopImmediatePropagation();}
},true);
window.addEventListener('blur',()=>finishPlanDrag(false));

document.addEventListener('keydown',e=>{
 if(e.key==='Escape'&&planDrag){e.preventDefault();finishPlanDrag(false);return;}
 if(e.key==='Escape'&&e.target.id==='recipe-query'){
  e.preventDefault();
  $('.recipe-search-toggle').click();
 }
});

document.addEventListener('input',e=>{const el=e.target;if(el.id==='recipe-query'&&!e.isComposing){recipeSearchQuery=el.value;updateRecipeSearch();}if(el.id==='recipe-name'){let tr=recipeDraft.translations.find(x=>x.language_tag===locale);if(!tr){tr={language_tag:locale,name:''};recipeDraft.translations.push(tr);}tr.name=el.value;const clear=$('.recipe-name-clear');if(clear)clear.hidden=!el.value;}if(el.dataset.limit){recipeDraft[el.dataset.limit]=el.value===''?0:Number(el.value);}});
document.addEventListener('compositionend',e=>{if(e.target.id==='recipe-query'){recipeSearchQuery=e.target.value;updateRecipeSearch();}});
document.addEventListener('change',e=>{
 const el=e.target;
 if(el.dataset.source){
  let val=el.value===''?null:(el.dataset.source==='book'&&el.value==='personal'?'personal':Number(el.value));
  if(el.dataset.source==='book'&&sourceDraft.book!==val){sourceDraft.chapters=[];sourceDraft.from=null;sourceDraft.to=null;sourceDraft.kanji_ids=[];materialOpenFilter=null;}
  sourceDraft[el.dataset.source]=val;
  if(el.dataset.source==='book'&&val!=='personal'&&typeof val!=='number'){sourceDraft.from=null;sourceDraft.to=null;}
  if(el.dataset.source==='book'&&val==='personal'){sourceDraft.from=null;sourceDraft.to=null;}
  if(el.dataset.source!=='word')sourceDraft.word=null;
  if(materialType(sourceDraft)==='book'&&['book','from','to'].includes(el.dataset.source)){
   const allowed=new Set(materialKanji(sourceDraft).map(k=>k.id));
   sourceDraft.kanji_ids=(sourceDraft.kanji_ids||[]).filter(id=>allowed.has(id));
   if(sourceDraft.kanji&&!allowed.has(sourceDraft.kanji))sourceDraft.kanji=null;
  }
  render();
 }
 if(el.id==='source-query'){sourceQuery=el.value;sourceDraft.word=null;render();$('#source-query').focus();}
});
document.addEventListener('submit',e=>{if(e.target.id==='recipe-form'){e.preventDefault();saveRecipe();}});
captions.recipes='Черновик для согласования. Компактные карточки, кот из каталога и меню действий. Сохранение — до перезагрузки страницы; запуск тренировок по рецептам не реализован.';
captions['recipe-detail']='Просмотр рецепты. Структура как у редактора: название, материалы, лимиты; материал открывается отдельно.';
captions['recipe-material-view']='Просмотр материала рецепты без редактирования.';
captions['recipe-editor']='Конструктор рецептов. Объединение источников, параметры рецепта и таймер.';
screen=screens.includes(location.hash.slice(1))?location.hash.slice(1):'home';if(['catalog','book','chapter'].includes(screen))go(screen);else render();
