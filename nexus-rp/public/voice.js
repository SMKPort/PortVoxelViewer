"use strict";
(function(){
window.NexusRPVoiceUpgrade=function(rp,views){
  const E=rp.escape,toast=rp.toast;
  const SpeechRecognition=window.SpeechRecognition||window.webkitSpeechRecognition;
  const WAKE=/^(?:(?:ei|ol[aá])\\s+)?(?:nexus|n[eé]xo|nexos)(?:\\s+a[eé]terna|\\s+aterna)?(?=$|[\\s,.!?:;])/i;
  let active=false,recognition=null,busy=false,waitingUntil=0,lastTranscript="",lastReply="",lastError="",lastInterim="",restartTimer=0,lastHandled="",lastHandledAt=0,everStarted=false;
  const btn=(name,label,style)=>'<button type="button" class="btn '+style+'" data-voice="'+name+'">'+label+'</button>';
  const esc=E;
  function view(){
    const online=!!SpeechRecognition;
    return '<div class="eyebrow">NEXUS ATERNA / RECONHECIMENTO DE VOZ</div>'+
      '<h1 class="heading">Ouvir e responder</h1>'+
      '<p class="subheading">Fale normalmente e chame o Nexus pelo nome. A resposta será enviada apenas como mensagem dourada no chat local do Minecraft.</p>'+
      '<div class="grid two">'+
      '<section class="card voice-card"><div class="card-header"><h2>MICROFONE</h2></div>'+
      '<div class="voice-indicator '+(active?"listening":"")+'" id="voiceIndicator"><span class="voice-dot"></span><div>'+
      '<strong id="voiceState">'+(active?"ESCUTANDO":"MICROFONE DESLIGADO")+'</strong><small id="voiceDetail">'+(busy?"Processando resposta...":waitingUntil>Date.now()?"Diga sua pergunta, Port.":"Diga 'Nexus, como estamos?'")+'</small></div></div>'+
      '<div class="buttons">'+btn("start","🎙 ATIVAR MICROFONE","gold")+btn("stop","■ DESLIGAR","ghost")+'</div>'+
      '<div class="divider"></div><p class="hint"><strong>Palavra de ativação:</strong> Nexus. Após dizer apenas o nome, você tem alguns segundos para fazer a pergunta.</p>'+
      (online?'':'<div class="notice"><strong>Reconhecimento indisponível neste navegador.</strong> Abra o painel no Chrome ou Edge atualizado.</div>')+
      '<div class="notice">Você precisa autorizar o microfone e manter este painel aberto. O reconhecimento pode ser interrompido pelo navegador em segundo plano. Em alguns navegadores, o áudio é processado por um serviço online do próprio navegador.</div></section>'+
      '<section class="card voice-card"><div class="card-header"><h2>COMUNICAÇÃO</h2></div>'+
      '<div class="voice-transcript"><small>VOCÊ DISSE</small><p id="voiceTranscript">'+esc(lastTranscript||"Aguardando comando de voz...")+'</p></div>'+
      '<div class="voice-transcript"><small>RESPOSTA DO NEXUS</small><p id="voiceReply">'+esc(lastReply||"Aguardando conexão...")+'</p></div>'+
      '<p class="hint" id="voiceError">'+esc(lastError)+'</p>'+
      '<form id="voiceTestForm"><label class="field"><span class="field-label">TESTE SEM MICROFONE</span><input name="transcript" maxlength="260" placeholder="Nexus, como estamos?" required></label>'+
      '<div class="buttons"><button class="btn alt" type="submit">TESTAR RESPOSTA NO MINECRAFT</button></div></form>'+
      '<p class="hint">Requer Nexus Client pareado e /nexusclient chat on. Os textos são enviados via /l &amp;6&amp;l[Nexus Aterna]. O microfone não grava arquivos neste painel.</p></section></div>';
  }
  views.voz=view;
  const nav=document.querySelector("#nav"),config=nav?.querySelector('[data-page="config"]');
  if(config&&!nav.querySelector('[data-page="voz"]')){
    const item=document.createElement("button");
    item.className="nav-button";item.dataset.page="voz";
    item.innerHTML='<span>◉</span> Voz do Nexus';
    config.before(item);
  }
  function update(){
    const indicator=document.querySelector("#voiceIndicator");
    if(indicator)indicator.classList.toggle("listening",active);
    const txt=document.querySelector("#voiceState");
    if(txt)txt.textContent=active?"ESCUTANDO":"MICROFONE DESLIGADO";
    const detail=document.querySelector("#voiceDetail");
    if(detail)detail.textContent=busy?"Processando resposta...":waitingUntil>Date.now()?"Nome reconhecido. Diga sua pergunta.":active?"Aguardando 'Nexus'...":"Diga 'Nexus, como estamos?'";
    const heard=document.querySelector("#voiceTranscript");if(heard)heard.textContent=lastInterim||lastTranscript||"Aguardando comando de voz...";
    const response=document.querySelector("#voiceReply");if(response)response.textContent=lastReply||"Aguardando conexão...";
    const error=document.querySelector("#voiceError");if(error)error.textContent=lastError;
    const start=document.querySelector('[data-voice="start"]');if(start)start.disabled=active||!SpeechRecognition;
    const stop=document.querySelector('[data-voice="stop"]');if(stop)stop.disabled=!active;
  }
  async function relay(transcript){
    if(busy)return;
    if(rp.state.config.connectionMode!=="client"){
      lastError="Selecione NEXUS CLIENT em Configurações para enviar respostas por voz.";update();return;
    }
    busy=true;lastTranscript=transcript;lastInterim="";lastError="";update();
    try {
      if(!rp.clientQueue)throw Error("Módulo Nexus Client não carregado.");
      const result=await rp.clientQueue("/voice",{transcript});
      lastReply=result.reply||"Resposta enviada ao Minecraft.";
      rp.log("Resposta por voz encaminhada ao chat do Minecraft.","VOZ");
    } catch(error) {
      lastError=String(error?.message||error);
      toast("Voz Nexus: "+lastError);
    } finally {busy=false;update();}
  }
  function consume(raw){
    const text=String(raw||"").trim();
    if(!text)return;
    const now=Date.now();
    if(text===lastHandled&&now-lastHandledAt<7000)return;
    lastHandled=text;lastHandledAt=now;
    const match=text.match(WAKE);
    if(match){
      const remaining=text.slice(match[0].length).replace(/^[\\s,.!?:;]+/,"").trim();
      if(!remaining){waitingUntil=now+10000;lastTranscript=text;lastReply="Nome reconhecido. Aguardando pergunta.";update();return;}
      waitingUntil=0;
      relay(text);return;
    }
    if(waitingUntil>now){
      waitingUntil=0;
      relay("Nexus, "+text);
    }
  }
  function stop(){
    active=false;waitingUntil=0;lastInterim="";
    clearTimeout(restartTimer);
    try{recognition?.stop();}catch(error){}
    update();
  }
  function start(){
    if(!SpeechRecognition){lastError="Seu navegador não suporta SpeechRecognition.";update();return;}
    if(active)return;
    if(!rp.getAccess()){lastError="Informe a chave HTTPS em Configurações antes de ativar.";update();return;}
    active=true;lastError="";waitingUntil=0;everStarted=true;
    const rec=recognition=new SpeechRecognition();
    rec.lang="pt-BR";rec.continuous=true;rec.interimResults=true;rec.maxAlternatives=1;
    rec.onresult=(event)=>{
      for(let i=event.resultIndex;i<event.results.length;i++){
        const result=event.results[i],phrase=result?.[0]?.transcript||"";
        if(result.isFinal){lastInterim="";consume(phrase);}
        else lastInterim=phrase;
      }
      update();
    };
    rec.onerror=(event)=>{
      const code=String(event.error||"desconhecido");
      if(["not-allowed","service-not-allowed","audio-capture","network","language-not-supported"].includes(code)){
        active=false;clearTimeout(restartTimer);
      }
      lastError="Reconhecimento: "+code+". Verifique a permissão do microfone e a conexão.";
      update();
    };
    rec.onend=()=>{
      if(!active){update();return;}
      clearTimeout(restartTimer);
      restartTimer=setTimeout(()=>{if(active)try{rec.start();}catch(error){active=false;lastError="O navegador interrompeu o microfone. Ative novamente.";update();}},700);
    };
    try{rec.start();}catch(error){active=false;lastError="Não consegui ativar o microfone: "+error.message;}
    update();
  }
  document.querySelector("#content")?.addEventListener("click",event=>{
    const b=event.target.closest("[data-voice]");if(!b)return;
    if(b.dataset.voice==="start")start();
    if(b.dataset.voice==="stop")stop();
  });
  document.querySelector("#content")?.addEventListener("submit",event=>{
    if(event.target.id!=="voiceTestForm")return;
    event.preventDefault();
    const data=new FormData(event.target);
    consume(String(data.get("transcript")||""));
  });
  window.addEventListener("beforeunload",stop);
  return views;
};
})();
