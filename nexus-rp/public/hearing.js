"use strict";
(function(){
window.NexusRPHearingUpgrade=function(rp,views){
  const E=rp.escape;
  let state=null,loading=false,saving=false,error="",dirty=false;
  let values={listening:false,memory:false,answer:false,allowedPlayers:[]};
  const current=()=>location.hash==="#audicao"&&document.visibilityState!=="hidden";
  async function fetchState(){
    if(loading||saving||!current())return;
    const key=rp.getAccess?.();
    if(!key){error="Configure o código HTTPS em Configurações.";update();return;}
    loading=true;
    try{
      const res=await fetch("/api/client/hearing",{headers:{"X-Nexus-Key":key},cache:"no-store",signal:AbortSignal.timeout(9000)});
      const data=await res.json();
      if(!res.ok||data.ok===false)throw Error(data.error||"Falha na sincronização.");
      error="";state=data;
      if(!dirty&&data.state){
        const fresh={listening:data.state.listening,memory:data.state.memory,answer:data.state.answer,
          allowedPlayers:data.state.allowedPlayers||[]};
        if(JSON.stringify(fresh)!==JSON.stringify(values)){values=fresh;rp.rerender();}
      }
      update();
    }catch(ex){error=String(ex.message||ex);update();}
    finally{loading=false;}
  }
  function status(){
    if(error)return "Erro: "+error;
    if(!state)return "Sincronizando com a Render...";
    if(!state.online)return "Minecraft desconectado. Abra o jogo para alterar a audição.";
    if(!state.state)return "Aguardando Nexus Client 1.7.0 transmitir seu estado.";
    if(!state.state.ready)return "Plasmo Voice ainda não foi detectado pelo Nexus Client.";
    return state.state.listening?"Audição ativa somente para jogadores autorizados.":"Audição desativada.";
  }
  function update(){
    const target=document.querySelector("#nexusHearingStatus");
    if(target)target.textContent=status();
    const count=document.querySelector("#nexusHearingCount");
    if(count)count.textContent=String(state?.state?.memoryCount||0);
  }
  views.audicao=()=>{
    const ready=state?.online===true&&state.state?.ready===true;
    const disabled=(!ready||saving)?" disabled":"";
    return '<div class="eyebrow">NEXUS ATERNA / PLASMO VOICE / PRIVACIDADE</div>'+
      '<h1 class="heading">Audição do Nexus</h1>'+
      '<p class="subheading">Vozes próximas no Plasmo Voice podem virar transcrições e memórias textuais. A audição é desligada por padrão e deve ser autorizada novamente a cada partida.</p>'+
      '<div class="grid two"><section class="card"><div class="card-header"><h2>ESCUTA AUTORIZADA</h2></div>'+
      '<div class="notice" id="nexusHearingStatus">'+E(status())+'</div>'+
      '<form id="nexusHearingForm" class="nexus-alert-form">'+
      '<label class="nexus-alert-option"><input type="checkbox" name="listening"'+(values.listening?" checked":"")+disabled+'><span><strong>Ativar audição</strong><small>Analisa apenas vozes dos jogadores autorizados, recebidas pelo seu Plasmo Voice.</small></span></label>'+
      '<label class="nexus-alert-option"><input type="checkbox" name="memory"'+(values.memory?" checked":"")+disabled+'><span><strong>Guardar memórias textuais</strong><small>Salva até 200 transcrições no seu computador. Nenhum áudio é gravado no disco.</small></span></label>'+
      '<label class="nexus-alert-option"><input type="checkbox" name="answer"'+(values.answer?" checked":"")+disabled+'><span><strong>Responder quando chamado</strong><small>Se ouvir “Nexus, ...”, gera resposta pelo chat /l (requer chat, mente e autonomia ativados).</small></span></label>'+
      '<label class="field"><span class="field-label">JOGADORES QUE CONSENTIRAM</span>'+
      '<textarea name="allowedPlayers" rows="3" maxlength="340" placeholder="Nick1, Nick2"'+disabled+'>'+
      E(values.allowedPlayers.join(", "))+'</textarea><small>Separe nicks por vírgula. Até 20 jogadores. Nunca inclua alguém sem obter autorização explícita.</small></label>'+
      '<label class="nexus-alert-option"><input type="checkbox" name="consentConfirmed"'+disabled+'><span><strong>Confirmo que os jogadores listados foram informados e consentiram</strong><small>O áudio será processado pelo Gemini na Render. Memórias serão armazenadas localmente se você ativar a opção acima.</small></span></label>'+
      '<button type="submit" class="btn gold"'+disabled+'>'+(saving?"SALVANDO NO CLIENTE...":"SALVAR CONFIGURAÇÕES DE AUDIÇÃO")+'</button></form></section>'+
      '<section class="card"><div class="card-header"><h2>MEMÓRIA E CONTROLE</h2></div>'+
      '<div class="kv"><span>Memórias textuais no Minecraft</span><span class="value" id="nexusHearingCount">'+(state?.state?.memoryCount||0)+'</span></div>'+
      '<p class="hint">As memórias ficam no arquivo <code>.minecraft/config/nexus-hearing-memories.json</code>. O painel só recebe a contagem, não os textos nem o áudio.</p>'+
      '<div class="notice">Para consultar memórias no jogo: <code>/nexusclient hearing memories</code>. Para apagar todas: <code>/nexusclient hearing forget confirm</code>. Para interromper imediatamente: <code>/nexusclient hearing off</code>.</div>'+
      '<p class="hint">O Nexus só escuta quem seu Plasmo Voice permite ouvir. O reconhecimento depende da conexão com a Render e da chave Gemini. Atrasos e erros de transcrição podem ocorrer.</p>'+
      '<p class="hint">Os outros jogadores precisam saber que suas falas podem ser transcritas. Remova qualquer nick imediatamente se a pessoa retirar o consentimento.</p></section></div>';
  };
  const root=document.querySelector("#content");
  root.addEventListener("input",e=>{if(e.target.closest?.("#nexusHearingForm"))dirty=true;});
  root.addEventListener("change",e=>{if(e.target.closest?.("#nexusHearingForm"))dirty=true;});
  root.addEventListener("submit",async e=>{
    if(e.target.id!=="nexusHearingForm")return;
    e.preventDefault();e.stopImmediatePropagation();
    if(saving||!state?.online||!state?.state?.ready)return rp.toast("Conecte o Nexus Client 1.7.0 e o Plasmo Voice.");
    const form=new FormData(e.target);
    const list=String(form.get("allowedPlayers")||"").split(/[,;\n]+/).map(s=>s.trim()).filter(Boolean);
    if(list.some(s=>!/^[A-Za-z0-9_]{3,16}$/.test(s))||list.length>20)
      return rp.toast("Use até 20 nicks válidos do Minecraft.");
    const body={listening:form.has("listening"),memory:form.has("memory"),
      answer:form.has("answer"),allowedPlayers:[...new Set(list)],
      consentConfirmed:form.has("consentConfirmed")};
    if(body.listening&&!body.consentConfirmed)
      return rp.toast("Confirme o consentimento dos jogadores para ativar a audição.");
    if(body.listening&&!body.allowedPlayers.length)
      return rp.toast("Inclua pelo menos um jogador que consentiu.");
    saving=true;rp.rerender();
    try{
      await rp.clientQueue("/hearing/settings",body);
      values={listening:body.listening,memory:body.memory,answer:body.answer,allowedPlayers:body.allowedPlayers};
      dirty=false;rp.toast("Audição confirmada e salva no Minecraft.");
      state=null;await fetchState();
    }catch(ex){rp.toast("Falha ao salvar: "+String(ex.message||ex));}
    finally{saving=false;rp.rerender();}
  },true);
  window.addEventListener("hashchange",()=>{if(current())fetchState();});
  document.addEventListener("visibilitychange",()=>{if(current())fetchState();});
  setInterval(()=>{if(current())fetchState();},3000);
  if(current())setTimeout(fetchState,0);
  return views;
};
})();
