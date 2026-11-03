'use strict';
// All public legacy URLs open the same shell. Standalone is reserved for renderer regression tests.
(()=>{
 const query=new URLSearchParams(location.search),embedded=query.has('embedded');
 if(embedded){document.documentElement.classList.add('viewer-embedded');return}
 if(query.has('standalone'))return;
 const module=document.documentElement.dataset.viewerModule;
 const url=new URL('../index.html',location.href);
 if(module==='theme')url.hash='theme';else{url.searchParams.set('module',module);url.searchParams.set('route',location.hash.slice(1)|| (module==='catalog'?'catalog':'home'))}
 location.replace(url.href);
})();
