'use strict';
// Design fixtures only; no Android settings or recognition services are invoked.
const settingsCopy = {
  ru: {
    settings: 'Настройки',
    settingsBack: 'Назад',
    flowGroup: 'Ход тренировки',
    autoAdvanceKanji: 'Автопереход к следующему кандзи',
    helpAutoAdvanceKanji: 'Переход к следующему знаку после верного написания текущего.',
    autoAdvance: 'Автопереход к следующему слову',
    helpAutoAdvance: 'Переход к следующему слову, когда всё текущее слово решено верно.',
    settingOn: 'Вкл',
    settingOff: 'Выкл',
    hintsGroup: 'Подсказки',
    maxHints: 'Количество подсказок',
    helpMaxHints: 'Количество доступных подсказок на знак (0–12). При превышении знак считается неверным.',
    fullHintDisplay: 'Время показа полного знака',
    helpFullHintDisplay: 'Время показа эталона знака перед его скрытием.',
    millisUnit: 'мс',
    checksGroup: 'Модули проверки',
    readabilityCheck: 'Читаемость',
    helpReadability: 'Распознавание рукописного ввода моделью ML Kit.',
    readabilityModelRequired: 'Требуется загрузка японской модели',
    writingCheck: 'Порядок и направление черт',
    helpWriting: 'Проверка порядка и направления начертания черт.',
    shapeCheck: 'Форма знака',
    helpShape: 'Геометрическое сравнение формы знака с эталоном.',
    japaneseModel: 'Японская языковая модель',
    modelLoaded: 'Загружена',
    modelLoading: 'Загружается…',
    modelMissing: 'Не загружена · Скачать',
    calibrationGroup: 'Калибровка формы кандзи',
    checkerSettings: 'Калибровка формы кандзи',
    checksHint: 'Пороги совпадения формы, допуски трансформаций и компенсаций',
    dataGroup: 'Данные и память',
    clearHistory: 'Очистить историю тренировок',
    clearHistoryHint: 'Удалить все сохранённые тренировки',
    clearConfirmTitle: 'Очистить историю?',
    clearConfirmMessage: 'Все сохранённые тренировки будут удалены безвозвратно.',
    clearConfirmAction: 'Очистить',
    strictness: 'Строгость проверки',
    strictness_soft: 'Мягкая',
    strictness_standard: 'Стандартная',
    strictness_strict: 'Строгая',
    strictness_custom: 'Ручная настройка',
    strictnessHint: 'Мягкая допускает больше отклонений, строгая требует более точного письма.',
    algorithmParameters: 'Параметры алгоритмов',
    shapeGroup: 'Сходство формы',
    compensationGroup: 'Компенсации',
    budgetGroup: 'Общий бюджет',
    rulesGroup: 'Словарные правила',
    checkerMenu: 'Меню калибровки',
    resetSettings: 'Сбросить параметры',
    settingsPreview: 'Значения меняются только в макете.',
    zeroHelp: '0 выключает компенсацию. Нулевой общий бюджет снимает только общее ограничение.',
    paramMatch: 'Порог сходства',
    paramDistance: 'Допустимое расстояние',
    paramTranslation: 'Сдвиг',
    paramSize: 'Размер',
    paramStretchX: 'Растяжение по горизонтали',
    paramStretchY: 'Растяжение по вертикали',
    paramRotation: 'Поворот',
    paramShear: 'Наклон',
    paramJitter: 'Дрожание руки',
    paramBudget: 'Общий бюджет коррекции',
    paramLength: 'Разница длин штрихов',
    paramConnection: 'Соединение штрихов',
    paramProtrusion: 'Выступы штрихов',
    ruleStructure: 'Проверять структуру',
    ruleOrder: 'Проверять порядок',
    ruleDirection: 'Проверять направление'
  },
  en: {
    settings: 'Settings',
    settingsBack: 'Back',
    flowGroup: 'Practice flow',
    autoAdvanceKanji: 'Auto-advance to next kanji',
    helpAutoAdvanceKanji: 'Advance to the next character once the current one is correct.',
    autoAdvance: 'Auto-advance to next word',
    helpAutoAdvance: 'Advance to the next word once all its characters are correct.',
    settingOn: 'On',
    settingOff: 'Off',
    hintsGroup: 'Hints',
    maxHints: 'Number of hints',
    helpMaxHints: 'Number of hints allowed per character (0–12). Exceeding this limit fails the character.',
    fullHintDisplay: 'Full hint display time',
    helpFullHintDisplay: 'How long the reference character stays visible before hiding.',
    millisUnit: 'ms',
    checksGroup: 'Verification modules',
    readabilityCheck: 'Readability',
    helpReadability: 'Handwriting recognition via ML Kit Japanese model.',
    readabilityModelRequired: 'Japanese model download required',
    writingCheck: 'Stroke order and direction',
    helpWriting: 'Validates stroke sequence and drawing direction.',
    shapeCheck: 'Character shape',
    helpShape: 'Geometric comparison against the vector reference.',
    japaneseModel: 'Japanese language model',
    modelLoaded: 'Downloaded',
    modelLoading: 'Downloading…',
    modelMissing: 'Not downloaded · Download',
    calibrationGroup: 'Kanji shape calibration',
    checkerSettings: 'Kanji shape calibration',
    checksHint: 'Shape match thresholds, transform tolerances and compensations',
    dataGroup: 'Data and storage',
    clearHistory: 'Clear training history',
    clearHistoryHint: 'Delete all saved trainings',
    clearConfirmTitle: 'Clear history?',
    clearConfirmMessage: 'All saved trainings will be permanently deleted.',
    clearConfirmAction: 'Clear',
    strictness: 'Checking strictness',
    strictness_soft: 'Gentle',
    strictness_standard: 'Standard',
    strictness_strict: 'Strict',
    strictness_custom: 'Custom settings',
    strictnessHint: 'Gentle allows more variation; strict expects more precise handwriting.',
    algorithmParameters: 'Algorithm parameters',
    shapeGroup: 'Shape similarity',
    compensationGroup: 'Compensations',
    budgetGroup: 'Combined budget',
    rulesGroup: 'Dictionary rules',
    checkerMenu: 'Calibration menu',
    resetSettings: 'Reset parameters',
    settingsPreview: 'Values change only in this prototype.',
    zeroHelp: '0 disables a compensation. Zero total budget removes only the combined limit.',
    paramMatch: 'Similarity threshold',
    paramDistance: 'Distance tolerance',
    paramTranslation: 'Translation',
    paramSize: 'Size',
    paramStretchX: 'Horizontal stretch',
    paramStretchY: 'Vertical stretch',
    paramRotation: 'Rotation',
    paramShear: 'Shear',
    paramJitter: 'Hand jitter',
    paramBudget: 'Combined correction budget',
    paramLength: 'Stroke length difference',
    paramConnection: 'Stroke connections',
    paramProtrusion: 'Stroke protrusions',
    ruleStructure: 'Check structure',
    ruleOrder: 'Check order',
    ruleDirection: 'Check direction'
  }
};

