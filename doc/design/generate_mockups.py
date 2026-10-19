from pathlib import Path
from html import escape
out=Path(__file__).resolve().parent
ink='#263e36'; muted='#758178'; green='#426b53'; blue='#416d99'; paper='#faf8f1'
def text(x,y,t,size=14,color=ink,weight=400,anchor='start',extra=''):
 return f'<text x="{x}" y="{y}" font-size="{size}" fill="{color}" font-weight="{weight}" text-anchor="{anchor}" {extra}>{escape(t)}</text>'
def rect(x,y,w,h,fill,stroke='none',r=16,sw=1):
 return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>'
def icon(x,y,kind,color=ink):
 paths={'hint':'M9 18H15 M10 21H14 M9 15C9 12 6 12 6 8A6 6 0 0 1 18 8C18 12 15 12 15 15H9 M12 0V1 M3 3L4 4 M20 4L21 3', 'undo':'M7 7H3V3 M3 7C6 2 15 3 18 8C21 14 17 20 11 20H7','bin':'M5 6H19 M9 3H15 M7 6L8 21H16L17 6 M10 10V17 M14 10V17','arrow':'M5 12H19 M13 6L19 12L13 18','check':'M15.5 3C8 .5 2.4 5.6 2.8 12.8C3.2 21 14 24 19.8 17.7C25.1 11.3 20.2 2.9 12.8 3.5','error':'M4.2 3.6C8.3 8 14.8 15 20.3 20.6 M20.6 3.2C16.1 8.1 9.4 16.3 3.3 20.8','back':'M15 5L8 12L15 19','chevron':'M9 6L15 12L9 18'}
 size=26 if kind in ('check','error') else 24
 width=2.5 if kind in ('check','error') else 2
 return f'<g transform="translate({x-size/2} {y-size/2}) scale({size/24})" fill="none" stroke="{color}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"><path d="{paths[kind]}"/></g>'
def drawing(x,y,size,partial=False,wrong=False):
 paths=['M23 20Q47 18 77 20','M17 31Q48 29 83 31','M26 42Q50 40 74 42','M49 12Q48 36 50 51','M20 52Q49 50 80 52','M29 63Q50 61 71 63','M29 63L30 86','M71 63L70 86','M31 75Q50 74 70 75','M31 86Q50 85 70 86']
 if partial: paths=paths[:4]
 if wrong: paths=paths[:-2]
 return f'<g transform="translate({x} {y}) scale({size/100})" fill="none" stroke="{ink}" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">'+''.join(f'<path d="{p}"/>' for p in paths)+'</g>'
