// Shared pure resolver used by the offline generator and live theme editor.
import {TonalPalette} from './node_modules/@material/material-color-utilities/palettes/tonal_palette.js';
import {Hct} from './node_modules/@material/material-color-utilities/hct/hct.js';
import {argbFromHex,hexFromArgb} from './node_modules/@material/material-color-utilities/utils/string_utils.js';
export function resolveTheme(source){
const palettes=Object.fromEntries(Object.entries(source.key_colors).map(([k,v])=>[k,TonalPalette.fromInt(argbFromHex(v))]));
const hct=Hct.fromInt(argbFromHex(source.key_colors.primary));
palettes.neutral=TonalPalette.fromHueAndChroma(hct.hue,source.neutral_chroma);
palettes.neutralVariant=TonalPalette.fromHueAndChroma(hct.hue,source.neutral_variant_chroma);
const tone=(p,n)=>hexFromArgb(palettes[p].tone(n)).toUpperCase();
const schemes={};
for(const dark of [false,true]){
 const roles={};
 for(const p of ['primary','secondary','tertiary','error']){
  // Brand key colors are also the exact light-theme foreground roles; pair contrast is validated.
  roles[p]=dark?tone(p,80):source.key_colors[p];
  roles['on-'+p]=tone(p,dark?20:100);
  roles[p+'-container']=tone(p,dark?30:90);
  roles['on-'+p+'-container']=tone(p,dark?90:10);
  if(p!=='error'){
   roles[p+'-fixed']=tone(p,90);roles[p+'-fixed-dim']=tone(p,80);
   roles['on-'+p+'-fixed']=tone(p,10);roles['on-'+p+'-fixed-variant']=tone(p,30);
  }
 }
 const surfaces=dark?{'surface':6,'surface-dim':6,'surface-bright':24,'surface-container-lowest':4,'surface-container-low':10,'surface-container':12,'surface-container-high':17,'surface-container-highest':22,'on-surface':90,'inverse-surface':90,'inverse-on-surface':20}: {'surface':98,'surface-dim':87,'surface-bright':98,'surface-container-lowest':100,'surface-container-low':96,'surface-container':94,'surface-container-high':92,'surface-container-highest':90,'on-surface':10,'inverse-surface':20,'inverse-on-surface':95};
 for(const [k,n] of Object.entries(surfaces))roles[k]=tone('neutral',n);
 for(const [k,n] of Object.entries({'on-surface-variant':dark?80:30,'surface-variant':dark?30:90,outline:dark?60:50,'outline-variant':dark?30:80}))roles[k]=tone('neutralVariant',n);
 roles.background=roles.surface;roles['on-background']=roles['on-surface'];roles['surface-tint']=roles.primary;
 roles['inverse-primary']=tone('primary',dark?40:80);roles.shadow=tone('neutral',0);roles.scrim=tone('neutral',0);
 schemes[dark?'dark':'light']=roles;
}
return schemes;
}
