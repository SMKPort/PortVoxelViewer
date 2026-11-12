"use strict";
(function(){
window.NexusRPVoiceUpgrade=function(rp,views){
  const E=rp.escape,toast=rp.toast;
  const Recognition=window.SpeechRecognition||window.webkitSpeechRecognition;
  const WAKE=/^(?:(?:ei|ol[aá])\s+)?(?:nexus|n[eé]xo|nexos)(?:\s+a[eé]?terna)?(?=$|[\s,.!?:;])/i;
  const HISTORY_KEY="nexus-rp-voice-history-v3";
  const OPTIONS_KEY="nexus-rp-voice-preferences-v3";
  let settings={continuous:true,remember:false},history=[];
  try{
    const saved=JSON.parse(localStorage.getItem(OPTIONS_KEY)||"null");
    if(saved&&typeof saved==="object"){
      settings.continuous=saved.continuous!==false;
      settings.remember=saved.remember===true;
    }
    if(settings.remember){
      const stored=JSON.parse(localStorage.getItem(HISTORY_KEY)||"[]");
      if(Array.isArray(stored))history=stored.filter(x=>x&&(x.role==="user"||x.role==="model")&&typeof x.text==="string").slice(-24);
    }
  }catch(error){history=[];}
  let active=false,recognition=null,busy=false,waitingUntil=0,conversationUntil=0;
  let lastTranscript="",lastReply="",lastError="",lastInterim="",restartTimer=0;
  let lastHandled="",lastHandledAt=0,aiInfo=null,pending=[];
  const button=(id,label,style)=>'<button type="button" class="btn '+style+'" data-voice="'+id+'">'+label+'</button>';
  function store(){
    try{
      localStorage.setItem(OPTIONS_KEY,JSON.stringify(settings));
      if(settings.remember)localStorage.setItem(HISTORY_KEY,JSON.stringify(history.slice(-24)));
      else localStorage.removeItem(HISTORY_KEY);
    }catch(error){lastError="Não foi possível salvar a conversa neste navegador.";}
  }
  function view(){
    const capable=!!Recognition;
    const status=aiInfo===null?"VERIFICAÇÃO PENDENTE":aiInfo.configured?"IA DISPONÍVEL":"CONFIGURE CHAVE DE IA";
    const recent=history.slice(-10).map(h=>'<div class="voice-history-line '+(h.role==="model"?"nexus":"port")+'"><strong>'+ (h.role==="model"?"NEXUS":"PORT")+'</strong><span>'+E(h.text)+'</span></div>').join("");
    return '<div class="eyebrow">NEXUS ATERNA / CONVERSAÇÃO LIVRE</div>'+
      '<h1 class="heading">Fale com o Nexus</h1>'+
      '<p class="subheading">A IA conversa sobre qualquer assunto e lembra o contexto das falas anteriores. Você fala pelo microfone, e o Nexus responde exclusivamente pelo chat do Minecraft.</p>'+
      '<div class="grid two">'+
      '<section class="card voice-card"><div class="card-header"><h2>MICROFONE E CONTEXTO</h2></div>'+
        '<div class="voice-indicator '+(active?"listening":"")+'" id="voiceIndicator"><span class="voice-dot"></span><div>'+
          '<strong id="voiceState">'+(active?"ESCUTANDO":"MICROFONE DESLIGADO")+'</strong>'+
          '<small id="voiceDetail">'+E(detail())+'</small></div></div>'+
        '<div class="buttons">'+button("start","🎙 ATIVAR MICROFONE","gold")+button("stop","■ DESLIGAR","ghost")+'</div>'+
        '<div class="divider"></div>'+
        '<label class="field inline"><input type="checkbox" data-voice-option="continuous" '+(settings.continuous?"checked":"")+'><span>Conversa contínua (não repetir “Nexus” por até 2 minutos entre falas)</span></label>'+
        '<label class="field inline"><input type="checkbox" data-voice-option="remember" '+(settings.remember?"checked":"")+'><span>Lembrar conversa neste navegador após fechar a página</span></label>'+
        '<div class="voice-ai-state" id="voiceAIStatus"><strong>'+status+'</strong><small>'+(aiInfo?.model?E(aiInfo.model):"Gemini via Render")+'</small></div>'+
        '<div class="buttons">'+button("check","VERIFICAR IA","ghost")+button("clear","LIMPAR CONVERSA","ghost")+'</div>'+
        (!capable?'<div class="notice">Reconhecimento de voz não disponível neste navegador. Use Chrome ou Edge com microfone habilitado, ou o campo de texto ao lado.</div>':'')+
        '<p class="hint">Ative o microfone e diga “Nexus, preciso te contar algo”. Depois converse naturalmente. O navegador precisa permanecer ativo e pode enviar o áudio ao serviço de reconhecimento próprio. Sua transcrição é encaminhada ao Gemini para responder.</p></section>'+
      '<section class="card voice-card"><div class="card-header"><h2>DIÁLOGO</h2></div>'+
        '<div class="voice-transcript"><small>VOCÊ</small><p id="voiceTranscript">'+E(lastInterim||lastTranscript||"Aguardando sua voz...")+'</p></div>'+
        '<div class="voice-transcript"><small>NEXUS ATERNA</small><p id="voiceReply">'+E(lastReply||"Aguardando pergunta...")+'</p></div>'+
        '<p class="hint voice-error" id="voiceError">'+E(lastError)+'</p>'+
        '<form id="voiceTestForm"><label class="field"><span class="field-label">CONVERSAR POR TEXTO (SEM MICROFONE)</span>'+
        '<textarea name="transcript" maxlength="500" rows="2" placeholder="Pode falar de qualquer assunto ou continuar a conversa..." required></textarea></label>'+
        '<div class="buttons"><button class="btn alt" type="submit">ENVIAR PARA O NEXUS</button></div></form>'+
        '<div class="divider"></div><div class="voice-history" id="voiceHistory">'+(recent||'<p class="hint">Sua conversa aparecerá aqui.</p>')+'</div>'+
        '<p class="hint">As respostas usam /l &amp;6&amp;l[Nexus Aterna], pelo seu jogador. Requer o mod pareado e /nexusclient chat on. A IA não executa comandos do Minecraft por voz.</p></section></div>';
  }
  function detail(){
    if(busy)return "Aguardando a resposta da IA...";
    if(waitingUntil>Date.now())return "Nome detectado. Diga sua pergunta.";
    if(conversationUntil>Date.now())return "Conversa ativa. Pode continuar falando.";
    return active?"Aguardando você chamar “Nexus”...":"Diga “Nexus, como estamos?”";
  }
  views.voz=view;
  const nav=document.querySelector("#nav");
  const config=nav?.querySelector('[data-page="config"]');
  if(config&&!nav.querySelector('[data-page="voz"]')){
    const item=document.createElement("button");item.className="nav-button";
    item.dataset.page="voz";item.innerHTML='<span>◉</span> Voz do Nexus';config.before(item);
  }
  function update(){
    const indicator=document.querySelector("#voiceIndicator");
    if(indicator)indicator.classList.toggle("listening",active);
    const state=document.querySelector("#voiceState");
    if(state)state.textContent=active?"ESCUTANDO":"MICROFONE DESLIGADO";
    const text=document.querySelector("#voiceDetail");
    if(text)text.textContent=detail();
    const said=document.querySelector("#voiceTranscript");
    if(said)said.textContent=lastInterim||lastTranscript||"Aguardando sua voz...";
    const reply=document.querySelector("#voiceReply");
    if(reply)reply.textContent=lastReply||"Aguardando pergunta...";
    const error=document.querySelector("#voiceError");
    if(error)error.textContent=lastError;
    const ai=document.querySelector("#voiceAIStatus");
    if(ai)ai.querySelector("strong").textContent=aiInfo===null?"VERIFICAÇÃO PENDENTE":aiInfo.configured?"IA DISPONÍVEL":"CONFIGURE CHAVE DE IA";
    const h=document.querySelector("#voiceHistory");
    if(h){
      h.replaceChildren();
      if(!history.length){const p=document.createElement("p");p.className="hint";p.textContent="Sua conversa aparecerá aqui.";h.appendChild(p);}
      for(const turn of history.slice(-10)){
        const line=document.createElement("div");line.className="voice-history-line "+(turn.role==="model"?"nexus":"port");
        const name=document.createElement("strong");name.textContent=turn.role==="model"?"NEXUS":"PORT";
        const message=document.createElement("span");message.textContent=turn.text;
        line.append(name,message);h.appendChild(line);
      }
      h.scrollTop=h.scrollHeight;
    }
    const start=document.querySelector('[data-voice="start"]');if(start)start.disabled=active||!Recognition;
    const stop=document.querySelector('[data-voice="stop"]');if(stop)stop.disabled=!active;
  }
  async function checkAI(){
    const access=rp.getAccess?.()||"";
    if(!access){lastError="Configure NEXUS_RP_ACCESS_KEY em Configurações antes de continuar.";update();return;}
    try{
      const resp=await fetch("/api/client/ai/status",{headers:{"X-Nexus-Key":access,"Accept":"application/json"},signal:AbortSignal.timeout(12000)});
      const data=await resp.json();
      if(!resp.ok)throw Error(data.error||"Erro ao verificar IA.");
      aiInfo=data;
      if(!data.configured)lastError="Falta NEXUS_GEMINI_API_KEY nas variáveis privadas da Render. Sem ela, as conversas livres ficam indisponíveis.";
      else lastError="";
    }catch(error){lastError=String(error.message||error);}
    update();
  }
  async function sendTurn(turn){
    if(rp.state.config.connectionMode!=="client")throw Error("Ative o modo NEXUS CLIENT nas Configurações.");
    if(!rp.clientQueue)throw Error("Módulo Nexus Client indisponível.");
    const result=await rp.clientQueue("/voice",{
      transcript:turn.raw,followUp:turn.followUp,
      history:history.slice(-16)
    });
    lastReply=result.reply||"Resposta encaminhada ao Minecraft.";
    history.push({role:"user",text:turn.question.slice(0,500)},{role:"model",text:lastReply.slice(0,450)});
    history=history.slice(-24);
    store();
    rp.log("Conversa livre do Nexus encaminhada ao Minecraft.","VOZ");
    return result;
  }
  async function drain(){
    if(busy)return;
    busy=true;update();
    while(pending.length){
      const turn=pending.shift();
      lastTranscript=turn.question;lastInterim="";lastError="";update();
      try{await sendTurn(turn);}
      catch(error){lastError=String(error?.message||error);toast("Nexus: "+lastError);}
      if(settings.continuous)conversationUntil=Date.now()+120000;
      update();
    }
    busy=false;update();
  }
  function add(raw,question,followUp){
    if(pending.length>=3){toast("Conversa ocupada. Aguarde uma resposta.");return;}
    pending.push({raw,question,followUp});
    conversationUntil=settings.continuous?Date.now()+120000:0;
    drain();
  }
  function consume(raw,typed=false){
    const phrase=String(raw||"").trim();
    if(!phrase)return;
    const now=Date.now();
    if(!typed&&phrase===lastHandled&&now-lastHandledAt<6500)return;
    lastHandled=phrase;lastHandledAt=now;
    if(typed){
      const cleaned=phrase.replace(WAKE,"").replace(/^[\s,.!?:;]+/,"").trim()||phrase;
      add(phrase,cleaned,true);
      return;
    }
    const match=phrase.match(WAKE);
    if(match){
      const question=phrase.slice(match[0].length).replace(/^[\s,.!?:;]+/,"").trim();
      if(!question){waitingUntil=now+10000;conversationUntil=0;lastTranscript=phrase;lastReply="Estou ouvindo. Continue.";update();return;}
      waitingUntil=0;add(phrase,question,false);return;
    }
    if(waitingUntil>now||settings.continuous&&conversationUntil>now){
      waitingUntil=0;add(phrase,phrase,true);
    }
  }
  function stop(){
    active=false;waitingUntil=0;conversationUntil=0;lastInterim="";pending=[];
    clearTimeout(restartTimer);
    try{recognition?.stop();}catch(error){}
    update();
  }
  function start(){
    if(!Recognition){lastError="Reconhecimento de fala indisponível neste navegador.";update();return;}
    if(active)return;
    if(!rp.getAccess?.()){lastError="Informe o código HTTPS em Configurações.";update();return;}
    active=true;lastError="";waitingUntil=0;
    const rec=recognition=new Recognition();
    rec.lang="pt-BR";rec.continuous=true;rec.interimResults=true;rec.maxAlternatives=1;
    rec.onresult=event=>{
      for(let i=event.resultIndex;i<event.results.length;i++){
        const r=event.results[i],heard=r?.[0]?.transcript||"";
        if(r.isFinal){lastInterim="";consume(heard);}
        else lastInterim=heard;
      }
      update();
    };
    rec.onerror=event=>{
      const error=String(event.error||"desconhecido");
      if(["not-allowed","service-not-allowed","audio-capture","network","language-not-supported"].includes(error)){active=false;clearTimeout(restartTimer);}
      if(error!=="no-speech")lastError="Erro no microfone ("+error+"). Verifique as permissões e sua conexão.";
      update();
    };
    rec.onend=()=>{
      if(!active){update();return;}
      clearTimeout(restartTimer);
      restartTimer=setTimeout(()=>{
        if(active)try{rec.start();}
        catch(error){active=false;lastError="O navegador interrompeu o microfone. Ative novamente.";update();}
      },700);
    };
    try{rec.start();}
    catch(error){active=false;lastError="Não foi possível ligar o microfone: "+error.message;}
    checkAI();update();
  }
  document.querySelector("#content")?.addEventListener("click",event=>{
    const el=event.target.closest("[data-voice]");
    if(!el)return;
    const action=el.dataset.voice;
    if(action==="start")start();
    if(action==="stop")stop();
    if(action==="check")checkAI();
    if(action==="clear"){
      history=[];pending=[];lastReply="";lastTranscript="";
      conversationUntil=0;waitingUntil=0;store();update();toast("Histórico de diálogo apagado deste navegador.");
    }
  });
  document.querySelector("#content")?.addEventListener("change",event=>{
    const key=event.target?.dataset?.voiceOption;
    if(!["continuous","remember"].includes(key))return;
    settings[key]=event.target.checked===true;
    if(!settings.continuous)conversationUntil=0;
    store();update();
  });
  document.querySelector("#content")?.addEventListener("submit",event=>{
    if(event.target.id!=="voiceTestForm")return;
    event.preventDefault();
    const input=event.target.querySelector('[name="transcript"]');
    const text=input?.value||"";
    if(input)input.value="";
    consume(text,true);
  });
  window.addEventListener("beforeunload",stop);
  return views;
};
})();