for (const lang of Object.keys(settingsCopy)) {
  if (typeof copy !== 'undefined' && copy[lang]) {
    Object.assign(copy[lang], settingsCopy[lang]);
  }
}

if (typeof glyphs !== 'undefined') {
  glyphs.checker = '<path d="M3 6h18M3 12h18M3 18h18M8 3v6M16 9v6M8 15v6"/>';
  glyphs['setting-info'] = '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>';
  glyphs['help-collapse'] = '<path d="m18 15-6-6-6 6"/>';
}

let settingsReturn = 'home', modelState = 'ready';
const expandedHelp = {};
const parameterGroups = {
  shapeGroup: [
    ['match', 'paramMatch', 72, 1, 100, 1, '%'],
    ['distance', 'paramDistance', 5, 1, 15, 1, '%']
  ],
  compensationGroup: [
    ['translation', 'paramTranslation', 12, 0, 100, 1, '%'],
    ['size', 'paramSize', 30, 0, 200, 1, '%'],
    ['stretchX', 'paramStretchX', 15, 0, 100, 1, '%'],
    ['stretchY', 'paramStretchY', 15, 0, 100, 1, '%'],
    ['rotation', 'paramRotation', 8, 0, 20, 1, '°'],
    ['shear', 'paramShear', 8, 0, 20, 1, '°'],
    ['jitter', 'paramJitter', 0.5, 0, 5, 0.1, '%']
  ],
  budgetGroup: [
    ['budget', 'paramBudget', 35, 0, 100, 1, '%']
  ],
  rulesGroup: [
    ['length', 'paramLength', 5, 0, 30, 1, '%'],
    ['connection', 'paramConnection', 5, 0, 10, 1, '%'],
    ['protrusion', 'paramProtrusion', 3, 0, 10, 1, '%']
  ]
};

