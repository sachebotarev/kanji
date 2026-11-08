'use strict';

const historyCopy = {
  ru: {
    trainingHistory: 'Следы тренировок',
    trainingHistoryDetail: 'Тренировка',
    historyWordsToday: 'слова дня',
    historySearchHint: 'Название рецепта',
    findHistory: 'Найти тренировку',
    hideHistorySearch: 'Скрыть поиск',
    clearHistorySearch: 'Очистить',
    historyPeriod: 'Период',
    historyPeriodToday: 'Сегодня',
    historyPeriod7: '7 дней',
    historyPeriod30: '30 дней',
    historyPeriodAll: 'Все',
    historyChoosePeriod: 'Выбери период',
    historyEmpty: 'Пока нет тренировок',
    historyEmptyHint: 'Пройди тренировку — и следы тренировок появятся здесь.',
    historyNoResults: 'Ничего не нашлось',
    historyNoResultsHint: 'Сбрось период или поиск.',
    historyToday: 'Сегодня',
    historyYesterday: 'Вчера',
    historyDeleteTitle: 'Удалить тренировку?',
    historyDeleteText: 'Запись исчезнет из следов тренировок. Учебные материалы останутся.',
    historyDuration: (m) => `${m} мин`,
    historyWordStatus: {
      correct: 'Верно',
      assisted: 'С подсказкой',
      skipped: 'Пропуск',
      timed_out: 'По времени',
    },
    historySummary: (c, a, m) => `Верно ${c} · с подсказкой ${a} · пропуск или таймаут ${m}`,
    historySessionActions: 'Действия с тренировкой',
  },
  en: {
    trainingHistory: 'Training footprints',
    trainingHistoryDetail: 'Training',
    historyWordsToday: "today's words",
    historySearchHint: 'Recipe name',
    findHistory: 'Find a training',
    hideHistorySearch: 'Hide search',
    clearHistorySearch: 'Clear',
    historyPeriod: 'Period',
    historyPeriodToday: 'Today',
    historyPeriod7: '7 days',
    historyPeriod30: '30 days',
    historyPeriodAll: 'All',
    historyChoosePeriod: 'Choose a period',
    historyEmpty: 'No trainings yet',
    historyEmptyHint: 'Finish a training — and training footprints will show up here.',
    historyNoResults: 'Nothing found',
    historyNoResultsHint: 'Clear the period or search.',
    historyToday: 'Today',
    historyYesterday: 'Yesterday',
    historyDeleteTitle: 'Delete training?',
    historyDeleteText: 'This record leaves your training footprints. Study materials stay.',
    historyDuration: (m) => `${m} min`,
    historyWordStatus: {
      correct: 'Correct',
      assisted: 'With a hint',
      skipped: 'Skipped',
      timed_out: 'Timed out',
    },
    historySummary: (c, a, m) => `Correct ${c} · hints ${a} · skipped or timed out ${m}`,
    historySessionActions: 'Training actions',
  },
};
for (const lang of Object.keys(historyCopy)) Object.assign(copy[lang], historyCopy[lang]);

const dayMs = 86400000;
function historyAnchor() {
  const d = new Date();
  d.setHours(12, 0, 0, 0);
  return d.getTime();
}
const historyNow = historyAnchor();

