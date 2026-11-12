'use strict';

const $=id=>document.getElementById(id);
const storeKey='port-metamorph-session';
let token=sessionStorage.getItem(storeKey)||'';
let lastOnline=false;
let draftDirty=false;
let pendingConfig=null;
let state={
 name:'Normal',profile:'normal',enabled:false,wings:false,nexus:false,horns:false,
 wingScale:1,wingSpeed:2,wingMotion:25,armScale:1,transitionSeconds:1,material:'gold',cubes:[]
};
const presets={
 normal:{name:'Normal',wings:false,nexus:false,horns:false},
 nexus:{name:'Nexus Aterna',wings:false,nexus:true,horns:false},
 angel:{name:'Forma Angelical',wings:true,nexus:false,horns:false},
 hybrid:{name:'Forma Híbrida',wings:true,nexus:true,horns:true}
};
const labels={
 wingScale:{unit:'×',decimals:2},wingSpeed:{unit:'',decimals:1},
 wingMotion:{unit:'°',decimals:0},armScale:{unit:'×',decimals:2},
 transitionSeconds:{unit:' s',decimals:1}
};
function msg(message){$('pairStatus').textContent=message;}
async function api(path,bodyValue=null,authorize=false){
 const headers={'Accept':'application/json'};
 if(bodyValue!==null)headers['Content-Type']='application/json';
 if(authorize&&token)headers.Authorization='Bearer '+token;
 const response=await fetch(path,{
  method:bodyValue===null?'GET':'POST',headers,
  body:bodyValue===null?undefined:JSON.stringify(bodyValue),
  cache:'no-store'
 });
 let data={};
 try{data=await response.json();}catch{}
 if(!response.ok)throw new Error(data.error||'Falha de comunicação: '+response.status);
 return data;
}
function clearSession(){
 token='';sessionStorage.removeItem(storeKey);lastOnline=false;renderConnection();
}
function renderConnection(){
 $('connectionChip').classList.toggle('connected',!!token&&lastOnline);
 $('connectionChip').classList.toggle('offline',!!token&&!lastOnline);
 $('connectionLabel').textContent=!token?'Não vinculado':lastOnline?'Minecraft conectado':'Pareado / Jogo offline';
 $('pairedActions').hidden=!token;
 $('applyButton').disabled=!token;
 $('modeTitle').textContent=token?'Painel vinculado':'Modo demonstração';
 $('modeCaption').textContent=token?
  'Envie configurações para o mod aberto no Minecraft.':
  'Vincule o mod para aplicar essas configurações no jogo.';
}
function preview(){
 $('wingPreview').classList.toggle('is-hidden',!state.enabled||!state.wings);
 $('nexusPreview').classList.toggle('is-hidden',!state.enabled||!state.nexus);
 $('hornPreview').classList.toggle('is-hidden',!state.enabled||!state.horns);
 $('previewName').textContent=state.name||'Personalizada';
 $('previewModules').textContent=String(Number(state.wings)+Number(state.nexus)+Number(state.horns)+state.cubes.length)+' MÓDULOS';
 const kinds={'gold':['#fff0ae','#c79a5e','#6e402b'],'iron':['#f0f2f3','#a6b3b8','#56666d'],
 'netherite':['#91849b','#514656','#27222c'],'amethyst':['#e8c7fa','#9c6bc9','#523379'],
 'redstone':['#ff9e91','#c33c38','#68222c'],'copper':['#f1c092','#c27a50','#77412c']};
 const k=kinds[state.material]||kinds.gold;
 const stops=document.querySelectorAll('#metalGrad stop');
 for(let i=0;i<stops.length;i++)stops[i].setAttribute('stop-color',k[i]);
 for(const [prop,meta] of Object.entries(labels)){
  $(prop+'Out').textContent=Number(state[prop]).toLocaleString('pt-BR',{minimumFractionDigits:meta.decimals,maximumFractionDigits:meta.decimals})+meta.unit;
 }
 for(const b of document.querySelectorAll('[data-preset]'))
  b.classList.toggle('current',b.dataset.preset===state.profile);
}
function renderControls(){
 $('formName').value=state.name;
 $('wings').checked=state.wings;$('nexus').checked=state.nexus;$('horns').checked=state.horns;
 $('material').value=state.material;
 Object.keys(labels).forEach(prop=>$(prop).value=state[prop]);
 $('offButton').textContent=state.enabled?'Desativar forma':'Ativar forma';
 preview();renderParts();
}
function modified(){
 draftDirty=true;
 if(state.profile!=='custom')state.profile='custom';
 if(!state.enabled)state.enabled=true;
 preview();
}
for(const b of document.querySelectorAll('[data-preset]')){
 b.addEventListener('click',()=>{
  const name=b.dataset.preset;
  state={...state,...presets[name],profile:name,enabled:true};
  draftDirty=true;
  renderControls();
 });
}
$('formName').addEventListener('input',e=>{state.name=e.target.value.slice(0,40);state.profile='custom';draftDirty=true;preview()});
for(const key of ['wings','nexus','horns']){
 $(key).addEventListener('change',e=>{state[key]=e.target.checked;modified();});
}
$('material').addEventListener('change',e=>{state.material=e.target.value;modified();});
for(const key of Object.keys(labels)){
 $(key).addEventListener('input',e=>{state[key]=Number(e.target.value);modified();});
}
function inputFor(part,key,label,min,max,step){
 const wrap=document.createElement('label');
 wrap.textContent=label;
 const inp=document.createElement('input');inp.type='number';inp.min=min;inp.max=max;inp.step=step;
 inp.value=part[key];inp.addEventListener('change',()=>{
  const raw=Number(inp.value);
  part[key]=Number.isFinite(raw)?Math.min(max,Math.max(min,raw)):part[key];
  inp.value=part[key];modified();
 });
 wrap.append(inp);return wrap;
}
function choiceFor(part,key,label,options){
 const wrap=document.createElement('label');wrap.textContent=label;
 const sel=document.createElement('select');
 for(const [value,text] of options){
  const o=document.createElement('option');o.value=value;o.textContent=text;sel.append(o);
 }
 sel.value=part[key];
 sel.addEventListener('change',()=>{part[key]=sel.value;modified();});
 wrap.append(sel);return wrap;
}
function renderParts(){
 const list=$('partsList');list.replaceChildren();
 $('partCount').textContent=state.cubes.length+' / 24 peças';
 state.cubes.forEach((part,index)=>{
  const card=document.createElement('div');card.className='part';
  const head=document.createElement('div');head.className='part-header';
  const title=document.createElement('strong');title.textContent='PEÇA '+String(index+1).padStart(2,'0');
  const remove=document.createElement('button');remove.className='subtle-button danger';remove.textContent='Excluir';
  remove.addEventListener('click',()=>{state.cubes.splice(index,1);modified();renderParts()});
  head.append(title,remove);card.append(head);
  const grid=document.createElement('div');grid.className='part-grid';
  grid.append(
   choiceFor(part,'material','Material',[['gold','Ouro'],['iron','Ferro'],['netherite','Netherita'],['amethyst','Ametista'],['redstone','Redstone'],['copper','Cobre']]),
   inputFor(part,'x','Posição X',-1.7,1.7,.05),
   inputFor(part,'y','Altura Y',0,2.8,.05),
   inputFor(part,'z','Posição Z',-1.7,1.7,.05),
   inputFor(part,'sx','Largura',.02,.9,.05),
   inputFor(part,'sy','Altura',.02,.9,.05),
   inputFor(part,'sz','Profundidade',.02,.9,.05)
  );
  card.append(grid);list.append(card);
 });
}
$('bbFile').addEventListener('change',async event=>{
 const file=event.target.files?.[0];
 if(!file)return;
 try{
  if(file.size>3_000_000)throw new Error('Arquivo muito grande. Limite de 3 MB.');
  const source=JSON.parse(await file.text());
  const list=Array.isArray(source.elements)?source.elements:[];
  const cubes=list.filter(e=>(!e.type||e.type==='cube')&&Array.isArray(e.from)&&
   Array.isArray(e.to)&&e.from.length===3&&e.to.length===3&&
   [...e.from,...e.to].every(v=>typeof v==='number'&&Number.isFinite(v)))
   .map(e=>({
    from:e.from,to:e.to,
    volume:Math.abs(e.to[0]-e.from[0])*Math.abs(e.to[1]-e.from[1])*Math.abs(e.to[2]-e.from[2]),
    rotated:Array.isArray(e.rotation)&&e.rotation.some(n=>Number(n)!==0)
   }))
   .filter(e=>e.volume>0);
  if(!cubes.length)throw new Error('Nenhum cubo compatível em elements[]. Confira a exportação do Blockbench.');
  const boundMin=[0,1,2].map(a=>Math.min(...cubes.map(c=>Math.min(c.from[a],c.to[a]))));
  const boundMax=[0,1,2].map(a=>Math.max(...cubes.map(c=>Math.max(c.from[a],c.to[a]))));
  const span=Math.max(...boundMax.map((a,i)=>a-boundMin[i]));
  const scale=Math.min(1,22/Math.max(1,span));
  const center=boundMax.map((v,i)=>(v+boundMin[i])/2);
  const clamp=(v,lo,hi)=>Math.max(lo,Math.min(hi,v));
  const selected=[...cubes].sort((a,b)=>b.volume-a.volume).slice(0,24);
  state.cubes=selected.map(c=>{
   const centerAt=[0,1,2].map(i=>(c.from[i]+c.to[i])/2);
   const size=[0,1,2].map(i=>clamp(Math.abs(c.to[i]-c.from[i])/16*scale,.02,.9));
   return {
    anchor:'body',material:state.material,
    x:clamp((centerAt[0]-center[0])/16*scale,-1.7,1.7),
    y:clamp(1.32+(centerAt[1]-center[1])/16*scale,0,2.8),
    z:clamp((centerAt[2]-center[2])/16*scale,-1.7,1.7),
    sx:size[0],sy:size[1],sz:size[2]
   };
  });
  state.profile='custom';state.name=String(file.name).replace(/\.bbmodel$|\.json$/i,'').slice(0,40)||'Modelo importado';
  state.enabled=true;draftDirty=true;
  renderControls();
  const truncated=cubes.length>24?' Foram utilizados os 24 cubos de maior volume.':'';
  const rotations=cubes.some(c=>c.rotated)?' Rotações internas foram ignoradas.':'';
  const detail=cubes.length+' cubos detectados.'+truncated+rotations+' Texturas e bones não são preservados.';
  $('importNote').textContent=detail;
  msg('Geometria carregada localmente. Revise peças e clique em Aplicar no Minecraft.');
 }catch(error){
  $('importNote').textContent=error.message;
  msg('Falha ao importar: '+error.message);
 }finally{
  event.target.value='';
 }
});