const defaultParameters = Object.fromEntries(
  Object.values(parameterGroups).flat().map(([id,, value]) => [id, value])
);

const strictnessPresets = {
  soft: { ...defaultParameters, match: 64, distance: 8, translation: 18, size: 40, rotation: 12, budget: 45 },
  standard: { ...defaultParameters },
  strict: { ...defaultParameters, match: 84, distance: 3, translation: 8, size: 20, rotation: 4, budget: 20 }
};

let parameterValues = { ...defaultParameters }, strictnessPreset = 'standard';
let enabledChecks = { readability: true, shape: true, strokes: true, ruleStructure: true, ruleOrder: false, ruleDirection: false };
let flowSettings = { autoAdvanceKanji: true, autoAdvance: false, maxHints: 3, fullHintDisplay: 500 };

function binaryChoiceRow(tag, labelKey, helpKey, checked, allowEnable) {
  const isExpanded = !!expandedHelp[tag];
  return `
    <div class="setting-binary-block" data-tag="${tag}">
      <div class="setting-help-row" data-help-toggle="${tag}" role="button" tabindex="0" aria-expanded="${isExpanded}">
        <span class="setting-help-title">${t(labelKey)}</span>
        ${icon(isExpanded ? 'help-collapse' : 'setting-info', 'setting-help-icon')}
      </div>
      ${isExpanded ? `<div class="setting-help-box" data-testid="help-content-${tag}">${t(helpKey)}</div>` : ''}
      <div class="binary-choice-buttons">
        <button type="button" class="binary-choice-btn ${checked ? 'active' : ''}" data-binary="${tag}:true" ${(!checked && !allowEnable) ? 'disabled' : ''}>${t('settingOn')}</button>
        <button type="button" class="binary-choice-btn ${!checked ? 'active' : ''}" data-binary="${tag}:false">${t('settingOff')}</button>
      </div>
    </div>
  `;
}

function sliderSetting(tag, labelKey, helpKey, value, min, max, step, unit) {
  const isExpanded = !!expandedHelp[tag];
  const formatted = unit ? `${value} ${unit}` : `${value}`;
  return `
    <div class="setting-slider-block" data-tag="${tag}">
      <div class="setting-help-row" data-help-toggle="${tag}" role="button" tabindex="0" aria-expanded="${isExpanded}">
        <span class="setting-help-title">${t(labelKey)}</span>
        <span class="setting-help-value">${formatted}</span>
        ${icon(isExpanded ? 'help-collapse' : 'setting-info', 'setting-help-icon')}
      </div>
      ${isExpanded ? `<div class="setting-help-box" data-testid="help-content-${tag}">${t(helpKey)}</div>` : ''}
      <input class="settings-slider" type="range" min="${min}" max="${max}" step="${step}" value="${value}" data-flow="${tag}" aria-label="${t(labelKey)}" aria-valuetext="${formatted}">
    </div>
  `;
}

