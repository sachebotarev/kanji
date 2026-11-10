'use strict';
// Design fixtures only; no Android settings or recognition services are invoked.
const settingsCopy={
 ru:{strictness:'Строгость проверки',strictness_soft:'Мягкая',strictness_standard:'Стандартная',strictness_strict:'Строгая',strictness_custom:'Ручная настройка',strictnessHint:'Мягкая допускает больше отклонений, строгая требует более точного письма.',algorithmParameters:'Параметры алгоритмов',checkerSettings:'Калибровка формы кандзи',checks:'Калибровка формы кандзи',checksHint:'Пороги совпадения, допуски трансформаций и компенсаций (только develop-сборка)',japaneseModel:'Японская модель',modelReady:'Модель готова',modelMissing:'Модель не загружена',modelLoading:'Загрузка модели…',modelError:'Не удалось загрузить модель',modelHint:'Модель нужна для проверки читаемости.',modelDownload:'Загрузить модель',modelState:'Состояние модели в макете',shapeGroup:'Сходство формы',compensationGroup:'Компенсации',budgetGroup:'Общий бюджет',rulesGroup:'Словарные правила',checksGroup:'Модули проверки',resetSettings:'Сбросить параметры',settingsPreview:'Значения меняются только в макете.',zeroHelp:'0 выключает компенсацию. Нулевой общий бюджет снимает только общее ограничение.',paramMatch:'Порог сходства',paramDistance:'Допустимое расстояние',paramTranslation:'Сдвиг',paramSize:'Размер',paramStretchX:'Растяжение по горизонтали',paramStretchY:'Растяжение по вертикали',paramRotation:'Поворот',paramShear:'Наклон',paramJitter:'Дрожание руки',paramBudget:'Общий бюджет коррекции',paramLength:'Разница длин штрихов',paramConnection:'Соединение штрихов',paramProtrusion:'Выступы штрихов',ruleStructure:'Проверять структуру',ruleOrder:'Проверять порядок',ruleDirection:'Проверять направление',flowGroup:'Ход тренировки',autoAdvance:'Автопереход к следующему слову',hintsGroup:'Подсказки',maxHints:'Максимум подсказок',fullHintDisplay:'Время показа полного знака',dataGroup:'Данные и память',clearHistory:'Очистить историю тренировок',clearHistoryHint:'Удалить все сохранённые тренировки',clearConfirmTitle:'Очистить историю?',clearConfirmMessage:'Все сохранённые тренировки будут удалены безвозвратно.',readabilityCheck:'Читаемость',shapeCheck:'Форма знака',writingCheck:'Порядок и направление черт'},
 en:{strictness:'Checking strictness',strictness_soft:'Gentle',strictness_standard:'Standard',strictness_strict:'Strict',strictness_custom:'Custom settings',strictnessHint:'Gentle allows more variation; strict expects more precise handwriting.',algorithmParameters:'Algorithm parameters',checkerSettings:'Kanji shape calibration',checks:'Kanji shape calibration',checksHint:'Match thresholds, transform tolerances and compensations (develop build only)',japaneseModel:'Japanese model',modelReady:'Model ready',modelMissing:'Model not downloaded',modelLoading:'Downloading model…',modelError:'Could not download model',modelHint:'The model is needed for readability checking.',modelDownload:'Download model',modelState:'Model state in the prototype',shapeGroup:'Shape similarity',compensationGroup:'Compensations',budgetGroup:'Combined budget',rulesGroup:'Dictionary rules',checksGroup:'Verification modules',resetSettings:'Reset parameters',settingsPreview:'Values change only in this prototype.',zeroHelp:'0 disables a compensation. Zero total budget removes only the combined limit.',paramMatch:'Similarity threshold',paramDistance:'Distance tolerance',paramTranslation:'Translation',paramSize:'Size',paramStretchX:'Horizontal stretch',paramStretchY:'Vertical stretch',paramRotation:'Rotation',paramShear:'Shear',paramJitter:'Hand jitter',paramBudget:'Combined correction budget',paramLength:'Stroke length difference',paramConnection:'Stroke connections',paramProtrusion:'Stroke protrusions',ruleStructure:'Check structure',ruleOrder:'Check order',ruleDirection:'Check direction',flowGroup:'Practice flow',autoAdvance:'Auto-advance to next word',hintsGroup:'Hints',maxHints:'Maximum hints',fullHintDisplay:'Full hint display time',dataGroup:'Data and storage',clearHistory:'Clear training history',clearHistoryHint:'Delete all saved trainings',clearConfirmTitle:'Clear history?',clearConfirmMessage:'All saved trainings will be permanently deleted.',readabilityCheck:'Readability',shapeCheck:'Character shape',writingCheck:'Stroke order and direction'}
};
for(const lang of Object.keys(settingsCopy))Object.assign(copy[lang],settingsCopy[lang]);
let settingsReturn='home',modelState='ready';
const parameterGroups={shapeGroup:[['match','paramMatch',72,1,100,1,'%'],['distance','paramDistance',5,1,15,1,'%']],compensationGroup:[['translation','paramTranslation',12,0,100,1,'%'],['size','paramSize',30,0,200,1,'%'],['stretchX','paramStretchX',15,0,100,1,'%'],['stretchY','paramStretchY',15,0,100,1,'%'],['rotation','paramRotation',8,0,20,1,'°'],['shear','paramShear',8,0,20,1,'°'],['jitter','paramJitter',.5,0,5,.1,'%']],budgetGroup:[['budget','paramBudget',35,0,100,1,'%']],rulesGroup:[['length','paramLength',5,0,30,1,'%'],['connection','paramConnection',5,0,30,1,'%'],['protrusion','paramProtrusion',3,0,30,1,'%']]};
const defaultParameters=Object.fromEntries(Object.values(parameterGroups).flat().map(([id,,value])=>[id,value]));
const strictnessPresets={soft:{...defaultParameters,match:64,distance:8,translation:18,size:40,rotation:12,budget:45},standard:{...defaultParameters},strict:{...defaultParameters,match:84,distance:3,translation:8,size:20,rotation:4,budget:20}};
let parameterValues={...defaultParameters},strictnessPreset='standard';
let enabledChecks={readability:true,shape:true,strokes:true,ruleStructure:true,ruleOrder:false,ruleDirection:false};
let flowSettings={autoAdvance:false,maxHints:3,fullHintDisplay:1500};
function settingsView(){
  const key={ready:'modelReady',missing:'modelMissing',loading:'modelLoading',error:'modelError'}[modelState];
  return header(t('settings'),'home')+
    `<div class="settings-content">
      <section class="settings-panel">
        <h3>${t('flowGroup')}</h3>
        <label class="settings-toggle">
          <span>${t('autoAdvance')}</span>
          <input type="checkbox" role="switch" data-flow="autoAdvance" ${flowSettings.autoAdvance?'checked':''}>
        </label>
      </section>
      <section class="settings-panel">
        <h3>${t('hintsGroup')}</h3>
        <div class="settings-parameter">
          <label for="flow-max-hints">${t('maxHints')}</label>
          <span class="settings-value"><input id="flow-max-hints" type="number" min="1" max="12" step="1" value="${flowSettings.maxHints}" data-flow="maxHints"></span>
          <input class="settings-slider" type="range" min="1" max="12" step="1" value="${flowSettings.maxHints}" data-flow="maxHints">
        </div>
        <div class="settings-parameter">
          <label for="flow-full-hint">${t('fullHintDisplay')}</label>
          <span class="settings-value"><input id="flow-full-hint" type="number" min="200" max="3000" step="50" value="${flowSettings.fullHintDisplay}" data-flow="fullHintDisplay"><span>мс</span></span>
          <input class="settings-slider" type="range" min="200" max="3000" step="50" value="${flowSettings.fullHintDisplay}" data-flow="fullHintDisplay">
        </div>
      </section>
      <section class="settings-panel">
        <h3>${t('checksGroup')}</h3>
        <label class="settings-toggle"><span>${t('readabilityCheck')}</span><input type="checkbox" role="switch" data-setting-toggle="readability" ${enabledChecks.readability?'checked':''}></label>
        <div class="model-row" style="margin: 8px 0;">
          <p class="model-status ${modelState==='error'?'is-error':''}" role="status">${modelState==='ready'?'✓ ':modelState==='error'?'! ':''}${t(key)}</p>
          ${modelState==='ready'?'':`<button class="tonal-button" data-settings="model" style="margin-top:4px;">${t(modelState==='error'?'retry':'modelDownload')}</button>`}
        </div>
        <label class="settings-toggle"><span>${t('writingCheck')}</span><input type="checkbox" role="switch" data-setting-toggle="strokes" ${enabledChecks.strokes?'checked':''}></label>
        <label class="settings-toggle"><span>${t('shapeCheck')}</span><input type="checkbox" role="switch" data-setting-toggle="shape" ${enabledChecks.shape?'checked':''}></label>
      </section>
      <section class="settings-panel">
        <h3>${t('calibrationGroup')}</h3>
        <button class="settings-entry" data-action="go:checker-settings">${icon('check')}<span><strong>${t('checks')}</strong><small>${t('checksHint')}</small></span>${icon('chevron')}</button>
      </section>
      <section class="settings-panel">
        <h3>${t('dataGroup')}</h3>
        <button class="settings-entry" data-settings="clear-history"><span><strong>${t('clearHistory')}</strong><small>${t('clearHistoryHint')}</small></span></button>
      </section>
    </div>`;
}
function checkerToggle(key){return `<label class="settings-toggle"><span>${t(key)}</span><input type="checkbox" role="switch" data-setting-toggle="${key}" ${enabledChecks[key]?'checked':''}></label>`;}
function checkerParameter([id,label,,min,max,step,unit]){const value=parameterValues[id];return `<div class="settings-parameter"><label for="parameter-${id}">${t(label)}</label><span class="settings-value"><input id="parameter-${id}" type="number" inputmode="decimal" min="${min}" max="${max}" step="${step}" value="${value}" data-parameter="${id}" aria-label="${t(label)}"><span>${unit}</span></span><input class="settings-slider" type="range" min="${min}" max="${max}" step="${step}" value="${value}" data-parameter-slider="${id}" aria-label="${t(label)}" aria-valuetext="${value}${unit}"></div>`;}
function checkerSettingsView(){
  return header(t('checkerSettings'),settingsReturn==='practice'?'practice':'settings')+
    `<div class="settings-content">
      <fieldset class="settings-panel settings-presets"><legend>${t('strictness')}</legend>${Object.keys(strictnessPresets).map(id=>`<label><input type="radio" name="strictness" value="${id}" data-strictness ${strictnessPreset===id?'checked':''}><span>${t('strictness_'+id)}</span></label>`).join('')}<p>${t('strictnessHint')}</p><p data-strictness-status role="status" ${strictnessPreset==='custom'?'':'hidden'}>${t('strictness_'+strictnessPreset)}</p></fieldset>
      <details class="settings-panel settings-algorithms" open><summary>${t('algorithmParameters')}</summary>${Object.entries(parameterGroups).map(([name,items])=>`<section class="settings-parameter-group"><h3>${t(name)}</h3>${name==='rulesGroup'?checkerToggle('ruleStructure'):''}${items.map(checkerParameter).join('')}${name==='compensationGroup'||name==='budgetGroup'?`<p class="settings-note">${t('zeroHelp')}</p>`:''}</section>`).join('')}</details>
      <p class="settings-note">${t('settingsPreview')}</p>
      <button class="tonal-button" data-settings="reset">${icon('undo')}${t('resetSettings')}</button>
    </div>`;
}
function updateParameter(control){const id=control.dataset.parameter||control.dataset.parameterSlider,value=Number(control.value);if(!control.checkValidity()||!Number.isFinite(value)){control.value=parameterValues[id];return;}parameterValues[id]=value;strictnessPreset='custom';document.querySelectorAll('[data-strictness]').forEach(radio=>radio.checked=false);const status=document.querySelector('[data-strictness-status]');status.hidden=false;status.textContent=t('strictness_custom');document.querySelectorAll(`[data-parameter="${id}"],[data-parameter-slider="${id}"]`).forEach(input=>{input.value=value;if(input.type==='range'){const unit=Object.values(parameterGroups).flat().find(item=>item[0]===id)[6];input.setAttribute('aria-valuetext',value+unit);}});}
document.addEventListener('click',e=>{const b=e.target.closest('[data-settings]');if(!b)return;if(b.dataset.settings==='model'){modelState='ready';render()}if(b.dataset.settings==='clear-history')showModal(t('clearConfirmTitle'),`<p>${t('clearConfirmMessage')}</p>`,[{label:t('cancel'),fn:closeModal},{label:t('clearHistory'),fn:closeModal}]);if(b.dataset.settings==='reset')showModal(t('resetSettings'),`<p>${t('settingsPreview')}</p>`,[{label:t('cancel'),fn:closeModal},{label:t('resetSettings'),fn:()=>{parameterValues={...defaultParameters};strictnessPreset='standard';closeModal();render()}}]);});
document.addEventListener('input',e=>{if(e.target.dataset.parameterSlider)updateParameter(e.target);if(e.target.dataset.flow){flowSettings[e.target.dataset.flow]=Number(e.target.value);document.querySelectorAll(`[data-flow="${e.target.dataset.flow}"]`).forEach(input=>input.value=e.target.value);}});
document.addEventListener('change',e=>{if(e.target.dataset.settingToggle)enabledChecks[e.target.dataset.settingToggle]=e.target.checked;if(e.target.dataset.flow==='autoAdvance')flowSettings.autoAdvance=e.target.checked;if(e.target.hasAttribute('data-strictness')){strictnessPreset=e.target.value;parameterValues={...strictnessPresets[strictnessPreset]};render();document.querySelector(`[data-strictness][value="${strictnessPreset}"]`)?.focus();}if(e.target.dataset.parameter||e.target.dataset.parameterSlider)updateParameter(e.target);if(e.target.id==='model-state'){modelState=e.target.value;render()}});
captions.settings='Настройки и состояние японской модели. Данные демонстрационные; сетевой загрузки нет.';
captions['checker-settings']='Пресеты строгости и раскрываемые параметры алгоритмов. Значения демонстрационные, не влияют на Android.';
