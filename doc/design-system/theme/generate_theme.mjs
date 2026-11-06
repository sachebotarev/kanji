// Shared theme generator. Only the input radius also generates an Android dimension.
// Change theme-source.json, then npm run build. Never generates Android screen code.
import fs from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {resolveTheme} from './palette.mjs';
const here=path.dirname(fileURLToPath(import.meta.url));
const source=JSON.parse(fs.readFileSync(path.join(here,'theme-source.json'),'utf8'));
const radius=source.component_shape.text_field;
if(!Number.isFinite(radius)||radius<0)throw Error('component_shape.text_field must be a non-negative number');
const schemes=resolveTheme(source);
const out=path.resolve(here,'../prototypes/shared');
fs.writeFileSync(path.join(out,'material-theme.json'),JSON.stringify({source:'../../theme/theme-source.json',...source,schemes},null,2)+'\n');
let css='/* Generated from ../../theme/theme-source.json. Do not hand edit. */\n';
for(const [theme,roles] of Object.entries(schemes))css+=`:root[data-theme=${theme}], #phone[data-theme=${theme}] {\n${Object.entries(roles).map(([k,v])=>` --md-sys-color-${k}: ${v};`).join('\n')}\n}\n`;
css+=':root, #phone {\n --md-sys-typescale-font-family: '+source.font_family+';\n --md-ref-artwork-dark-outline: '+source.artwork.dark_outline+';\n'+Object.entries(source.shape).map(([k,v])=>` --md-sys-shape-corner-${k}: ${v}px;`).join('\n')+'\n';
css+=Object.entries(source.elevation).map(([k,v])=>` --md-sys-elevation-${k}: ${v}px;`).join('\n')+'\n';
css+=Object.entries(source.state_opacity).map(([k,v])=>` --md-sys-state-${k.replaceAll('_','-')}: ${v};`).join('\n')+'\n';
for(const [k,[size,line,weight,tracking]] of Object.entries(source.typography))css+=` --md-sys-typescale-${k}-size: ${size}px; --md-sys-typescale-${k}-line-height: ${line}px; --md-sys-typescale-${k}-weight: ${weight}; --md-sys-typescale-${k}-tracking: ${tracking}px;\n`;
css+=` --app-input-corner-radius: ${source.component_shape.text_field}px;\n`;
css+='}\n';
fs.writeFileSync(path.join(out,'material-theme.css'),css);
const androidOut=path.resolve(here,'../../../core/designsystem/src/main/res/values/input-shape.xml');
fs.mkdirSync(path.dirname(androidOut),{recursive:true});
fs.writeFileSync(androidOut,`<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated from doc/design-system/theme/theme-source.json. Do not hand edit. -->\n<resources>\n    <dimen name="kanji_input_corner_radius">${radius}dp</dimen>\n</resources>\n`);
console.log('Generated theme and shared HTML/Android input radius from theme-source.json');

const registry=JSON.parse(fs.readFileSync(path.join(here,'screens.json'),'utf8'));
const allowedStatus=new Set(['approved','unapproved','retired']);
if(!registry.screens.every(s=>allowedStatus.has(s.status)))throw Error('Unknown screen status');
const activeScreens=registry.screens.filter(s=>s.status!=='retired');
const keys=activeScreens.map(s=>s.module+':'+s.key);if(new Set(keys).size!==keys.length)throw Error('Duplicate screen key');
// Screen approval is read directly from screens.json; do not generate a second registry.
// Retired IDs stay in the registry for history; viewers should skip them in navigation.