def screen(state):
 long=state==4; blank=state==0; hint=state==5; final=state==6; done=state in (3,5,6); error=state==2
 s=rect(0,0,392,844,paper,'#d9dfd5',28)
 s+=text(24,29,'9:41',13,weight=600)+text(365,29,'▰',14,anchor='end')
 s+=icon(32,67,'back')+text(60,72,'Практика',16,weight=600)
 s+=rect(304,51,64,32,'#ecefe5',r=16)+text(336,72,'漢',19,green,600,'middle')
 s+=text(24,111,'Слово 10 из 10' if final else 'Слово 4 из 10',12,ink,600)
 s+=text(368,111,'Осталось 0' if final else 'Осталось 6' if done else 'Осталось 7',12,muted,anchor='end')
 s+=rect(24,122,344,4,'#e2e7db',r=2)+rect(24,122,344 if final else 137.6 if done else 103.2,4,green,r=2)
 if long:
  s+=text(24,157,'Международный',27,weight=600)+text(24,190,'секретариат',27,weight=600)
 else: s+=text(24,169,'Библиотека',30,weight=600)
 s+='<defs><clipPath id="cells"><rect x="24" y="216" width="344" height="80"/></clipPath></defs><g clip-path="url(#cells)">'
 chars=list('国際連合事務局') if long else ['図','書','館']
 start=24 if long else 88
 for i,c in enumerate(chars):
  x=start+i*76; active=i==(0 if long or blank else 1)
  preset=not long and i==2; correct=not blank and not long and (i==0 or done and i==1)
  bad=error and i==1
  editing=state==1 and i==1
  fill='#edeae2' if preset or hint and i==1 else '#ddedd8' if correct else '#ffede6' if bad else '#eaf1f8' if editing else '#fffdf7'
  border='#a64935' if bad else green if correct else blue if active or editing else '#d6dccf'
  cell=rect(x,220,64,64,fill,border,12,2 if active or correct or bad else 1)
  if editing: cell=cell.replace('/>', ' stroke-dasharray="4 3"/>')
  s+=cell
  if active: s+=rect(x+20,289,24,3,blue,r=1.5)
  if preset or correct or long and i>0: s+=text(x+32,253,c,37,muted if preset else ink,anchor='middle',extra='dominant-baseline="central" font-family="Hiragino Mincho ProN,serif"')
  elif (not blank and i==1): s+=drawing(x+8,227,48,state==1,bad)
 s+='</g>'
 if long:
  s+=f'<circle cx="352" cy="252" r="16" fill="{green}"/>'+icon(352,252,'chevron','#fff')
 s+=text(68,314,'1 / 7' if long else '1 / 3' if blank else '2 / 3',12,muted,500,'middle',extra='dominant-baseline="central"')
 if state==1: s+=text(196,314,'Не проверен',12,blue,600,'middle',extra='dominant-baseline="central"')
 if error or done:
  status_color=green if done else '#a64935'
  s+=icon(159,314,'hint' if hint else 'check' if done else 'error',status_color)
  s+=text(205,314,'Подсказка' if hint else 'Верно' if done else 'Неверно',12,status_color,600,'middle',extra='dominant-baseline="central"')
 s+=f'<g transform="translate(289 305)" fill="none" stroke="{muted}" stroke-width="1.4" stroke-linecap="round"><path d="M0 0Q7 1 14 0 M0 18Q7 17 14 18 M2 1Q2 5 7 9Q12 13 12 17 M12 1Q12 5 7 9Q2 13 2 17"/></g>'
 s+=text(333,314,'00:00' if blank or long else '00:18' if state==1 else '00:32' if error else '00:46',12,muted,500,'middle',extra='dominant-baseline="central" style="font-variant-numeric:tabular-nums"')
 s+=rect(24,340,344,344,'#fffdf7','#b9c5b3',16)
 s+='<path d="M196 341V683 M25 512H367 M28 344L364 680 M364 344L28 680" stroke="#dce3d3" stroke-dasharray="5 6" fill="none"/>'
 if hint: s+=text(196,524,'書',240,ink,anchor='middle',extra='dominant-baseline="central" font-family="Hiragino Mincho ProN,serif"')
 elif not blank and not long: s+=drawing(62,378,268,state==1,error)
 for cx,kind in [(224,'hint'),(284,'undo'),(344,'bin')]:
  s+=rect(cx-24,700,48,48,'#edeFE5',r=16)+icon(cx,724,kind,'#a7ada1' if ((blank or long) and kind!='hint') or (hint and kind=='hint') else green)
 s+=text(106,796,'Пропустить',15,'#996452',500,'middle',extra='dominant-baseline="central"')
 s+=rect(204,772,164,48,green if done else '#e3e5dc',r=24)
 s+=text(286 if final else 274,796,'Завершить' if final else 'Далее',15,'#fff' if done else '#9bA294',600,'middle',extra='dominant-baseline="central"')+('' if final else icon(323,796,'arrow','#fff' if done else '#9bA294'))
 s+=rect(144,832,104,4,ink,r=2)
 return s
labels=['Начало задания','Ввод кандзи','Нужно исправить','Задание выполнено','Длинное слово']
header='<svg xmlns="http://www.w3.org/2000/svg" width="392" height="844" viewBox="0 0 392 844"><g font-family="Arial,sans-serif">'
for i in range(7): (out/f'practice-{i+1}.svg').write_text(header+screen(i)+'</g></svg>')
s='<svg xmlns="http://www.w3.org/2000/svg" width="2160" height="1044" viewBox="0 0 2160 1044"><rect width="2160" height="1044" fill="#e8ece3"/><g font-family="Arial,sans-serif">'
s+=text(36,52,'Кандзи · ежедневная практика',30,weight=600)+text(36,82,'Material Design · тёплая бумага, тушь и оттенок зелёного чая',15,muted)
for i,label in enumerate(labels):
 x=36+i*420
 s+=text(x,128,f'0{i+1}  {label}',16,weight=600)+f'<g transform="translate({x} 152)">'+screen(i).replace('id="cells"',f'id="cells{i}"').replace('url(#cells)',f'url(#cells{i})')+'</g>'
s+='</g></svg>'
(out/'practice-states.svg').write_text(s)
import xml.etree.ElementTree as ET
for p in out.glob('*.svg'): ET.parse(p)
extra='<svg xmlns="http://www.w3.org/2000/svg" width="876" height="960" viewBox="0 0 876 960"><rect width="876" height="960" fill="#e8ece3"/><g font-family="Arial,sans-serif">'
for i,label in [(5,'06 · После подсказки'),(6,'07 · Последнее слово выполнено')]:
 x=32+(i-5)*420
 extra+=text(x,40,label,18,weight=600)+f'<g transform="translate({x} 64)">'+screen(i).replace('id="cells"',f'id="extra{i}"').replace('url(#cells)',f'url(#extra{i})')+'</g>'