let historySessions = [
  {
    id: 'h1',
    recipe: { ru: 'Умный микс', en: 'Smart mix' },
    startedAt: historyNow - 2 * 3600000,
    durationSec: 420,
    words: [
      { written: '山', reading: 'やま', gloss: { ru: 'гора', en: 'mountain' }, status: 'correct', elapsedMs: 38000 },
      { written: '川', reading: 'かわ', gloss: { ru: 'река', en: 'river' }, status: 'correct', elapsedMs: 41000 },
      { written: '木', reading: 'き', gloss: { ru: 'дерево', en: 'tree' }, status: 'assisted', elapsedMs: 52000 },
      { written: '火', reading: 'ひ', gloss: { ru: 'огонь', en: 'fire' }, status: 'skipped', elapsedMs: 0 },
      { written: '水', reading: 'みず', gloss: { ru: 'вода', en: 'water' }, status: 'timed_out', elapsedMs: 60000 },
    ],
  },
  {
    id: 'h2',
    recipe: { ru: 'Tobira · главы 1–2', en: 'Tobira · chapters 1–2' },
    startedAt: historyNow - dayMs - 5 * 3600000,
    durationSec: 610,
    words: [
      { written: '友達', reading: 'ともだち', gloss: { ru: 'друг', en: 'friend' }, status: 'correct', elapsedMs: 44000 },
      { written: '図書館', reading: 'としょかん', gloss: { ru: 'библиотека', en: 'library' }, status: 'assisted', elapsedMs: 70000 },
      { written: '勉強', reading: 'べんきょう', gloss: { ru: 'учёба', en: 'study' }, status: 'correct', elapsedMs: 39000 },
    ],
  },
  {
    id: 'h3',
    recipe: { ru: 'Повтор N5', en: 'N5 review' },
    startedAt: historyNow - 3 * dayMs - 3600000,
    durationSec: 300,
    words: [
      { written: '一', reading: 'いち', gloss: { ru: 'один', en: 'one' }, status: 'correct', elapsedMs: 22000 },
      { written: '二', reading: 'に', gloss: { ru: 'два', en: 'two' }, status: 'correct', elapsedMs: 25000 },
      { written: '三', reading: 'さん', gloss: { ru: 'три', en: 'three' }, status: 'correct', elapsedMs: 21000 },
      { written: '四', reading: 'よん', gloss: { ru: 'четыре', en: 'four' }, status: 'assisted', elapsedMs: 48000 },
    ],
  },
  {
    id: 'h4',
    recipe: { ru: 'Умный микс', en: 'Smart mix' },
    startedAt: historyNow - 10 * dayMs,
    durationSec: 540,
    words: [
      { written: '月', reading: 'つき', gloss: { ru: 'луна; месяц', en: 'moon; month' }, status: 'correct', elapsedMs: 36000 },
      { written: '日', reading: 'ひ', gloss: { ru: 'день; солнце', en: 'day; sun' }, status: 'skipped', elapsedMs: 0 },
    ],
  },
];

let historySearchOpen = false;
let historySearchQuery = '';
let historyPeriod = 'all'; // today | 7 | 30 | all
let historyDetailId = null;

screens.push('training-history', 'training-history-detail');
names['training-history'] = 'trainingHistory';
names['training-history-detail'] = 'trainingHistoryDetail';
captions['training-history'] =
  'Черновик HOME.02: тренировки по дням, поиск и фильтр периода. Из HOME.W02 — пресет «Сегодня», из HOME.W03 — все. Требует согласования.';
captions['training-history-detail'] =
  'Черновик HOME.02.01: сводка тренировки и слова со статусами. Не итог PRACTICE.01. Требует согласования.';

function historyRecipeName(session) {
  return session.recipe[locale] || session.recipe.ru;
}

function historyCounts(session) {
  let correct = 0;
  let assisted = 0;
  let missed = 0;
  for (const w of session.words) {
    if (w.status === 'correct') correct++;
    else if (w.status === 'assisted') assisted++;
    else missed++;
  }
  return { correct, assisted, missed, total: correct + assisted + missed };
}

function historyMiniRing(session) {
  const { correct, assisted, missed, total } = historyCounts(session);
  if (!total) {
    return `<span class="history-mini-ring is-empty" aria-hidden="true"><strong>0</strong></span>`;
  }
  const cEnd = (correct / total) * 100;
  const aEnd = cEnd + (assisted / total) * 100;
  const gradient = `conic-gradient(var(--md-sys-color-primary) 0 ${cEnd}%, #E4B94F ${cEnd}% ${aEnd}%, var(--md-sys-color-error) ${aEnd}% 100%)`;
  return `<span class="history-mini-ring" style="--history-ring:${gradient}" aria-hidden="true"><strong>${total}</strong></span>`;
}

