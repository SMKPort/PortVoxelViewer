"use strict";
(function(){
window.NexusRPEvolution=function(rp,views){
  const S=rp.state,E=rp.escape,nav=rp.navigate,toast=rp.toast,save=rp.save,log=rp.log;
  const K=v=>String(v||"").trim();
  const clamp=(v,a,b)=>Math.max(a,Math.min(b,Number(v)||0));
  const uid=()=>Math.random().toString(36).slice(2,10);
  const profiles=[
    {name:"Celestial",kind:"Equilibrada",spell:30,cost:9,color:"#d9b474",description:"Placas douradas, conduítes equilibrados e amplificação sagrada.",locked:true},
    {name:"Combate",kind:"Ofensiva",spell:60,cost:14,color:"#cf575d",description:"Blindagem reforçada e canais de conjuração ofensiva.",locked:true},
    {name:"Ritual",kind:"Canalização",spell:45,cost:11,color:"#b98ad7",description:"Runas expostas e circulação de energia ritualística.",locked:true},
    {name:"Repouso",kind:"Recuperação",spell:10,cost:4,color:"#82bfb0",description:"Redução do consumo arcano, com placas parcialmente recolhidas.",locked:true}
  ];
  if(!Array.isArray(S.armorProfiles))S.armorProfiles=profiles;
  else profiles.forEach(p=>{if(!S.armorProfiles.some(x=>x.name===p.name))S.armorProfiles.push(p);});
  if(!Array.isArray(S.sequences))S.sequences=[
    {id:"opening",name:"Manifestação Completa",steps:[{action:"desintegrar",delay:1100},{action:"recuperar",delay:900},{action:"remodelar",form:"Celestial",delay:1300},{action:"integrar",delay:1500}]},
    {id:"battle",name:"Configuração de Combate",steps:[{action:"desintegrar",delay:900},{action:"remodelar",form:"Combate",delay:1400},{action:"integrar",delay:1000}]}
  ];
  if(!S.emergency||typeof S.emergency!=="object")S.emergency={enabled:true,active:false,threshold:25,lastAt:""};
  if(!S.emotes||typeof S.emotes!=="object")S.emotes={integrar:"",desintegrar:"",remodelar:"",reparar:"",ritual:"",emergencia:""};
  if(!S.gameEvents||typeof S.gameEvents!=="object")S.gameEvents={enabled:false,lastId:0,bootstrapped:false,damageFactor:5,healEnergy:3,cursor:0,feed:[],lastSync:"",error:""};
  const G=S.gameEvents,EM=S.emergency;
  G.enabled=Boolean(G.enabled);G.lastId=Number(G.lastId)||0;G.damageFactor=clamp(G.damageFactor,1,10);G.healEnergy=clamp(G.healEnergy,0,15);if(!Array.isArray(G.feed))G.feed=[];
  let chosenSequence=S.sequences[0]?.id||"", seqToken=0, seqStatus="",seqIndex=-1,pollBusy=false,lastStatus={},activeRefresh=null;
  const button=(act,title,style="alt",attrs="")=>'<button type="button" class="btn '+style+'" data-evo="'+E(act)+'" '+attrs+'>'+title+'</button>';
  const block=(label,contents)=>'<section class="card"><div class="card-header"><h2>'+E(label)+'</h2></div>'+contents+'</section>';
  const intro=(code,title,sub)=>'<div class="eyebrow">'+E(code)+'</div><h1 class="heading">'+E(title)+'</h1><p class="subheading">'+E(sub)+'</p>';
  const kv=(label,value)=>'<div class="kv"><span>'+E(label)+'</span><span class="value">'+E(value)+'</span></div>';
  const meter=n=>'<div class="meter"><span style="width:'+clamp(n,0,100)+'%"></span></div>';
  const validFn=v=>!v||/^[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,90}$/.test(v);
  const activeProfile=()=>S.armorProfiles.find(p=>p.name===S.form)||S.armorProfiles[0];
  rp.allowedForms=()=>S.armorProfiles.map(p=>p.name);
  rp.formCost=name=>clamp(S.armorProfiles.find(p=>p.name===name)?.cost??9,0,50);
  rp.canArmorAction=kind=>!EM.active||!["integrar","remodelar"].includes(kind);
  function checkEmergency(){
    EM.threshold=clamp(EM.threshold,5,80);
    if(EM.enabled&&S.integrity<=EM.threshold&&!EM.active){
      EM.active=true;EM.lastAt=new Date().toISOString();
      log("CONTENÇÃO ATIVADA: integridade "+S.integrity+"%. Integração e remodelagem bloqueadas.","EMERGÊNCIA");
      toast("NEXUS EM CONTENÇÃO. Integridade crítica.");
      triggerEmote("emergencia");
    }
    save();
  }
  const oldAlert=rp.checkAlerts||(()=>{});
  rp.checkAlerts=()=>{oldAlert();checkEmergency();};
  const oldAdjust=rp.adjustIntegrity||(()=>{});
  rp.adjustIntegrity=value=>{oldAdjust(value);checkEmergency();};
  const oldAfterArmor=rp.afterArmor||(()=>{});
  rp.afterArmor=kind=>{oldAfterArmor(kind);triggerEmote(kind);rp.checkAlerts();};
  const oldAfterRitual=rp.afterRitual||(()=>{});
  rp.afterRitual=()=>{oldAfterRitual();triggerEmote("ritual");rp.checkAlerts();};
  const oldReset=rp.resetUpgrade||(()=>{});
  rp.resetUpgrade=()=>{oldReset();S.armorProfiles=profiles.map(x=>({...x}));S.sequences=[];Object.assign(EM,{enabled:true,active:false,threshold:25,lastAt:""});S.emergency=EM;S.emotes={integrar:"",desintegrar:"",remodelar:"",reparar:"",ritual:"",emergencia:""};Object.assign(G,{enabled:false,lastId:0,bootstrapped:false,damageFactor:5,healEnergy:3,cursor:0,feed:[],lastSync:"",error:""});S.gameEvents=G;seqToken++;save();};
  function triggerEmote(kind){
    const functionId=K(S.emotes?.[kind]);
    if(!rp.isConnected()||!functionId)return;
    rp.runFunctionId(functionId).then(()=>log("Função de emote executada: "+functionId,"EMOTE")).catch(err=>toast("Emote indisponível: "+err.message));
  }
  function renderForms(){
    const items=S.armorProfiles.map((p,i)=>'<article class="rp-evo-form" style="--form-accent:'+E(p.color)+'"><div class="rp-evo-form-head"><strong>'+E(p.name)+'</strong><span>'+E(p.kind)+'</span></div>'+
      '<div class="rp-evo-values"><span>Holy Spellpower <b>+'+clamp(p.spell,0,60)+'%</b></span><span>Remodelagem <b>'+clamp(p.cost,0,50)+' EN</b></span></div>'+
      '<p>'+E(p.description||"Sem descrição")+'</p><div class="buttons">'+button("form-use","APLICAR","gold",'data-name="'+E(p.name)+'"')+
      (p.locked?"":button("form-delete","EXCLUIR","ghost",'data-name="'+E(p.name)+'"'))+'</div></article>').join("");
    return '<h2 class="section-title">Protocolos de armadura personalizados</h2><div class="rp-evo-forms">'+items+'</div>'+
      block("CRIAR NOVA FORMA",
      '<form id="rpFormCreator" class="grid two"><label class="field"><span class="field-label">NOME DA FORMA</span><input name="name" required maxlength="28" placeholder="Ex.: Serafim"></label>'+
      '<label class="field"><span class="field-label">ESTILO</span><input name="kind" required maxlength="28" placeholder="Ex.: Celestial ofensiva"></label>'+
      '<label class="field"><span class="field-label">HOLY SPELLPOWER (0 A 60%)</span><input type="number" name="spell" min="0" max="60" value="35" required></label>'+
      '<label class="field"><span class="field-label">CUSTO ENERGÉTICO (0 A 50)</span><input type="number" name="cost" min="0" max="50" value="10" required></label>'+
      '<label class="field"><span class="field-label">COR DA PROJEÇÃO</span><input type="color" name="color" value="#d6aa6e"></label>'+
      '<label class="field"><span class="field-label">DESCRIÇÃO</span><input name="description" maxlength="180" placeholder="Aparência e capacidades narrativas"></label>'+
      '<button type="submit" class="btn gold">+ CRIAR CONFIGURAÇÃO</button></form>'+
      '<p class="hint">As amplificações são valores de lore. Para alterar atributos reais do Minecraft, será necessário um mod ou datapack correspondente.</p>');
  }
  const originalArmor=views.armadura;
  views.armadura=()=>{
    let html=originalArmor();
    const extra=S.armorProfiles.filter(x=>!["Celestial","Combate","Ritual","Repouso"].includes(x.name));
    const extraOptions=extra.map(p=>'<option '+(S.form===p.name?"selected":"")+'>'+E(p.name)+'</option>').join("");
    html=html.replace(/(<select id="armorForm">)([\s\S]*?)(<\/select>)/,(_all,a,b,c)=>a+b+extraOptions+c);
    return html+renderForms();
  };
  const currentStep=()=>S.sequences.find(x=>x.id===chosenSequence)||S.sequences[0];
  const ACTS=[["desintegrar","Desintegrar armadura"],["remodelar","Remodelar armadura"],["integrar","Integrar armadura"],["reparar","Reparar armadura"],["recuperar","Recuperar energia"],["emote","Executar emote configurado"],["esperar","Apenas aguardar"]];
  function sequenceView(){
    const current=currentStep();
    const rows=(current?.steps||[]).map((step,i)=>{
      const label=ACTS.find(x=>x[0]===step.action)?.[1]||step.action;
      return '<div class="rp-evo-step"><div><strong>ETAPA '+(i+1)+' · '+E(label)+'</strong><small>Intervalo: '+clamp(step.delay,250,12000)/1000+' s'+(step.form?" · "+E(step.form):"")+'</small></div>'+
      '<div class="buttons">'+button("seq-up","↑","ghost",'data-index="'+i+'"')+button("seq-down","↓","ghost",'data-index="'+i+'"')+
      button("seq-remove","✕","ghost",'data-index="'+i+'"')+'</div></div>';
    }).join("");
    const names=S.armorProfiles.map(x=>'<option>'+E(x.name)+'</option>').join("");
    return intro("NEXUS RP / AUTOMAÇÃO NARRATIVA","Sequenciador de Transformações","Crie sequências ordenadas para desintegrar placas, ajustar energia, remodelar a armadura e executar emotes configurados.")+
    '<div class="grid two">'+
    block("SEQUÊNCIAS SALVAS",
      '<div class="field"><label for="rpSequenceList">SEQUÊNCIA ATIVA</label><select id="rpSequenceList">'+S.sequences.map(x=>'<option value="'+E(x.id)+'" '+(current?.id===x.id?"selected":"")+'>'+E(x.name)+'</option>').join("")+'</select></div>'+
      '<form id="rpNewSequence" class="rp-new-sequence"><label class="field"><span class="field-label">CRIAR NOVA SEQUÊNCIA</span><input name="name" required maxlength="45" placeholder="Ex.: Forma de combate"></label><button class="btn gold" type="submit">+ CRIAR</button></form>'+
      '<div class="rp-evo-status">'+(seqToken&&seqStatus?"ESTADO: "+E(seqStatus):"Nenhuma execução em andamento")+'</div>'+
      '<div class="buttons">'+button("seq-run","▶ EXECUTAR","gold")+button("seq-stop","■ INTERROMPER","ghost")+
      button("seq-delete","EXCLUIR SEQUÊNCIA","ghost")+'</div>')+
    block("ETAPAS E INTERVALOS",
      rows||'<div class="empty">Adicione a primeira etapa desta sequência.</div>'+
      '<form id="rpNewStep" class="rp-evo-step-form"><label class="field"><span class="field-label">AÇÃO</span><select name="action">'+ACTS.map(([key,label])=>'<option value="'+key+'">'+label+'</option>').join("")+'</select></label>'+
      '<label class="field"><span class="field-label">FORMA PARA REMODELAGEM</span><select name="form">'+names+'</select></label>'+
      '<label class="field"><span class="field-label">INTERVALO (MILISSEGUNDOS)</span><input name="delay" type="number" min="250" max="12000" value="1250" required></label>'+
      '<button type="submit" class="btn gold">+ ADICIONAR ETAPA</button></form>'+
      '<p class="hint">As etapas só executam quando você toca em Executar. Não alteram o Minecraft sem a ponte configurada e os comandos correspondentes.</p>')+'</div>';
  }
  views.sequencias=sequenceView;
  const originalLive=views.ao_vivo;
  views.ao_vivo=()=>originalLive()+block("CONTROLES AVANÇADOS RP 1.2",
    '<div class="buttons">'+button("open-sequences","SEQUENCIADOR","gold")+button("open-emergency","EMERGÊNCIA","alt")+button("open-events","EVENTOS DO JOGO","ghost")+"</div>");
  function emergencyView(){
    return intro("PROTEÇÃO DO NÚCLEO / CONTENÇÃO","Protocolo de Emergência","Bloqueia transformações quando a integridade está crítica e permite uma intervenção manual durante o RP.")+
    '<div class="grid two">'+block("ESTADO DE CONTENÇÃO",
      '<div class="rp-evo-emergency '+(EM.active?"active":"")+'"><strong>'+(EM.active?"CONTENÇÃO ATIVA":"SISTEMA ESTÁVEL")+'</strong><span>Integridade: '+S.integrity+'% · Limiar: '+EM.threshold+'%</span></div>'+
      '<form id="rpEmergencySettings"><label class="field inline"><input type="checkbox" name="enabled" '+(EM.enabled?"checked":"")+'><span>Ativar protocolo quando atingir o limiar</span></label>'+
      '<label class="field"><span class="field-label">LIMIAR DE INTEGRIDADE (5 A 80%)</span><input type="number" name="threshold" min="5" max="80" value="'+EM.threshold+'"></label>'+
      '<button class="btn gold" type="submit">SALVAR PROTOCOLO</button></form>'+
      '<div class="buttons">'+button("emergency-activate","ATIVAR CONTENÇÃO","alt")+
      button("emergency-release","LIBERAR CONTENÇÃO","ghost")+'</div>'+
      '<p class="hint">Quando o sistema está em contenção, comandos de integrar e remodelar são bloqueados até a liberação manual. Reparo e desintegração continuam disponíveis.</p>')+
    block("DIAGNÓSTICO E RECUPERAÇÃO",
      kv("Integridade média",S.integrity+"%")+kv("Energia arcana",S.energy+"%")+kv("Bloqueios",EM.active?"Integração e remodelagem":"Nenhum")+
      '<div class="buttons">'+button("emergency-repair","✧ REPARAR ARMADURA","gold")+
      button("emergency-rest","RECUPERAR 15 ENERGIA","ghost")+'</div>'+
      '<div class="notice">Este é um mecanismo de jogo narrativo. Não corresponde a uma proteção elétrica ou eletrônica real.</div>')+'</div>';
  }
  views.emergencia=emergencyView;
  const AGENT_TYPE={damage:"Dano recebido",heal:"Regeneração",death:"Morte do personagem",respawn:"Renascimento",effect:"Efeito ativo modificado",effect_end:"Efeito removido",health:"Leitura de vida",armor:"Armadura",energy:"Energia",join:"Entrou no servidor",leave:"Saiu do servidor"};
  function eventView(){
    const feed=G.feed.slice(-20).reverse().map(x=>'<div class="rp-evo-event"><span>'+E(AGENT_TYPE[x.type]||x.type)+' · '+E(x.player)+'</span><small>'+E(x.at)+' · '+E(x.note||x.amount||"")+'</small></div>').join("");
    return intro("TELEMETRIA / MINECRAFT 1.21.1","Eventos do Minecraft","O agente opcional observa seu personagem, aproxima eventos de dano/regeneração e envia os dados ao Nexus pela ponte HTTPS.")+
    '<div class="grid two">'+block("MONITORAMENTO DO PERSONAGEM",
      '<div class="rp-evo-emergency '+(lastStatus.online?"online":"")+'"><strong>'+(lastStatus.online?"AGENTE CONECTADO":"AGENTE NÃO CONFIRMADO")+'</strong><span>'+E(lastStatus.lastSeen||"Sem comunicação recente")+'</span></div>'+
      '<form id="rpEventSettings"><label class="field inline"><input name="enabled" type="checkbox" '+(G.enabled?"checked":"")+'><span>Usar eventos do Minecraft para atualizar o Nexus</span></label>'+
      '<label class="field"><span class="field-label">DANO ESTRUTURAL POR PONTO DE DANO (1 A 10)</span><input name="damageFactor" type="number" min="1" max="10" value="'+G.damageFactor+'"></label>'+
      '<label class="field"><span class="field-label">ENERGIA RECUPERADA POR CURA (0 A 15)</span><input name="healEnergy" type="number" min="0" max="15" value="'+G.healEnergy+'"></label>'+
      '<button class="btn gold" type="submit">SALVAR MONITORAMENTO</button></form>'+
      '<div class="buttons">'+button("events-refresh","↻ ATUALIZAR EVENTOS","gold")+
      button("events-test","SIMULAR DANO LOCAL","ghost")+'</div>'+
      '<p class="hint">Requer agente de servidor em execução e o código de acesso ao Nexus informado no painel. Eventos de dano/cura vêm de leituras periódicas da vida do jogador e podem não registrar impactos muito rápidos.</p>')+
    block("REGISTRO DE TELEMETRIA",feed||'<div class="empty">Nenhum evento recebido. O aplicativo não pode observar o Minecraft diretamente sem uma integração.</div>'+
      '<div class="notice">Para não expor a porta RCON, prefira o modo Agente HTTPS, disponível em Configurações. O processo Python roda junto ao servidor Minecraft.</div>')+'</div>';
  }
  views.eventos=eventView;
  function emoteView(){
    return intro("ANIMAÇÕES / INTEGRAÇÃO OPCIONAL","Emotes e Transformações","Vincule uma função do datapack ou mod para cada ação. O Nexus solicitará essa função ao servidor quando o comando ocorrer.")+
    block("MAPEAMENTO DE EMOTES",
      '<form id="rpEmotesForm" class="grid two">'+
      [["integrar","INTEGRAR"],["desintegrar","DESINTEGRAR"],["remodelar","REMODELAR"],["reparar","REPARAR"],["ritual","RITUAL"],["emergencia","EMERGÊNCIA"]].map(([key,label])=>
      '<label class="field"><span class="field-label">'+label+'</span><input name="'+key+'" maxlength="130" value="'+E(S.emotes[key]||"")+'" placeholder="nexus:emotes/'+key+'"></label>'
      ).join("")+
      '<button class="btn gold" type="submit">SALVAR MAPEAMENTOS</button></form>'+
      '<p class="hint">O painel usa funções Minecraft no formato <code>namespace:caminho</code>. É necessário que o mod ou datapack realmente implemente cada função. O Armourer’s Workshop não terá animações acionadas automaticamente sem uma API/integração própria.</p>');
  }
  views.emotes=emoteView;
  // Append connection mode to the existing settings form, preserving its fields.
  const originalConfig=views.config;
  views.config=()=>{
    let html=originalConfig();
    const selector='<div class="field"><label for="connectionMode">MODO DE CONEXÃO DO MINECRAFT</label>'+
      '<select id="connectionMode" name="connectionMode"><option value="public" '+(S.config.connectionMode!=="agent"?"selected":"")+'>RCON DIRETO (PORTA PÚBLICA)</option>'+
      '<option value="agent" '+(S.config.connectionMode==="agent"?"selected":"")+'>AGENTE HTTPS LOCAL (RECOMENDADO)</option></select>'+
      '<small>No modo Agente HTTPS, host e senha RCON permanecem no computador do servidor. No painel, apenas o código da ponte é necessário.</small></div>';
    html=html.replace('<form id="configForm">','<form id="configForm">'+selector);
    return html+'<h2 class="section-title">Integração avançada</h2>'+
      block("AGENTE E EVENTOS",'<p class="hint">A ponte segura recebe comandos e eventos por HTTPS. Instale o agente Python na máquina do servidor Minecraft, configure NEXUS_RP_AGENT_KEY e sua senha RCON localmente.</p>'+
      '<div class="buttons">'+button("open-events","GERENCIAR EVENTOS","gold")+
      button("bridge-status","TESTAR AGENTE","ghost")+'</div>')+
      block("EMOTES E PROTOCOLOS",'<div class="buttons">'+button("open-emotes","EDITAR EMOTES","gold")+
      button("open-emergency","PROTOCOLO DE EMERGÊNCIA","ghost")+
      button("open-sequences","ABRIR SEQUENCIADOR","ghost")+'</div>');
  };
  // Register menu entries before the Configuration button.
  const configNav=document.querySelector('#nav [data-page="config"]');
  if(configNav){
    for(const [key,label,icon] of [
      ["sequencias","Sequenciador","◇"],["eventos","Eventos do Jogo","⌁"],
      ["emergencia","Emergência","⚠"],["emotes","Emotes","✦"]
    ]){
      const btn=document.createElement("button");
      btn.className="nav-button";btn.dataset.page=key;
      btn.innerHTML='<span>'+icon+'</span> '+label;configNav.before(btn);
    }
  }
  // Event handling does not replace the existing RP 1.1 actions or forms.
  const container=document.querySelector("#content");
  function switchNav(page){nav(page);}
  function adjustSegmentDamage(amount){
    if(!Array.isArray(S.segments)||!S.segments.length)return;
    const index=G.cursor%S.segments.length;G.cursor++;
    const part=S.segments[index];
    part.integrity=clamp(part.integrity-amount*G.damageFactor,0,100);
    if(part.integrity===0)part.integrated=false;
    S.integrity=clamp(Math.round(S.segments.reduce((a,x)=>a+x.integrity,0)/S.segments.length),0,100);
    S.armor=S.segments.some(p=>p.integrated)?"integrada":"desintegrada";
    rp.checkAlerts?.();
  }
  function gameEvent(item,simulate=false){
    const label=AGENT_TYPE[item.type]||item.type;
    const value=clamp(item.amount,0,40);
    if(item.type==="damage")adjustSegmentDamage(value);
    else if(item.type==="death"){S.segments.forEach(p=>{p.integrity=0;p.integrated=false;});S.integrity=0;S.armor="desintegrada";rp.checkAlerts?.();}
    else if(item.type==="heal"||item.type==="respawn"){S.energy=clamp(S.energy+G.healEnergy,0,100);rp.checkAlerts?.();}
    const name=simulate?"SIMULAÇÃO LOCAL":"MINECRAFT";
    const msg=label+": "+(value?value.toFixed(1)+" ponto(s) · ":"")+item.player;
    log(msg,name);
    G.feed.push({...item,simulated:simulate});G.feed=G.feed.slice(-80);save();
  }
  async function refreshEvents(force=false){
    if(pollBusy||!G.enabled&&!force)return;
    if(S.config.connectionMode!=="agent"){if(force)toast("Selecione Agente HTTPS nas Configurações.");return;}
    pollBusy=true;
    try{
      const result=await rp.requestEvents(G.lastId,S.config.player);
      lastStatus=result.bridge||{};
      if(Number(result.lastId)<G.lastId){G.lastId=0;G.bootstrapped=false;}
      G.error="";G.lastSync=new Date().toISOString();
      if(!G.bootstrapped){
        G.bootstrapped=true;G.lastId=result.lastId||0;
        if(force)toast("Monitor sincronizado. Os próximos eventos atualizarão o Nexus.");
      }else{
        for(const ev of result.events||[]){if(ev.id<=G.lastId)continue;gameEvent(ev);}
        G.lastId=Math.max(G.lastId,Number(result.lastId)||0);
      }
      save();
      if(force)rp.rerender();
    }catch(err){
      G.error=err.message||String(err);
      if(force)toast("Monitor não sincronizado: "+G.error);
    }finally{pollBusy=false;}
  }
  // Updates are explicitly enabled by the operator. Background polling stops on tab close.
  const interval=setInterval(()=>{if(G.enabled&&document.visibilityState!=="hidden")refreshEvents();},5000);
  function executeSequenceStep(step){
    const kind=step.action;
    if(["integrar","desintegrar","remodelar","reparar"].includes(kind)){
      if(EM.active&&["integrar","remodelar"].includes(kind))throw Error("Transformação bloqueada pelo protocolo de emergência.");
      if(kind==="remodelar"&&step.form){if(S.energy<rp.formCost(step.form))throw Error("Energia insuficiente para remodelar.");S.form=step.form;}
      return rp.armorAction(kind);
    }
    if(kind==="recuperar"){
      S.energy=clamp(S.energy+15,0,100);rp.checkAlerts?.();log("Sequência: núcleo recuperou 15 pontos de energia.","SEQUÊNCIA");save();rp.rerender();return;
    }
    if(kind==="emote"){triggerEmote("remodelar");return;}
  }
  async function runSequence(){
    const chosen=currentStep();
    if(!chosen||!chosen.steps?.length)return toast("Adicione etapas antes de executar.");
    const ticket=++seqToken;seqStatus="INICIADA";seqIndex=-1;rp.rerender();
    log("Sequência iniciada: "+chosen.name,"SEQUÊNCIA");
    try{
      for(let i=0;i<chosen.steps.length;i++){
        if(seqToken!==ticket)break;
        const step=chosen.steps[i];seqIndex=i;seqStatus="ETAPA "+(i+1)+" DE "+chosen.steps.length;rp.rerender();
        await new Promise(resolve=>setTimeout(resolve,clamp(step.delay,250,12000)));
        if(ticket!==seqToken)break;
        await executeSequenceStep(step);
      }
      if(ticket===seqToken){seqStatus="CONCLUÍDA";log("Sequência concluída: "+chosen.name,"SEQUÊNCIA");}
    }catch(err){
      seqStatus="ERRO: "+(err.message||String(err)).slice(0,100);
      toast(seqStatus);
    }
    rp.rerender();
  }
  container.addEventListener("submit",async e=>{
    if(!["rpFormCreator","rpNewSequence","rpNewStep","rpEmergencySettings","rpEventSettings","rpEmotesForm"].includes(e.target.id))return;
    e.preventDefault();e.stopImmediatePropagation();
    const d=new FormData(e.target),form=e.target.id;
    if(form==="rpFormCreator"){
      const name=K(d.get("name")).slice(0,28),kind=K(d.get("kind")).slice(0,28);
      if(name.length<2||!kind)return toast("Preencha o nome e o estilo da forma.");
      if(S.armorProfiles.some(p=>p.name.toLowerCase()===name.toLowerCase()))return toast("Já existe uma forma com esse nome.");
      if(S.armorProfiles.length>=20)return toast("Limite de 20 formas cadastradas.");
      S.armorProfiles.push({name,kind,spell:clamp(d.get("spell"),0,60),cost:clamp(d.get("cost"),0,50),color:K(d.get("color"))||"#d6aa6e",description:K(d.get("description")).slice(0,180),locked:false});
      log("Nova forma criada: "+name,"ARMADURA");
    }else if(form==="rpNewSequence"){
      const name=K(d.get("name")).slice(0,45);if(name.length<2)return toast("Nome muito curto.");
      if(S.sequences.length>=30)return toast("Limite de 30 sequências.");
      const id=uid();S.sequences.push({id,name,steps:[]});chosenSequence=id;log("Sequência criada: "+name,"ENGENHARIA");
    }else if(form==="rpNewStep"){
      const current=currentStep();if(!current)return toast("Crie uma sequência primeiro.");
      if(current.steps.length>=20)return toast("Limite de 20 etapas por sequência.");
      const action=K(d.get("action"));if(!ACTS.some(x=>x[0]===action))return toast("Ação desconhecida.");
      const selected=K(d.get("form"));current.steps.push({action,delay:clamp(d.get("delay"),250,12000),form:S.armorProfiles.some(p=>p.name===selected)?selected:S.form});
    }else if(form==="rpEmergencySettings"){
      EM.enabled=d.has("enabled");EM.threshold=clamp(d.get("threshold"),5,80);checkEmergency();log("Protocolo de emergência configurado.","SISTEMA");
    }else if(form==="rpEventSettings"){
      G.enabled=d.has("enabled");G.damageFactor=clamp(d.get("damageFactor"),1,10);G.healEnergy=clamp(d.get("healEnergy"),0,15);
      G.bootstrapped=false;G.lastId=0;log("Monitoramento de eventos "+(G.enabled?"ativado":"desativado")+".","TELEMETRIA");
      if(G.enabled)refreshEvents(true);
    }else if(form==="rpEmotesForm"){
      const saved={};for(const key of ["integrar","desintegrar","remodelar","reparar","ritual","emergencia"]){
        const fn=K(d.get(key));if(!validFn(fn))return toast("Função inválida: "+key);saved[key]=fn;
      }S.emotes=saved;log("Mapeamento de emotes atualizado.","EMOTES");
    }
    save();rp.rerender();toast("Configuração salva no Nexus RP.");
  },true);
  container.addEventListener("change",e=>{
    if(e.target.id==="rpSequenceList"){chosenSequence=e.target.value;seqStatus="";rp.rerender();}
  },true);
  container.addEventListener("click",async e=>{
    const target=e.target.closest("[data-evo]");if(!target)return;
    e.preventDefault();e.stopImmediatePropagation();
    const action=target.dataset.evo;
    if(action.startsWith("open-"))return switchNav({events:"eventos",emotes:"emotes",emergency:"emergencia",sequences:"sequencias"}[action.slice(5)]||action.slice(5));
    if(action==="form-use"){
      const name=target.dataset.name;if(!S.armorProfiles.some(p=>p.name===name))return;
      if(EM.active)return toast("Protocolo de emergência ativo: remodelagem bloqueada.");
      const select=document.querySelector("#armorForm");if(select)select.value=name;
      S.form=name;save();return rp.armorAction("remodelar");
    }
    if(action==="form-delete"){
      const name=target.dataset.name,p=S.armorProfiles.find(x=>x.name===name);
      if(!p||p.locked)return;if(!confirm("Excluir forma "+name+"?"))return;
      S.armorProfiles=S.armorProfiles.filter(x=>x.name!==name);
      if(S.form===name)S.form="Celestial";save();rp.rerender();return;
    }
    if(action==="seq-run")return runSequence();
    if(action==="seq-stop"){seqToken++;seqStatus="INTERROMPIDA";rp.rerender();return;}
    if(action==="seq-delete"){const current=currentStep();if(!current||!confirm("Excluir "+current.name+"?"))return;S.sequences=S.sequences.filter(s=>s.id!==current.id);chosenSequence=S.sequences[0]?.id||"";save();rp.rerender();return;}
    if(["seq-up","seq-down","seq-remove"].includes(action)){
      const current=currentStep();if(!current)return;const i=Number(target.dataset.index);
      if(!Number.isInteger(i)||i<0||i>=current.steps.length)return;
      if(action==="seq-remove")current.steps.splice(i,1);
      else {const j=i+(action==="seq-up"?-1:1);if(j<0||j>=current.steps.length)return;[current.steps[i],current.steps[j]]=[current.steps[j],current.steps[i]];}
      save();rp.rerender();return;
    }
    if(action==="emergency-activate"){EM.active=true;EM.lastAt=new Date().toISOString();log("Contenção acionada manualmente.","EMERGÊNCIA");triggerEmote("emergencia");save();rp.rerender();return;}
    if(action==="emergency-release"){
      if(S.integrity<=EM.threshold&&EM.enabled&&!confirm("Integridade abaixo do limiar. Liberar mesmo assim?"))return;
      EM.active=false;log("Contenção desativada manualmente.","EMERGÊNCIA");save();rp.rerender();return;
    }
    if(action==="emergency-repair")return rp.armorAction("reparar");
    if(action==="emergency-rest"){S.energy=clamp(S.energy+15,0,100);rp.checkAlerts?.();save();rp.rerender();return;}
    if(action==="events-refresh")return refreshEvents(true);
    if(action==="events-test"){
      const player=S.config.player||"Port";gameEvent({id:uid(),type:"damage",player,amount:3.5,health:13,at:new Date().toISOString(),note:"Teste manual, sem evento real"},true);rp.rerender();return;
    }
    if(action==="bridge-status"){
      try{lastStatus=await rp.requestBridgeStatus();toast(lastStatus.online?"Agente HTTPS conectado.":"Agente HTTPS sem contato recente.");}
      catch(err){toast("Falha na consulta do agente: "+err.message);}
      rp.rerender();
    }
  },true);
  return views;
};
})();