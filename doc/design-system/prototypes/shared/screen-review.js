'use strict';
// Review controls live outside the mobile UI. Screens without a locked design have no approved renderer.
window.ScreenReview = (() => {
  const memory=new Map();
  const valid=new Set(['baseline','approved','wip']);
  function get(key,fallback='baseline') {
    try { const value=sessionStorage.getItem('kanji-screen-review:'+key);if(valid.has(value))return value; } catch {}
    return memory.get(key)||fallback;
  }
  function set(key,value) {
    if(!valid.has(value))return;
    memory.set(key,value);
    try {sessionStorage.setItem('kanji-screen-review:'+key,value);} catch {}
  }
  function describe(variant,approved) {
    return variant==='baseline'?{title:'ТЕКУЩИЙ · КАК В ПРИЛОЖЕНИИ',badge:'Android'}:
      variant==='wip'?{title:'В РАБОТЕ',badge:'Черновик'}:
      approved?{title:'СОГЛАСОВАННЫЙ ДИЗАЙН',badge:'Эталон'}:{title:'СОГЛАСОВАННЫЙ ДИЗАЙН',badge:'Ещё не задан'};
  }
  return {get,set,describe};
})();
