// Design-only generator. Never writes Android sources. Change theme-source.json, then npm run build.
import fs from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {resolveTheme} from './palette.mjs';
const here=path.dirname(fileURLToPath(import.meta.url));
const source=JSON.parse(fs.readFileSync(path.join(here,'theme-source.json'),'utf8'));
const schemes=resolveTheme(source);
const out=path.resolve(here,'../prototypes/shared');
fs.writeFileSync(path.join(out,'material-theme.json'),JSON.stringify({source:'../../theme/theme-source.json',...source,schemes},null,2)+'\n');
let css='/* Generated from ../../theme/theme-source.json. Do not hand edit. */\n';
for(const [theme,roles] of Object.entries(schemes))css+=`:root[data-theme=${theme}], #phone[data-theme=${theme}] {\n${Object.entries(roles).map(([k,v])=>` --md-sys-color-${k}: ${v};`).join('\n')}\n}\n`;
css+=':root, #phone {\n --md-sys-typescale-font-family: '+source.font_family+';\n --md-ref-artwork-dark-outline: '+source.artwork.dark_outline+';\n'+Object.entries(source.shape).map(([k,v])=>` --md-sys-shape-corner-${k}: ${v}px;`).join('\n')+'\n';
css+=Object.entries(source.elevation).map(([k,v])=>` --md-sys-elevation-${k}: ${v}px;`).join('\n')+'\n';
css+=Object.entries(source.state_opacity).map(([k,v])=>` --md-sys-state-${k.replaceAll('_','-')}: ${v};`).join('\n')+'\n';
for(const [k,[size,line,weight,tracking]] of Object.entries(source.typography))css+=` --md-sys-typescale-${k}-size: ${size}px; --md-sys-typescale-${k}-line-height: ${line}px; --md-sys-typescale-${k}-weight: ${weight}; --md-sys-typescale-${k}-tracking: ${tracking}px;\n`;
css+='}\n';
fs.writeFileSync(path.join(out,'material-theme.css'),css);
console.log('Generated light/dark roles, typography, shapes, elevation and states from theme-source.json');

const registry=JSON.parse(fs.readFileSync(path.join(here,'screens.json'),'utf8'));
if(!registry.screens.every(s=>['approved','unapproved'].includes(s.status)))throw Error('Unknown screen status');
const keys=registry.screens.map(s=>s.module+':'+s.key);if(new Set(keys).size!==keys.length)throw Error('Duplicate screen key');
fs.writeFileSync(path.resolve(here,'../05-screens/catalog/review-status.json'),JSON.stringify({revision:registry.revision,generated_from:'../../theme/screens.json',default_status:registry.default_status,screens:registry.screens.filter(s=>s.module==='catalog')},null,2)+'\n');