(out/'practice-extra-states.svg').write_text(extra+'</g></svg>')
print('Generated mockups')

# Canonical side-by-side comparison. Selection never uses the success color.
comparison='<svg xmlns="http://www.w3.org/2000/svg" width="876" height="960" viewBox="0 0 876 960"><rect width="876" height="960" fill="#e8ece3"/><g font-family="Arial,sans-serif">'
for col,(state,label) in enumerate([(1,'Ввод · ещё не проверен'),(3,'Верно · проверка завершена')]):
 x=32+col*420
 comparison+=text(x,40,label,18,weight=600)+f'<g transform="translate({x} 64)">'+screen(state).replace('id="cells"',f'id="compare{col}"').replace('url(#cells)',f'url(#compare{col})')+'</g>'
(out/'practice-input-vs-correct.svg').write_text(comparison+'</g></svg>')

board='<svg xmlns="http://www.w3.org/2000/svg" width="1180" height="650" viewBox="0 0 1180 650"><g font-family="Arial,sans-serif">'
board+=rect(0,0,1180,650,'#e8ece3',r=0)+text(32,42,'UI-CELL · выбор и результат — разные признаки',25,weight=600)
board+=text(32,72,'Синий пунктир = ввод. Зелёная сплошная рамка = верно. Черта снизу = выбрано.',16,muted)
items=[('Пустая','empty',True),('Ввод','editing',True),('Не проверен','editing',False),('Верно','correct',True),('Неверно','incorrect',True),('Подсказка','hint',False)]
for row,dark in enumerate([False,True]):
 y=100+row*260
 bg='#181e19' if dark else '#faf8f1'; fg='#e7e9df' if dark else ink
 b='#9cc6eb' if dark else blue; g='#adcdb0' if dark else green
 er='#ffb59f' if dark else '#a64935'; normal='#202720' if dark else '#fffdf7'
 board+=rect(16,y,1148,244,bg,r=20)+text(36,y+32,'Тёмная тема' if dark else 'Светлая тема',18,fg,600)
 for col,(label,state,selected) in enumerate(items):
  x=36+col*188
  fill=('#23384d' if dark else '#eaf1f8') if state=='editing' else ('#244f32' if dark else '#ddedd8') if state=='correct' else ('#613125' if dark else '#ffede6') if state=='incorrect' else ('#354038' if dark else '#edeae2') if state=='hint' else normal
  border=g if state=='correct' else er if state=='incorrect' else b if selected or state=='editing' else '#758178'
  cell=rect(x,y+52,96,96,fill,border,18,3 if selected or state in ('correct','incorrect') else 1.5)
  if state=='editing': cell=cell.replace('/>',' stroke-dasharray="6 4.5"/>')
  board+=cell
  if state!='empty': board+=drawing(x+12,y+64,72,partial=state=='editing').replace(ink,fg)
  if selected: board+=rect(x+30,y+156,36,4.5,b,r=2)
  if state in ('correct','incorrect'):
   color=g if state=='correct' else er
   board+=icon(x+13,y+184,'check' if state=='correct' else 'error',color)+text(x+34,y+190,label,16,color,600)
  else: board+=text(x,y+190,label,16,b if state=='editing' else fg,600)
  board+=text(x,y+216,'Выбрана' if selected else 'Не выбрана',13,fg)
board+='</g></svg>'
(out/'cell-states.svg').write_text(board)
for name in ['cell-states.svg','practice-input-vs-correct.svg']: ET.parse(out/name)

marks='<svg xmlns="http://www.w3.org/2000/svg" width="880" height="370" viewBox="0 0 880 370"><g font-family="Arial,sans-serif">'
marks+=rect(0,0,880,370,'#e8ece3',r=0)+text(28,38,'UI-STATUS · отметка учителя',25,weight=600)
marks+=text(28,68,'Отдельный рисунок 26 × 26 dp · штрих 2,5 dp · отступ от текста 6 dp',15,muted)
for row,dark in enumerate([False,True]):
 y=92+row*132
 bg='#181e19' if dark else '#faf8f1'; fg='#e7e9df' if dark else ink
 g='#adcdb0' if dark else green; e='#ffb59f' if dark else '#a64935'
 marks+=rect(16,y,848,116,bg,r=16)+text(36,y+28,'Тёмная тема' if dark else 'Светлая тема',14,fg)
 marks+=icon(58,y+74,'check',g)+text(77,y+80,'Верно',18,g,600)
 marks+=icon(360,y+74,'error',e)+text(379,y+80,'Неверно',18,e,600)
marks+='</g></svg>'
(out/'status-marks.svg').write_text(marks)
ET.parse(out/'status-marks.svg')
