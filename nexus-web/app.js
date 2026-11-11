"use strict";
(() => {
const STORAGE="nexus-aterna-v1";
const PAGES={nucleo:"NÚCLEO CENTRAL",tecnologia:"LINK TECNOLÓGICO",arcano:"VISÃO ARCANA",rituais:"LABORATÓRIO RITUALÍSTICO",energia:"MAPA ENERGÉTICO",engenharia:"ENGENHARIA ARCANA",diagnostico:"DIAGNÓSTICO",registros:"ARQUIVOS E SEGURANÇA"};
Object.assign(PAGES,window.NexusV2?.PAGE_LABELS||{});
const defaults=()=>({
  isolated:true,
  devices:[],signatures:[],rituals:[],reports:[],
  readings:[{id:"sample1",source:"Amostra demonstrativa",type:"Divina",value:43,at:"2026-11-01T10:00:00Z",demo:true},{id:"sample2",source:"Amostra demonstrativa",type:"Arcana",value:63,at:"2026-11-01T11:00:00Z",demo:true},{id:"sample3",source:"Amostra demonstrativa",type:"Híbrida",value:53,at:"2026-11-01T12:00:00Z",demo:true},{id:"sample4",source:"Amostra demonstrativa",type:"Arcana",value:76,at:"2026-11-01T13:00:00Z",demo:true},{id:"sample5",source:"Amostra demonstrativa",type:"Divina",value:68,at:"2026-11-01T14:00:00Z",demo:true}],
  projects:[{id:"arm",name:"Braço Arcano Direito",kind:"Prótese celestial",status:"Em desenvolvimento",notes:"Placas móveis, braço fantasma dourado e canalização Holy Spellpower de +10% a +60% (lore).",at:new Date().toISOString()},{id:"cane",name:"Bengala-Grimório",kind:"Artefato",status:"Projeto registrado",notes:"Mecanismo de transformação e conjuração da Bênção Divina.",at:new Date().toISOString()},{id:"core",name:"Núcleo Nexus Aterna",kind:"Sistema híbrido",status:"Operacional em simulação",notes:"Interface e protocolos de isolamento narrativo TS.",at:new Date().toISOString()}],
  logs:[{id:"init",kind:"sistema",message:"Protocolo ATERNA inicializado pela primeira vez.",at:new Date().toISOString()}]
});
let state=defaults();
try{const loaded=JSON.parse(localStorage.getItem(STORAGE)||"null");if(loaded&&typeof loaded==="object"){state={...state,...loaded};for(const key of ["devices","signatures","rituals","reports","readings","projects","logs"]){if(!Array.isArray(state[key]))state[key]=[];}}}catch(_){}
let page=PAGES[location.hash.slice(1)]?location.hash.slice(1):"nucleo";
let ha={url:"",token:"",entities:[]};let previewUrl="";let editingRitual=null;let lastRitual=null;
const $=s=>document.querySelector(s);
const E=v=>String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const D=v=>{const d=new Date(v);return Number.isNaN(d.getTime())?"Data desconhecida":d.toLocaleString("pt-BR",{dateStyle:"short",timeStyle:"short"});};
const uid=()=>Math.random().toString(36).slice(2,10)+Date.now().toString(36);
const clamp=(n,a,b)=>Math.max(a,Math.min(b,n));
const persist=()=>{try{localStorage.setItem(STORAGE,JSON.stringify(state));return true;}catch(e){toast("Limite de armazenamento local atingido.");return false;}};
const log=(message,kind="sistema")=>{state.logs.unshift({id:uid(),kind,message,at:new Date().toISOString()});state.logs=state.logs.slice(0,180);persist();};
const toast=message=>{const root=$("#toasts");const el=document.createElement("div");el.className="toast";el.textContent=message;root.appendChild(el);setTimeout(()=>el.remove(),4500);};
const intro=(eyebrow,title,subtitle,label)=>'<div class="page-intro"><div><div class="eyebrow">'+E(eyebrow)+'</div><h1 class="page-title">'+E(title)+'</h1><p class="page-subtitle">'+E(subtitle)+'</p></div>'+(label?'<span class="tag">'+E(label)+'</span>':"")+'</div>';
const panel=(title,body,extra="")=>'<section class="panel '+extra+'"><div class="panel-header"><h2 class="panel-title">'+E(title)+'</h2></div>'+body+'</section>';
const button=(act,label,cls="ghost",extra="")=>'<button type="button" class="btn '+cls+'" data-act="'+E(act)+'" '+extra+'>'+label+'</button>';
const kv=(a,b)=>'<div class="kv"><span>'+E(a)+'</span><span class="value">'+E(b)+'</span></div>';
const empty=msg=>'<div class="empty">'+E(msg)+'</div>';
const txt=(field,label,value="",type="text",attrs="")=>'<div class="field"><label for="'+field+'">'+E(label)+'</label><input id="'+field+'" name="'+field+'" type="'+type+'" value="'+E(value)+'" '+attrs+'></div>';
const textarea=(field,label,value="")=>'<div class="field"><label for="'+field+'">'+E(label)+'</label><textarea id="'+field+'" name="'+field+'">'+E(value)+'</textarea></div>';
const select=(field,label,options,selected="")=>'<div class="field"><label for="'+field+'">'+E(label)+'</label><select id="'+field+'" name="'+field+'">'+options.map(x=>'<option '+(selected===x?"selected":"")+'>'+E(x)+'</option>').join("")+'</select></div>';
const note=msg=>'<div class="notice">'+E(msg)+'</div>';
const timeItem=item=>'<div class="activity-entry"><span class="activity-dot"></span><div><strong>'+E(item.message)+'</strong><p>'+E(item.kind.toUpperCase())+' · '+D(item.at)+'</p></div></div>';
function nucleusSvg(){return '<svg class="nucleus-svg" viewBox="0 0 400 400" role="img" aria-label="Representação holográfica animada do núcleo Nexus Aterna"><defs><radialGradient id="g"><stop offset="0" stop-color="#dfb56c" stop-opacity=".28"/><stop offset="1" stop-color="#7d2734" stop-opacity="0"/></radialGradient><linearGradient id="metal" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#f6ddaa"/><stop offset=".6" stop-color="#a6464d"/><stop offset="1" stop-color="#d6ac68"/></linearGradient></defs><circle cx="200" cy="200" r="188" fill="url(#g)"/><g class="spin" fill="none" stroke="#cc9b62" stroke-width="1"><circle cx="200" cy="200" r="157" stroke-dasharray="14 9" opacity=".58"/><circle cx="200" cy="200" r="145" opacity=".5"/><path d="M200 29V56 M200 344V371 M29 200H56 M344 200H371 M79 79l18 18 M303 303l18 18 M321 79l-18 18 M97 303l-18 18" stroke-width="4"/></g><g class="spin rev" fill="none" stroke="#c34e56"><circle cx="200" cy="200" r="129" stroke-width="1.6" stroke-dasharray="54 10 8 10"/><circle cx="200" cy="200" r="111" stroke-width="1.2"/><path d="M200 74L326 200L200 326L74 200Z" opacity=".6"/></g><g fill="none" stroke="url(#metal)"><circle cx="200" cy="200" r="89" stroke-width="2"/><path d="M200 112L276 200L200 288L124 200Z" stroke-width="2"/><path d="M200 130L259 200L200 270L141 200Z" stroke-width="1"/><path d="M200 154L235 200L200 246L165 200Z" stroke-width="2"/></g><circle class="pulse" cx="200" cy="200" r="25" fill="#ddad69" fill-opacity=".16" stroke="#ffe4ab" stroke-width="1.4"/><path d="M200 176l14 24-14 24-14-24z" fill="#ebcf95"/><circle cx="200" cy="200" r="6" fill="#fff2d8"/><g fill="#e2b977"><circle cx="200" cy="43" r="3"/><circle cx="200" cy="357" r="3"/><circle cx="43" cy="200" r="3"/><circle cx="357" cy="200" r="3"/></g></svg>';}
function renderNucleo(){
const last=state.logs.slice(0,5).map(timeItem).join("");
return intro("PROTOCOLO ATERNA · PAINEL DE CONTROLE","Núcleo Central","Interface de engenharia arcana, tecnologia e análise narrativa. Operações reais exigem integrações e permissões.","SISTEMA LOCAL")+
'<div class="grid four stats">'+
'<div class="panel stat-card"><div class="stat-mini">LINKS CATALOGADOS</div><div class="big-number">'+state.devices.length+'</div><div class="muted">Dispositivos registrados</div></div>'+
'<div class="panel stat-card"><div class="stat-mini">RITUAIS</div><div class="big-number">'+state.rituals.length+'</div><div class="muted">Estudos armazenados</div></div>'+
'<div class="panel stat-card"><div class="stat-mini">PROJETOS</div><div class="big-number">'+state.projects.length+'</div><div class="muted">Engenharia arcana</div></div>'+
'<div class="panel stat-card"><div class="stat-mini">ISOLAMENTO NARRATIVO</div><div class="big-number" style="font-size:24px;color:'+(state.isolated?'#91d9b0':'#e88a94')+'">'+(state.isolated?'ATIVO':'INATIVO')+'</div><div class="muted">Protocolo ficcional TS</div></div></div>'+
'<div class="hero"><section class="panel nucleus-card"><div class="corner-label">ATERNA / CORE 01</div><div class="corner-code">✦ PORT MORNINGSTAR</div>'+nucleusSvg()+'<div class="nucleus-overlay"><strong>NÚCLEO ARCANO SINCRONIZADO</strong><small>Interface demonstrativa · sem leitura mágica real</small></div></section>'+
panel("ESTADO DO SISTEMA",kv("Integridade de interface","100%")+kv("Armazenamento","Neste navegador")+kv("Conexões físicas","Sob demanda")+kv("Camada arcana","Simulação / lore")+kv("Sessão do operador","Sem autenticação remota")+kv("TS / isolamento",state.isolated?"Protocolo ativo (lore)":"Protocolo inativo (lore)")+'<div class="divider"></div><div class="actions">'+button("open-terminal","⌨ TERMINAL","gold")+button("toggle-isolate",state.isolated?"DESATIVAR PROTOCOLO":"ISOLAR TS","ghost")+'</div>')+'</div>'+
'<h2 class="section-heading">EXPANSÃO ATERNA 2.0</h2><div class="grid three">'+[['aterna','✦ ATERNA AI','Voz e memória'],['gemeo','⬡ Gêmeo Digital','Braço 3D interativo'],['rituais2','✧ Rituais Avançados','Editor de runas'],['tecnologia2','⌘ Tecnologia Real','Conexões autorizadas'],['ts','⊗ Protocolo TS','Narrativa dinâmica'],['espectrometro','⌁ Espectrômetro','Análise de sinais']].map(([id,name,detail])=>'<button class="v2-launch" data-v2="nav-v2" data-to="'+id+'"><strong>'+name+'</strong><small>'+detail+'</small></button>').join('')+'</div>'+'<h2 class="section-heading">Comandos rápidos</h2><div class="actions">'+button("scan-devices","⌘ LINK TECNOLÓGICO")+button("go-rituais","✧ ANALISAR RITUAL")+button("go-energia","⌁ ENERGIA")+button("go-diagnostico","◎ DIAGNÓSTICO")+button("hologram","◈ MODO HOLOGRÁFICO","gold")+'</div>'+
'<h2 class="section-heading">Últimas operações</h2>'+panel("REGISTRO DE EVENTOS",last||empty("Ainda não há registros."))}
function renderTecnologia(){
return intro("MÓDULO 01 · INTEGRAÇÃO REAL","Link Tecnológico","Cadastre dispositivos, solicite conexão a aparelhos autorizados e integre automações compatíveis. Acesso universal a qualquer dispositivo não existe.","HARDWARE + AUTOMAÇÃO")+
'<div class="grid three">'+
panel("BLUETOOTH",'<p class="panel-caption">Pareamento Web Bluetooth (somente dispositivos BLE compatíveis, em navegadores que ofereçam suporte).</p>'+button("bluetooth","PROCURAR BLUETOOTH","gold","style='margin-top:12px'"))+
panel("PORTA SERIAL",'<p class="panel-caption">Solicite acesso explícito a uma porta serial compatível via navegador.</p>'+button("serial","SELECIONAR PORTA","gold","style='margin-top:12px'"))+
panel("USB",'<p class="panel-caption">Solicite acesso a dispositivos USB autorizados, se a API WebUSB estiver disponível.</p>'+button("usb","SELECIONAR USB","gold","style='margin-top:12px'"))+'</div>'+
'<h2 class="section-heading">Catálogo de dispositivos</h2><div class="grid two">'+
panel("CADASTRAR EQUIPAMENTO",'<form id="deviceForm" class="stack">'+txt("deviceName","NOME DO DISPOSITIVO","","text","required maxlength='100'")+select("deviceType","TIPO",["Sensor","Iluminação","Máquina","Computador","CLP / gateway","Outro"])+textarea("deviceNotes","OBSERVAÇÕES")+'<button class="btn" type="submit">+ SALVAR NO CATÁLOGO</button></form>')+
panel("EQUIPAMENTOS REGISTRADOS",state.devices.length?state.devices.slice(0,20).map(x=>'<div class="item-card" style="margin-bottom:10px"><div class="row between"><h3>'+E(x.name)+'</h3><span class="chip">'+E(x.type)+'</span></div><p>'+E(x.notes||"Nenhuma observação")+'</p><span class="muted">'+D(x.at)+' · '+E(x.status||"Catalogado")+'</span></div>').join(""):empty("Nenhum dispositivo registrado. Adicione um ou use uma API de conexão compatível."))+'</div>'+
'<h2 class="section-heading">Automação Home Assistant (opcional)</h2>'+
panel("API DE AUTOMAÇÃO",note("A integração real requer Home Assistant acessível por HTTPS com CORS autorizado. O token de acesso permanece somente nesta sessão de página e não é salvo no armazenamento local.")+'<form id="haForm" class="form-grid" style="margin-top:17px">'+txt("haUrl","URL HTTPS DO HOME ASSISTANT",ha.url,"url","placeholder='https://seu-servidor.exemplo' required")+txt("haToken","TOKEN TEMPORÁRIO","","password","placeholder='Token de acesso longa duração' required")+'<button type="submit" class="btn wide">CONECTAR E LISTAR ENTIDADES</button></form><div id="haEntities" style="margin-top:14px">'+(ha.entities.length?ha.entities.filter(x=>["light","switch","input_boolean"].includes(x.entity_id.split(".")[0])).slice(0,24).map(x=>'<div class="kv"><span>'+E(x.attributes?.friendly_name||x.entity_id)+' <small class="muted">('+E(x.entity_id)+')</small></span>'+button("ha-toggle","ALTERNAR","tiny","data-id='"+E(x.entity_id)+"'")+'</div>').join(""):empty("Nenhuma entidade conectada nesta sessão."))+'</div>')+
'<h2 class="section-heading">Limites de compatibilidade</h2>'+note("Conexões dependem do sistema operacional, do navegador e das permissões. Dispositivos que não ofereçam API compatível não poderão ser controlados automaticamente pelo Nexus.")}
function renderArcano(){
return intro("MÓDULO 02 · SIMULAÇÃO NARRATIVA","Visão Arcana","Catalogue runas, assinaturas mágicas e inscrições. As classificações são fornecidas pelo operador para o roleplay.","CAMADA FICCIONAL")+
'<div class="grid two">'+
panel("NOVA ASSINATURA",'<form id="signatureForm" class="stack">'+txt("sigName","NOME DO ELEMENTO","","text","required maxlength='120'")+select("sigSchool","ESCOLA DE ENERGIA",["Divina","Demoníaca","Arcana","Ancestral","Profana","Artificial","Híbrida"])+txt("sigPower","INTENSIDADE SIMULADA (0–100)",60,"number","min='0' max='100' required")+textarea("sigNotes","RUNAS, SELOS E DESCRIÇÃO")+'<div class="upload-box"><label for="sigImage" class="muted">FOTOGRAFIA / SÍMBOLO (VISUALIZAÇÃO TEMPORÁRIA)</label><input id="sigImage" type="file" accept="image/*" capture="environment"><div id="sigPreview"></div></div><button class="btn" type="submit">REGISTRAR ASSINATURA</button></form>')+
panel("ASSINATURAS CONHECIDAS",state.signatures.length?state.signatures.slice(0,25).map(x=>'<div class="item-card" style="margin-bottom:10px"><div class="row between"><h3>'+E(x.name)+'</h3><span class="chip">'+E(x.school)+'</span></div><p>'+E(x.notes||"Sem descrição")+'</p><div class="meter"><span style="width:'+clamp(Number(x.power)||0,0,100)+'%"></span></div><small class="muted">Intensidade de lore: '+E(x.power)+'/100</small></div>').join(""):empty("Ainda não há assinaturas. Envie uma descrição e registre sua primeira análise."))+'</div>'+
'<h2 class="section-heading">Visão do olho esquerdo</h2>'+panel("PROTOCOLO DO OLHO ARCANO",'<div class="schematic">'+nucleusSvg()+'<small>VARREDURA ARCANA · REPRESENTAÇÃO FICTÍCIA</small></div><p class="panel-caption">A interface simula a leitura do olho arcano do Port e cruza descrições cadastradas. Não detecta energia sobrenatural com sensores reais.</p>')}
function ritualScore(r){
const n=String(r.components||"").split(",").map(x=>x.trim()).filter(Boolean).length;
return clamp(Math.round(32+Number(r.symmetry)*.53-Number(r.energy)*.17+Math.min(n,6)*3),0,99);
}
function renderRituais(){
const r=editingRitual||{name:"Rastreio do Crucifixo Perdido",school:"Divina",components:"Essência arcana, Holy ingot, Sangue de demônio, Sangue de anjo",energy:54,symmetry:72,steps:"Traçar o círculo. Posicionar os quatro componentes. Invocar Anael. Interpretar os sinais."};
const result=lastRitual?'<div class="notice" style="margin-top:15px"><strong>SIMULAÇÃO REGISTRADA</strong><div class="big-number">'+lastRitual.score+'% </div><div>Estabilidade narrativa estimada. '+E(lastRitual.score>=70?"Geometria coerente para o cenário.":"Revisar componentes e geometria no roleplay.")+'</div></div>':"";
return intro("MÓDULO 03 · ESTÚDIO RITUALÍSTICO","Laboratório Ritualístico","Monte círculos, liste materiais e simule custos e estabilidade dentro das regras fictícias do seu universo.","SIMULADOR DE LORE")+
'<div class="grid two">'+panel("EDITOR DE RITUAIS",'<form id="ritualForm" class="stack">'+txt("ritName","NOME DO RITUAL",r.name,"text","required maxlength='120'")+select("ritSchool","CLASSE",["Divina","Demoníaca","Arcana","Ancestral","Profana","Artificial","Híbrida"],r.school)+textarea("ritComponents","COMPONENTES (SEPARADOS POR VÍRGULA)",r.components)+txt("ritEnergy","CUSTO ENERGÉTICO FICTÍCIO (0–100)",r.energy,"number","required min='0' max='100'")+txt("ritSymmetry","SIMETRIA DO CÍRCULO (0–100)",r.symmetry,"number","required min='0' max='100'")+textarea("ritSteps","ETAPAS DE EXECUÇÃO",r.steps)+'<div class="actions"><button type="submit" class="btn gold">SIMULAR E SALVAR</button>'+button("ritual-reset","NOVO RITUAL","ghost")+'</div></form>'+result)+
panel("CÍRCULO E ACERVO",'<div class="schematic"><svg viewBox="0 0 260 230" role="img" aria-label="Diagrama geométrico ilustrativo de um círculo ritualístico"><g stroke="#d3ad73" fill="none"><circle cx="130" cy="115" r="96" stroke-dasharray="8 4"/><circle cx="130" cy="115" r="79"/><circle cx="130" cy="115" r="52" stroke="#a9424f"/><path d="M130 22L218 170H42Z"/><path d="M130 208L42 60H218Z" stroke="#b13e4b"/><path d="M130 51L185 115L130 179L75 115Z"/></g><circle cx="130" cy="115" r="8" fill="#e4c48f"/></svg><small>GEOMETRIA RITUALÍSTICA / CONCEITO</small></div><h3 class="section-heading">Rituais salvos ('+state.rituals.length+')</h3>'+(state.rituals.length?state.rituals.slice(0,12).map(x=>'<div class="kv"><div><strong>'+E(x.name)+'</strong><div class="muted">'+E(x.school)+' · '+E(x.score)+'% de estabilidade simulada</div></div>'+button("load-ritual","ABRIR","tiny","data-id='"+E(x.id)+"'")+'</div>').join(""):empty("Nenhum ritual salvo ainda.")))+'</div>'+
'<h2 class="section-heading">Interpretador ritualístico</h2>'+note("Os percentuais são calculados por uma regra de jogo ilustrativa, com base nos campos inseridos. Não representam uma previsão real nem orientam rituais físicos. Componentes como sangue e sacrifícios pertencem somente à narrativa.")}
function graph(){
const items=state.readings.slice(-16);
if(!items.length)return empty("Sem medições registradas.");
const xs=items.map((_,i)=>20+(i/Math.max(1,items.length-1))*660);
const ys=items.map(x=>180-clamp(Number(x.value)||0,0,100)*1.55);
const points=xs.map((x,i)=>x.toFixed(1)+","+ys[i].toFixed(1)).join(" ");
const dots=xs.map((x,i)=>'<circle cx="'+x+'" cy="'+ys[i]+'" r="3.5" fill="#e6bf7d"><title>'+E(items[i].source)+': '+E(items[i].value)+'</title></circle>').join("");
return '<div class="chart-box"><svg viewBox="0 0 700 200" preserveAspectRatio="none" role="img" aria-label="Gráfico de intensidade energética simulada"><defs><linearGradient id="lines" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#d59d6c" stop-opacity=".26"/><stop offset="1" stop-color="#bc414e" stop-opacity=".01"/></linearGradient></defs><g stroke="#302932" stroke-width="1"><path d="M10 25H690 M10 65H690 M10 105H690 M10 145H690 M10 185H690"/></g><polygon points="20,185 '+points+' 680,185" fill="url(#lines)"/><polyline points="'+points+'" fill="none" stroke="#e1b77a" stroke-width="3" stroke-linejoin="round"/>'+dots+'</svg></div>';
}
function renderEnergia(){
const vals=state.readings.filter(x=>!x.demo);const latest=vals.at(-1);
return intro("MÓDULO 04 · DADOS MANUAIS","Mapa Energético","Visualize tendências e fontes registradas. Os valores arcanos são fornecidos pelo operador e não são medições físicas.","SIMULAÇÃO + HISTÓRICO")+
'<div class="grid two">'+panel("TENDÊNCIA DE ENERGIA",graph()+'<p class="chart-caption">AMOSTRAS DEMONSTRATIVAS E REGISTROS MANUAIS · UNIDADE FICTÍCIA 0–100</p>'+kv("Última leitura real cadastrada",latest?latest.value+"/100 ("+latest.type+")":"Nenhuma")+kv("Registros inseridos",vals.length)+kv("Registros demonstrativos",state.readings.filter(x=>x.demo).length))+
panel("NOVA LEITURA",'<form id="energyForm" class="stack">'+txt("energySource","FONTE","Núcleo Aterna","text","required maxlength='100'")+select("energyType","NATUREZA",["Divina","Demoníaca","Arcana","Ancestral","Profana","Artificial","Híbrida"])+txt("energyValue","INTENSIDADE FICTÍCIA (0–100)",50,"number","required min='0' max='100'")+'<button type="submit" class="btn">ADICIONAR MEDIÇÃO</button></form><div class="divider"></div>'+note("Sensores tecnológicos reais podem alimentar uma integração futura com dados identificados como físicos. Eles jamais serão apresentados como detectores de magia."))+'</div>'+
'<h2 class="section-heading">Leituras recentes</h2>'+panel("HISTÓRICO",state.readings.slice(-12).reverse().map(x=>kv(x.source+" · "+x.type+(x.demo?" [DEMO]":""),x.value+"/100")).join(""))}
function renderEngenharia(){
return intro("MÓDULO 05 · PROJETOS DO PORT","Engenharia Arcana","Oficina de esquemas, construção de artefatos e gestão de projetos técnico-narrativos.","OFICINA ARCANA")+
'<div class="grid two">'+panel("NOVO PROJETO",'<form id="projectForm" class="stack">'+txt("projectName","NOME DO PROJETO","","text","required maxlength='140'")+select("projectKind","CATEGORIA",["Prótese celestial","Artefato","Sistema híbrido","Grimório","Armamento","Outra"])+select("projectStatus","ESTADO",["Ideia","Em desenvolvimento","Em testes","Projeto registrado","Concluído"])+textarea("projectNotes","DESCRIÇÃO, COMPONENTES E MECANISMOS")+'<button type="submit" class="btn">SALVAR PROJETO</button></form>')+
panel("MODELO DE PRÓTESE",'<div class="schematic"><svg viewBox="0 0 260 240" role="img" aria-label="Esquema ilustrativo de braço mecânico com núcleo dourado"><g fill="none" stroke="#d6b47e" stroke-width="2"><path d="M102 18L155 26L163 55L135 76L100 59Z M102 67L138 82L146 133L124 157L91 141Z M92 150L123 169L119 204L103 214L77 198Z M125 168L148 190L143 217 M91 206L88 230 M110 209L109 231 M130 205L125 235"/><path d="M109 34L144 40 M105 103L131 105 M89 178L110 188" stroke="#b74c58"/><circle cx="116" cy="124" r="12" fill="#b88343" fill-opacity=".25"/><circle cx="116" cy="124" r="4" fill="#f2d79a"/></g></svg><small>PROJETO BRAÇO ARCANO DIREITO</small></div><p class="panel-caption">Próximos avanços: visualização 3D, arquivos Blockbench e animações de articulação, com importação de modelos.</p>')+'</div>'+
'<h2 class="section-heading">Projetos catalogados</h2><div class="grid three">'+state.projects.map(x=>'<div class="item-card"><span class="chip">'+E(x.kind)+'</span><h3>'+E(x.name)+'</h3><p>'+E(x.notes)+'</p><span class="muted">'+E(x.status)+'</span></div>').join("")+'</div>'}
function renderDiagnostico(){
return intro("MÓDULO 06 · INSPEÇÃO","Diagnóstico","Registre uma entidade, artefato ou equipamento e gere relatórios organizados de suas propriedades.","RELATÓRIOS")+
'<div class="grid two">'+panel("NOVA INSPEÇÃO",'<form id="diagnosticForm" class="stack">'+txt("diagName","OBJETO / ENTIDADE","","text","required maxlength='120'")+select("diagType","CATEGORIA",["Artefato","Entidade","Máquina","Sistema híbrido","Ambiente","Outro"])+select("diagRisk","RISCO DECLARADO",["Baixo","Moderado","Elevado","Crítico"])+textarea("diagNotes","OBSERVAÇÕES TÉCNICAS OU ARCANAS")+'<button class="btn" type="submit">EMITIR RELATÓRIO</button></form>')+
panel("RELATÓRIOS RECENTES",state.reports.length?state.reports.slice(0,15).map(x=>'<div class="item-card" style="margin-bottom:9px"><div class="row between"><h3>'+E(x.name)+'</h3><span class="chip '+(x.risk==="Crítico"?"danger":"")+'">'+E(x.risk)+'</span></div><p>'+E(x.type)+" · "+E(x.notes)+'</p><span class="muted">'+D(x.at)+'</span></div>').join(""):empty("Não existem relatórios. Crie uma inspeção para iniciar."))+'</div>'+note("Os relatórios refletem as informações digitadas. O Nexus não diagnostica entidades nem detecta corrupção ou maldições de verdade.")}
function renderRegistros(){
return intro("MÓDULO 07 · MEMÓRIA E CONTROLE","Arquivos e Segurança","Gerencie dados locais, exporte backups e monitore o histórico narrativo do Nexus.","ARMAZENAMENTO LOCAL")+
'<div class="grid two">'+panel("PROTOCOLOS",'<div class="kv"><span>Isolamento fictício dos TS</span><button type="button" class="switch '+(state.isolated?"on":"")+'" data-act="toggle-isolate" aria-pressed="'+state.isolated+'" aria-label="Alternar isolamento narrativo dos TS"><i></i></button></div>'+kv("Persistência","localStorage do navegador")+kv("Autenticação real","Não configurada")+kv("Sincronização em nuvem","Não configurada")+kv("Registros existentes",state.logs.length)+note("O isolamento TS é um recurso de roleplay. Não cria firewall, criptografia nem barreira de segurança real."))+
panel("BACKUP E RECUPERAÇÃO",'<div class="stack"><p class="panel-caption">Exporte seus registros como JSON para guardar em local seguro. Restaurar substitui os dados da interface neste navegador.</p>'+button("export","↓ EXPORTAR JSON","gold")+'<div class="field"><label for="importFile">RESTAURAR BACKUP</label><input id="importFile" type="file" accept=".json,application/json"></div>'+button("clear","APAGAR TODOS OS DADOS LOCAIS","warn")+'</div>')+'</div>'+
'<h2 class="section-heading">Diário de operações</h2>'+panel("EVENTOS REGISTRADOS",state.logs.length?state.logs.slice(0,100).map(timeItem).join(""):empty("Nenhum evento salvo."))+
'<h2 class="section-heading">Memória Arcana</h2>'+note("Dados de lore, rituais, assinaturas, projetos e relatórios permanecem neste navegador, mesmo após fechar a página (salvo se os dados do navegador forem apagados). Não há banco compartilhado entre dispositivos nesta versão.")}
function render(){
window.NexusV2?.beforeRender?.(page);
$("#content").innerHTML=window.NexusV2?.hasPage?.(page)?window.NexusV2.renderPage(page):({nucleo:renderNucleo,tecnologia:renderTecnologia,arcano:renderArcano,rituais:renderRituais,energia:renderEnergia,engenharia:renderEngenharia,diagnostico:renderDiagnostico,registros:renderRegistros}[page]||renderNucleo)();
window.NexusV2?.afterRender?.(page);
$("#breadcrumb").textContent=PAGES[page];
document.querySelectorAll("[data-v2-page]").forEach(x=>{const isActive=x.dataset.v2Page===page;x.classList.toggle("active",isActive);x.setAttribute("aria-current",isActive?"page":"false");});
document.querySelectorAll("[data-page]").forEach(x=>{x.classList.toggle("active",x.dataset.page===page);x.setAttribute("aria-current",x.dataset.page===page?"page":"false");});
$("#isolationSide").textContent=state.isolated?"ATIVO":"INATIVO";
}
function navigate(next){if(!PAGES[next])return;page=next;location.hash=next;render();$("#main").scrollTop=0;window.scrollTo({top:0,behavior:"instant"});setMenu(false);}
function setMenu(open){$("#sidebar").classList.toggle("open",open);$("#overlay").hidden=!open&&!$("#terminalDrawer").classList.contains("open");}
function terminalOpen(open){$("#terminalDrawer").classList.toggle("open",open);$("#terminalDrawer").setAttribute("aria-hidden",String(!open));$("#overlay").hidden=!open&&!$("#sidebar").classList.contains("open");if(open)$("#terminalInput").focus();}
function setHologram(){document.body.classList.toggle("hologram-mode");if(page!=="nucleo")navigate("nucleo");toast(document.body.classList.contains("hologram-mode")?"Projeção holográfica ativada.":"Modo operacional restaurado.");}
function toggleIsolate(){state.isolated=!state.isolated;log("Isolamento narrativo TS "+(state.isolated?"ativado.":"desativado."),"segurança");render();toast("Protocolo TS "+(state.isolated?"ativado":"desativado")+" (somente lore).");}
const value=(f,n)=>String(new FormData(f).get(n)||"").trim();
function handleForm(e){
const f=e.target;if(!["deviceForm","signatureForm","ritualForm","energyForm","projectForm","diagnosticForm","haForm"].includes(f.id))return;
e.preventDefault();
if(f.id==="haForm"){connectHA(f);return;}
if(f.id==="deviceForm"){const name=value(f,"deviceName");if(!name)return;state.devices.unshift({id:uid(),name,type:value(f,"deviceType"),notes:value(f,"deviceNotes"),status:"Catalogado (sem conexão)",at:new Date().toISOString()});log("Dispositivo "+name+" catalogado.","tecnologia");}
if(f.id==="signatureForm"){const name=value(f,"sigName");if(!name)return;state.signatures.unshift({id:uid(),name,school:value(f,"sigSchool"),power:clamp(Number(value(f,"sigPower")),0,100),notes:value(f,"sigNotes"),at:new Date().toISOString()});log("Assinatura arcana "+name+" registrada.","arcano");}
if(f.id==="ritualForm"){const r={id:uid(),name:value(f,"ritName"),school:value(f,"ritSchool"),components:value(f,"ritComponents"),energy:clamp(Number(value(f,"ritEnergy")),0,100),symmetry:clamp(Number(value(f,"ritSymmetry")),0,100),steps:value(f,"ritSteps"),at:new Date().toISOString()};if(!r.name)return;r.score=ritualScore(r);state.rituals.unshift(r);lastRitual=r;editingRitual=r;log("Simulação ritual "+r.name+" concluída ("+r.score+"%).","rituais");}
if(f.id==="energyForm"){const source=value(f,"energySource");if(!source)return;state.readings.push({id:uid(),source,type:value(f,"energyType"),value:clamp(Number(value(f,"energyValue")),0,100),at:new Date().toISOString()});log("Leitura energética fictícia cadastrada: "+source+".","energia");}
if(f.id==="projectForm"){const name=value(f,"projectName");if(!name)return;state.projects.unshift({id:uid(),name,kind:value(f,"projectKind"),status:value(f,"projectStatus"),notes:value(f,"projectNotes"),at:new Date().toISOString()});log("Projeto de engenharia "+name+" criado.","engenharia");}
if(f.id==="diagnosticForm"){const name=value(f,"diagName");if(!name)return;state.reports.unshift({id:uid(),name,type:value(f,"diagType"),risk:value(f,"diagRisk"),notes:value(f,"diagNotes"),at:new Date().toISOString()});log("Relatório de diagnóstico emitido para "+name+".","diagnóstico");}
persist();render();toast("Registro salvo no navegador.");}
async function connectHA(f){
const u=value(f,"haUrl").replace(/\/+$/,"");const token=value(f,"haToken");if(!/^https:\/\/[^ ]+/i.test(u)){toast("Informe uma URL HTTPS válida.");return;}if(!token){toast("Informe o token para esta sessão.");return;}
ha.url=u;ha.token=token;
try{const response=await fetch(u+"/api/states",{headers:{Authorization:"Bearer "+token,"Content-Type":"application/json"}});if(!response.ok)throw Error("HTTP "+response.status);const entities=await response.json();if(!Array.isArray(entities))throw Error("Resposta inesperada");ha.entities=entities;log("Home Assistant consultado por API HTTPS.","tecnologia");render();toast(entities.length+" entidades consultadas. Use alternar apenas em dispositivos seus.");}
catch(err){ha.entities=[];toast("Conexão falhou: "+err.message+". Verifique HTTPS, CORS e autorização.");}
}
async function toggleHA(id){
if(!ha.url||!ha.token)return toast("Conecte o Home Assistant primeiro.");
if(!ha.entities.some(x=>x.entity_id===id))return toast("Entidade não listada nesta sessão.");
if(!confirm("Enviar comando ALTERNAR para "+id+"?"))return;
try{const response=await fetch(ha.url+"/api/services/homeassistant/toggle",{method:"POST",headers:{Authorization:"Bearer "+ha.token,"Content-Type":"application/json"},body:JSON.stringify({entity_id:id})});if(!response.ok)throw Error("HTTP "+response.status);log("Comando Home Assistant enviado para "+id+".","tecnologia");toast("Comando enviado. Atualize a listagem para verificar o estado.");}
catch(err){toast("Erro na automação: "+err.message);}
}
async function connectHardware(kind){
try{
 if(kind==="bluetooth"){if(!navigator.bluetooth)throw Error("Web Bluetooth indisponível neste navegador");const device=await navigator.bluetooth.requestDevice({acceptAllDevices:true,optionalServices:["battery_service","device_information"]});state.devices.unshift({id:uid(),name:device.name||"BLE sem nome",type:"Bluetooth LE",notes:"Dispositivo escolhido via diálogo do navegador; controle depende de serviços GATT compatíveis.",status:"Selecionado via Web Bluetooth",at:new Date().toISOString()});log("Dispositivo Bluetooth autorizado: "+(device.name||"sem nome"),"tecnologia");}
 if(kind==="serial"){if(!navigator.serial)throw Error("Web Serial indisponível neste navegador");const port=await navigator.serial.requestPort();state.devices.unshift({id:uid(),name:"Porta serial autorizada",type:"Serial",notes:JSON.stringify(port.getInfo?.()||{}),status:"Porta selecionada (não aberta)",at:new Date().toISOString()});log("Porta serial autorizada pelo usuário.","tecnologia");}
 if(kind==="usb"){if(!navigator.usb)throw Error("WebUSB indisponível neste navegador");const device=await navigator.usb.requestDevice({filters:[]});state.devices.unshift({id:uid(),name:device.productName||"Dispositivo USB",type:"USB",notes:"Fabricante: "+(device.manufacturerName||"não informado"),status:"USB autorizado (sem comando)",at:new Date().toISOString()});log("Acesso USB autorizado pelo usuário.","tecnologia");}
 persist();render();toast("Dispositivo adicionado ao catálogo.");}
catch(err){toast(err.name==="NotFoundError"?"Seleção cancelada.":"Não foi possível conectar: "+err.message);}
}
function exportData(){const data=JSON.stringify({format:"nexus-aterna-v1",exportedAt:new Date().toISOString(),data:state,v2:window.NexusV2?.exportState?.()||null},null,2);const blob=new Blob([data],{type:"application/json"});const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download="nexus-aterna-backup-"+new Date().toISOString().slice(0,10)+".json";a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);toast("Backup JSON preparado para download.");}
async function restoreFile(file){
if(!file)return;if(file.size>2*1024*1024)return toast("Backup muito grande (máximo 2 MB).");
try{const obj=JSON.parse(await file.text());const data=obj.format==="nexus-aterna-v1"?obj.data:null;if(!data||typeof data!=="object")throw Error("Formato incompatível");for(const k of ["devices","signatures","rituals","reports","readings","projects","logs"]){if(!Array.isArray(data[k]))throw Error("Campo inválido: "+k);data[k]=data[k].slice(0,500);}if(!confirm("Substituir todos os dados deste navegador pelo backup?"))return;state={...defaults(),...data,isolated:Boolean(data.isolated)};if(obj.v2)window.NexusV2?.importState?.(obj.v2);log("Backup restaurado pelo operador.","sistema");persist();render();toast("Backup restaurado.");}catch(err){toast("Falha ao restaurar: "+err.message);}
}
function commandLine(text,css){const el=document.createElement("div");el.className=css;el.textContent=text;$("#terminalOutput").appendChild(el);$("#terminalOutput").scrollTop=$("#terminalOutput").scrollHeight;}
function assistant(command){
const nexus2=window.NexusV2?.command?.(command);if(nexus2)return nexus2;
let c=command.toUpperCase().replace(/^NEXUS\s+/,"").trim();
if(!c)return "Aguardando parâmetros, Port. Digite AJUDA.";
if(/^(AJUDA|HELP|COMANDOS)$/.test(c))return "COMANDOS:\nSTATUS · ANALISAR RITUAL · LINK TECNOLÓGICO · VISÃO ARCANA\nMAPA ENERGÉTICO · ENGENHARIA · DIAGNÓSTICO · REGISTROS\nISOLAR TS · LIBERAR TS · EXPORTAR · HOLOGRAMA\nPergunte também sobre braço, grimório, ritual ou energia.";
if(/STATUS|ESTADO/.test(c))return "Núcleo ativo na sessão local. "+state.rituals.length+" rituais, "+state.devices.length+" dispositivos registrados e "+state.projects.length+" projetos. Isolamento TS (lore): "+(state.isolated?"ativo":"inativo")+".";
if(/LIBERAR TS|DESATIVAR ISOLAMENTO/.test(c)){if(state.isolated)toggleIsolate();return "Isolamento narrativo desativado. Esta mudança não altera a segurança tecnológica real.";}
if(/ISOLAR TS|ATIVAR ISOLAMENTO/.test(c)){if(!state.isolated)toggleIsolate();return "Protocolo de isolamento narrativo TS ativado. Proteções tecnológicas reais devem ser configuradas separadamente.";}
if(/EXPORTAR|BACKUP/.test(c)){exportData();return "Exportação de registros solicitada.";}
if(/HOLOGRAMA|HOLOGRÁFICO/.test(c)){setHologram();return "Projeção holográfica alterada.";}
if(/RITUAL|CRUCIFIXO/.test(c)){navigate("rituais");return "Laboratório aberto. Para o ritual do crucifixo, confira os quatro catalisadores canônicos. A simulação produz apenas métricas fictícias.";}
if(/TECNOLOG|BLUETOOTH|WI.FI|DISPOSITIV|SENSOR/.test(c)){navigate("tecnologia");return "Link Tecnológico aberto. Controle real depende de protocolos compatíveis e autorização explícita.";}
if(/ARCAN|RUNA|MALDI|VISÃO/.test(c)){navigate("arcano");return "Visão Arcana aberta. Descreva a assinatura e registre sua escola de energia para o arquivo ficcional.";}
if(/ENERG|FLUXO|ANOMALIA/.test(c)){navigate("energia");return "Mapa Energético aberto. Leituras arcanas são declaradas manualmente e marcadas como simulação.";}
if(/ENGENH|BRAÇO|PRÓTESE|GRIMÓRIO|BENGALA/.test(c)){navigate("engenharia");return "Oficina aberta. Referências canônicas incluem o braço direito, o olho arcano esquerdo e a bengala-grimório.";}
if(/DIAGN|RELATÓRIO|ARTEFAT|ENTIDADE/.test(c)){navigate("diagnostico");return "Painel de Diagnóstico aberto. Registre as observações para gerar um relatório descritivo.";}
if(/REGIST|ARQUIVO|HISTÓRICO|LOG/.test(c)){navigate("registros");return "Arquivos abertos. Todas as alterações estão restritas ao armazenamento local deste navegador.";}
return "Comando não mapeado no interpretador local. Posso direcionar para tecnologia, arcano, rituais, energia, engenharia, diagnóstico e registros. Para conversas livres com IA será necessário configurar um serviço de IA seguro no servidor.";
}
function runCommand(command){const c=command.trim();if(!c)return;commandLine("PORT ❯ "+c,"t-cmd");commandLine("[ATERNA] "+assistant(c),"t-gold");}
$("#sideNav").addEventListener("click",e=>{const b=e.target.closest("[data-page]");if(b)navigate(b.dataset.page);});
$("#mobileMenu").addEventListener("click",()=>setMenu(true));
$("#terminalToggle").addEventListener("click",()=>terminalOpen(true));
$("#openTerminalSide").addEventListener("click",()=>terminalOpen(true));
$("#terminalClose").addEventListener("click",()=>terminalOpen(false));
$("#holoToggle").addEventListener("click",setHologram);
$("#overlay").addEventListener("click",()=>{setMenu(false);terminalOpen(false);});
window.addEventListener("keydown",e=>{if(e.key==="Escape"){setMenu(false);terminalOpen(false);}if(e.ctrlKey&&e.key.toLowerCase()==="k"){e.preventDefault();terminalOpen(true);}});
$("#terminalForm").addEventListener("submit",e=>{e.preventDefault();const v=$("#terminalInput").value;$("#terminalInput").value="";runCommand(v);});
$("#content").addEventListener("submit",handleForm);
$("#content").addEventListener("click",e=>{
const el=e.target.closest("[data-act]");if(!el)return;const act=el.dataset.act;
if(act.startsWith("go-"))return navigate(act.slice(3));
if(act==="open-terminal")return terminalOpen(true);
if(act==="hologram")return setHologram();
if(act==="toggle-isolate")return toggleIsolate();
if(act==="scan-devices")return navigate("tecnologia");
if(["bluetooth","serial","usb"].includes(act))return connectHardware(act);
if(act==="ritual-reset"){editingRitual={name:"",school:"Arcana",components:"",energy:50,symmetry:50,steps:""};lastRitual=null;return render();}
if(act==="load-ritual"){const r=state.rituals.find(x=>x.id===el.dataset.id);if(r){editingRitual=r;lastRitual=r;render();window.scrollTo({top:0,behavior:"smooth"});}return;}
if(act==="ha-toggle")return toggleHA(el.dataset.id);
if(act==="export")return exportData();
if(act==="clear"){if(!confirm("Apagar permanentemente os dados locais deste navegador? Faça backup antes."))return;state=defaults();window.NexusV2?.clearState?.();editingRitual=null;lastRitual=null;persist();render();toast("Armazenamento local reiniciado.");}
});
$("#content").addEventListener("change",async e=>{
if(e.target.id==="importFile")return restoreFile(e.target.files?.[0]);
if(e.target.id==="sigImage"){if(previewUrl)URL.revokeObjectURL(previewUrl);const file=e.target.files?.[0];if(file){if(!file.type.startsWith("image/"))return toast("Selecione uma imagem.");previewUrl=URL.createObjectURL(file);$("#sigPreview").innerHTML='<img src="'+E(previewUrl)+'" alt="Prévia temporária do símbolo"><small class="muted">Prévia local. A imagem não é salva no backup.</small>';}}
});
window.addEventListener("hashchange",()=>{const target=location.hash.slice(1);if(PAGES[target]&&target!==page){page=target;render();}});
const tick=()=>$("#clock").textContent=new Date().toLocaleString("pt-BR",{dateStyle:"short",timeStyle:"medium"});
window.NexusV2?.init?.({getState:()=>state,log,toast,navigate,render,persistOld:persist,terminalOpen});
if(window.NexusV2)window.NexusV2.persistOld=persist;
tick();setInterval(tick,1000);render();
if("serviceWorker" in navigator)window.addEventListener("load",()=>navigator.serviceWorker.register("./sw.js").catch(()=>{}));
})();