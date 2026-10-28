/* Port Voxel Viewer v2.0: biblioteca e edicao local */
(()=>{'use strict';
const api=window.PortVoxelViewer, el=id=>document.getElementById(id);
if(!api)return;
const notify=s=>{const t=el('toast');t.textContent=s;t.classList.remove('hidden');clearTimeout(notify.id);notify.id=setTimeout(()=>t.classList.add('hidden'),4500);};
const safe=s=>String(s||'Modelo').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^A-Za-z0-9_-]+/g,'_').slice(0,60);
const panel=document.createElement('section');panel.className='v2-tools';panel.innerHTML=[
'<h2>✦ Port Voxel Studio <small>v2.0</small></h2>',
'<details><summary>▦ Biblioteca de modelos</summary><div class="v2-body">',
'<button id="v2Store">Guardar modelo atual</button><div id="v2Gallery"></div>',
'<small>Salvos neste dispositivo. Limpar dados do aplicativo pode apagar os projetos.</small>',
'</div></details>',
'<details><summary>✎ Editar peças</summary><div class="v2-body">',
'<label>Buscar grupo ou cubo<input id="v2Search" placeholder="Ex.: placa, folha, corrente"></label>',
'<select id="v2Part"></select><small id="v2Kind"></small>',
'<b>Rotação em graus</b><div class="v2-three">',
'<label>X<input type="number" id="v2Rx" step="1"></label>',
'<label>Y<input type="number" id="v2Ry" step="1"></label>',
'<label>Z<input type="number" id="v2Rz" step="1"></label></div>',
'<div id="v2Cube"><b>Centro do cubo</b><div class="v2-three">',
'<label>X<input type="number" id="v2Cx" step="0.25"></label>',
'<label>Y<input type="number" id="v2Cy" step="0.25"></label>',
'<label>Z<input type="number" id="v2Cz" step="0.25"></label></div>',
'<b>Tamanho do cubo</b><div class="v2-three">',
'<label>X<input type="number" id="v2Sx" step="0.25"></label>',
'<label>Y<input type="number" id="v2Sy" step="0.25"></label>',
'<label>Z<input type="number" id="v2Sz" step="0.25"></label></div></div>',
'<div class="v2-two"><button id="v2Apply">Aplicar</button><button id="v2Undo">Desfazer</button></div>',
'<button id="v2Download">↓ Exportar .bbmodel editado</button>',
'<small>Suporta dimensões e rotação de cubos e rotação de grupos. Malhas avançadas ficam em modo visualização.</small>',
'</div></details>',
'<div id="v2Extra"></div>'
].join('');
const style=document.createElement('style');style.textContent='.v2-tools{padding:18px 20px 26px;border-top:1px solid #494052;background:#151826;color:#e5d8d7}.v2-tools h2{font-size:17px;color:#f5c68a}.v2-tools h2 small{font-size:10px;color:#b9aabb}.v2-tools details{margin:10px 0;border:1px solid #4b4150;border-radius:10px;overflow:hidden;background:#1a1d2c}.v2-tools summary{padding:13px;font-size:12px;font-weight:800;color:#ebc08d;cursor:pointer}.v2-body{padding:12px;display:flex;flex-direction:column;gap:10px}.v2-tools input,.v2-tools select{box-sizing:border-box;width:100%;min-width:0;background:#111624;color:#f6eee7;border:1px solid #52495e;padding:9px;border-radius:7px;font-size:13px}.v2-tools label{font-size:11px;color:#b9b8c8}.v2-tools label input{margin-top:5px}.v2-tools button{padding:10px;background:#5a3c4d;border:1px solid #ac766d;color:#fff0d8;border-radius:8px;font-size:12px;min-height:34px}.v2-tools small{font-size:10px;color:#a4a3b6;line-height:1.6}.v2-tools b{font-size:11px;color:#e8c28c}.v2-three{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:7px}.v2-two{display:grid;grid-template-columns:1fr 1fr;gap:8px}#v2Gallery{max-height:230px;overflow:auto}.v2-item{display:flex;align-items:center;gap:7px;border-bottom:1px solid #37384a;padding:7px 0}.v2-item img{width:42px;height:42px;object-fit:cover}.v2-item b{flex:1;overflow-wrap:anywhere}.v2-item button{padding:7px;font-size:10px}.v2-tools input[type=range]{padding:0;accent-color:#d59a71}.v2-tools button:disabled{opacity:.5}';
document.head.append(style);document.querySelector('.control-panel').append(panel);
const v2={api,panel,notify,safe,saveBlob:async(blob,name,mime)=>{if(window.PortAndroid?.saveFile){const url=await new Promise((resolve,reject)=>{const f=new FileReader();f.onload=()=>resolve(f.result);f.onerror=reject;f.readAsDataURL(blob);});window.PortAndroid.saveFile(url,name,mime);}
else{const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),30000);}notify('Exportação enviada para salvar.');}};
window.PortV2=v2;
let databasePromise;
const database=()=>{if(databasePromise)return databasePromise;databasePromise=new Promise((resolve,reject)=>{const r=indexedDB.open('port-voxel-v2',1);r.onupgradeneeded=()=>r.result.createObjectStore('models',{keyPath:'id',autoIncrement:true});r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error);});return databasePromise;};
async function op(method,value){let db=null;
try{db=await database();}catch(error){console.warn('IndexedDB indisponível, usando biblioteca básica local',error);}
if(!db){
  const key='port-voxel-v2-library-fallback';
  let entries=[];try{entries=JSON.parse(localStorage.getItem(key)||'[]');}catch(error){}
  if(method==='getAll')return entries;
  if(method==='get')return entries.find(x=>x.id===value);
  if(method==='delete'){entries=entries.filter(x=>x.id!==value);localStorage.setItem(key,JSON.stringify(entries));return;}
  if(method==='put'){value.id=Date.now();entries.push(value);localStorage.setItem(key,JSON.stringify(entries));return value.id;}
  throw Error('Ação inválida na biblioteca');
}
return new Promise((res,rej)=>{const tr=db.transaction('models',method==='getAll'||method==='get'?'readonly':'readwrite'),store=tr.objectStore('models');const r=method==='put'?store.add(value):store[method](...(value===undefined?[]:[value]));r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error);});}
function thumbnail(){const src=api.getCanvas(),c=document.createElement('canvas');c.width=128;c.height=128;const x=c.getContext('2d'),r=Math.min(128/Math.max(src.width,1),128/Math.max(src.height,1));x.fillStyle='#181724';x.fillRect(0,0,128,128);x.drawImage(src,(128-src.width*r)/2,(128-src.height*r)/2,src.width*r,src.height*r);return c.toDataURL('image/jpeg',.6);}
async function gallery(){const root=el('v2Gallery');root.replaceChildren();try{const rows=(await op('getAll')).sort((a,b)=>b.date-a.date);if(!rows.length){root.textContent='Biblioteca vazia.';return;}for(const entry of rows){const item=document.createElement('div');item.className='v2-item';const img=document.createElement('img');img.alt='Miniatura';img.src=entry.thumb;const title=document.createElement('b');title.textContent=entry.name;const open=document.createElement('button');open.textContent='Abrir';open.onclick=async()=>{try{const d=await op('get',entry.id);api.openModel(JSON.parse(d.json),d.name+'.bbmodel',false);notify('Modelo restaurado.');}catch(e){notify('Erro: '+e.message);}};const del=document.createElement('button');del.textContent='×';del.onclick=async()=>{if(confirm('Excluir modelo desta biblioteca?')){await op('delete',entry.id);gallery();}};item.append(img,title,open,del);root.append(item);}}catch(e){root.textContent='Biblioteca indisponível: '+e.message;}}
el('v2Store').onclick=async()=>{try{const j=api.getModel();if(!j)throw Error('Abra um modelo');const json=JSON.stringify(j);if(json.length>16*1024*1024)throw Error('Projeto excede 16 MB');await op('put',{name:j.name||'Modelo',json,thumb:thumbnail(),date:Date.now()});await gallery();notify('Modelo guardado neste dispositivo.');}catch(e){notify(e.message);}};
let selected='',undo=[];
const num=id=>Number(el(id).value);
function selectedPart(){const d=api.getNodes().find(x=>x.id===selected);el('v2Kind').textContent=d?(d.type==='group'?'Grupo · editar rotação':d.from&&d.to?'Cubo · posição, tamanho e rotação':'Malha · editar rotação'):'Nenhuma peça';for(let i=0;i<3;i++)el('v2R'+'XYZ'[i]).value=d?.rotation?.[i]??0;const cube=!!(d&&Array.isArray(d.from)&&Array.isArray(d.to));el('v2Cube').classList.toggle('hidden',!cube);if(cube)for(let i=0;i<3;i++){el('v2C'+'XYZ'[i]).value=((Number(d.from[i])+Number(d.to[i]))/2).toFixed(2);el('v2S'+'XYZ'[i]).value=Math.abs(d.to[i]-d.from[i]).toFixed(2);}}
function pieces(){const q=el('v2Search').value.toLocaleLowerCase('pt-BR'),list=api.getNodes().filter(n=>String(n.name).toLocaleLowerCase('pt-BR').includes(q)).slice(0,400),s=el('v2Part');s.replaceChildren();for(const part of list){const o=document.createElement('option');o.value=part.id;o.textContent=(part.type==='group'?'▤ ':'▪ ')+part.name;s.append(o);}if(!list.some(n=>n.id===selected))selected=list[0]?.id||'';s.value=selected;selectedPart();}
el('v2Search').oninput=pieces;el('v2Part').onchange=e=>{selected=e.target.value;selectedPart();};
el('v2Apply').onclick=()=>{try{const p=api.getNodes().find(n=>n.id===selected);if(!p)throw Error('Selecione uma peça');const rotation=['X','Y','Z'].map(ax=>num('v2R'+ax));if(rotation.some(n=>!Number.isFinite(n)))throw Error('Rotação inválida');const update={rotation};if(Array.isArray(p.from)&&Array.isArray(p.to)){const center=['X','Y','Z'].map(ax=>num('v2C'+ax)),size=['X','Y','Z'].map(ax=>num('v2S'+ax));if([...center,...size].some(x=>!Number.isFinite(x))||size.some(x=>x<.01))throw Error('Tamanho inválido');update.from=center.map((v,i)=>v-size[i]/2);update.to=center.map((v,i)=>v+size[i]/2);}const snap=JSON.stringify(api.getModel()),camera=api.getCamera();api.editPart(selected,update);undo.push({snap,camera});if(undo.length>12)undo.shift();notify('Alteração aplicada. Exporte a cópia para preservar.');pieces();}catch(e){notify(e.message);}};
el('v2Undo').onclick=()=>{const last=undo.pop();if(!last)return notify('Nada para desfazer');api.openModel(JSON.parse(last.snap),'edicao_restaurada.bbmodel',false);api.setCamera(last.camera);pieces();notify('Desfeito.');};
el('v2Download').onclick=async()=>{try{const j=api.getModel();await v2.saveBlob(new Blob([JSON.stringify(j,null,2)],{type:'application/json'}),safe(j?.name)+'_editado.bbmodel','application/json');}catch(e){notify(e.message);}};
window.addEventListener('port-model-opened',()=>{selected='';undo=[];pieces();});
pieces();gallery();
})();