$('addPart').addEventListener('click',()=>{
 if(state.cubes.length>=24){msg('Limite de 24 peças atingido.');return;}
 state.cubes.push({anchor:'body',material:state.material,x:0,y:1.4,z:.32,sx:.15,sy:.15,sz:.15});
 modified();renderParts();
});
$('offButton').addEventListener('click',()=>{
 state.enabled=!state.enabled;
 draftDirty=true;
 renderControls();
 msg(state.enabled?'Forma ativada nesta prévia. Clique em Aplicar para sincronizar.':'Forma desativada nesta prévia. Clique em Aplicar para sincronizar.');
});
$('applyButton').addEventListener('click',async()=>{
 if(!token){msg('Vincule o Minecraft primeiro.');return;}
 const b=$('applyButton');b.disabled=true;b.textContent='Enviando...';
 try{
  await api('/api/control',{config:state},true);
  pendingConfig=JSON.stringify(state);
  draftDirty=false;
  msg('Configuração enviada. O mod aplicará na próxima consulta (normalmente em até 4 segundos).');
 }catch(e){
  msg(e.message);
  if(e.message.includes('Sessão')||e.message.includes('dispositivo'))clearSession();
 }finally{b.textContent='Aplicar no Minecraft';b.disabled=!token;}
});
$('pairButton').addEventListener('click',async()=>{
 const code=$('pairCode').value.trim().toUpperCase();
 if(!/^[0-9A-F]{8}$/.test(code)){msg('Digite um código hexadecimal de oito caracteres.');return;}
 const button=$('pairButton');button.disabled=true;
 try{
  const result=await api('/api/pair',{code});
  token=result.token;sessionStorage.setItem(storeKey,token);$('pairCode').value='';
  msg('Vinculação concluída. Carregando a configuração atual do jogo...');
  await refresh();
 }catch(e){msg(e.message)}
 finally{button.disabled=false}
});
async function refresh(){
 if(!token){renderConnection();return;}
 try{
  const data=await api('/api/me',null,true);
  lastOnline=!!data.online;
  if(data.config){
   if(pendingConfig&&JSON.stringify(data.config)===pendingConfig)pendingConfig=null;
   if(!draftDirty&&!pendingConfig){
    state={...state,...data.config,cubes:Array.isArray(data.config.cubes)?data.config.cubes:[]};
    renderControls();
   }
  }
  renderConnection();
  if(lastOnline)msg('Dispositivo online. Última sincronização: '+new Date(data.lastSeen).toLocaleTimeString('pt-BR')+'.');
  else msg('Vinculado, mas o Minecraft não está respondendo. Abra o jogo e verifique /metamorph status.');
 }catch(e){
  if(/Sessão|desvinculado|dispositivo/i.test(e.message))clearSession();
  msg(e.message);
 }
}
$('refreshButton').addEventListener('click',refresh);
$('unpairButton').addEventListener('click',async()=>{
 try{await api('/api/unpair',{},true);}catch{}
 clearSession();msg('Desvinculado. Gere um novo código no Minecraft para reconectar.');
});
setInterval(()=>{if(token)refresh()},8000);
renderControls();renderConnection();
if(token)refresh();
