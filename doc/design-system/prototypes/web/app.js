'use strict';
const $ = selector => document.querySelector(selector);
const escapeHTML = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const copy = {
  ru: {app:'Кандзи дэс нэ',home:'Главная',practice:'Тренировка',dictionary:'Мой словарь',word:'Слово',editor:'Новое слово',edit:'Редактирование',catalog:'Учебники',book:'Учебник',chapter:'Глава',back:'Назад',settings:'Настройки',traces:'Следы учёбы',streak:'7 дней подряд',current:'Выбранный рецепт',choose:'Выбрать',mix:'Умный микс',longName:'Кандзи из первых глав учебника и слова для повторения',config:'10 слов · 1 мин на слово',notYet:'Ещё не тренировались',launch:'Ня! Начнём?',newTraining:'Новая тренировка',dictionaries:'Словари / 単語',catalogLabel:'Каталог',catalogDesc:'Учебники, кандзи и ключи',wordsCount:'156 слов',search:'Слово, чтение или перевод',addWord:'Добавить слово',empty:'Здесь будут твои слова',emptyHint:'Добавь первое — и словарь начнёт расти.',noResults:'Пока ничего не нашлось',retryHint:'Попробуй другое слово или чтение.',loading:'Загружаем словарь',loadError:'Не удалось открыть словарь',retry:'Повторить',written:'Написание',reading:'Чтение',translations:'Переводы',translation:'Перевод',addTranslation:'Добавить перевод',save:'Сохранить',create:'Добавить в словарь',cancel:'Отмена',delete:'Удалить',deleteTitle:'Удалить слово?',deleteText:'Эта запись исчезнет из твоего словаря.',unsaved:'Сбросить изменения?',unsavedText:'Все внесённые правки будут потеряны.',leaveUnsaved:'Выйти',discard:'Не сохранять',keepEditing:'Продолжить редактирование',required:'Заполни написание, чтение и хотя бы один перевод.',duplicateLanguage:'Для каждого перевода выбери отдельный язык.',duplicateWord:'Запись с таким написанием и чтением уже есть.',language:'Язык перевода',russian:'Русский',english:'Английский',otherLanguage:'Другой язык…',languageTag:'Код языка, например de или pt-BR',languageError:'Введи корректный код языка.',removeTranslation:'Удалить перевод',personalOrigin:'Добавлено тобой',translationMissing:'Перевод пока не добавлен',kanji:'Кандзи',words:'Слова',radicals:'Ключи',origin:'Источник',hint:'Подсказка',undo:'Отменить',clear:'Очистить',skip:'Пропустить',next:'Далее',readability:'Читаемость',shape:'Форма',strokes:'Черты',pending:'Пока —',correct:'Верно',incorrect:'Исправь',editing:'Продолжай',assisted:'С подсказкой',unavailable:'Недоступно',checkUnavailable:'Проверка сейчас недоступна. Рисунок сохранён.',prompt:'Электроэнцефалография',hintPrompt:'Обведи знак по подсказке',wordPosition:'Слово 1 из 10',result:'Задание завершено',skipped:'Задание пропущено',resultHint:'Можно вернуться на главную.',close:'Закрыть',recipeOptions:'Выбрать рецепт',practiceLabel:'Практика',bookDesc:'Учебник кандзи',chapterOne:'Глава 1',chapterTwo:'Глава 2',chapterThree:'Глава 3',chapterFour:'Глава 4',catalogSearch:'Найти учебник',info:'Подробнее',chapterContents:'Содержание главы',strokeCount:'Количество штрихов',studyKanji:'Знак из учебного материала',statTitle:'Следы учёбы',statInfo:'Здесь собираются твои занятия: самостоятельно, с подсказкой и с исправлениями.',activityInfo:'Каждая клетка — день, когда ты занимался. Продолжим сегодня?',modelInfo:'Настройки проверки и загрузка модели доступны в приложении Android.',bookInfo:'Главы и учебные записи показаны для обсуждения структуры.',chooseHint:'10 слов · без спешки',summary:'Результат',saved:'Сохранено',wordCount:n=>`${n} ${new Intl.PluralRules('ru').select(n)==='one'?'слово':new Intl.PluralRules('ru').select(n)==='few'?'слова':'слов'}`},
  en: {app:'Kanji Desu Ne',home:'Home',practice:'Training',dictionary:'My dictionary',word:'Word',editor:'New word',edit:'Edit word',catalog:'Textbooks',book:'Textbook',chapter:'Chapter',back:'Back',settings:'Settings',traces:'Learning footprints',streak:'7-day streak',current:'Selected recipe',choose:'Choose',mix:'Smart mix',longName:'Kanji from the first textbook chapters and words to review',config:'10 words · 1 min per word',notYet:'No trainings yet',launch:'Meow! Ready?',newTraining:'New training',dictionaries:'Dictionaries / 単語',catalogLabel:'Catalog',catalogDesc:'Textbooks, kanji and radicals',wordsCount:'156 words',search:'Word, reading or translation',addWord:'Add word',empty:'Your words will live here',emptyHint:'Add the first one to start your collection.',noResults:'No words found',retryHint:'Try another word or reading.',loading:'Loading dictionary',loadError:'Could not open dictionary',retry:'Try again',written:'Written form',reading:'Reading',translations:'Translations',translation:'Translation',addTranslation:'Add translation',save:'Save',create:'Add to dictionary',cancel:'Cancel',delete:'Delete',deleteTitle:'Delete word?',deleteText:'This entry will be removed from your dictionary.',unsaved:'Discard changes?',unsavedText:'All edits will be lost.',leaveUnsaved:'Leave',discard:'Discard',keepEditing:'Keep editing',required:'Enter the written form, reading and at least one translation.',duplicateLanguage:'Choose a different language for each translation.',duplicateWord:'An entry with this written form and reading already exists.',language:'Translation language',russian:'Russian',english:'English',otherLanguage:'Another language…',languageTag:'Language tag, e.g. de or pt-BR',languageError:'Enter a valid language tag.',removeTranslation:'Remove translation',personalOrigin:'Added by you',translationMissing:'No translation yet',kanji:'Kanji',words:'Words',radicals:'Radicals',origin:'Source',hint:'Hint',undo:'Undo',clear:'Clear',skip:'Skip',next:'Next',readability:'Reading',shape:'Shape',strokes:'Strokes',pending:'Not yet',correct:'Correct',incorrect:'Try again',editing:'Keep going',assisted:'With a hint',unavailable:'Unavailable',checkUnavailable:'Checking is unavailable. Your drawing is kept.',prompt:'Electroencephalography',hintPrompt:'Trace the character',wordPosition:'Word 1 of 10',result:'Task complete',skipped:'Task skipped',resultHint:'You can return to the home screen.',close:'Close',recipeOptions:'Choose recipe',practiceLabel:'Practice',bookDesc:'Kanji textbook',chapterOne:'Chapter 1',chapterTwo:'Chapter 2',chapterThree:'Chapter 3',chapterFour:'Chapter 4',catalogSearch:'Find a textbook',info:'Details',chapterContents:'Chapter contents',strokeCount:'Stroke count',studyKanji:'Character from the learning material',statTitle:'Learning footprints',statInfo:'Your practice comes together here: on your own, with hints and with corrections.',activityInfo:'Each square is a day you practised. Shall we continue today?',modelInfo:'Checker settings and model download are available in the Android app.',bookInfo:'Chapters and sample entries illustrate the proposed structure.',chooseHint:'10 words · at your own pace',summary:'Result',saved:'Saved',wordCount:n=>`${n} ${n===1?'word':'words'}`}
};
let locale='ru';
const t = key => copy[locale]?.[key] ?? copy.ru[key] ?? key;
Object.assign(copy.ru,{learnedUnit:'изученных слов',chooseCount:n=>`Выбрать (${n})`});
Object.assign(copy.en,{learnedUnit:'words learned',chooseCount:n=>`Choose (${n})`});
Object.assign(copy.ru,{
 practiceComplete:'Итог тренировки',
 practiceCompleteTitle:'Тренировка завершена',
 gradeExcellent:'Отлично',
 gradeSteady:'Хорошо',
 gradeKeepGoing:'Ещё потренируемся',
 gradeExcellentHint:'Почти всё получилось самостоятельно.',
 gradeSteadyHint:'Есть прогресс — можно закрепить сложные слова.',
 gradeKeepGoingHint:'Нормально ошибаться. Повторим ещё раз?',
 statCorrect:'Верно',
 statAssisted:'С подсказкой',
 statSkipped:'Пропуск',
 statTimedOut:'По времени',
 statTime:'Время',
 percentLabel:'Оценка',
 toHome:'На главную',
 demoRecipe:'Умный микс',
 recipeBook:'Рецепты тренировок',
 recipeBookDesc:'Создай и настрой рецепты',
 startTrainingHint:'Начать тренировку',
 unfinishedHint:'Незавершённая тренировка',
 pickRecipe:'Выбери рецепт',
 pickRecipeHint:'Нажми рецепт, чтобы начать тренировку',
 pickEmptyHint:'Сначала создай рецепт в рецептах тренировок.',
 openRecipeBook:'Открыть рецепты тренировок',
 trainingPick:'Выбор рецепта',
 continueLaunch:'Продолжим?',
 wordProgress:(n,m)=>`Слово ${n} из ${m}`,
 startNewTraining:'Начать новую',
 abandonTraining:'Начать новую тренировку?',
 abandonTrainingText:'Незавершённая тренировка будет прервана. Прогресс по ней сохранится как прерванный.',
 unfinishedBadge:'Незавершённая тренировка',
 unfinishedChoice:'Продолжить или начать новую?',
 continueTraining:'Продолжить',
});
Object.assign(copy.en,{
 practiceComplete:'Training summary',
 practiceCompleteTitle:'Training complete',
 gradeExcellent:'Excellent',
 gradeSteady:'Well done',
 gradeKeepGoing:'Keep practising',
 gradeExcellentHint:'Most words were done on your own.',
 gradeSteadyHint:'Solid progress — revisit the harder words.',
 gradeKeepGoingHint:'Mistakes are fine. Shall we try again?',
 statCorrect:'Correct',
 statAssisted:'With a hint',
 statSkipped:'Skipped',
 statTimedOut:'Timed out',
 statTime:'Time',
 percentLabel:'Score',
 toHome:'Back home',
 demoRecipe:'Smart mix',
 recipeBook:'Training recipes',
 recipeBookDesc:'Create and edit recipes',
 startTrainingHint:'Start training',
 unfinishedHint:'Unfinished training',
 pickRecipe:'Choose a recipe',
 pickRecipeHint:'Tap a recipe to start training',
 pickEmptyHint:'Create a recipe in training recipes first.',
 openRecipeBook:'Open training recipes',
 trainingPick:'Choose recipe',
 continueLaunch:'Continue?',
 wordProgress:(n,m)=>`Word ${n} of ${m}`,
 startNewTraining:'Start new',
 abandonTraining:'Start a new training?',
 abandonTrainingText:'The unfinished training will be interrupted. Its progress stays as interrupted.',
 unfinishedBadge:'Unfinished training',
 unfinishedChoice:'Continue or start new?',
 continueTraining:'Continue',
});
const glyphs={settings:'<path d="m9 3-.7 2.4-2 .9L4 5.7 2 9.2l1.7 1.7v2.2L2 14.8l2 3.5 2.3-.6 2 .9L9 21h6l.7-2.4 2-.9 2.3.6 2-3.5-1.7-1.7v-2.2L22 9.2l-2-3.5-2.3.6-2-.9L15 3Z"/><circle cx="12" cy="12" r="3"/>',close:'<path d="m6 6 12 12M6 18 18 6"/>',back:'<path d="m15 5-7 7 7 7M8 12h13"/>',arrow:'<path d="M4 12h16m-7-7 7 7-7 7"/>',chevron:'<path d="m9 5 7 7-7 7"/>',search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',plus:'<path d="M12 4v16M4 12h16"/>',edit:'<path d="m15 4 5 5M4 20l4-1 13-13-4-4L4 15v5Z"/>',more:'<circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/>',hint:'<path d="M8 17c0-4-3-4-3-9a7 7 0 0 1 14 0c0 5-3 5-3 9M8 17h8M9 21h6"/>',undo:'<path d="m8 4-5 5 5 5M3 9h10a6 6 0 0 1 0 12"/>',trash:'<path d="M4 6h16M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7M14 10v7"/>',check:'<path d="m4 12 5 5L20 6"/>',paw:'<ellipse cx="12" cy="16" rx="6" ry="5" fill="currentColor" stroke="none"/><ellipse cx="4" cy="9" rx="2.5" ry="3" fill="currentColor" stroke="none"/><ellipse cx="9" cy="4" rx="2.5" ry="3" fill="currentColor" stroke="none"/><ellipse cx="15" cy="4" rx="2.5" ry="3" fill="currentColor" stroke="none"/><ellipse cx="20" cy="9" rx="2.5" ry="3" fill="currentColor" stroke="none"/>',book:'<path d="M5 4h7v16l-6-2a1 1 0 0 1-.1-.02L5 17.7V4Zm14 0h-7v16l6-2 .1-.02L19 17.7V4Z"/>',dictionary:'<path d="M6 4h11a2 2 0 0 1 2 2v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1ZM9 4v16M12 9h4M12 13h3"/>',kanji:'<path d="M5 5h14v14H5ZM12 8v9M8.5 11.5h7M9.5 11.5 12 17M14.5 11.5 12 17"/>',words:'<path d="M5 6h14M5 12h14M5 18h9"/>','word-stack':'<rect x="6" y="4" width="13" height="15" rx="3"/><path d="M3 8v10a4 4 0 0 0 4 4M10 9h5M10 13h4"/>',timer:'<circle cx="12" cy="14" r="8"/><path d="M12 10v4l3 2M9 2h6M12 2v4M19 6l2 2"/>','no-timer':'<path d="M6 9c-4 0-5 8 0 8 4 0 8-8 12-8 5 0 4 8 0 8-4 0-8-8-12-8Z"/>'};
const icon=(name,cls='svg-icon')=>`<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true">${glyphs[name]||glyphs.chevron}</svg>`;
const iconButton=(name,label,action)=>`<button class="icon-button" aria-label="${escapeHTML(label)}" data-action="${action}">${icon(name)}</button>`;
const header=(title,back='home',actions='')=>`<header class="appbar">${iconButton('back',t('back'),`go:${back}`)}<h2>${escapeHTML(title)}</h2>${actions}</header>`;
const sampleWords=[
 {id:-1,written:'木漏れ日',reading:'こもれび',translations:[{language_tag:'ru',text:'солнечный свет сквозь листву'},{language_tag:'en',text:'sunlight filtering through leaves'}],personal:true},
 {id:-2,written:'図書館',reading:'としょかん',translations:[{language_tag:'ru',text:'библиотека'},{language_tag:'en',text:'library'}],personal:false},
 {id:-3,written:'日本語を勉強しています。',reading:'にほんごをべんきょうしています。',translations:[{language_tag:'ru',text:'Я изучаю японский язык.'},{language_tag:'en',text:'I am studying Japanese.'}],personal:true},
 {id:-4,written:'友達',reading:'ともだち',translations:[{language_tag:'ru',text:'друг; подруга'},{language_tag:'en',text:'friend'}],personal:true},
 {id:-5,written:'山',reading:'やま',translations:[{language_tag:'ru',text:'гора'},{language_tag:'en',text:'mountain'}],personal:false}
];
let words=[...structuredClone(sampleWords.filter(w=>!w.personal)),...PersonalWords.read()],selectedId=-1,wordSource='dictionary',query='',catalogQuery='',bookId=0,chapterId=1,chapterTab='kanji',recipeName=null;
let draft=null,draftOriginal='',editingId=null,lastEditorScreen='dictionary',practiceState='empty',practiceCompleteGrade='excellent',homeTraining='idle',hint=false,selectedCell=0;
const strokes=[[],[],[]];
let modalClose=null;
let selectedWordOrigin=null;
let screen='home';
const screens=['dictionary-kanji','dictionary-translations','dictionary-tags','material-filter','recipe-words','recipe-source','recipe-material-view','settings','checker-settings','recipes','recipe-detail','recipe-editor','training-pick','home','practice','practice-complete','dictionary','word','editor','catalog','book','chapter'];
const names={'dictionary-kanji':'kanji','dictionary-translations':'otherTranslations','dictionary-tags':'dictionaryTags','material-filter':'materials','recipe-words':'wordPool','recipe-source':'addSource','recipe-material-view':'materialType',settings:'settings','checker-settings':'checkerSettings',recipes:'recipes','recipe-detail':'recipeDetail','recipe-editor':'recipeEditor','training-pick':'trainingPick',home:'home',practice:'practice','practice-complete':'practiceComplete',dictionary:'dictionary',word:'word',editor:'editor',catalog:'catalog',book:'book',chapter:'chapter'};
const captions={home:'Черновик: книга рецептов отдельно; незавершённая — статус на кнопке и диалог продолжить/новая. Статистика демонстрационная.',practice:'Предложение: зелёная тема, видимый возврат, статусы со знаком результата. Поле принимает штрихи; состояния проверки выбираются слева и не вычисляются по рисунку.','practice-complete':'Итог тренировки: процент, три уровня оценки и маскот. Состояния оценки переключаются слева; расчёт — training-grade.md.','training-pick':'Черновик: выбор рецепта перед стартом тренировки. Тап по карточке открывает PRACTICE. Требует согласования.',dictionary:'Черновик модуля словаря. Работают поиск, открытие, создание и редактирование записей. Можно переключить пустое состояние, загрузку и ошибку.',word:'Просмотр записи. Японское написание, чтение и переводы разделены по ролям. Записи из каталога доступны для просмотра; пользовательские — для изменения.',editor:'Редактор слов, выражений и предложений. Переводы хранятся по language_tag. Сохранение и подтверждение выхода работают в памяти прототипа.',catalog:'Предложение каталога: учебник как точка входа к главам. Главы и кандзи взяты из базы приложения; для каждой главы показаны первые восемь слов. Обложки условные.',book:'Реальные главы и счётчики из базы приложения. Работают вкладки глав, кандзи и слов. Прогресс обучения не придуман.',chapter:'Кандзи и слова связаны с главами по базе приложения. Все кандзи главы и первые восемь слов; значения счётчиков относятся к полной базе.'};
const wordById=()=>words.find(w=>w.id===selectedId)||window.CATALOG_DATA.flatMap(b=>b.chapters.flatMap(c=>c.words)).find(w=>w.id===selectedId)||words[0];
function translation(word){return word.translations.find(x=>x.language_tag===locale)?.text||word.translations.find(x=>x.language_tag==='ru')?.text||t('translationMissing');}
function openCatalogRoute(route){PersonalWords.write(words,dictionaryTags);if(parent!==window){parent.postMessage({type:'kanji-viewer-navigate',module:'catalog',route},location.origin);return;}location.href='../textbooks/#'+route;}
function go(next){if(['catalog','references','book','chapter'].includes(next)){openCatalogRoute(next==='catalog'?'catalog':next==='references'?'references':next==='book'?'source/2':'chapter/2/1');return;}if(!screens.includes(next))next='home';closeModal();screen=next;location.hash=next;render();$('#app').scrollTop=0;}
function requestGo(next){if(screen==='recipe-editor'&&recipeDirty()){recipeLeave(next);return;}if(['editor','dictionary-translations'].includes(screen)&&!['editor','dictionary-translations'].includes(next)&&draft&&JSON.stringify(draft)!==draftOriginal){showModal(t('unsaved'),`<p>${t('unsavedText')}</p>`,[{label:t('cancel'),fn:closeModal},{label:t('leaveUnsaved'),danger:true,fn:()=>{draft=null;go(next)}}]);}else go(next);}
function webReviewKey(){if(screen==='material-filter')return 'material-filter:'+(materialOpenFilter||'kanji');if(screen==='dictionary-tags')return 'dictionary-tags:'+(dictionaryTagsMode==='manage'?'manage':'filter');if(screen==='recipe-source')return sourceIndex===null?'recipe-source:create':'recipe-source:edit';return screen==='editor'?(editingId?'editor:edit':'editor:create'):screen==='recipe-editor'?(recipeDraft?.id?'recipe-editor:edit':'recipe-editor:create'):screen;}
function applyScreenReview(){
 const state=ScreenReview.status('web',webReviewKey(),locale),en=locale==='en';
 $('#phone').dataset.screenVariant='wip';$('#phone').dataset.approval=state.status;
 $('#view-status').textContent=en?'DESIGN PROTOTYPE':'МАКЕТ ДИЗАЙНА';
 $('#view-badge').textContent=state.status==='approved'?(en?'Approved':'Согласовано'):(en?'Not approved':'Не согласовано');
 $('#view-badge').dataset.approval=state.status;
 $('#review-capture').hidden=true;$('#review-notice').hidden=false;
 $('#review-notice').innerHTML=`<h2>${state.error?(en?'Approval registry unavailable':'Реестр статусов недоступен'):state.status==='approved'?(en?'Approved design':'Согласованный дизайн'):(en?'Screen not approved':'Экран не согласован')}</h2><p>${escapeHTML(state.changes)}</p>`;
 $('#screen-caption').textContent=(en?'Design only. Android code is unchanged. ':'Только дизайн. Android-код не менялся. ')+(captions[screen]||'');
 for(const id of ['large-text','long-text','locale'])$('#'+id).disabled=false;
}
function render(){
 PersonalWords.write(words,dictionaryTags);
 $('#phone').hidden=false;
 $('#phone').dataset.webScreen=screen;
 const catalogSelector=['dictionary','word','dictionary-tags','dictionary-translations','dictionary-kanji','recipe-words'].includes(screen)||screen==='material-filter'||(screen==='recipe-source'&&sourceDraft&&materialType(sourceDraft)==='kanji');
 $('#phone').classList.toggle('native-phone',Boolean(catalogSelector));
 $('#phone').dataset.workingCatalogScreen=String(Boolean(catalogSelector));
 $('#phone').dataset.screenVariant=catalogSelector?'wip':'';
 $('#app').classList.toggle('native-app',Boolean(catalogSelector));
 $('#phone').dataset.recipeScreen=String(['recipes','recipe-detail','recipe-editor','recipe-source','recipe-material-view','recipe-words','training-pick'].includes(screen));
 document.documentElement.lang=locale;$('#app').lang=locale;
 $('#screen-nav').innerHTML=screens.map((id,i)=>`<a class="nav-link ${id===screen?'active':''}" href="#${id}" ${id===screen?'aria-current="page"':''}><span class="nav-number">${String(i+1).padStart(2,'0')}</span>${escapeHTML(t(names[id]))}</a>`).join('');
 $('#screen-title').textContent=t(screen==='editor'&&editingId?'edit':names[screen]);$('#screen-caption').textContent=captions[screen];
 $('#model-state-label').hidden=screen!=='settings';
 $('#practice-state-label').hidden=screen!=='practice';$('#practice-complete-grade-label').hidden=screen!=='practice-complete';$('#dictionary-state-label').hidden=screen!=='dictionary';
 $('#home-state-label').hidden=screen!=='home';
 if(screen==='home'){const homeState=$('#home-state');if(homeState)homeState.value=homeTraining;}
 const views={'dictionary-kanji':()=>dictionaryKanjiView(),'dictionary-translations':()=>dictionaryTranslationsView(),'dictionary-tags':()=>dictionaryTagsView(),'material-filter':renderMaterialFilter,'recipe-words':recipeWordsView,'recipe-source':sourcePicker,'recipe-material-view':recipeMaterialView,settings:settingsView,'checker-settings':checkerSettingsView,recipes:recipesView,'recipe-detail':recipeDetailView,'recipe-editor':recipeEditorView,'training-pick':trainingPickView,home:homeView,practice:practiceView,'practice-complete':practiceCompleteView,dictionary:dictionaryView,word:wordView,editor:editorView,catalog:catalogView,book:bookView,chapter:chapterView};
 $('#app').innerHTML=views[screen]();
 if(screen==='material-filter'||screen==='dictionary'||screen==='dictionary-kanji')CatalogSelection.bindFilterReveal(document.querySelector('.recipe-source-content'));
 if(screen==='practice')setupCanvas();
 if(screen==='editor')resizeWrittenField();
 applyScreenReview();
}
function homeResumeContext(){
 const recipe=$('#long-text')?.checked?t('longName'):(typeof selectedRecipeName==='function'?selectedRecipeName():t('demoRecipe'));
 const progress=t('wordProgress')(4,20);
 return {recipe,progress};
}
function openUnfinishedChoice(){
 const {recipe,progress}=homeResumeContext();
 showModal(t('unfinishedChoice'),`<p>${escapeHTML(recipe)} · ${escapeHTML(progress)}</p>`,[
  {label:t('startNewTraining'),fn:()=>{homeTraining='idle';const sel=$('#home-state');if(sel)sel.value='idle';closeModal();go('training-pick');}},
  {label:t('continueTraining'),primary:true,fn:()=>{closeModal();go('practice');}},
 ]);
}
function homeLaunchBlock(){
 const {recipe,progress}=homeResumeContext();
 if(homeTraining==='unfinished'){
  const label=`${t('launch')}. ${t('unfinishedHint')}`;
  return `<section class="launch-block" aria-label="${t('newTraining')}"><button class="launch launch-with-unfinished" data-action="unfinished-choice" aria-label="${escapeHTML(label)}"><span class="launch-copy"><strong>${t('launch')}</strong><small class="launch-unfinished">${escapeHTML(t('unfinishedHint'))}</small></span><img class="cat" src="assets/mascot.png" alt="" aria-hidden="true">${icon('arrow')}</button></section>`;
 }
 return `<section class="launch-block" aria-label="${t('newTraining')}"><button class="launch" data-action="go:training-pick" aria-label="${escapeHTML(t('launch')+'. '+t('startTrainingHint'))}"><span><strong>${t('launch')}</strong><small>${t('startTrainingHint')}</small></span><img class="cat" src="assets/mascot.png" alt="" aria-hidden="true">${icon('arrow')}</button></section>`;
}
function homeView(){
 const heat=Array.from({length:42},(_,i)=>`<i style="--heat:${[.12,.35,.2,.65,.4,.8,.25][(i*5+i%3)%7]}"></i>`).join('');
 return `<div class="home"><header class="appbar"><h2>${t('app')}</h2><button class="icon-button" data-action="settings" aria-label="${t('settings')}"><img class="settings-art" src="assets/settings.png" alt=""></button></header><div class="stats-row"><button class="stat-card" data-action="stats" aria-label="${t('traces')}: 1248 ${t('learnedUnit')}"><span class="stat-title">${t('traces')}</span>${icon('paw','paw')}<div class="ring"><strong>1248</strong></div><span class="stat-unit">${t('learnedUnit')}</span></button><button class="stat-card" data-action="activity" aria-label="${t('streak')}"><span class="stat-title"><span class="flame" aria-hidden="true">♨</span> ${t('streak')}</span><div class="heatmap" aria-hidden="true">${heat}</div></button></div>${homeLaunchBlock()}<section class="home-entry" aria-label="${t('recipeBook')}"><button class="navigation-row" data-action="go:recipes"><img src="../../assets/stationery/training-recipes.png" alt=""><span><strong>${t('recipeBook')}</strong><small>${t('recipeBookDesc')}</small></span></button></section><div class="section-heading"><h3>${t('dictionaries')}</h3></div><div class="vocabulary"><button class="navigation-row" data-action="go:catalog"><img src="../../assets/stationery/textbook.png" alt=""><span><strong>${t('catalog')}</strong><small>${t('textbookCount')(TC.sources.length)}</small></span></button><button class="navigation-row" data-action="go:references"><img src="../../assets/stationery/inkstone.png" alt=""><span><strong>${t('references')}</strong><small>${t('referencesDesc')}</small></span></button><button class="navigation-row" data-action="go:dictionary"><img src="../../assets/stationery/notebook.png" alt=""><span><strong>${t('dictionary')}</strong><small>${t('wordsCount')}</small></span></button></div></div>`;
}
function wordRows(list){return list.map(w=>CatalogSelection.wordRow(w,{gloss:translation(w),html:escapeHTML})).join('');}
function resizeWrittenField(){const field=document.querySelector('[data-auto-grow-written]');if(!field)return;field.style.height='auto';field.style.height=field.scrollHeight+'px';}
function emptyView(title,body='',image=true){return `<div class="empty-state">${image?'<img src="assets/magnify.png" alt="">':''}<h3>${title}</h3>${body?`<p>${body}</p>`:''}</div>`;}
function dictionaryView(){return dictionaryTaggedView();}
function wordView(){const w=wordById();if(!w)return header(t('word'),'dictionary')+emptyView(t('empty'));const actions=w.personal?`<span class="dictionary-word-menu-wrap"><button type="button" class="icon-button" data-action="word-menu" aria-label="${t('wordActions')}" aria-haspopup="menu" aria-expanded="false" aria-controls="dictionary-word-menu">${icon('more')}</button></span>`:'';return `<div class="recipe-editor-screen dictionary-word-screen">${header(t('word'),wordSource,actions)}<div class="recipe-content recipe-source-content">${dictionaryWordDetails(w)}${w.personal?'':`<p class="origin">${escapeHTML(selectedWordOrigin?.title||books[bookId].title)} · ${t('chapter')} ${selectedWordOrigin?.chapter||chapterId}</p>`}</div>${w.personal?`<div class="recipe-editor-footer"><button type="button" class="recipe-save-fab" data-action="edit">${icon('edit')}${t('edit')}</button></div>`:''}</div>`;}
function initDraft(id=null){editingId=id;const w=words.find(w=>w.id===id);draft=w?structuredClone(w):{written:'',reading:'',translations:[{language_tag:locale,text:''}],personal:true,tag_ids:[]};draft.tag_ids=draft.tag_ids||[];draftOriginal=JSON.stringify(draft);lastEditorScreen=screen==='word'?'word':'dictionary';}
function languageOptions(tag){return [...new Set(['ru','en',tag])].map(code=>`<option value="${escapeHTML(code)}" ${code===tag?'selected':''} ${code!==tag&&(code===locale||draft.translations.some(item=>item.language_tag===code))?'disabled':''}>${escapeHTML(code==='ru'?t('russian'):code==='en'?t('english'):new Intl.DisplayNames([locale],{type:'language'}).of(code)||code)}</option>`).join('');}
function editorView(){if(!draft)initDraft();return `<div class="recipe-editor-screen">${header(t(editingId?'edit':'editor'),lastEditorScreen)}<div class="recipe-content recipe-source-content"><form class="editor dictionary-word-editor" id="word-form" novalidate><label class="field">${t('written')}<textarea name="written" class="auto-grow-written" data-auto-grow-written rows="1" lang="ja" placeholder="木漏れ日" required>${escapeHTML(draft.written)}</textarea></label><label class="field">${t('reading')}<input name="reading" lang="ja" value="${escapeHTML(draft.reading)}" placeholder="こもれび" required></label>${dictionaryTranslationFields()}${dictionaryEditorTags()}<div id="form-error" class="form-error" role="alert"></div></form></div><div class="recipe-editor-footer"><button class="recipe-save-fab" type="submit" form="word-form">${icon(editingId?'check':'plus')}${t(editingId?'save':'create')}</button></div></div>`;}
function saveWord(next=editingId?'word':'dictionary'){
 const invalid=!draft.written.trim()||!draft.reading.trim()||!draft.translations.length||draft.translations.some(x=>!x.text.trim());
 let error=invalid?t('required'):new Set(draft.translations.map(x=>x.language_tag.toLowerCase())).size!==draft.translations.length?t('duplicateLanguage'):words.some(w=>w.id!==editingId&&w.written===draft.written.trim()&&w.reading===draft.reading.trim())?t('duplicateWord'):'';
 if(error){closeModal();if(screen!=='editor'){go('editor')}$('#form-error').textContent=error;$('#form-error').scrollIntoView({block:'nearest'});return false;}
 dictionaryTags.push(...(draft.pending_tags||[]));
 const w={...structuredClone(draft),id:editingId||Date.now(),written:draft.written.trim(),reading:draft.reading.trim(),personal:true};delete w.pending_tags;
 if(editingId)words=words.map(x=>x.id===editingId?w:x);else words.unshift(w);
 selectedId=w.id;wordSource='dictionary';draft=null;query='';$('#dictionary-state').value='ready';go(next);return true;
}
function practiceView(){
 const complete=['correct','assisted'].includes(practiceState),error=practiceState==='incorrect';
 const status=complete?t('correct'):error?t('incorrect'):practiceState==='editing'?t('editing'):t('pending');
 const token=complete?'success':error?'failure':'';
 return `<div class="practice">${header(t('practiceLabel'),'home',iconButton('more',t('settings'),'practice-info'))}<div class="practice-content"><div class="practice-meta"><span>${t('wordPosition')}</span></div><h3 class="practice-prompt">${t('prompt')}</h3><div class="cells" aria-label="${t('kanji')}">${['脳','波','検','査'].map((c,i)=>`<button class="kanji-cell ${selectedCell===i?'selected':''} ${i<3&&complete?'correct':i===selectedCell&&error?'incorrect':''}" ${i===3?'disabled':''} data-action="cell:${i}" aria-label="${locale==='ru'?'Знак':'Character'} ${i+1}${selectedCell===i?', '+status:''}" aria-pressed="${selectedCell===i}">${i===3?c:((i<3&&complete)||(i===selectedCell&&error)?c:'')}${i<3&&complete?'<small aria-hidden="true">○</small>':i===selectedCell&&error?'<small aria-hidden="true">×</small>':''}</button>`).join('')}</div><div class="checkers">${[['readability',complete?'○ '+t('correct'):error?'× '+t('incorrect'):'—'],['shape',complete?'○ 96%':error?'× 68%':'—'],['strokes',String(complete?[11,8,12][selectedCell]:(strokes[selectedCell]?.length||0))]].map(([k,v])=>`<button class="checker ${token}" data-action="check:${k}"><span>${t(k)}</span><strong>${v}</strong></button>`).join('')}</div><div class="drawing-wrap"><svg class="drawing-grid" viewBox="0 0 320 320" aria-hidden="true"><path d="M160 0v320M0 160h320"/><path class="diagonal" d="m0 0 320 320M320 0 0 320"/></svg>${hint||complete?`<img class="hint-reference ${complete?'sample-answer':''}" src="assets/${['08133','06ce2','0691c'][selectedCell]}.svg" alt="">`:''}<canvas id="ink" aria-label="${locale==='ru'?'Поле для письма. Рисуйте мышью или пальцем.':'Drawing pad. Use a pointer or touch.'}"></canvas></div><div class="tools"><button class="tool ${hint?'active':''}" data-action="hint" aria-pressed="${hint}">${icon('hint')}${t('hint')}</button><button class="tool" data-action="undo" ${!strokes[selectedCell]?.length?'disabled':''}>${icon('undo')}${t('undo')}</button><button class="tool" data-action="clear" ${!strokes[selectedCell]?.length?'disabled':''}>${icon('trash')}${t('clear')}</button></div><p class="practice-feedback ${error?'error':''}" aria-live="polite">${practiceState==='unavailable'?t('checkUnavailable'):hint?t('hintPrompt'):practiceState==='assisted'?t('assisted'):error?t('incorrect'):complete?t('correct'):''}</p></div><div class="practice-actions"><button class="text-button" data-action="skip">${t('skip')}</button><button class="primary-button" data-action="finish" ${complete?'':'disabled'}>${t('next')}${icon('arrow')}</button></div></div>`;
}
function practiceCompleteDemo(grade){
 const demos={
  excellent:{percent:92,correct:8,assisted:1,skipped:0,timedOut:1,elapsedSec:412},
  steady:{percent:68,correct:5,assisted:3,skipped:1,timedOut:1,elapsedSec:480},
  keep_going:{percent:38,correct:2,assisted:2,skipped:4,timedOut:2,elapsedSec:510},
 };
 return demos[grade]||demos.excellent;
}
function practiceCompleteView(){
 const grade=practiceCompleteGrade;
 const demo=practiceCompleteDemo(grade);
 const title=grade==='excellent'?t('gradeExcellent'):grade==='steady'?t('gradeSteady'):t('gradeKeepGoing');
 const hint=grade==='excellent'?t('gradeExcellentHint'):grade==='steady'?t('gradeSteadyHint'):t('gradeKeepGoingHint');
 const mascot=grade==='excellent'?'mascot_sticker_completed_sheet.png':grade==='steady'?'mascot_sticker_encourage.png':'mascot_sticker_jp_shodo_retry.png';
 const mins=Math.floor(demo.elapsedSec/60), secs=demo.elapsedSec%60;
 const timeLabel=mins?`${mins}:${String(secs).padStart(2,'0')}`:`0:${String(secs).padStart(2,'0')}`;
 return `<div class="practice-complete">
  <header class="appbar"><h2>${t('practiceCompleteTitle')}</h2></header>
  <div class="practice-complete-body">
   <img class="practice-complete-mascot" src="../../assets/mascot/${mascot}" alt="" width="168" height="168">
   <p class="practice-complete-recipe">${escapeHTML(t('demoRecipe'))}</p>
   <p class="practice-complete-grade" data-grade="${grade}">${escapeHTML(title)}</p>
   <p class="practice-complete-percent" aria-label="${t('percentLabel')}"><strong>${demo.percent}</strong><span>%</span></p>
   <p class="practice-complete-hint">${escapeHTML(hint)}</p>
   <ul class="practice-complete-stats" aria-label="${t('summary')}">
    <li><span>${t('statCorrect')}</span><strong>${demo.correct}</strong></li>
    <li><span>${t('statAssisted')}</span><strong>${demo.assisted}</strong></li>
    <li><span>${t('statSkipped')}</span><strong>${demo.skipped}</strong></li>
    <li><span>${t('statTimedOut')}</span><strong>${demo.timedOut}</strong></li>
    <li class="practice-complete-time"><span>${t('statTime')}</span><strong>${timeLabel}</strong></li>
   </ul>
  </div>
  <div class="practice-complete-actions"><button type="button" class="primary-button" data-action="go:home">${t('toHome')}</button></div>
 </div>`;
}
function setupCanvas(){const canvas=$('#ink');const rect=canvas.getBoundingClientRect();const ratio=devicePixelRatio||1;canvas.width=rect.width*ratio;canvas.height=rect.height*ratio;const ctx=canvas.getContext('2d');ctx.scale(ratio,ratio);ctx.lineCap='round';ctx.lineJoin='round';ctx.lineWidth=3;const ink=getComputedStyle($('#phone')).getPropertyValue('--ink');ctx.strokeStyle=ink;const draw=()=>{ctx.clearRect(0,0,rect.width,rect.height);for(const stroke of strokes[selectedCell]||[]){ctx.beginPath();stroke.forEach(([x,y],i)=>{if(i)ctx.lineTo(x*rect.width,y*rect.height);else ctx.moveTo(x*rect.width,y*rect.height)});ctx.stroke()}};draw();let active=null;const point=e=>{const r=canvas.getBoundingClientRect();return [(e.clientX-r.left)/r.width,(e.clientY-r.top)/r.height]};canvas.onpointerdown=e=>{canvas.setPointerCapture(e.pointerId);active=[point(e)];strokes[selectedCell].push(active);};canvas.onpointermove=e=>{if(active){active.push(point(e));draw()}};const finish=()=>{if(active){active=null;if(['correct','assisted','incorrect','empty'].includes(practiceState))practiceState='editing';$('#practice-state').value=practiceState;const scroll=$('#app').scrollTop;render();$('#app').scrollTop=scroll;}};canvas.onpointerup=finish;canvas.onpointercancel=finish;}
const books=window.CATALOG_DATA.map((b,i)=>({...b,subtitle:b.kanjiCount+' '+(locale==='ru'?'кандзи':'kanji'),color:i?'blue':''}));
let bookTab='chapters';
const currentChapter=()=>books[bookId].chapters.find(c=>c.number===chapterId)||books[bookId].chapters[0];
function cover(book){return `<span class="book-cover ${book.color}" aria-hidden="true"><small>${book.kanjiCount} KANJI</small><span lang="ja">${book.color?'とびら':'漢字'}</span></span>`;}
function catalogView(){const list=books.map((b,i)=>({...b,index:i})).filter(b=>b.title.toLowerCase().includes(catalogQuery.toLowerCase()));return header(t('catalogLabel'))+`<div class="search">${icon('search')}<input id="catalog-query" type="search" placeholder="${t('catalogSearch')}" aria-label="${t('catalogSearch')}" value="${escapeHTML(catalogQuery)}"></div><div class="book-list" id="catalog-results">${list.map(b=>`<button class="book-card" data-action="book:${b.index}">${cover(b)}<span><h3>${b.title}</h3><p>${b.chapters.length} ${locale==='ru'?'глав':'chapters'} · ${b.kanjiCount} ${t('kanji').toLowerCase()}</p><span class="small-chip">${t('bookDesc')}</span></span></button>`).join('')||emptyView(t('noResults'),t('retryHint'))}</div>`;}
function kanjiGrid(items){return `<div class="kanji-grid">${items.map(k=>`<button data-action="kanji:${k.symbol}"><strong lang="ja">${k.symbol}</strong><small>${escapeHTML(k.meanings[locale]||k.meanings.ru||t('translationMissing'))}</small></button>`).join('')}</div>`;}
function bookView(){const b=books[bookId];const chapterRows=b.chapters.map(c=>`<button class="chapter-row" data-action="chapter:${c.number}"><span class="chapter-number">${c.number}</span><span style="flex:1"><strong>${t('chapter')} ${c.number}</strong><small>${t('kanji')}: ${c.kanji.length} · ${t('words').toLowerCase()}: ${c.wordCount}</small></span>${icon('chevron')}</button>`).join('');const tabs=`<div class="tabs">${['chapters','kanji','words'].map(tab=>`<button class="tab ${bookTab===tab?'active':''}" role="tab" aria-selected="${bookTab===tab}" data-action="book-tab:${tab}">${tab==='chapters'?(locale==='ru'?'Главы':'Chapters'):t(tab)}</button>`).join('')}</div>`;return header(t('book'),'catalog')+`<div class="content"><div class="book-heading">${cover(b)}<div><h3>${b.title}</h3><p class="body-copy">${b.kanjiCount} ${t('kanji').toLowerCase()}</p></div></div></div>`+tabs+(bookTab==='chapters'?`<div class="content chapter-list">${chapterRows}</div>`:bookTab==='kanji'?kanjiGrid(b.chapters.flatMap(c=>c.kanji)):`<div class="word-list">${wordRows([...new Map(b.chapters.flatMap(c=>c.words).map(w=>[w.id,w])).values()])}</div>`);}
function chapterView(){const c=currentChapter();return header(`${t('chapter')} ${c.number}`,'book')+`<p class="list-count">${books[bookId].title}</p><div class="tabs">${['kanji','words'].map(tab=>`<button class="tab ${chapterTab===tab?'active':''}" role="tab" aria-selected="${chapterTab===tab}" data-action="tab:${tab}">${t(tab)} · ${tab==='kanji'?c.kanji.length:c.wordCount}</button>`).join('')}</div>`+(chapterTab==='words'?`<div class="word-list">${wordRows(c.words)}</div>`:kanjiGrid(c.kanji));}
function showModal(title,body,actions=[]){closeModal();const previous=document.activeElement;$('#modal-root').innerHTML=`<div class="modal-backdrop"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="dialog-title"><h3 id="dialog-title">${escapeHTML(title)}</h3>${body}<div class="modal-actions">${actions.map((a,i)=>`<button data-modal-action="${i}" class="${a.danger?'danger':a.primary?'primary':''}">${escapeHTML(a.label)}</button>`).join('')}</div></section></div>`;const root=$('#modal-root');root.querySelectorAll('[data-modal-action]').forEach(b=>b.onclick=()=>actions[Number(b.dataset.modalAction)].fn());const key=e=>{if(e.key==='Escape'){closeModal();return;}if(e.key==='Tab'){const items=[...root.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]')];const first=items[0],last=items.at(-1);if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus()}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus()}}};document.addEventListener('keydown',key);modalClose=()=>{document.removeEventListener('keydown',key);if(previous?.isConnected)previous.focus()};root.querySelector('button,input,select,textarea')?.focus();}
function closeModal(){const close=modalClose;modalClose=null;$('#modal-root').innerHTML='';close?.();}
function infoModal(title,text){showModal(title,`<p>${escapeHTML(text)}</p>`,[{label:t('close'),fn:closeModal}]);}
document.addEventListener('click',e=>{
 const nav=e.target.closest('.nav-link,.brand');if(nav){e.preventDefault();requestGo(nav.hash.slice(1));return;}
 const button=e.target.closest('[data-action]');if(!button||button.disabled)return;const [action,value]=button.dataset.action.split(':');
 if(action==='go')requestGo(value);
 else if(action==='settings'){settingsReturn=screen;go('settings');}
 else if(action==='practice-info'){settingsReturn='practice';go('checker-settings');}
 else if(action==='stats')infoModal(t('statTitle'),t('statInfo'));
 else if(action==='activity')infoModal(t('streak'),t('activityInfo'));
 else if(action==='choose')requestGo('recipes');
 else if(action==='open-recipe-book')requestGo('recipes');
 else if(action==='unfinished-choice')openUnfinishedChoice();

 else if(action==='word'){selectedId=Number(value);wordSource=['chapter','book'].includes(screen)?screen:'dictionary';selectedWordOrigin=wordSource!=='dictionary'?{title:books[bookId].title,chapter:chapterId}:null;go('word');}
 else if(action==='new'){initDraft();go('editor');}
 else if(action==='edit'){initDraft(selectedId);go('editor');}
 else if(action==='delete'){showModal(t('deleteTitle'),`<p>${escapeHTML(wordById().written)}<br>${t('deleteText')}</p>`,[{label:t('cancel'),fn:closeModal},{label:t('delete'),danger:true,fn:()=>{words=words.filter(w=>w.id!==selectedId);go('dictionary')}}]);}
 else if(action==='retry'){$('#dictionary-state').value='ready';render();}
 else if(action==='add-translation'){const used=[locale,...draft.translations.map(x=>x.language_tag)],language=['ru','en'].find(code=>!used.includes(code));if(!language)return;draft.translations.push({language_tag:language,text:''});render();document.querySelector('#translations .dictionary-additional-translation:last-child textarea')?.focus();}
 else if(action==='remove-translation'){draft.translations.splice(Number(value),1);render();}
 else if(action==='hint'){hint=!hint;render();}
 else if(action==='undo'){strokes[selectedCell].pop();practiceState=strokes[selectedCell].length?'editing':'empty';$('#practice-state').value=practiceState;render();}
 else if(action==='clear'){strokes[selectedCell]=[];practiceState='empty';$('#practice-state').value=practiceState;render();}
 else if(action==='cell'){selectedCell=Number(value);render();}
 else if(action==='check'){infoModal(t(value),`${value==='strokes'?t('strokeCount'):t('info')}: ${['correct','assisted'].includes(practiceState)?t('correct'):practiceState==='incorrect'?t('incorrect'):t('pending')}`);}
 else if(action==='skip'||action==='finish'){showModal(t(action==='skip'?'skipped':'result'),`<p>${t('resultHint')}</p>`,[{label:t('home'),fn:()=>go('home')},{label:t('close'),fn:closeModal}]);}
 else if(action==='book'){bookId=Number(value);bookTab='chapters';chapterId=1;go('book');}
 else if(action==='chapter'){chapterId=Number(value);chapterTab='kanji';go('chapter');}
 else if(action==='book-tab'){bookTab=value;render();}
 else if(action==='tab'){chapterTab=value;render();}
 else if(action==='kanji'){const k=TC.kanji.find(k=>k.id===Number(value)||k.symbol===value);if(k)openCatalogRoute(`kanji/${k.id}/${wordById()?.personal?-1:0}`);}
});
document.addEventListener('input',e=>{
 if(e.target.id==='dictionary-query'){query=e.target.value;const list=words.filter(w=>[w.written,w.reading,...w.translations.map(x=>x.text)].join(' ').toLowerCase().includes(query.toLowerCase()));$('#dictionary-results').innerHTML=list.length?`<div class="word-list">${wordRows(list)}</div>`:emptyView(t('noResults'),t('retryHint'));}
 if(e.target.id==='catalog-query'){catalogQuery=e.target.value;const position=e.target.selectionStart;render();$('#catalog-query').focus();try{$('#catalog-query').setSelectionRange(position,position)}catch{}}
 if(e.target.name==='written'&&draft){draft.written=e.target.value;resizeWrittenField();}
 if(e.target.name==='reading'&&draft)draft.reading=e.target.value;
 if(e.target.hasAttribute('data-current-translation')&&draft){let item=draft.translations.find(x=>x.language_tag===locale);if(!item){item={language_tag:locale,text:''};draft.translations.push(item);}item.text=e.target.value;}
 if(e.target.dataset.translation!==undefined&&draft)draft.translations[Number(e.target.dataset.translation)].text=e.target.value;
});
document.addEventListener('change',e=>{
 const target=e.target;
 if(target.id==='theme'){$('#phone').dataset.theme=target.value;applyScreenReview();if(screen==='practice'&&!$('#phone').hidden)setupCanvas();}
 else if(target.id==='width'){$('#phone').style.width=target.value+'px';applyScreenReview();if(screen==='practice'&&!$('#phone').hidden)setupCanvas();}
 else if(target.id==='large-text'){$('#phone').classList.toggle('large-text',target.checked);if(screen==='practice')setupCanvas();}
 else if(target.id==='locale'){locale=target.value;render();}
 else if(target.id==='long-text'||target.id==='dictionary-state')render();
 else if(target.id==='home-state'){homeTraining=target.value==='resume'||target.value==='badge'?'unfinished':target.value;render();}
 else if(target.id==='practice-state'){practiceState=target.value;hint=practiceState==='assisted';render();}
 else if(target.id==='practice-complete-grade'){practiceCompleteGrade=target.value;render();}
 else if(target.dataset.translationLanguage!==undefined){const i=Number(target.dataset.translationLanguage);if(target.value==='other'){showModal(t('language'),`<label class="field">${t('languageTag')}<input id="language-tag" placeholder="de" autocomplete="off"></label><p class="form-error" id="language-error" role="alert"></p>`,[{label:t('cancel'),fn:()=>{closeModal();render()}},{label:t('choose'),fn:()=>{const raw=$('#language-tag').value.trim();try{if(!raw)throw Error();const tag=Intl.getCanonicalLocales(raw)[0];draft.translations[i].language_tag=tag;closeModal();render()}catch{$('#language-error').textContent=t('languageError')}}}]);}else{draft.translations[i].language_tag=target.value;render();}}
});
document.addEventListener('submit',e=>{if(e.target.id==='word-form'){e.preventDefault();saveWord();}});
window.addEventListener('hashchange',()=>{const next=location.hash.slice(1);if(screens.includes(next)&&next!==screen)requestGo(next);});
