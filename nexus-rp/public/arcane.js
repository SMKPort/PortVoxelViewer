"use strict";
(function(){
window.NexusRPArcaneUpgrade=function(rp,views){
  const E=rp.escape;
  let remote=null,error="",busy=false,dirty=false,loading=false;
  let values={enabled:false,reactions:false,scanNearby:false};
  const visible=()=>location.hash==="#arcano"&&document.visibilityState!=="hidden";
  function number(value,decimals=0){
    const n=Number(value);
    return value===null||value===undefined||!Number.isFinite(n)?
      "sem leitura":n.toLocaleString("pt-BR",{maximumFractionDigits:decimals,
        minimumFractionDigits:decimals});
  }
  const state=()=>remote?.state||null;
  function status(){
    if(error)return "Falha de comunicação: "+error;
    if(!remote)return "Aguardando sincronização com a Render...";
    if(!remote.online)return "Nexus Client offline. Entre em um mundo e verifique o pareamento.";
    if(!state())return "Aguardando o Nexus Client 2.1.0 enviar leituras.";
    if(!state().enabled)return "Sensor desativado. Ative o Núcleo Arcano para começar.";
    if(!state().present)return "Iron's Spells indisponível ou incompatível no cliente.";
    if(!state().available)return "Aguardando mana e estado de conjuração sincronizados pelo servidor.";
    return "LEITURA ARCANA ATIVA · dados locais do Iron's Spells";
  }
  function meter(title,amount,detail,variant="gold"){
    const known=typeof amount==="number"&&Number.isFinite(amount);
    const pct=known?Math.max(0,Math.min(100,amount)):0;
    return '<div class="nexus-arcane-meter"><div class="nexus-arcane-meter-label"><span>'+
      E(title)+'</span><strong>'+E(known?number(amount,1)+"%":"sem leitura")+'</strong></div>'+
      '<div class="nexus-arcane-track"><span class="nexus-arcane-fill '+E(variant)+
      '" style="width:'+pct+'%"></span></div>'+
      '<small>'+E(detail)+'</small></div>';
  }
  function readouts(){
    const s=state();
    const detected=s?.enabled&&s?.available;
    const mana=detected?s.manaPercent:null;
    const spell=detected&&s.casting&&s.spellId?
      E(s.spellName||s.spellId):"Nenhuma conjuração detectada";
    const school=detected&&s.casting?s.school:"";
    const progress=detected&&s.casting?s.castProgress*100:null;
    const holy=detected?s.holyPower:null;
    const magic=detected?s.spellPower:null;
    const arm=s?.armAvailable;
    return '<div class="nexus-arcane-reading">'+
      '<div class="nexus-arcane-mana">'+meter("Reserva de mana (atributo do jogo)",mana,
        detected&&typeof s.mana==="number"&&typeof s.maxMana==="number"?
          number(s.mana,0)+" / "+number(s.maxMana,0)+" de mana":"Leitura depende do Iron’s Spells e da sincronização do servidor.",
        "gold")+'</div>'+
      '<div class="nexus-arcane-spell"><span class="eyebrow">CONJURAÇÃO OBSERVADA</span>'+
      '<strong class="nexus-arcane-spell-name">'+spell+'</strong>'+
      '<small>'+(school?"Escola: "+E(school)+" · ":"")+(
        detected&&s.casting?"Nível "+number(s.spellLevel):"Aguardando nova conjuração")+'</small>'+
      meter("Progresso estimado da conjuração",progress,
        "O fim da canalização não garante que a magia foi executada com sucesso.","red")+
      '</div></div>'+
      '<div class="nexus-arcane-readout-grid">'+
      '<div class="nexus-arcane-stat"><small>POTÊNCIA MÁGICA</small><strong>'+
        (magic===null||magic===undefined?"sem leitura":"×"+number(magic,2))+
        '</strong><span>Atributo sincronizado do jogo</span></div>'+
      '<div class="nexus-arcane-stat"><small>PODER HOLY</small><strong>'+
        (holy===null||holy===undefined?"sem leitura":"×"+number(holy,2))+
        '</strong><span>Não altera os bônus do personagem</span></div>'+
      '<div class="nexus-arcane-stat"><small>CONJURAÇÕES REGISTRADAS</small><strong>'+
        number(s?.totalCasts||0)+'</strong><span>Contagem local do Nexus</span></div>'+
      '</div>'+
      '<div class="nexus-arcane-arm"><div class="card-header"><h3>PLACA ARCANA · DIAGNÓSTICO RP</h3></div>'+
      '<p class="hint">Estes indicadores vêm da integridade narrativa do Nexus. Não equivalem à mana nem à resistência real de equipamentos.</p>'+
      meter("Integridade das placas",arm?s.armIntegrity:null,
        "Estado narrativo das placas do braço direito","red")+
      meter("Energia do Nexus",arm?s.armEnergy:null,
        "Energia simulada do sistema de armadura","gold")+'</div>'+nearbyPlayers();
  }
  function nearbyPlayers(){
    const s=state(),list=s?.nearby||[];
    if(!s?.scanNearby)return '<div class="nexus-arcane-nearby"><small>Percepção de magia próxima: desligada.</small></div>';
    const rows=list.length?list.map(item=>
      '<div class="nexus-arcane-nearby-row"><strong>'+E(item.player)+'</strong>'+
      '<span>'+E(item.spellName||item.spellId)+' · '+number(item.distance,0)+
      ' blocos</span></div>').join(""):'<p class="hint">Nenhuma conjuração próxima detectada em até 24 blocos.</p>';
    return '<div class="nexus-arcane-nearby"><div class="card-header"><h3>PERCEPÇÃO MÁGICA PRÓXIMA</h3></div>'+
      '<p class="hint">Apenas jogadores visíveis e dados de conjuração recebidos pelo cliente. Não permite enxergar magias através de chunks descarregados.</p>'+
      rows+'</div>';
  }
  function eventHistory(){
    const events=state()?.events||[];
    if(!events.length)return '<div class="empty">Nenhuma atividade arcana registrada ainda. Conjure uma magia com o sensor ativo.</div>';
    return events.slice(-24).reverse().map(item=>{
      let date="Sem data";
      try{date=rp.date(new Date(Number(item.at)).toISOString());}catch(_){}
      return '<div class="rp-evo-event"><div class="nexus-arcane-event-line"><strong>'+
        E(item.message)+'</strong><small>'+E(date)+'</small></div>'+
        (item.school?'<small>Escola: '+E(item.school)+'</small>':"")+
        '</div>';
    }).join("");
  }
  function settings(){
    const disabled=!remote?.online||!state()||busy?" disabled":"";
    return '<form id="nexusArcaneForm" class="nexus-alert-form">'+
      '<label class="nexus-alert-option"><input type="checkbox" name="enabled"'+
      (values.enabled?" checked":"")+disabled+'><span><strong>Monitoramento arcano</strong>'+
      '<small>Lê somente o estado mágico sincronizado com este cliente. Opcional e desativado por padrão.</small></span></label>'+
      '<label class="nexus-alert-option"><input type="checkbox" name="reactions"'+
      (values.reactions?" checked":"")+disabled+'><span><strong>Respostas narrativas no chat</strong>'+
      '<small>Emite falas do Nexus quando há conjuração ou alterações importantes. Exige mind, chat e autonomy ativados.</small></span></label>'+
      '<label class="nexus-alert-option"><input type="checkbox" name="scanNearby"'+
      (values.scanNearby?" checked":"")+disabled+'><span><strong>Percepção mágica próxima (24 blocos)</strong>'+
      '<small>Observa estados de conjuração de jogadores visíveis pelo seu cliente. Não consulta dados administrativos do servidor.</small></span></label>'+
      '<button class="btn gold" type="submit"'+disabled+'>'+
      (busy?"APLICANDO NO MINECRAFT...":"SALVAR CONFIGURAÇÕES")+'</button></form>';
  }
  function mark(){
    const s=state();return s?.lastError||"";
  }
  views.arcano=()=>
    '<div class="eyebrow">NEXUS ATERNA / SENSORES DO CLIENTE / IRON’S SPELLS</div>'+
    '<h1 class="heading">Núcleo Arcano</h1>'+
    '<p class="subheading">Monitoramento opcional de magia real do modpack, associado ao diagnóstico narrativo do braço Nexus. Nenhuma magia ou atributo do servidor é alterado.</p>'+
    '<div class="notice" id="nexusArcaneStatus">'+E(status())+'</div>'+
    '<div class="grid two"><section class="card"><div class="card-header"><h2>ATIVIDADE MÁGICA</h2></div>'+
    '<div id="nexusArcaneData">'+readouts()+'</div></section>'+
    '<section class="card"><div class="card-header"><h2>COMPORTAMENTO DO NEXUS</h2></div>'+
    '<div id="nexusArcaneSettings">'+settings()+'</div>'+
    '<p class="hint">Para usar pelo Minecraft: <code>/nexusclient arcane on</code>, <code>arcane status</code>, <code>arcane history</code> e <code>arcane reactions on</code>.</p>'+
    '<p class="hint">O cliente registra início e encerramento da canalização. Uma magia instantânea pode não aparecer se não durar o suficiente entre leituras.</p>'+
    '<div class="notice" id="nexusArcaneError">'+E(mark())+'</div></section></div>'+
    '<section class="card"><div class="card-header"><h2>HISTÓRICO ARCANO</h2></div>'+
    '<div id="nexusArcaneEvents">'+eventHistory()+'</div>'+
    '<p class="hint">Os últimos 80 eventos são guardados no computador em <code>config/nexus-arcane.json</code>; apenas os 40 mais recentes são enviados temporariamente ao painel.</p></section>';
  function update(){
    const el=document.querySelector("#nexusArcaneStatus");
    if(el)el.textContent=status();
    for(const [selector,html] of [
      ["#nexusArcaneData",readouts()],
      ["#nexusArcaneEvents",eventHistory()]]){
      const target=document.querySelector(selector);
      if(target&&target.innerHTML!==html)target.innerHTML=html;
    }
    const error=document.querySelector("#nexusArcaneError");
    if(error)error.textContent=mark();
  }
  async function refresh(){
    if(!visible()||loading||busy)return;
    const key=rp.getAccess?.();
    if(!key){error="Informe o código de acesso em Configurações.";update();return;}
    loading=true;
    try{
      const reply=await fetch("/api/client/arcane",{
        headers:{"X-Nexus-Key":key},cache:"no-store",signal:AbortSignal.timeout(9000)});
      const json=await reply.json();
      if(!reply.ok||json.ok===false)throw Error(json.error||"Falha na API.");
      remote=json;error="";
      if(!dirty&&json.state){
        const next={enabled:json.state.enabled,reactions:json.state.reactions,scanNearby:json.state.scanNearby===true};
        if(JSON.stringify(next)!==JSON.stringify(values)){
          values=next;rp.rerender();
        }
      }
      update();
    }catch(ex){error=String(ex.message||ex);update();}
    finally{loading=false;}
  }
  const root=document.querySelector("#content");
  root.addEventListener("change",e=>{if(e.target.closest?.("#nexusArcaneForm"))dirty=true;});
  root.addEventListener("submit",async e=>{
    if(e.target.id!=="nexusArcaneForm")return;
    e.preventDefault();e.stopImmediatePropagation();
    if(busy||!remote?.online)return;
    const form=new FormData(e.target);
    const settings={enabled:form.has("enabled"),reactions:form.has("reactions"),scanNearby:form.has("scanNearby")};
    if(settings.reactions&&!settings.enabled)return rp.toast("Ative primeiro o Núcleo Arcano.");
    busy=true;rp.rerender();
    try{
      await rp.clientQueue("/arcane/settings",settings);
      values=settings;dirty=false;rp.toast("Núcleo Arcano atualizado no Minecraft.");
      remote=null;
    }catch(ex){rp.toast("Erro: "+String(ex.message||ex));}
    finally{busy=false;rp.rerender();refresh();}
  },true);
  window.addEventListener("hashchange",()=>{if(visible())refresh();});
  document.addEventListener("visibilitychange",()=>{if(visible())refresh();});
  setInterval(()=>{if(visible())refresh();},4000);
  if(visible())setTimeout(refresh,0);
  return views;
};
})();