function historyDayKey(ms) {
  const d = new Date(ms);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function historyDayLabel(ms) {
  const todayKey = historyDayKey(historyNow);
  const key = historyDayKey(ms);
  if (key === todayKey) return t('historyToday');
  const y = new Date(historyNow);
  y.setDate(y.getDate() - 1);
  if (key === historyDayKey(y.getTime())) return t('historyYesterday');
  return new Intl.DateTimeFormat(locale === 'en' ? 'en' : 'ru', {
    day: 'numeric',
    month: 'long',
  }).format(new Date(ms));
}

function historyPeriodCutoff() {
  if (historyPeriod === 'today') return historyNow - (historyNow % dayMs);
  if (historyPeriod === '7') return historyNow - 7 * dayMs;
  if (historyPeriod === '30') return historyNow - 30 * dayMs;
  return null;
}

function historyPeriodTitle() {
  if (historyPeriod === 'today') return t('historyPeriodToday');
  if (historyPeriod === '7') return t('historyPeriod7');
  if (historyPeriod === '30') return t('historyPeriod30');
  return t('historyPeriod');
}

function filteredHistorySessions() {
  const cut = historyPeriodCutoff();
  const q = historySearchQuery.trim().toLowerCase();
  return historySessions
    .filter((s) => (cut === null ? true : s.startedAt >= cut))
    .filter((s) => !q || historyRecipeName(s).toLowerCase().includes(q))
    .sort((a, b) => b.startedAt - a.startedAt);
}

function historyGrouped(list) {
  const groups = [];
  for (const s of list) {
    const key = historyDayKey(s.startedAt);
    let g = groups.find((x) => x.key === key);
    if (!g) {
      g = { key, label: historyDayLabel(s.startedAt), items: [] };
      groups.push(g);
    }
    g.items.push(s);
  }
  return groups;
}

function historyTimeLabel(ms) {
  return new Intl.DateTimeFormat(locale === 'en' ? 'en' : 'ru', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(ms));
}

function historyCard(session) {
  const minutes = Math.max(1, Math.round(session.durationSec / 60));
  const name = historyRecipeName(session);
  return `<div class="history-swipe" role="listitem" data-history-id="${escapeHTML(session.id)}">
    <div class="history-swipe-bg" aria-hidden="true"><span>${escapeHTML(t('delete'))}</span></div>
    <div class="history-swipe-front">
      <button type="button" class="history-card-main" data-action="history-open:${session.id}" aria-label="${escapeHTML(name)}">
        ${historyMiniRing(session)}
        <span class="history-card-text">
          <strong class="history-card-title">${escapeHTML(name)}</strong>
          <small class="history-card-meta">${escapeHTML(historyTimeLabel(session.startedAt))} · ${escapeHTML(t('historyDuration')(minutes))}</small>
        </span>
      </button>
    </div>
  </div>`;
}

function historySearchControl() {
  if (!historySearchOpen) return '';
  return `<div id="history-search"><label class="n-search"><span class="n-search-icon" aria-hidden="true">${icon('search')}</span><input id="history-query" type="search" value="${escapeHTML(historySearchQuery)}" placeholder="${t('historySearchHint')}" aria-label="${t('findHistory')}" autocomplete="off"><button type="button" class="n-clear-query" data-action="history-search-clear" ${historySearchQuery ? '' : 'hidden'}>${t('clearHistorySearch')}</button></label></div>`;
}

function historyPeriodChip() {
  const active = historyPeriod !== 'all';
  const arrow =
    typeof materialFilterArrow === 'function'
      ? materialFilterArrow()
      : '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6" fill="none" stroke="currentColor" stroke-width="1.8"/></svg>';
  return CatalogSelection.filterChip({
    title: historyPeriodTitle(),
    type: 'history-period',
    active,
    clear: 'history-period-clear',
    clearLabel: t('clear'),
    arrow,
    close: '<span aria-hidden="true">×</span>',
    html: escapeHTML,
  });
}

function openHistoryPeriodPicker() {
  const options = [
    ['today', t('historyPeriodToday')],
    ['7', t('historyPeriod7')],
    ['30', t('historyPeriod30')],
    ['all', t('historyPeriodAll')],
  ];
  const body = `<div class="history-period-options">${options
    .map(
      ([id, label]) =>
        `<button type="button" class="history-period-option${historyPeriod === id ? ' is-selected' : ''}" data-action="history-period-set:${id}">${escapeHTML(label)}</button>`,
    )
    .join('')}</div>`;
  showModal(t('historyChoosePeriod'), body, [{ label: t('cancel'), fn: closeModal }]);
}

function trainingHistoryView() {
  const list = filteredHistorySessions();
  const groups = historyGrouped(list);
  const searchToggle = `<button type="button" class="recipe-search-toggle" data-action="history-search-toggle" aria-label="${t(historySearchOpen ? 'hideHistorySearch' : 'findHistory')}" aria-expanded="${historySearchOpen}">${icon('search')}</button>`;
  let body;
  if (!historySessions.length) {
    body = `<div class="recipe-search-empty"><img src="../../assets/mascot/mascot_sticker_peek_empty_box.png" alt=""><h3>${t('historyEmpty')}</h3><p>${t('historyEmptyHint')}</p></div>`;
  } else if (!list.length) {
    body = `<div class="recipe-search-empty"><img src="../../assets/mascot/mascot_sticker_peek_empty_box.png" alt=""><h3>${t('historyNoResults')}</h3><p>${t('historyNoResultsHint')}</p></div>`;
  } else {
    body = groups
      .map(
        (g) =>
          `<section class="history-day" aria-label="${escapeHTML(g.label)}"><h3 class="history-day-title">${escapeHTML(g.label)}</h3><div class="history-day-list" role="list">${g.items.map(historyCard).join('')}</div></section>`,
      )
      .join('');
  }
  return `<div class="recipe-list-screen history-screen">${header(t('trainingHistory'), 'home', searchToggle)}<div class="recipe-list-scroll" tabindex="0" role="region" aria-label="${t('trainingHistory')}">${historySearchControl()}<div class="w-textbook-filterbar">${historyPeriodChip()}</div><div class="recipe-content recipe-list-content history-content">${body}</div></div></div>`;
}

function trainingHistoryDetailView() {
  const session = historySessions.find((s) => s.id === historyDetailId) || historySessions[0];
  if (!session) {
    return header(t('trainingHistoryDetail'), 'training-history') + emptyView(t('historyEmpty'));
  }
  const { correct, assisted, missed, total } = historyCounts(session);
  const minutes = Math.max(1, Math.round(session.durationSec / 60));
  const name = historyRecipeName(session);
  const words = session.words
    .map((w) => {
      const gloss = w.gloss[locale] || w.gloss.ru;
      const elapsed =
        w.elapsedMs > 0 ? `${Math.round(w.elapsedMs / 1000)} ${locale === 'en' ? 's' : 'с'}` : '—';
      return `<div class="history-word-row">
        <div class="history-word-main">
          <strong lang="ja">${escapeHTML(w.written)}</strong>
          <small lang="ja">${escapeHTML(w.reading)}</small>
          <span>${escapeHTML(gloss)}</span>
        </div>
        <div class="history-word-meta">
          <span class="history-word-status status-${w.status}">${escapeHTML(t('historyWordStatus')[w.status])}</span>
          <small>${escapeHTML(elapsed)}</small>
        </div>
      </div>`;
    })
    .join('');
  return `<div class="recipe-editor-screen history-detail-screen">${header(name, 'training-history')}<div class="recipe-content recipe-source-content history-detail-content">
    <div class="history-detail-summary">
      ${historyMiniRing(session)}
      <div>
        <p class="history-detail-when">${escapeHTML(historyDayLabel(session.startedAt))} · ${escapeHTML(historyTimeLabel(session.startedAt))} · ${escapeHTML(t('historyDuration')(minutes))}</p>
        <p class="history-detail-counts">${escapeHTML(t('historySummary')(correct, assisted, missed))}</p>
        <p class="history-detail-total">${total} ${locale === 'en' ? 'words' : 'слов'}</p>
      </div>
    </div>
    <section aria-label="${escapeHTML(t('words'))}"><h3>${t('words')}</h3><div class="history-word-list">${words}</div></section>
  </div></div>`;
}

function openTrainingHistory(period = 'all') {
  historyPeriod = period;
  historySearchOpen = false;
  historySearchQuery = '';
  go('training-history');
}

function openHistoryDetail(id) {
  historyDetailId = id;
  go('training-history-detail');
}

function confirmHistoryDelete(id) {
  const session = historySessions.find((s) => s.id === id);
  if (!session) return;
  showModal(
    t('historyDeleteTitle'),
    `<p>${escapeHTML(historyRecipeName(session))}</p><p>${t('historyDeleteText')}</p>`,
    [
      { label: t('cancel'), fn: closeModal },
      {
        label: t('delete'),
        danger: true,
        fn: () => {
          historySessions = historySessions.filter((s) => s.id !== id);
          closeModal();
          if (screen === 'training-history-detail') go('training-history');
          else render();
        },
      },
    ],
  );
}

document.addEventListener('click', (e) => {
  const button = e.target.closest('[data-action]');
  if (!button || button.disabled) return;
  const [action, value] = button.dataset.action.split(':');
  if (action === 'history-search-toggle') {
    historySearchOpen = !historySearchOpen;
    if (!historySearchOpen) historySearchQuery = '';
    render();
    if (historySearchOpen) $('#history-query')?.focus();
  } else if (action === 'history-search-clear') {
    historySearchQuery = '';
    render();
    $('#history-query')?.focus();
  } else if (action === 'filter' && value === 'history-period') {
    openHistoryPeriodPicker();
  } else if (action === 'history-period-clear') {
    historyPeriod = 'all';
    render();
  } else if (action === 'history-period-set') {
    historyPeriod = value;
    closeModal();
    render();
  } else if (action === 'history-open') {
    openHistoryDetail(value);
  } else if (action === 'history-swipe-delete') {
    confirmHistoryDelete(value);
  }
});

let historySwipe = null;
const HISTORY_SWIPE_MAX = 96;
const HISTORY_SWIPE_TRIGGER = 64;

function resetHistorySwipe(front) {
  if (!front) return;
  front.style.transition = 'transform 160ms ease';
  front.style.transform = 'translateX(0)';
}

function beginHistorySwipe(target, clientX, clientY, pointerId, touch) {
  if (historySwipe) return;
  const front = target.closest?.('.history-swipe-front');
  const row = front?.closest('.history-swipe');
  if (!row || !front) return;
  historySwipe = {
    row,
    front,
    id: row.dataset.historyId,
    startX: clientX,
    startY: clientY,
    pointerId,
    touch: !!touch,
    axis: null,
    dx: 0,
  };
  front.style.transition = 'none';
}

function moveHistorySwipe(clientX, clientY) {
  if (!historySwipe?.front) return;
  const dx = clientX - historySwipe.startX;
  const dy = clientY - historySwipe.startY;
  if (!historySwipe.axis) {
    if (Math.abs(dx) < 6 && Math.abs(dy) < 6) return false;
    historySwipe.axis = Math.abs(dx) > Math.abs(dy) ? 'x' : 'y';
    if (historySwipe.axis === 'y') {
      historySwipe = null;
      return false;
    }
  }
  if (historySwipe.axis !== 'x') return false;
  const offset = Math.max(-HISTORY_SWIPE_MAX, Math.min(0, dx));
  historySwipe.dx = offset;
  historySwipe.front.style.transform = `translateX(${offset}px)`;
  return true;
}

function endHistorySwipe(commit) {
  const swipe = historySwipe;
  historySwipe = null;
  if (!swipe?.front) return;
  const opened = commit && swipe.dx <= -HISTORY_SWIPE_TRIGGER;
  if (opened) {
    swipe.front.style.transition = 'transform 160ms ease';
    swipe.front.style.transform = `translateX(${-HISTORY_SWIPE_MAX}px)`;
    confirmHistoryDelete(swipe.id);
    setTimeout(() => resetHistorySwipe(swipe.front), 220);
  } else {
    resetHistorySwipe(swipe.front);
  }
}

document.addEventListener('pointerdown', (e) => {
  if (e.pointerType === 'touch' || !e.isPrimary || e.button !== 0) return;
  beginHistorySwipe(e.target, e.clientX, e.clientY, e.pointerId, false);
});
document.addEventListener('pointermove', (e) => {
  if (!historySwipe || historySwipe.touch || historySwipe.pointerId !== e.pointerId) return;
  if (moveHistorySwipe(e.clientX, e.clientY)) e.preventDefault();
});
document.addEventListener('pointerup', (e) => {
  if (!historySwipe || historySwipe.touch || historySwipe.pointerId !== e.pointerId) return;
  endHistorySwipe(true);
});
document.addEventListener('pointercancel', (e) => {
  if (!historySwipe || historySwipe.touch || historySwipe.pointerId !== e.pointerId) return;
  endHistorySwipe(false);
});
document.addEventListener(
  'touchstart',
  (e) => {
    if (e.touches.length !== 1) {
      endHistorySwipe(false);
      return;
    }
    const touch = e.touches[0];
    beginHistorySwipe(e.target, touch.clientX, touch.clientY, touch.identifier, true);
  },
  { passive: true },
);
document.addEventListener(
  'touchmove',
  (e) => {
    if (!historySwipe?.touch) return;
    const touch = [...e.touches].find((item) => item.identifier === historySwipe.pointerId);
    if (!touch) return;
    if (moveHistorySwipe(touch.clientX, touch.clientY)) e.preventDefault();
  },
  { passive: false },
);
document.addEventListener('touchend', () => {
  if (historySwipe?.touch) endHistorySwipe(true);
});
document.addEventListener('touchcancel', () => {
  if (historySwipe?.touch) endHistorySwipe(false);
});

document.addEventListener('input', (e) => {
  if (e.target.id !== 'history-query') return;
  historySearchQuery = e.target.value;
  const position = e.target.selectionStart;
  render();
  const field = $('#history-query');
  if (field) {
    field.focus();
    try {
      field.setSelectionRange(position, position);
    } catch (_) {}
  }
});
