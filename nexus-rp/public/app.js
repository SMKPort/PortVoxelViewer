"use strict";
(()=>{
const KEY="nexus-aterna-rp-v1";
const PRESETS=[
{id:"bencao",name:"Bênção Divina",school:"Divina",cost:16,description:"A bênção de Anael percorre as placas douradas do Nexus, estabilizando o núcleo."},
{id:"selo",name:"Selo de Contenção",school:"Arcana",cost:22,description:"Runas douradas emergem ao redor da prótese e consolidam o selo ritualístico."},
{id:"purificar",name:"Purificação",school:"Divina",cost:27,description:"Uma onda de luz atravessa os conduítes arcanos e dissipa interferências."},
{id:"renovar",name:"Renovação do Núcleo",school:"Híbrida",cost:32,description:"O núcleo é recalibrado e a energia se redistribui entre as placas."},
{id:"custom",name:"Personalizado",school:"Personalizado",cost:20,description:"O Port executa uma sequência de símbolos e componentes escolhidos."}
];
const PAGES={painel:"NÚCLEO",chat:"COMUNICADOR",armadura:"ARMADURA ARCANA",rituais:"RITUAIS",config:"CONFIGURAÇÕES"};
const DEFAULT=()=>({
integrity:100,energy:85,armor:"integrada",form:"Celestial",selectedRitual:"bencao",
logs:[{id:"origin",at:new Date().toISOString(),kind:"SISTEMA",text:"Nexus Aterna RP inicializado. Aguardando instruções do Port."}],
chat:[],rituals:[],
config:{host:"",port:25575,player:"Port",prefix:"[NEXUS ATERNA]",target:"global",recipient:"",color:"white",autoAnnounce:true,functions:{integrar:"",desintegrar:"",remodelar:"",ritual:""}}
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
const $=x=>document.querySelector(x);
const esc=v=>String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const clamp=(n,a,b)=>Math.min(b,Math.max(a,n));
const date=t=>new Date(t).toLocaleString("pt-BR",{day:"2-digit",month:"2-digit",hour:"2-digit",minute:"2-digit"});
const uid=()=>Math.random().toString(36).slice(2,10)+Date.now().toString(36);
const secrets={access:"",password:""};
let connected=false,healthy=false,busy=false;
let page=PAGES[location.hash.slice(1)]?location.hash.slice(1):"painel";
function save(){try{localStorage.setItem(KEY,JSON.stringify(state));}catch(e){toast("Não foi possível salvar os registros locais.");}}
function log(text,kind="RP"){state.logs.unshift({id:uid(),at:new Date().toISOString(),kind,text});state.logs=state.logs.slice(0,140);save();}
function toast(text){const el=document.createElement("div");el.className="toast";el.textContent=text;$("#toasts").appendChild(el);setTimeout(()=>el.remove(),4600);}
function addChat(text,status){state.chat.push({id:uid(),at:new Date().toISOString(),text,status});state.chat=state.chat.slice(-100);save();}
const rp={state,escape:esc,date,presets:PRESETS,isConnected:()=>connected,isHealthy:()=>healthy};
const views=window.NexusRPViews(rp);
function render(){
$("#content").innerHTML=views[page]?.()||views.painel();
$("#pageLabel").textContent=PAGES[page]||"NÚCLEO";
document.querySelectorAll("[data-page]").forEach(el=>{const yes=el.dataset.page===page;el.classList.toggle("active",yes);el.setAttribute("aria-current",yes?"page":"false");});
$("#sideConnection").textContent=connected?"MINECRAFT CONECTADO":"NARRATIVA LOCAL";
$("#topStatus").innerHTML='<span class="light '+(connected?"connected":"")+'"></span> '+(connected?"RCON CONECTADO":"MODO LOCAL");
const feed=$("#chatFeed");if(feed)feed.scrollTop=feed.scrollHeight;
}
function setMenu(open){$("#sidebar").classList.toggle("open",open);$("#overlay").hidden=!open;}
function navigate(next){if(!PAGES[next])return;page=next;if(location.hash!=="#"+page)location.hash="#"+page;render();setMenu(false);window.scrollTo({top:0,behavior:"instant"});}
function readConfig(){
const form=$("#configForm");if(!form)return;
const d=new FormData(form),c=state.config;
c.host=String(d.get("host")||"").trim().slice(0,253);
c.port=clamp(Number(d.get("port"))||25575,1024,65535);
c.player=String(d.get("player")||"Port").trim().replace(/[^a-z0-9_]/ig,"").slice(0,16)||"Port";
c.prefix=String(d.get("prefix")||"[NEXUS]").trim().slice(0,38)||"[NEXUS]";
c.target=d.get("target")==="player"?"player":"global";
c.recipient=String(d.get("recipient")||"").trim().slice(0,16);
c.color=String(d.get("color")||"white");
c.autoAnnounce=d.has("autoAnnounce");
if(d.get("password"))secrets.password=String(d.get("password"));
if(d.get("access"))secrets.access=String(d.get("access"));
save();
}
async function api(route,payload={}){
if(!secrets.access)throw Error("Digite o código de acesso em Configurações.");
if(!secrets.password)throw Error("Digite a senha RCON em Configurações.");
if(!state.config.host)throw Error("Informe o host público do seu Minecraft.");
const ctrl=new AbortController(),timer=setTimeout(()=>ctrl.abort(),14500);
try{
const res=await fetch("/api/rcon/"+route,{method:"POST",headers:{"Content-Type":"application/json","X-Nexus-Key":secrets.access},body:JSON.stringify({server:{host:state.config.host,port:state.config.port,password:secrets.password},...payload}),signal:ctrl.signal});
const out=await res.json().catch(()=>({}));
if(!res.ok||!out.ok)throw Error(out.error||"Erro HTTP "+res.status);
return out;
}catch(e){if(e.name==="AbortError")throw Error("O Minecraft não respondeu. Confira a porta, o firewall e a conectividade da Render.");throw e;}
finally{clearTimeout(timer);}
}
async function testConnection(){
if(busy)return;readConfig();busy=true;toast("Verificando acesso ao servidor...");
try{const out=await api("test");connected=true;log("Autenticação RCON confirmada. "+String(out.response||"").slice(0,150),"MINECRAFT");toast("Minecraft conectado com sucesso!");}
catch(e){connected=false;log("Falha de conexão: "+e.message,"CONEXÃO");toast(e.message);}
finally{busy=false;render();}
}
async function messageToMinecraft(text,category="COMUNICADOR"){
const c=state.config;
if(c.target==="player"&&!/^[A-Za-z0-9_]{3,16}$/.test(c.recipient))throw Error("Informe um jogador de destino válido.");
await api("message",{message:text,target:c.target,player:c.recipient,prefix:c.prefix,color:c.color});
connected=true;addChat(text,"sent");log("Transmitido ao Minecraft ("+category+"): "+text,"MINECRAFT");return true;
}
async function gameFunction(which){
const fn=state.config.functions?.[which];
if(!connected||!fn)return false;
if(!/^[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,90}$/.test(fn)){toast("Função configurada inválida: "+which);return false;}
try{await api("function",{functionId:fn});log("Função do Minecraft enviada: "+fn,"MINECRAFT");return true;}
catch(e){log("Erro ao executar "+fn+": "+e.message,"CONEXÃO");toast("Falha na função: "+e.message);return false;}
}
async function perform(kind,message){
if(!connected)return;
if(state.config.autoAnnounce){
try{await messageToMinecraft(message.slice(0,280),kind.toUpperCase());toast("Narração publicada no Minecraft.");}
catch(e){toast("Narração não enviada: "+e.message);}
}
await gameFunction(kind);
}
async function armorAction(type){
const required=type==="integrar"?12:type==="remodelar"?9:type==="reparar"?14:0;
if(state.energy<required)return toast("Energia narrativa insuficiente.");
let text="",cost=0;
if(type==="integrar"){
if(state.armor==="integrada")return toast("A armadura já está integrada.");
cost=12;state.armor="integrada";text="As placas do Nexus Aterna se atraem e se encaixam no braço direito de Port.";
}else if(type==="desintegrar"){
if(state.armor==="desintegrada")return toast("As placas já estão dispersas.");
state.armor="desintegrada";state.energy=clamp(state.energy+5,0,100);text="A armadura do Nexus se fragmenta, revelando o braço fantasma dourado.";
}else if(type==="remodelar"){
cost=9;const form=$("#armorForm")?.value||state.form;state.form=["Celestial","Combate","Ritual","Repouso"].includes(form)?form:"Celestial";
text="As placas do Nexus se remodelam, assumindo a configuração "+state.form.toUpperCase()+".";
}else if(type==="reparar"){
cost=14;state.integrity=clamp(state.integrity+18,0,100);
text="O núcleo dourado libera um pulso restaurador que recompõe as fissuras da armadura.";
}else return;
if(state.energy<cost){toast("Energia narrativa insuficiente.");return;}
state.energy-=cost;log(text,"ARMADURA");save();render();toast("Estado narrativo atualizado.");
await perform(type==="reparar"?"":type,text);
}
async function performRitual(form){
const data=new FormData(form),name=String(data.get("name")||"").trim().slice(0,100),
description=String(data.get("description")||"").trim().slice(0,600),cost=clamp(Number(data.get("cost")),0,100);
if(!name||!description)throw Error("Defina nome e descrição do ritual.");
if(state.energy<cost)throw Error("Energia narrativa insuficiente.");
const score=clamp(Math.round(38+state.integrity*.38+state.energy*.2-cost*.27),0,99);
state.energy-=cost;
state.rituals.unshift({id:uid(),at:new Date().toISOString(),name,description,cost,score});state.rituals=state.rituals.slice(0,80);
log("Ritual "+name+" realizado: "+description+" (estabilidade "+score+"%).","RITUAL");save();render();toast("Ritual narrativo concluído.");
await perform("ritual","Ritual "+name+": "+description);
}
const templates=[
"Núcleo Aterna sincronizado. Prótese pronta para operação.",
"Oscilação identificada na integridade do braço direito.",
"Iniciando reorganização das placas arcanas do Nexus.",
"Canalização ritualística autorizada. Núcleo ativo."
];
$("#nav").addEventListener("click",e=>{const el=e.target.closest("[data-page]");if(el)navigate(el.dataset.page);});
$("#menuToggle").addEventListener("click",()=>setMenu(true));
$("#overlay").addEventListener("click",()=>setMenu(false));
$("#openSettings").addEventListener("click",()=>navigate("config"));
window.addEventListener("hashchange",()=>{const next=location.hash.slice(1);if(PAGES[next]&&next!==page){page=next;render();}});
document.addEventListener("keydown",e=>{if(e.key==="Escape")setMenu(false);});
$("#content").addEventListener("input",e=>{
if(e.target.id==="chatMessage"&&$("#chatPreview"))$("#chatPreview").textContent=e.target.value.trim()||"Sua mensagem aparecerá aqui.";
if(e.target.id==="integritySlider"&&$("#integrityLabel"))$("#integrityLabel").textContent=e.target.value+"%";
if(e.target.id==="energySlider"&&$("#energyLabel"))$("#energyLabel").textContent=e.target.value+"%";
});
$("#content").addEventListener("change",e=>{
if(e.target.id==="integritySlider"){state.integrity=clamp(Number(e.target.value),0,100);log("Integridade ajustada para "+state.integrity+"%.","NÚCLEO");render();}
if(e.target.id==="energySlider"){state.energy=clamp(Number(e.target.value),0,100);log("Energia ajustada para "+state.energy+"%.","NÚCLEO");render();}
});
$("#content").addEventListener("submit",async e=>{
e.preventDefault();const f=e.target;try{
if(f.id==="configForm"){readConfig();connected=false;toast("Configurações salvas. Execute o teste de conexão.");render();}
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
const initial=DEFAULT();for(const key of Object.keys(initial))state[key]=initial[key];save();connected=false;render();toast("Registros reiniciados.");
}
});
async function checkHealth(){try{const res=await fetch("/api/health"),j=await res.json();healthy=res.ok&&j.status==="online";}catch(e){healthy=false;}if(page==="config")render();}
function updateTime(){$("#time").textContent=new Date().toLocaleString("pt-BR",{day:"2-digit",month:"2-digit",hour:"2-digit",minute:"2-digit"});}
updateTime();setInterval(updateTime,30000);render();checkHealth();
})();