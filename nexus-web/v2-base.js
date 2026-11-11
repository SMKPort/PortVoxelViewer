"use strict";
(function(){
const KEY="nexus-aterna-v2";
const LABELS={aterna:"ATERNA AI",gemeo:"GÊMEO DIGITAL 3D",rituais2:"RITUAIS AVANÇADOS",tecnologia2:"TECNOLOGIA REAL",ts:"PROTOCOLO TS",espectrometro:"ESPECTRÔMETRO"};
const initial=()=>({chat:[{who:"assistant",text:"Saudações, Port. Núcleo ATERNA 2.0 iniciado. Tenho acesso aos registros deste navegador. Digite uma pergunta, cadastre uma memória ou ative a voz.",at:Date.now(),mode:"local"}],memories:[{id:"canon",title:"Cânone da oficina",text:"Port Morningstar: braço direito Nexus Aterna, braço fantasma dourado, placas retráteis, bengala-grimório e proteção narrativa contra os TS."}],ts:{chapter:0,integrity:100,choices:[],journal:[],unlocked:false},blueprint:{parts:[],selected:-1,hinge:25},ritualCanvas:{runes:[],name:"Novo círculo",complexity:50},spectro:{mode:"simulado",frequency:12,amplitude:65,noise:12,live:true,samples:[],imports:[]},deviceHistory:[],settings:{endpoint:"https://nexus-aterna-api.onrender.com",speak:false}});
let S=initial();try{const old=JSON.parse(localStorage.getItem(KEY)||"null");if(old&&typeof old==="object"){S={...S,...old,ts:{...initial().ts,...old.ts},blueprint:{...initial().blueprint,...old.blueprint},ritualCanvas:{...initial().ritualCanvas,...old.ritualCanvas},spectro:{...initial().spectro,...old.spectro},settings:{...initial().settings,...old.settings}};}}catch(e){}
const q=s=>document.querySelector(s);
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const date=t=>new Date(t).toLocaleString("pt-BR",{day:"2-digit",month:"2-digit",hour:"2-digit",minute:"2-digit"});
const id=()=>Math.random().toString(36).slice(2,10)+Date.now().toString(36);
const keep=()=>{try{localStorage.setItem(KEY,JSON.stringify(S));}catch(e){V.toast("Armazenamento cheio. Exporte um backup.");}};
const clamp=(v,a,b)=>Math.min(b,Math.max(a,v));
let bridge=null,activePage=null,disposers=[],speech=null,serialPort=null,voiceRecognition=null,authSession="",lastVoice="";
const V={PAGE_LABELS:LABELS,state:S,hasPage:p=>!!LABELS[p],pageLabels:LABELS,views:{},esc,save:keep,id,clamp,
getOld:()=>bridge?.getState?.()||{},toast:msg=>bridge?.toast?.(msg)||console.info(msg),log:(msg,kind="ATERNA 2.0")=>{bridge?.log?.(msg,kind);},navigate:p=>bridge?.navigate?.(p),rerender:()=>bridge?.render?.(),addDisposer:f=>disposers.push(f),
register:(name,fn,after)=>{V.views[name]={fn,after};},
beforeRender(next){if(activePage===next)return;disposers.splice(0).forEach(f=>{try{f();}catch(e){}});activePage=next;},
renderPage:p=>V.views[p]?.fn?.()||'<div class="notice">Módulo não disponível.</div>',
afterRender:p=>{V.views[p]?.after?.();},
init(b){bridge=b;const nav=q("#sideNav");if(nav&&!q("#nexusV2Nav")){const label=document.createElement("div");label.className="side-group-title";label.id="nexusV2Nav";label.textContent="EXPANSÃO ATERNA 2.0";label.style.marginTop="24px";nav.after(label);const ext=document.createElement("nav");ext.id="sideNavV2";label.after(ext);[["aterna","✦"],["gemeo","⬡"],["rituais2","✧"],["tecnologia2","⌘"],["ts","⊗"],["espectrometro","⌁"]].forEach(([name,icon])=>{const b=document.createElement("button");b.className="nav-item";b.setAttribute("data-v2-page",name);b.innerHTML='<span>'+icon+'</span> '+esc(LABELS[name]);ext.append(b);});ext.addEventListener("click",e=>{const btn=e.target.closest("[data-v2-page]");if(btn)V.navigate(btn.dataset.v2Page);});}
const version=q(".version");if(version)version.textContent="PROTOCOLO ATERNA / v2.0 WEB";
const term=q(".drawer-header small");if(term)term.textContent="INTERPRETADOR LOCAL + ASSISTENTE";
const output=q("#content");output.addEventListener("click",V.click);output.addEventListener("submit",V.submit);output.addEventListener("change",V.change);
},
click:async e=>{const target=e.target.closest("[data-v2]");if(!target)return;const action=target.dataset.v2;const handler=V.actions[action];if(handler){e.preventDefault();try{await handler(target,e);}catch(err){V.toast(err.message||String(err));}}},
submit:async e=>{const form=e.target.closest("[data-v2-form]");if(!form)return;e.preventDefault();const h=V.forms[form.dataset.v2Form];if(h){try{await h(form,e);}catch(err){V.toast(err.message||String(err));}}},
change:async e=>{const target=e.target.closest("[data-v2-change]");if(!target)return;const h=V.changes[target.dataset.v2Change];if(h){try{await h(target,e);}catch(err){V.toast(err.message||String(err));}}},
actions:{},forms:{},changes:{},command(text){const c=String(text||"").toUpperCase();const index=[["GÊMEO","gemeo"],["3D","gemeo"],["PROTECOLO TS","ts"],["PROTOCOLO TS","ts"],["ESPECTR","espectrometro"],["RITUAL AVAN","rituais2"],["LABORATÓRIO AVAN","rituais2"],["AUTOMAÇÃO AVAN","tecnologia2"],["TECNOLOGIA REAL","tecnologia2"],["ATERNA AI","aterna"],["ASSISTENTE","aterna"]];for(const [word,page] of index){if(c.includes(word)){V.navigate(page);return "Módulo "+LABELS[page]+" aberto. Mantenha os registros em dia, Port.";}}return null;}
};
window.NexusV2=V;
function intro(code,name,desc,tag){return '<div class="page-intro"><div><div class="eyebrow">'+esc(code)+'</div><h1 class="page-title">'+esc(name)+'</h1><p class="page-subtitle">'+esc(desc)+'</p></div><span class="tag">'+esc(tag)+'</span></div>';}
V.intro=intro;
function card(title,body,cls=""){return '<section class="panel '+cls+'"><div class="panel-header"><h2 class="panel-title">'+esc(title)+'</h2></div>'+body+'</section>';}
V.card=card;
function button(action,text,extra=""){return '<button type="button" class="btn '+extra+'" data-v2="'+action+'">'+text+'</button>';}
V.button=button;
function input(id,label,value="",type="text",attrs=""){return '<label class="field"><span>'+esc(label)+'</span><input name="'+esc(id)+'" type="'+type+'" value="'+esc(value)+'" '+attrs+'></label>';}
V.input=input;
function entry(text){return '<div class="v2-entry">'+esc(text)+'</div>';}
function contextFacts(){const old=V.getOld();return "Cânone: Port Morningstar, Nexus Aterna, Anael deusa do universo, braço protético direito, placas retráteis, braço fantasma dourado, bengala-grimório, TS e protocolo de isolamento. Projetos: "+(old.projects||[]).map(p=>p.name+" ("+p.notes+")").join("; ").slice(0,950)+". Rituais: "+(old.rituals||[]).slice(0,6).map(r=>r.name).join(", ")+". Memórias personalizadas: "+S.memories.map(m=>m.title+": "+m.text).join("; ").slice(0,1000);}
function offlineAnswer(question){
const t=question.toLowerCase();const keys=t.split(/[\W_]+/).filter(x=>x.length>3);const found=S.memories.map(m=>({m,hit:keys.filter(w=>(m.title+" "+m.text).toLowerCase().includes(w)).length})).sort((a,b)=>b.hit-a.hit).filter(x=>x.hit>0).slice(0,2);
if(/(mem[oó]ria|lembra|recorda)/i.test(t))return found.length?"Localizei estas memórias: "+found.map(x=>x.m.title+" = "+x.m.text).join(" | "):"Tenho "+S.memories.length+" memórias registradas. Use a coluna Memória Arcana para cadastrar novas informações.";
if(/(ritual|runa|c[ií]rculo)/i.test(t))return "Recomendo abrir Rituais Avançados, posicionar as runas no círculo e executar a simulação. Para o ritual do crucifixo, os componentes canônicos incluem essência arcana, Holy ingot e sangue de anjo e demônio (somente narrativa).";
if(/(bra[cç]o|nexus|placa|pr[oó]tese|modelo)/i.test(t))return "O Gêmeo Digital 3D permite rotacionar, inspecionar as placas, alterar o cotovelo e importar GLB/GLTF. O braço de referência é um protótipo geométrico, não o modelo final da oficina.";
if(/(energia|espectr|frequ[eê]ncia|sinal)/i.test(t))return "Abra o Espectrômetro para sinais sintéticos ou CSV de sensores reais. Vou separar os dados medidos fisicamente das assinaturas arcanas fictícias.";
if(/(ts|controle|invas[aã]o)/i.test(t))return "O Protocolo TS registra fragmentos ficcionais e decisões narrativas. O isolamento dos TS não representa uma proteção real de rede.";
if(/(bluetooth|wifi|wi-fi|porta|conect|home assistant|usb)/i.test(t))return "A Tecnologia Real solicita permissões ao navegador para dispositivos compatíveis. Para automações, o Link Tecnológico original permite integração com Home Assistant via HTTPS e autorização.";
if(found.length)return "Encontrei uma correspondência em Memória Arcana: "+found.map(x=>x.m.title+": "+x.m.text).join(" | ")+". Posso relacionar esse registro ao planejamento da oficina.";
return "Estou operando no modo local por regras, Port. Posso consultar memórias salvas e orientar os módulos. Para respostas livres com um modelo de IA, configure o servidor de IA com uma chave de API protegida na Render.";
}
function chatHTML(){const list=S.chat.slice(-50).map(m=>'<article class="v2-bubble '+(m.who==="user"?"sent":"received")+'"><div class="v2-bubble-label">'+(m.who==="user"?"PORT":"ATERNA")+" · "+date(m.at)+(m.mode==="local"?" · LOCAL":"")+'</div><p>'+esc(m.text)+'</p></article>').join("");return intro("MÓDULO 08 · INTELIGÊNCIA E MEMÓRIA","ATERNA AI","Assistente com voz, leitura dos registros do Nexus e conexão opcional a um backend de IA. O modo local responde por regras.","MEMÓRIA PERSISTENTE")+
'<div class="grid two v2-chat-grid">'+card("CANAL DE COMUNICAÇÃO",'<div class="v2-chat-list" id="v2Messages" role="log" aria-live="polite">'+list+'</div><form class="v2-chat-form" data-v2-form="chat"><textarea name="message" rows="2" placeholder="Pergunte algo sobre o Nexus, a oficina, rituais... " required maxlength="2500"></textarea><div class="actions"><button class="btn gold" type="submit">✦ ENVIAR</button>'+button("voice","🎙 FALAR","ghost")+button("read-last","🔊 OUVIR","ghost")+'</div></form><p class="panel-caption" id="v2AIStatus">Modo local: interpretação por regras. Para IA generativa configure o servidor seguro na Render.</p>')+
card("MEMÓRIA ARCANA",'<form class="stack" data-v2-form="memory">'+input("title","TÍTULO DA MEMÓRIA","","text","required maxlength='100'")+'<label class="field"><span>DESCRIÇÃO</span><textarea name="text" maxlength="1500" required placeholder="O que a ATERNA deve lembrar?"></textarea></label><button class="btn" type="submit">+ GRAVAR MEMÓRIA</button></form><div class="divider"></div><div class="v2-scroll-list">'+S.memories.map(m=>'<div class="item-card"><div class="row between"><strong>'+esc(m.title)+'</strong><button class="v2-icon-btn" aria-label="Excluir memória" data-v2="delete-memory" data-id="'+esc(m.id)+'">✕</button></div><p>'+esc(m.text)+'</p></div>').join("")+'</div><div class="divider"></div><h3 class="panel-title">SERVIDOR DE IA OPCIONAL</h3><form class="stack" data-v2-form="aiSettings">'+input("endpoint","ENDEREÇO DO BACKEND HTTPS",S.settings.endpoint,"url","required placeholder='https://...'")+'<label class="field"><span>CHAVE DE ACESSO DA SESSÃO (SE CONFIGURADA)</span><input name="sessionSecret" type="password" placeholder="Não é salva" autocomplete="off"></label><button class="btn ghost" type="submit">SALVAR ENDEREÇO / CONECTAR</button></form><p class="panel-caption">O servidor precisa ter GEMINI_API_KEY configurada nas variáveis privadas da Render. Nunca coloque a chave Gemini nesta interface.</p>')+'</div>';}
V.register("aterna",chatHTML,()=>{const list=q("#v2Messages");if(list)list.scrollTop=list.scrollHeight;});
V.forms.chat=async f=>{
const message=String(new FormData(f).get("message")||"").trim();if(!message)return;
S.chat.push({who:"user",text:message,at:Date.now()});keep();V.rerender();
let response="",mode="local";const endpoint=(S.settings.endpoint||"").replace(/\/+$/,"");
try{
const ctl=new AbortController();const timer=setTimeout(()=>ctl.abort(),14000);
try{const res=await fetch(endpoint+"/api/chat",{method:"POST",headers:{"Content-Type":"application/json",...(authSession?{"X-Aterna-Access":authSession}:{})},body:JSON.stringify({message,context:contextFacts(),history:S.chat.slice(-12).map(m=>({role:m.who==="user"?"user":"model",text:m.text}))}),signal:ctl.signal});if(res.ok){const out=await res.json();if(typeof out.reply==="string"&&out.reply.trim()){response=out.reply.slice(0,5000);mode="ia";}}}finally{clearTimeout(timer);}
}catch(e){}
if(!response){response=offlineAnswer(message);mode="local";}
S.chat.push({who:"assistant",text:response,at:Date.now(),mode});S.chat=S.chat.slice(-100);keep();V.log("Consulta à ATERNA AI ("+(mode==="ia"?"modelo remoto":"modo local")+").","ATERNA");V.rerender();if(S.settings.speak)readResponse(response);
};
V.forms.memory=f=>{const d=new FormData(f),title=String(d.get("title")||"").trim(),txt=String(d.get("text")||"").trim();if(!title||!txt)return;S.memories.unshift({id:id(),title,text:txt});S.memories=S.memories.slice(0,120);keep();V.log("Memória registrada: "+title,"ATERNA");V.rerender();V.toast("Memória salva no dispositivo.");};
V.forms.aiSettings=f=>{const d=new FormData(f);const url=String(d.get("endpoint")||"").trim().replace(/\/+$/,"");if(!/^https:\/\/[a-z0-9.-]+(?::\d+)?(?:\/.*)?$/i.test(url))return V.toast("Informe um endereço HTTPS válido.");S.settings.endpoint=url;authSession=String(d.get("sessionSecret")||"");keep();V.toast("Endereço salvo. A chave de acesso fica apenas nesta sessão.");};
V.actions["delete-memory"]=el=>{const m=S.memories.find(x=>x.id===el.dataset.id);if(m&&confirm("Excluir a memória '"+m.title+"'?")){S.memories=S.memories.filter(x=>x.id!==m.id);keep();V.rerender();}};
function readResponse(text){if(!("speechSynthesis" in window))return V.toast("Síntese de voz não suportada.");window.speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(text);u.lang="pt-BR";u.rate=.95;window.speechSynthesis.speak(u);}
V.actions["read-last"]=()=>{const m=[...S.chat].reverse().find(x=>x.who==="assistant");if(m)readResponse(m.text);};
V.actions.voice=()=>{
const R=window.SpeechRecognition||window.webkitSpeechRecognition;if(!R)return V.toast("Reconhecimento de voz indisponível neste navegador. Use Chrome compatível ou digite.");
if(voiceRecognition){try{voiceRecognition.stop();}catch(e){}voiceRecognition=null;return;}
const rec=new R();rec.lang="pt-BR";rec.interimResults=false;rec.maxAlternatives=1;voiceRecognition=rec;
rec.onresult=e=>{const t=e.results[0][0].transcript;const textarea=q('[data-v2-form="chat"] textarea[name="message"]');if(textarea)textarea.value=t;V.toast("Comando de voz transcrito. Confira e envie.");};
rec.onerror=e=>V.toast("Microfone: "+e.error);rec.onend=()=>{voiceRecognition=null;};rec.start();V.toast("Ouvindo... fale seu comando.");};
const story=[
{title:"Eco residual",code:"TS-001",text:"Uma sequência de pulsos antigos surge nos registros da oficina. A assinatura não corresponde ao Nexus reconstruído.",choices:[["Isolar a amostra",8],["Investigar a origem",22]],fragment:"01110100 01110011 / ainda reconhecemos você"},
{title:"Arquivo incompleto",code:"TS-014",text:"Um fragmento de memória revela o procedimento de vinculação utilizado pelos TS no antigo braço.",choices:[["Arquivar sob contenção",6],["Decodificar o arquivo",19]],fragment:"ROOT TS > PERMISSÃO REVOGADA // sujeito: PORT"},
{title:"Tentativa de sincronização",code:"TS-026",text:"O braço fantasma dourado responde a uma ordem que Port não enviou. O sistema solicita uma decisão.",choices:[["Recusar a sincronização",4],["Simular a resposta em ambiente isolado",16]],fragment:"VOCÊ NÃO PODE ENTRAR AQUI. VOCÊ NÃO PODE SAIR."},
{title:"Sinal de origem",code:"TS-041",text:"O terminal recupera uma coordenada fictícia e uma assinatura energética armazenada no núcleo antigo.",choices:[["Registrar sem executar",5],["Iniciar análise forense simulada",15]],fragment:"NÓ DE CONTROLE TS // integridade desconhecida"},
{title:"Declaração de autonomia",code:"TS-099",text:"ATERNA identifica o último fragmento e encerra o vínculo narrativo. Port decide como preservar a memória do que aconteceu.",choices:[["Selar os arquivos",0],["Preservar a verdade",0]],fragment:"O CONTROLE É SEU, PORT. // fim da transmissão"}];
function tsHTML(){const stage=story[Math.min(S.ts.chapter,story.length-1)];const complete=S.ts.chapter>=story.length;return intro("MÓDULO 11 · EXPERIÊNCIA NARRATIVA","Protocolo TS","Investigue os fragmentos de controle dos TS em uma história interativa, com escolhas e memória de eventos. Isto é ficção.","LORE INTERATIVA")+
'<div class="grid two">'+card("TERMINAL DE CONTENÇÃO",'<div class="v2-danger"><span class="signal-dot"></span> SIMULAÇÃO DE ANOMALIA NARRATIVA</div><div class="v2-signal" aria-label="Sinal codificado">1010 0011 0101 1110 0110 0111</div><div class="kv"><span>Progresso da história</span><span class="value">'+Math.min(S.ts.chapter,story.length)+' / '+story.length+'</span></div><div class="kv"><span>Integridade do núcleo (lore)</span><span class="value">'+S.ts.integrity+'%</span></div><div class="meter"><span style="width:'+S.ts.integrity+'%"></span></div><div class="divider"></div>'+(complete?'<h3>PROTOCOLO CONCLUÍDO</h3><p class="panel-caption">Os fragmentos foram examinados. Suas decisões foram registradas no diário narrativo.</p>':'<div class="eyebrow">'+esc(stage.code)+'</div><h3 class="section-heading" style="margin-top:4px">'+esc(stage.title)+'</h3><p class="panel-caption">'+esc(stage.text)+'</p><div class="v2-fragment">'+esc(stage.fragment)+'</div><div class="actions">'+stage.choices.map((c,i)=>'<button class="btn '+(i?"ghost":"gold")+'" data-v2="ts-choose" data-choice="'+i+'">'+esc(c[0])+'</button>').join("")+'</div>')+'<div class="divider"></div>'+button("ts-reset","REINICIAR CAMPANHA","ghost"))+
card("DIÁRIO DOS FRAGMENTOS",S.ts.journal.length?'<div class="v2-scroll-list">'+S.ts.journal.slice().reverse().map(x=>'<div class="item-card"><small class="muted">'+esc(x.code)+' · '+date(x.at)+'</small><strong>'+esc(x.title)+'</strong><p>'+esc(x.choice)+'</p><small class="mono gold">'+esc(x.fragment)+'</small></div>').join("")+'</div>':'<div class="empty">Nenhum fragmento desbloqueado. Inicie a investigação no terminal de contenção.</div>')+'</div>'+ '<div class="notice" style="margin-top:16px">Os eventos dos TS são fictícios e só acontecem quando você interage com o módulo. Não representam uma invasão real, nem um sistema de proteção cibernética.</div>';}
V.register("ts",tsHTML);
V.actions["ts-choose"]=el=>{if(S.ts.chapter>=story.length)return;const chapter=story[S.ts.chapter],choice=chapter.choices[Number(el.dataset.choice)===1?1:0];S.ts.journal.push({id:id(),code:chapter.code,title:chapter.title,choice:choice[0],fragment:chapter.fragment,at:Date.now()});S.ts.choices.push(choice[0]);S.ts.integrity=clamp(S.ts.integrity-choice[1],18,100);S.ts.chapter++;keep();V.log("TS "+chapter.code+": "+choice[0],"TS / ficção");V.rerender();V.toast("Fragmento "+chapter.code+" arquivado.");};
V.actions["ts-reset"]=()=>{if(!confirm("Reiniciar a campanha TS? O diário da campanha será apagado."))return;S.ts=initial().ts;keep();V.rerender();};
function techHTML(){return intro("MÓDULO 10 · CONEXÕES AUTORIZADAS","Tecnologia Real","Ferramentas experimentais para equipamentos físicos via APIs do navegador. Uma permissão explícita é necessária a cada acesso.","CONEXÕES REAIS")+
'<div class="grid three">'+card("BLUETOOTH LE",'<p class="panel-caption">Selecionar sensor BLE e tentar ler o nível da bateria padrão, quando disponível.</p>'+button("ble-battery","LER BATERIA BLE","gold")+'<p class="panel-caption">Bluetooth clássico e dispositivos sem serviço compatível não são suportados por este comando.</p>')+
card("WEB SERIAL",'<p class="panel-caption">Conectar a uma porta serial autorizada e enviar texto para o equipamento que você controla.</p>'+button("serial-connect","CONECTAR PORTA","gold")+'<div class="field"><span>COMANDO SERIAL (TEXTO UTF-8)</span><input id="v2SerialText" placeholder="Ex.: STATUS"></div><div class="actions">'+button("serial-send","ENVIAR COM CONFIRMAÇÃO","ghost")+button("serial-close","DESCONECTAR","ghost")+'</div><small class="muted" id="v2SerialStatus">Porta desconectada.</small>')+
card("WEB NFC E USB",'<p class="panel-caption">Leitura de etiquetas NFC autorizadas e seleção de dispositivos USB compatíveis.</p>'+button("nfc-read","LER ETIQUETA NFC","gold")+button("usb-info","CONSULTAR USB","ghost")+'<p class="panel-caption">NFC depende do Android e do suporte do navegador.</p>')+'</div>'+
'<h2 class="section-heading">Sessão de dispositivos</h2><div class="grid two">'+card("ATIVIDADE AUTORIZADA",S.deviceHistory.length?'<div class="v2-scroll-list">'+S.deviceHistory.slice(-12).reverse().map(x=>'<div class="item-card"><strong>'+esc(x.title)+'</strong><p>'+esc(x.detail)+'</p><small class="muted">'+date(x.at)+'</small></div>').join("")+'</div>':'<div class="empty">Nenhuma operação realizada.</div>')+
card("CENTRAL DE AUTOMAÇÕES",'<p class="panel-caption">O módulo Link Tecnológico da versão 1.0 continua disponível, incluindo Home Assistant e cadastro de equipamentos.</p><div class="actions">'+button("go-legacy-tech","IR PARA LINK TECNOLÓGICO","gold")+'</div><div class="divider"></div><div class="notice">O Nexus não conecta a qualquer aparelho arbitrariamente. Nunca envie comandos a sistemas sem autorização. Algumas APIs não funcionam em todos os navegadores.</div>')+'</div>';}
V.register("tecnologia2",techHTML);
V.actions["go-legacy-tech"]=()=>V.navigate("tecnologia");
function deviceEvent(title,detail){S.deviceHistory.push({id:id(),title,detail:String(detail),at:Date.now()});S.deviceHistory=S.deviceHistory.slice(-50);keep();V.log(title,"tecnologia");V.rerender();}
V.actions["ble-battery"]=async()=>{
if(!navigator.bluetooth)throw Error("Web Bluetooth indisponível. Use Chrome com suporte a BLE.");
const device=await navigator.bluetooth.requestDevice({acceptAllDevices:true,optionalServices:["battery_service"]});
let detail=device.name||"Dispositivo sem nome";
try{const server=await device.gatt.connect();const service=await server.getPrimaryService("battery_service");const level=await service.getCharacteristic("battery_level");const data=await level.readValue();detail+=" · Bateria: "+data.getUint8(0)+"%";}catch(e){detail+=" · Serviço de bateria indisponível ("+e.message+")";}deviceEvent("Bluetooth LE autorizado",detail);
};
V.actions["serial-connect"]=async()=>{
if(!navigator.serial)throw Error("Web Serial não disponível neste navegador.");if(serialPort)return V.toast("Já há uma porta conectada.");
serialPort=await navigator.serial.requestPort();try{await serialPort.open({baudRate:9600});deviceEvent("Porta serial aberta","9600 baud · sessão autorizada.");}catch(e){serialPort=null;throw e;}
};
V.actions["serial-send"]=async()=>{
if(!serialPort||!serialPort.writable)return V.toast("Conecte uma porta serial primeiro.");
const value=(q("#v2SerialText")?.value||"").trim();if(!value||value.length>200)return V.toast("Digite um comando de até 200 caracteres.");
if(!confirm("Enviar exatamente este comando à porta selecionada?\n\n"+value))return;
const writer=serialPort.writable.getWriter();try{await writer.write(new TextEncoder().encode(value+"\n"));deviceEvent("Comando serial enviado",value);}finally{writer.releaseLock();}
};
V.actions["serial-close"]=async()=>{if(serialPort){await serialPort.close();serialPort=null;deviceEvent("Porta serial desconectada","Conexão fechada pelo operador.");}};
V.actions["usb-info"]=async()=>{if(!navigator.usb)throw Error("WebUSB indisponível neste navegador.");const d=await navigator.usb.requestDevice({filters:[]});deviceEvent("Dispositivo USB autorizado",(d.productName||"USB")+" / VID "+d.vendorId+" PID "+d.productId);};
V.actions["nfc-read"]=async()=>{
if(!("NDEFReader" in window))throw Error("Web NFC indisponível. Tente Chrome no Android com NFC.");
const reader=new NDEFReader();await reader.scan();V.toast("Aproxime a etiqueta NFC de sua propriedade.");const timeout=setTimeout(()=>{reader.onreading=null;},30000);
reader.onreading=e=>{clearTimeout(timeout);const texts=[];for(const record of e.message.records){if(record.recordType==="text"){try{texts.push(new TextDecoder(record.encoding||"utf-8").decode(record.data));}catch(err){}}}deviceEvent("Etiqueta NFC lida","UID "+(e.serialNumber||"indisponível")+" · "+texts.join(" | ").slice(0,350));reader.onreading=null;};
};
})();