function settingsView() {
  const isModelReady = modelState === 'ready';
  const isModelLoading = modelState === 'loading';
  const modelBtnText = isModelReady ? t('modelLoaded') : isModelLoading ? t('modelLoading') : t('modelMissing');
  const modelBtnDisabled = isModelReady || isModelLoading;

  return header(t('settings'), 'home') +
    `<div class="settings-body" data-testid="settings-screen">
      <!-- Ход тренировки -->
      <section class="settings-section">
        <h3 class="kanji-section-title">${t('flowGroup')}</h3>
        ${binaryChoiceRow('autoAdvanceKanji', 'autoAdvanceKanji', 'helpAutoAdvanceKanji', flowSettings.autoAdvanceKanji, true)}
        ${binaryChoiceRow('autoAdvance', 'autoAdvance', 'helpAutoAdvance', flowSettings.autoAdvance, true)}
      </section>

      <hr class="horizontal-divider">

      <!-- Подсказки -->
      <section class="settings-section">
        <h3 class="kanji-section-title">${t('hintsGroup')}</h3>
        ${sliderSetting('maxHints', 'maxHints', 'helpMaxHints', flowSettings.maxHints, 0, 12, 1, '')}
        ${sliderSetting('fullHintDisplay', 'fullHintDisplay', 'helpFullHintDisplay', flowSettings.fullHintDisplay, 200, 3000, 50, t('millisUnit'))}
      </section>

      <hr class="horizontal-divider">

      <!-- Модули проверки -->
      <section class="settings-section">
        <h3 class="kanji-section-title">${t('checksGroup')}</h3>
        ${binaryChoiceRow('readability', 'readabilityCheck', 'helpReadability', enabledChecks.readability && isModelReady, isModelReady)}
        ${!isModelReady ? `<p class="setting-supporting-text" data-testid="readability-model-required">${t('readabilityModelRequired')}</p>` : ''}
        <button type="button" class="settings-model-btn" data-settings="model" ${modelBtnDisabled ? 'disabled' : ''} data-testid="download-japanese-model">${modelBtnText}</button>
        ${binaryChoiceRow('shape', 'shapeCheck', 'helpShape', enabledChecks.shape, true)}
        ${binaryChoiceRow('strokes', 'writingCheck', 'helpWriting', enabledChecks.strokes, true)}
      </section>

      <hr class="horizontal-divider">

      <!-- Калибровка формы кандзи (develop-only) -->
      <section class="settings-section">
        <h3 class="kanji-section-title">${t('calibrationGroup')}</h3>
        <button type="button" class="settings-action-card" data-action="go:checker-settings" data-testid="open-app-checker-settings">
          ${icon('checker', 'card-icon')}
          <span class="card-copy">
            <strong class="card-title">${t('checkerSettings')}</strong>
            <span class="card-detail">${t('checksHint')}</span>
          </span>
          <span class="card-chevron" aria-hidden="true">›</span>
        </button>
      </section>

      <hr class="horizontal-divider">

      <!-- Данные и память -->
      <section class="settings-section">
        <h3 class="kanji-section-title">${t('dataGroup')}</h3>
        <button type="button" class="settings-action-card" data-settings="clear-history" data-testid="clear-training-history">
          <span class="card-copy">
            <strong class="card-title">${t('clearHistory')}</strong>
            <span class="card-detail">${t('clearHistoryHint')}</span>
          </span>
        </button>
      </section>
    </div>`;
}

function checkerToggle(key) {
  return `<label class="settings-toggle"><span>${t(key)}</span><input type="checkbox" role="switch" data-setting-toggle="${key}" ${enabledChecks[key] ? 'checked' : ''}></label>`;
}

function checkerParameter([id, label,, min, max, step, unit]) {
  const value = parameterValues[id];
  return `
    <div class="settings-parameter">
      <label for="parameter-${id}">${t(label)}</label>
      <span class="settings-value">
        <input id="parameter-${id}" type="number" inputmode="decimal" min="${min}" max="${max}" step="${step}" value="${value}" data-parameter="${id}" aria-label="${t(label)}">
        <span>${unit}</span>
      </span>
      <input class="settings-slider" type="range" min="${min}" max="${max}" step="${step}" value="${value}" data-parameter-slider="${id}" aria-label="${t(label)}" aria-valuetext="${value}${unit}">
    </div>
  `;
}

