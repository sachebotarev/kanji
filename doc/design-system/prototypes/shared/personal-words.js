'use strict';
// Shared demonstration data, scoped to this browser tab; never Android persistence.
window.PersonalWords={
 key:'kanji-prototype-personal-words-v1',
 defaults:[
  {id:-1,written:'木漏れ日',reading:'こもれび',translations:[{language_tag:'ru',text:'солнечный свет сквозь листву'},{language_tag:'en',text:'sunlight filtering through leaves'}],personal:true},
  {id:-3,written:'日本語を勉強しています。',reading:'にほんごをべんきょうしています。',translations:[{language_tag:'ru',text:'Я изучаю японский язык.'},{language_tag:'en',text:'I am studying Japanese.'}],personal:true},
  {id:-4,written:'友達',reading:'ともだち',translations:[{language_tag:'ru',text:'друг; подруга'},{language_tag:'en',text:'friend'}],personal:true}
 ],
 read(){try{const saved=JSON.parse(sessionStorage.getItem(this.key));if(Array.isArray(saved))return saved;}catch{}return structuredClone(this.defaults);},
 readTags(){try{const tags=JSON.parse(sessionStorage.getItem(this.key+'-tags'));return Array.isArray(tags)?tags:null;}catch{return null;}},
 write(words,tags){try{sessionStorage.setItem(this.key,JSON.stringify(words.filter(w=>w.personal)));if(tags)sessionStorage.setItem(this.key+'-tags',JSON.stringify(tags));}catch{}}
};
