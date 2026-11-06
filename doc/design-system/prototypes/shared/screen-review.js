'use strict';
// Approval is configuration data. There is no browser action that grants approval.
window.ScreenReview=(()=>{
 let registry=null,error=null;
 const ready=fetch('../../theme/screens.json',{cache:'no-store'}).then(r=>{if(!r.ok)throw Error('Screen registry HTTP '+r.status);return r.json()}).then(value=>{registry=value;return value}).catch(e=>{error=e;return null});
 function status(module,key,language='ru'){
  const row=registry?.screens.find(x=>x.module===module&&x.key===key);
  const tr=row?.translations.find(t=>t.language_tag===language)||row?.translations.find(t=>t.language_tag==='ru');
  const approval=registry?.approval;
  const note=row?.status==='approved'&&approval?.features?.includes(row.feature)?(approval.translations?.find(t=>t.language_tag===language)||approval.translations?.find(t=>t.language_tag==='ru'))?.text:null;
  return {status:row?.status||registry?.default_status||'unapproved',id:row?.id||'',title:tr?.title||key,changes:note||tr?.changes||'',error};
 }
 return {ready,status,get registry(){return registry}};
})();