function checkerSettingsView() {
  const actions = `<span class="dictionary-word-menu-wrap"><button type="button" class="icon-button" data-action="checker-menu" aria-label="${t('checkerMenu')}" aria-haspopup="menu" aria-expanded="false" data-testid="checker-settings-menu">${icon('more')}</button></span>`;
  return header(t('checkerSettings'), settingsReturn === 'practice' ? 'practice' : 'settings', actions) +
    `<div class="settings-content" data-testid="checker-settings-screen">
      <fieldset class="settings-panel settings-presets">
        <legend>${t('strictness')}</legend>
        ${Object.keys(strictnessPresets).map(id => `
          <label>
            <input type="radio" name="strictness" value="${id}" data-strictness ${strictnessPreset === id ? 'checked' : ''}>
            <span>${t('strictness_' + id)}</span>
          </label>
        `).join('')}
        <p>${t('strictnessHint')}</p>
        <p data-strictness-status role="status" ${strictnessPreset === 'custom' ? '' : 'hidden'}>${t('strictness_' + strictnessPreset)}</p>
      </fieldset>
      <details class="settings-panel settings-algorithms" open>
        <summary>${t('algorithmParameters')}</summary>
        ${Object.entries(parameterGroups).map(([name, items]) => `
          <section class="settings-parameter-group">
            <h3>${t(name)}</h3>
            ${name === 'rulesGroup' ? checkerToggle('ruleStructure') : ''}
            ${items.map(checkerParameter).join('')}
            ${name === 'compensationGroup' || name === 'budgetGroup' ? `<p class="settings-note">${t('zeroHelp')}</p>` : ''}
          </section>
        `).join('')}
      </details>
      <p class="settings-note">${t('settingsPreview')}</p>
    </div>`;
}

function updateParameter(control) {
  const id = control.dataset.parameter || control.dataset.parameterSlider;
  const value = Number(control.value);
  if (!control.checkValidity() || !Number.isFinite(value)) {
    control.value = parameterValues[id];
    return;
  }
  parameterValues[id] = value;
  strictnessPreset = 'custom';
  document.querySelectorAll('[data-strictness]').forEach(radio => radio.checked = false);
  const status = document.querySelector('[data-strictness-status]');
  if (status) {
    status.hidden = false;
    status.textContent = t('strictness_custom');
  }
  document.querySelectorAll(`[data-parameter="${id}"],[data-parameter-slider="${id}"]`).forEach(input => {
    input.value = value;
    if (input.type === 'range') {
      const unit = Object.values(parameterGroups).flat().find(item => item[0] === id)[6];
      input.setAttribute('aria-valuetext', value + unit);
    }
  });
}

function closeCheckerMenu(focus = false) {
  document.querySelector('#checker-menu')?.remove();
  const trigger = document.querySelector('[data-action="checker-menu"]');
  trigger?.setAttribute('aria-expanded', 'false');
  if (focus) trigger?.focus();
}

