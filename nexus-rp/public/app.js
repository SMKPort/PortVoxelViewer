"use strict";
(()=>{
const KEY="nexus-aterna-rp-v1";
const ACCESS_REMEMBER_KEY="nexus-aterna-rp-remember-access-v1";
const PRESETS=[
{id:"bencao",name:"Bênção Divina",school:"Divina",cost:16,description:"A bênção de Anael percorre as placas douradas do Nexus, estabilizando o núcleo."},
{id:"selo",name:"Selo de Contenção",school:"Arcana",cost:22,description:"Runas douradas emergem ao redor da prótese e consolidam o selo ritualístico."},
{id:"purificar",name:"Purificação",school:"Divina",cost:27,description:"Uma onda de luz atravessa os conduítes arcanos e dissipa interferências."},
{id:"renovar",name:"Renovação do Núcleo",school:"Híbrida",cost:32,description:"O núcleo é recalibrado e a energia se redistribui entre as placas."},
{id:"custom",name:"Personalizado",school:"Personalizado",cost:20,description:"O Port executa uma sequência de símbolos e componentes escolhidos."}
];
const PAGES={painel:"NÚCLEO",chat:"COMUNICADOR",armadura:"ARMADURA ARCANA",rituais:"RITUAIS",ao_vivo:"RP AO VIVO",ts:"PROTOCOLO TS",sequencias:"SEQUENCIADOR",eventos:"EVENTOS DO JOGO",emergencia:"EMERGÊNCIA",emotes:"EMOTES",cliente:"NEXUS CLIENT",voz:"VOZ DO NEXUS",config:"CONFIGURAÇÕES"};
const DEFAULT=()=>({
integrity:100,energy:85,armor:"integrada",form:"Celestial",selectedRitual:"bencao",
logs:[{id:"origin",at:new Date().toISOString(),kind:"SISTEMA",text:"Nexus Aterna RP inicializado. Aguardando instruções do Port."}],
chat:[],rituals:[],
config:{connectionMode:"client",chatChannel:"local",chatCustomCommand:"rp",chatFormatting:"&6&l",chatUsePrefix:true,host:"",port:25575,player:"Port",prefix:"[Nexus Aterna]",target:"global",recipient:"",color:"white",autoAnnounce:true,functions:{integrar:"",desintegrar:"",remodelar:"",ritual:""}}
});
const state=DEFAULT();
try{
const s=JSON.parse(localStorage.getItem(KEY)||"null");
if(s&&typeof s==="object"){
Object.assign(state,s);
state.config={...DEFAULT().config,...(s.config||{}),functions:{...DEFAULT().config.functions,...(s.config?.functions||{})}};
for(const k of ["logs","chat","rituals"])if(!Array.isArray(state[k]))state[k]=[];
}
}catch(e){}
// Nexus-originated chat messages always carry an explicit sender label, even for existing stored profiles.
if(state.config.connectionMode==="public"&&!state.config.host){state.config.connectionMode="client";}
if(state.config.connectionMode==="client"){state.config.prefix="[Nexus Aterna]";state.config.chatUsePrefix=true;}
const $=x=>document.querySelector(x);
const esc=v=>String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const clamp=(n,a,b)=>Math.min(b,Math.max(a,n));
const date=t=>new Date(t).toLocaleString("pt-BR",{day:"2-digit",month:"2-digit",hour:"2-digit",minute:"2-digit"});
const uid=()=>Math.random().toString(36).slice(2,10)+Date.now().toString(36);
const secrets={access:"",password:""};
let rememberedAccess=false;
// The secret is never injected into HTML or the exportable RP state.
// Only browsers that explicitly opted in retain it between visits.
try{
  const remembered=localStorage.getItem(ACCESS_REMEMBER_KEY);
  if(remembered&&remembered.length>=20&&remembered.length<=512){
    secrets.access=remembered;
    rememberedAccess=true;
  }
}catch(e){}
let connected=false,healthy=false,busy=false;
let page=PAGES[location.hash.slice(1)]?location.hash.slice(1):"painel";
if(page==="cliente"){state.config.connectionMode="client";save();}
function save(){try{localStorage.setItem(KEY,JSON.stringify(state));}catch(e){toast("Não foi possível salvar os registros locais.");}}
function log(text,kind="RP"){state.logs.unshift({id:uid(),at:new Date().toISOString(),kind,text});state.logs=state.logs.slice(0,140);save();}
function toast(text){const el=document.createElement("div");el.className="toast";el.textContent=text;$("#toasts").appendChild(el);setTimeout(()=>el.remove(),4600);}
function addChat(text,status){state.chat.push({id:uid(),at:new Date().toISOString(),text,status});state.chat=state.chat.slice(-100);save();}
const rp={state,escape:esc,date,presets:PRESETS,isConnected:()=>connected,isHealthy:()=>healthy,save,log,toast,navigate,rerender:render,sendToChat:messageToMinecraft,armorAction,performRitual,runFunctionId:async function(functionId){return api("function",{functionId});},getAccess:()=>secrets.access,isAccessRemembered:()=>rememberedAccess,
forgetAccess:()=>{
  try{localStorage.removeItem(ACCESS_REMEMBER_KEY);}catch(e){}
  rememberedAccess=false;
  secrets.access="";
  connected=false;
  render();
},requestEvents:async function(since,player){if(!secrets.access)throw Error("Informe o código de acesso no menu Configurações.");const isClient=state.config.connectionMode==="client";const url=(isClient?"/api/client/events":"/api/game/events")+"?since="+encodeURIComponent(since)+"&player="+encodeURIComponent(player||"");const response=await fetch(url,{headers:{"X-Nexus-Key":secrets.access}});const result=await response.json();if(!response.ok)throw Error(result.error||"Falha na consulta de eventos.");return result;},requestBridgeStatus:async function(){if(!secrets.access)throw Error("Informe código da ponte no painel.");const response=await fetch(state.config.connectionMode==="client"?"/api/client/status":"/api/bridge/status",{headers:{"X-Nexus-Key":secrets.access}});const result=await response.json();if(!response.ok)throw Error(result.error||"Agente inacessível.");return result;}};
let views=window.NexusRPViews(rp);
if(window.NexusRPUpgrade)views=window.NexusRPUpgrade(rp,views);
if(window.NexusRPEvolution)views=window.NexusRPEvolution(rp,views);
if(window.NexusRPClientUpgrade)views=window.NexusRPClientUpgrade(rp,views);
if(window.NexusRPVoiceUpgrade)views=window.NexusRPVoiceUpgrade(rp,views);

// Preserve advanced pages, but group their menu entries instead of showing a
// dozen primary buttons. All module scripts run before this app initializer.
const PRIMARY_PAGES=new Set(["painel","voz","chat","cliente","config"]);
const advancedItems=document.querySelector("#advancedItems");
if(advancedItems){
  document.querySelectorAll("#nav .nav-button[data-page]").forEach(item=>{
    if(!PRIMARY_PAGES.has(item.dataset.page))advancedItems.appendChild(item);
  });
}
function render(){
$("#content").innerHTML=views[page]?.()||views.painel();
const advancedNav=document.querySelector("#advancedNav");
if(advancedNav&&!PRIMARY_PAGES.has(page))advancedNav.open=true;
$("#pageLabel").textContent=PAGES[page]||"NÚCLEO";
document.querySelectorAll("[data-page]").forEach(el=>{const yes=el.dataset.page===page;el.classList.toggle("active",yes);el.setAttribute("aria-current",yes?"page":"false");});
$("#sideConnection").textContent=connected?"MINECRAFT CONECTADO":"NARRATIVA LOCAL";
$("#topStatus").innerHTML='<span class="light '+(connected?"connected":"")+'"></span> '+(connected?(state.config.connectionMode==="client"?"NEXUS CLIENT CONECTADO":"MINECRAFT CONECTADO"):"MODO LOCAL");
const feed=$("#chatFeed");if(feed)feed.scrollTop=feed.scrollHeight;
}
function setMenu(open){$("#sidebar").classList.toggle("open",open);$("#overlay").hidden=!open;}
function navigate(next){if(!PAGES[next])return;page=next;if(next==="cliente"){state.config.connectionMode="client";connected=false;save();}if(location.hash!=="#"+page)location.hash="#"+page;render();setMenu(false);window.scrollTo({top:0,behavior:"instant"});}
function readConfig(){
const form=$("#configForm");if(!form)return;
const d=new FormData(form),c=state.config;
c.connectionMode=["agent","client"].includes(d.get("connectionMode"))?String(d.get("connectionMode")):"public";
if(d.has("chatChannel")){
 c.chatChannel=["global","local","custom"].includes(d.get("chatChannel"))?String(d.get("chatChannel")):"local";
 c.chatCustomCommand=String(d.get("chatCustomCommand")||"").trim().replace(/^\//,"").toLowerCase().slice(0,24);
 c.chatFormatting=String(d.get("chatFormatting")??"&6&l").trim().toLowerCase();
 if(!/^(?:&[0-9a-fk-or]){0,8}$/.test(c.chatFormatting))throw Error("Código de formatação inválido. Use, por exemplo, &6&l.");
 c.chatUsePrefix=d.has("chatUsePrefix");
}
c.host=String(d.get("host")||"").trim().slice(0,253);
c.port=clamp(Number(d.get("port"))||25575,1024,65535);
c.player=String(d.get("player")||"Port").trim().replace(/[^a-z0-9_]/ig,"").slice(0,16)||"Port";
c.prefix=String(d.get("prefix")||"[NEXUS]").trim().slice(0,38)||"[NEXUS]";
c.target=d.get("target")==="player"?"player":"global";
c.recipient=String(d.get("recipient")||"").trim().slice(0,16);
c.color=String(d.get("color")||"white");
c.autoAnnounce=d.has("autoAnnounce");
if(c.connectionMode==="client"){c.prefix="[Nexus Aterna]";c.chatUsePrefix=true;}
if(d.get("password"))secrets.password=String(d.get("password"));
if(d.get("access"))secrets.access=String(d.get("access")).trim();
if(c.connectionMode==="client"){
  if(d.has("rememberAccess")){
    if(!secrets.access)throw Error("Digite a chave HTTPS uma vez para ativar a opção de lembrar neste dispositivo.");
    try{
      localStorage.setItem(ACCESS_REMEMBER_KEY,secrets.access);
      rememberedAccess=true;
    }catch(e){
      rememberedAccess=false;
      throw Error("O navegador bloqueou o armazenamento. A chave ficará disponível apenas nesta aba.");
    }
  }else{
    try{localStorage.removeItem(ACCESS_REMEMBER_KEY);}catch(e){}
    rememberedAccess=false;
  }
}
save();
}
async function api(route,payload={}){
if(!secrets.access)throw Error("Digite o código de acesso em Configurações.");
if(!["agent","client"].includes(state.config.connectionMode)&&!secrets.password)throw Error("Digite a senha RCON em Configurações.");
if(!["agent","client"].includes(state.config.connectionMode)&&!state.config.host)throw Error("Informe o host público do seu Minecraft.");
if(state.config.connectionMode==="client"){
 const run=rp.clientQueue;
 if(!run)throw Error("Nexus Client não carregado.");
 if(route==="test"){
   const res=await fetch("/api/client/status",{headers:{"X-Nexus-Key":secrets.access,"Accept":"application/json"},cache:"no-store"});
   const contentType=String(res.headers.get("content-type")||"").toLowerCase();
   if(!contentType.includes("application/json"))
     throw Error("A API do Nexus devolveu uma página em vez de JSON (HTTP "+res.status+"). Verifique em Nexus Client > DIAGNÓSTICO DA API.");
   let result;
   try{result=await res.json();}catch(err){throw Error("Resposta JSON inválida da Render (HTTP "+res.status+"). Use Diagnóstico da API.");}
   if(!res.ok||result.ok===false)throw Error(result.error||"Erro HTTP "+res.status+" na API do Nexus.");
   if(!result.online)throw Error("A API respondeu corretamente, mas o mod está offline. Faça o pareamento no Minecraft.");
   return {ok:true,response:"Nexus Client conectado: "+result.player};
 }
 if(route==="message"){
   const base=String(payload.message||"").trim();
   const channel=["local","global","custom"].includes(state.config.chatChannel)?state.config.chatChannel:"local";
   const label="[Nexus Aterna]";
   const cleaned=base.replace(/^(?:\[nexus\s+aterna\]\s*)+/i,"");
   const message=(label+" "+cleaned).trim();
   if(!message||message.length>230)throw Error("A mensagem deve ter entre 1 e 230 caracteres. Reduza o texto ou remova o prefixo.");
   const format=channel==="local"?(state.config.chatFormatting??"&6&l"):"";
   if(!/^(?:&[0-9a-fk-or]){0,8}$/.test(format))throw Error("Código de formatação inválido.");
   return run("/chat",{message,channel,command:state.config.chatCustomCommand||"",format});
 }
 if(route==="action")return run("/action",payload);
 throw Error("O Nexus Client não pode executar funções administrativas do servidor.");
}
const ctrl=new AbortController(),timer=setTimeout(()=>ctrl.abort(),state.config.connectionMode==="agent"?25000:14500);
try{
const res=await fetch("/api/rcon/"+route,{method:"POST",headers:{"Content-Type":"application/json","X-Nexus-Key":secrets.access},body:JSON.stringify({server:{mode:state.config.connectionMode||"public",host:state.config.host,port:state.config.port,password:secrets.password},...payload}),signal:ctrl.signal});
const out=await res.json().catch(()=>({}));
if(!res.ok||!out.ok)throw Error(out.error||"Erro HTTP "+res.status);
if(out.queued&&out.jobId){
  for(let i=0;i<17;i++){
    await new Promise(resolve=>setTimeout(resolve,900));
    const statusRes=await fetch("/api/bridge/result?id="+encodeURIComponent(out.jobId),{headers:{"X-Nexus-Key":secrets.access},signal:ctrl.signal});
    const job=await statusRes.json();
    if(!statusRes.ok)throw Error(job.error||"Comando desconhecido.");
    if(job.done){if(!job.success)throw Error(job.error||"Agente não executou o comando.");return {ok:true,response:job.output,bridge:"agent"};}
  }
  throw Error("Agente não confirmou o comando. Verifique se está online.");
}
return out;
}catch(e){if(e.name==="AbortError")throw Error("O Minecraft não respondeu. Confira a porta, o firewall e a conectividade da Render.");throw e;}
finally{clearTimeout(timer);}
}
async function testConnection(){
if(busy)return;try{readConfig();}catch(e){return toast(e.message);}busy=true;toast(state.config.connectionMode==="client"?"Verificando se o mod está pareado...":"Verificando acesso ao servidor...");
try{const out=await api("test");connected=true;log("Conexão Minecraft confirmada: "+String(out.response||"").slice(0,150),"MINECRAFT");toast("Minecraft conectado com sucesso!");}
catch(e){connected=false;log("Falha de conexão: "+e.message,"CONEXÃO");toast(e.message);}
finally{busy=false;render();}
}
async function messageToMinecraft(text,category="COMUNICADOR"){
const c=state.config;
if(c.connectionMode==="client"&&c.target==="player")throw Error("No modo Nexus Client, a mensagem é enviada ao chat geral pela sua conta.");
if(c.target==="player"&&!/^[A-Za-z0-9_]{3,16}$/.test(c.recipient))throw Error("Informe um jogador de destino válido.");
await api("message",{message:text,target:c.target,player:c.recipient,prefix:c.prefix,color:c.color});
connected=true;addChat(text,"sent");log("Transmitido ao Minecraft ("+category+"): "+text,"MINECRAFT");return true;
}
async function gameFunction(which){
if(state.config.connectionMode==="client"){
  if(!connected)return false;
  try{await api("action",{action:which,form:state.form});return true;}catch(e){toast("Ação local não executada: "+e.message);return false;}
}
const fn=state.config.functions?.[which];
if(!connected||!fn)return false;
if(!/^[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,90}$/.test(fn)){toast("Função configurada inválida: "+which);return false;}
try{await api("function",{functionId:fn});log("Função do Minecraft enviada: "+fn,"MINECRAFT");return true;}
catch(e){log("Erro ao executar "+fn+": "+e.message,"CONEXÃO");toast("Falha na função: "+e.message);return false;}
}
async function perform(kind,message){
if(!connected)return;
if(state.config.autoAnnounce){
try{await messageToMinecraft(message.slice(0,280),kind.toUpperCase());toast("Status enviado ao Minecraft.");}
catch(e){toast("Status não enviado: "+e.message);}
}
await gameFunction(kind);
}
async function armorAction(type){
if(rp.canArmorAction&&!rp.canArmorAction(type))return toast("Protocolo de emergência: transformação temporariamente bloqueada.");
const selectedForm=$("#armorForm")?.value||state.form;
const remodelCost=rp.formCost?.(selectedForm)??9;
const required=type==="integrar"?12:type==="remodelar"?remodelCost:type==="reparar"?14:0;
if(state.energy<required)return toast("Energia narrativa insuficiente.");
let text="",cost=0;
if(type==="integrar"){
if(state.armor==="integrada")return toast("A armadura já está integrada.");
cost=12;state.armor="integrada";text="Integrando armadura";
}else if(type==="desintegrar"){
if(state.armor==="desintegrada")return toast("As placas já estão dispersas.");
state.armor="desintegrada";state.energy=clamp(state.energy+5,0,100);text="Desintegrando armadura";
}else if(type==="remodelar"){
cost=remodelCost;const form=$("#armorForm")?.value||state.form;state.form=(rp.allowedForms?.()||["Celestial","Combate","Ritual","Repouso"]).includes(form)?form:"Celestial";
text="Remodelando armadura: "+state.form;
}else if(type==="reparar"){
cost=14;state.integrity=clamp(state.integrity+18,0,100);
text="Reparando armadura";
}else return;
if(state.energy<cost){toast("Energia narrativa insuficiente.");return;}
state.energy-=cost;rp.afterArmor?.(type);log(text,"ARMADURA");save();render();toast("Estado narrativo atualizado.");
await perform(type,text);
}
async function performRitual(form){
const data=new FormData(form),name=String(data.get("name")||"").trim().slice(0,100),
description=String(data.get("description")||"").trim().slice(0,600),cost=clamp(Number(data.get("cost")),0,100);
if(!name||!description)throw Error("Defina nome e descrição do ritual.");
if(state.energy<cost)throw Error("Energia narrativa insuficiente.");
const score=clamp(Math.round(38+state.integrity*.38+state.energy*.2-cost*.27),0,99);
state.energy-=cost;
state.rituals.unshift({id:uid(),at:new Date().toISOString(),name,description,cost,score});state.rituals=state.rituals.slice(0,80);
log("Ritual "+name+" realizado: "+description+" (estabilidade "+score+"%).","RITUAL");rp.afterRitual?.();save();render();toast("Ritual narrativo concluído.");
await perform("ritual","Executando ritual: "+name);
}
const templates=[
"Núcleo sincronizado",
"Oscilação de integridade detectada",
"Remodelando armadura",
"Iniciando canalização ritualística"
];
$("#nav").addEventListener("click",e=>{const el=e.target.closest("[data-page]");if(el)navigate(el.dataset.page);});
$("#menuToggle").addEventListener("click",()=>setMenu(true));
$("#overlay").addEventListener("click",()=>setMenu(false));
$("#openSettings").addEventListener("click",()=>navigate("config"));
window.addEventListener("hashchange",()=>{const next=location.hash.slice(1);if(PAGES[next]&&next!==page){page=next;if(next==="cliente"){state.config.connectionMode="client";connected=false;save();}render();}});
document.addEventListener("keydown",e=>{if(e.key==="Escape")setMenu(false);});
$("#content").addEventListener("input",e=>{
if(e.target.id==="chatMessage"&&$("#chatPreview"))$("#chatPreview").textContent=e.target.value.trim()||"Sua mensagem aparecerá aqui.";
if(e.target.id==="integritySlider"&&$("#integrityLabel"))$("#integrityLabel").textContent=e.target.value+"%";
if(e.target.id==="energySlider"&&$("#energyLabel"))$("#energyLabel").textContent=e.target.value+"%";
});
$("#content").addEventListener("change",e=>{
if(e.target.id==="integritySlider"){state.integrity=clamp(Number(e.target.value),0,100);rp.adjustIntegrity?.(state.integrity);log("Integridade ajustada para "+state.integrity+"%.","NÚCLEO");render();}
if(e.target.id==="energySlider"){state.energy=clamp(Number(e.target.value),0,100);rp.checkAlerts?.();log("Energia ajustada para "+state.energy+"%.","NÚCLEO");render();}
if(e.target.id==="connectionMode"){try{readConfig();connected=false;render();toast(state.config.connectionMode==="client"?"Modo Nexus Client selecionado. Nenhuma senha RCON é necessária.":"Modo de conexão alterado.");}catch(err){toast(err.message);}}
});
$("#content").addEventListener("submit",async e=>{
e.preventDefault();const f=e.target;try{
if(f.id==="configForm"){
  readConfig();connected=false;
  toast(state.config.connectionMode==="client"
    ?(rememberedAccess?"Chave HTTPS salva neste dispositivo. Abra Nexus Client para parear.":"Chave HTTPS disponível nesta aba. Abra Nexus Client para parear.")
    :"Configurações salvas. Execute o teste de conexão.");
  render();
}
if(f.id==="functionForm"){
const fd=new FormData(f),next={};
for(const k of ["integrar","desintegrar","remodelar","ritual"]){const value=String(fd.get(k)||"").trim();if(value&&!/^[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,90}$/.test(value))throw Error("Função inválida: "+k);next[k]=value;}
state.config.functions=next;save();toast("Funções registradas.");render();
}
if(f.id==="chatForm"){
const msg=String(new FormData(f).get("message")||"").trim();if(!msg)return;if(busy)return;busy=true;
try{await messageToMinecraft(msg);toast("Mensagem entregue ao chat do Minecraft.");}
catch(err){addChat(msg,"failed");toast(err.message);}
finally{busy=false;render();}
}
if(f.id==="ritualForm")await performRitual(f);
}catch(err){toast(err.message||String(err));}
});
$("#content").addEventListener("click",async e=>{
const el=e.target.closest("[data-action]");if(!el)return;
const action=el.dataset.action;
if(action.startsWith("go-"))return navigate(action.slice(3));
if(action==="datapack-preset"){const functions={integrar:"nexus:armadura/integrar",desintegrar:"nexus:armadura/desintegrar",remodelar:"nexus:armadura/remodelar",ritual:"nexus:rituais/ativar"};for(const [key,val] of Object.entries(functions)){const field=document.querySelector('#functionForm [name="'+key+'"]');if(field)field.value=val;}toast("Funções preenchidas. Toque em SALVAR FUNÇÕES.");return;}
if(action==="forget-access"){
  rp.forgetAccess();
  toast("Chave HTTPS apagada deste navegador. Informe-a novamente quando quiser parear.");
  return;
}
if(action==="test-connection")return testConnection();
if(action==="template"){
const v=templates[Number(el.dataset.index)],input=$("#chatMessage");
if(input&&v){input.value=v;input.focus();if($("#chatPreview"))$("#chatPreview").textContent=v;}return;
}
if(action==="chat-local"){
const txt=$("#chatMessage")?.value?.trim();if(!txt)return toast("Digite uma mensagem.");
addChat(txt,"local");log("Anotação local: "+txt,"COMUNICADOR");render();return toast("Mensagem registrada apenas no painel.");
}
if(action.startsWith("armor-")){if(busy)return;busy=true;try{await armorAction(action.slice(6));}catch(e){toast(e.message);}finally{busy=false;}return;}
if(action==="ritual-preset"){state.selectedRitual=el.dataset.id;save();render();return;}
if(action==="export"){
const blob=new Blob([JSON.stringify({format:"nexus-rp-v1",exportedAt:new Date().toISOString(),data:state},null,2)],{type:"application/json"});
const url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download="nexus-aterna-rp-registros.json";a.click();setTimeout(()=>URL.revokeObjectURL(url),1200);return toast("Backup JSON exportado.");
}
if(action==="clear"){
if(!confirm("Reiniciar os registros narrativos e os indicadores locais?"))return;
const initial=DEFAULT();for(const key of Object.keys(initial))state[key]=initial[key];rp.resetUpgrade?.();save();connected=false;render();toast("Registros reiniciados.");
}
});
async function checkHealth(){try{const res=await fetch("/api/health"),j=await res.json();healthy=res.ok&&j.status==="online";}catch(e){healthy=false;}if(page==="config")render();}
function updateTime(){$("#time").textContent=new Date().toLocaleString("pt-BR",{day:"2-digit",month:"2-digit",hour:"2-digit",minute:"2-digit"});}
updateTime();setInterval(updateTime,30000);render();checkHealth();
})();