document.addEventListener('click', e => {
  const menuBtn = e.target.closest('[data-action="checker-menu"]');
  if (menuBtn) {
    e.stopImmediatePropagation();
    const open = !!document.querySelector('#checker-menu');
    closeCheckerMenu();
    if (!open) {
      menuBtn.setAttribute('aria-expanded', 'true');
      menuBtn.parentElement.insertAdjacentHTML(
        'beforeend',
        `<div id="checker-menu" class="plan-dropdown" role="menu" aria-label="${t('checkerMenu')}">
          <button type="button" role="menuitem" class="dropdown-item" data-settings="reset" data-testid="reset-checker-settings">
            ${icon('undo')}<span>${t('resetSettings')}</span>
          </button>
        </div>`
      );
      document.querySelector('#checker-menu button')?.focus();
    }
    return;
  }

  if (!e.target.closest('#checker-menu')) {
    closeCheckerMenu();
  }

  const helpToggle = e.target.closest('[data-help-toggle]');
  if (helpToggle) {
    const tag = helpToggle.dataset.helpToggle;
    expandedHelp[tag] = !expandedHelp[tag];
    render();
    return;
  }

  const binaryBtn = e.target.closest('[data-binary]');
  if (binaryBtn && !binaryBtn.disabled) {
    const [tag, valStr] = binaryBtn.dataset.binary.split(':');
    const val = valStr === 'true';
    if (tag === 'autoAdvanceKanji') {
      flowSettings.autoAdvanceKanji = val;
    } else if (tag === 'autoAdvance') {
      flowSettings.autoAdvance = val;
    } else if (tag === 'readability') {
      enabledChecks.readability = val;
    } else if (tag === 'strokes') {
      enabledChecks.strokes = val;
    } else if (tag === 'shape') {
      enabledChecks.shape = val;
    }
    render();
    return;
  }

  const b = e.target.closest('[data-settings]');
  if (!b) return;

  if (b.dataset.settings === 'model') {
    modelState = 'ready';
    const select = document.getElementById('model-state');
    if (select) select.value = 'ready';
    render();
  }

  if (b.dataset.settings === 'clear-history') {
    showModal(
      t('clearConfirmTitle'),
      `<p>${t('clearConfirmMessage')}</p>`,
      [
        { label: t('cancel'), fn: closeModal },
        { label: t('clearConfirmAction'), danger: true, fn: closeModal }
      ]
    );
  }

  if (b.dataset.settings === 'reset') {
    showModal(
      t('resetSettings'),
      `<p>${t('settingsPreview')}</p>`,
      [
        { label: t('cancel'), fn: closeModal },
        {
          label: t('resetSettings'),
          fn: () => {
            parameterValues = { ...defaultParameters };
            strictnessPreset = 'standard';
            closeModal();
            render();
          }
        }
      ]
    );
  }
});

document.addEventListener('keydown', e => {
  const checkerMenu = document.querySelector('#checker-menu');
  if (checkerMenu) {
    if (e.key === 'Escape') {
      e.preventDefault();
      e.stopImmediatePropagation();
      closeCheckerMenu(true);
      return;
    }
  }

  if (e.key === 'Enter' || e.key === ' ') {
    const helpToggle = e.target.closest('[data-help-toggle]');
    if (helpToggle) {
      e.preventDefault();
      const tag = helpToggle.dataset.helpToggle;
      expandedHelp[tag] = !expandedHelp[tag];
      render();
    }
  }
});

document.addEventListener('input', e => {
  if (e.target.dataset.parameterSlider) {
    updateParameter(e.target);
  }
  if (e.target.dataset.flow) {
    const tag = e.target.dataset.flow;
    const num = Number(e.target.value);
    flowSettings[tag] = num;
    const block = e.target.closest('[data-tag]');
    if (block) {
      const valSpan = block.querySelector('.setting-help-value');
      if (valSpan) {
        valSpan.textContent = tag === 'fullHintDisplay' ? `${num} ${t('millisUnit')}` : `${num}`;
      }
    }
  }
});

document.addEventListener('change', e => {
  if (e.target.dataset.settingToggle) {
    enabledChecks[e.target.dataset.settingToggle] = e.target.checked;
  }
  if (e.target.hasAttribute('data-strictness')) {
    strictnessPreset = e.target.value;
    parameterValues = { ...strictnessPresets[strictnessPreset] };
    render();
    document.querySelector(`[data-strictness][value="${strictnessPreset}"]`)?.focus();
  }
  if (e.target.dataset.parameter || e.target.dataset.parameterSlider) {
    updateParameter(e.target);
  }
  if (e.target.id === 'model-state') {
    modelState = e.target.value;
    render();
  }
});

if (typeof captions !== 'undefined') {
  captions.settings = 'Настройки хода тренировки, подсказок, модулей проверки ML Kit/черт/формы, калибровки и очистки истории.';
  captions['checker-settings'] = 'Пресеты строгости и параметры геометрических допусков алгоритма формы кандзи (только develop-сборка).';
}
