"use strict";
(function(){
window.NexusRPAlertsUpgrade=function(rp,views){
  const E=rp.escape;
  const TYPES=[
    ["heavy","Dano elevado","Alerta ao receber um impacto muito forte de uma vez."],
    ["streak","Ataques consecutivos","Alerta após várias leituras de dano em sequência."],
    ["structure","Integridade crítica","Alerta quando as placas caem a 25% de integridade."],
    ["risk","Risco combinado","Vida baixa ao mesmo tempo que a integridade está comprometida."],
    ["recovery","Regeneração das placas","Comenta quando a cura restaura a estrutura do Nexus."],
    ["critical","Vida crítica","Avisos da consciência evolutiva quando a vida cai a 30% ou menos."]
  ];
  const DEFAULT={heavy:true,streak:true,structure:true,risk:true,recovery:true,critical:true,cooldownSeconds:45};
  const LABELS=Object.fromEntries(TYPES.map(([kind,label])=>[kind,label]));
  LABELS.mind="Consciência evolutiva";
  let remote=null,error="",busy=false,pending=false,dirty=false,initialized=false;
  let values={...DEFAULT};
  const onPage=()=>window.location.hash==="#alertas";
  const active=()=>onPage()&&document.visibilityState!=="hidden";
  function statusText(){
    if(error)return "ERRO DE SINCRONIZAÇÃO: "+error;
    if(!remote)return "CONECTANDO AO NEXUS CLIENT...";
    if(!remote.online)return "MINECRAFT DESCONECTADO. As opções permanecem salvas no mod.";
    if(!remote.settings)return "MINECRAFT CONECTADO. Aguardando preferências do Nexus Client 1.6.0...";
    return "MINECRAFT CONECTADO · preferências e histórico sincronizados";
  }
  function historyHTML(){
    if(!remote?.history?.length)
      return '<div class="empty">Nenhuma fala automática sincronizada. O histórico será mostrado aqui depois que o Nexus falar no Minecraft.</div>';
    return remote.history.slice(-25).reverse().map(row=>
      '<div class="rp-evo-event"><span>'+E(LABELS[row.kind]||row.kind)+' · '+E(row.text)+'</span>'+
      '<small>'+E(rp.date(row.at))+'</small></div>'
    ).join("");
  }
  function updateLive(){
    const status=document.querySelector("#rpAlertsStatus");
    if(status)status.textContent=statusText();
    const sync=document.querySelector("#rpAlertsSync");
    if(sync)sync.textContent=remote?.syncedAt?
      "Última sincronização: "+rp.date(remote.syncedAt)+" · histórico armazenado no Minecraft":
      "Aguardando o mod transmitir as preferências e o histórico...";
    const feed=document.querySelector("#rpAlertsHistory");
    if(feed){const html=historyHTML();if(feed.innerHTML!==html)feed.innerHTML=html;}
    const count=document.querySelector("#rpAlertsCount");
    if(count)count.textContent=String(remote?.history?.length||0);
  }
  views.alertas=function(){
    const settingsReady=remote?.online===true&&remote.settings!==null&&remote.settings!==undefined;
    const disabled=(!settingsReady||pending)?" disabled":"";
    const toggles=TYPES.map(([key,title,subtitle])=>
      '<label class="nexus-alert-option"><input name="'+key+'" type="checkbox"'+
      (values[key]?" checked":"")+disabled+'><span><strong>'+E(title)+'</strong><small>'+
      E(subtitle)+'</small></span></label>').join("");
    return '<div class="eyebrow">NEXUS / CONSCIÊNCIA EVOLUTIVA / MONITORAMENTO</div>'+
      '<h1 class="heading">Alertas do Nexus</h1>'+
      '<p class="subheading">Escolha quando o Nexus deve falar por conta própria durante o jogo. O Minecraft continua monitorando mesmo com esta página fechada.</p>'+
      '<div class="grid two"><section class="card"><div class="card-header"><h2>COMPORTAMENTO AUTÔNOMO</h2></div>'+
      '<div class="rp-evo-emergency '+(remote?.online?"online":"")+'"><strong id="rpAlertsStatus">'+E(statusText())+'</strong></div>'+
      '<p class="hint">Para enviar falas, ative no Minecraft: <code>/nexusclient telemetry on</code>, <code>/nexusclient mind on</code>, <code>/nexusclient chat on</code> e <code>/nexusclient autonomy on</code>.</p>'+
      '<form id="rpAlertSettings" class="nexus-alert-form">'+toggles+
      '<label class="field"><span class="field-label">INTERVALO MÍNIMO ENTRE ALERTAS DE COMBATE (SEGUNDOS)</span>'+
      '<input type="number" name="cooldownSeconds" min="15" max="180" step="5" value="'+E(values.cooldownSeconds)+'"'+disabled+'>'+
      '<small>Entre 15 e 180 segundos. Também respeita o limite de mensagens do chat do Minecraft.</small></label>'+
      '<button type="submit" class="btn gold"'+disabled+'>'+ (pending?"SALVANDO NO MOD...":"SALVAR ALERTAS NO MOD")+'</button>'+
      '</form>'+
      '<p class="hint">As preferências são gravadas em config/nexus-alerts.json no computador. É necessário manter o Minecraft conectado para aplicar alterações, mas o site não precisa permanecer aberto.</p></section>'+
      '<section class="card"><div class="card-header"><h2>HISTÓRICO DE FALAS AUTOMÁTICAS</h2></div>'+
      '<div class="kv"><span>Últimas falas disponíveis</span><span class="value" id="rpAlertsCount">'+(remote?.history?.length||0)+'</span></div>'+
      '<p class="hint" id="rpAlertsSync">'+(remote?.syncedAt?"Última sincronização: "+E(rp.date(remote.syncedAt)):"Aguardando o mod transmitir o histórico...")+'</p>'+
      '<div id="rpAlertsHistory" aria-live="polite">'+historyHTML()+'</div>'+
      '<p class="hint">Mostramos somente falas espontâneas realmente enviadas. As últimas 80 ficam salvas no cliente e as 30 mais recentes são sincronizadas com o painel. Não registra o conteúdo de conversas privadas.</p></section></div>';
  };
  async function refresh(){
    if(busy||!active())return;
    const key=rp.getAccess?.();
    if(!key){error="Informe o código HTTPS em Configurações.";updateLive();return;}
    busy=true;
    try{
      const response=await fetch("/api/client/alerts",{
        headers:{"X-Nexus-Key":key,"Accept":"application/json"},
        cache:"no-store",signal:AbortSignal.timeout(10000)});
      const result=await response.json();
      if(!response.ok||result.ok===false)throw Error(result.error||"Falha na API do Nexus.");
      error="";
      const changed=!!result.settings&&JSON.stringify(result.settings)!==JSON.stringify(values);
      remote=result;
      if(!dirty&&result.settings&&changed){values={...DEFAULT,...result.settings};if(onPage())rp.rerender();}
      else if(!initialized&&onPage()){rp.rerender();}
      initialized=true;
      updateLive();
    }catch(ex){error=String(ex.message||ex);updateLive();}
    finally{busy=false;}
  }
  const root=document.querySelector("#content");
  root.addEventListener("input",e=>{
    if(e.target.closest?.("#rpAlertSettings"))dirty=true;
  });
  root.addEventListener("change",e=>{
    if(e.target.closest?.("#rpAlertSettings"))dirty=true;
  });
  root.addEventListener("submit",async e=>{
    if(e.target.id!=="rpAlertSettings")return;
    e.preventDefault();e.stopImmediatePropagation();
    if(pending)return;
    if(!remote?.online||!remote?.settings){
      rp.toast("Conecte o Nexus Client 1.6.0 e aguarde a sincronização.");return;
    }
    const d=new FormData(e.target);
    const proposed=Object.fromEntries(TYPES.map(([key])=>[key,d.has(key)]));
    proposed.cooldownSeconds=Number(d.get("cooldownSeconds"));
    if(!Number.isInteger(proposed.cooldownSeconds)||proposed.cooldownSeconds<15||proposed.cooldownSeconds>180)
      return rp.toast("Defina um intervalo entre 15 e 180 segundos.");
    pending=true;const button=e.target.querySelector('button[type="submit"]');
    if(button){button.disabled=true;button.textContent="SALVANDO NO MOD...";}
    try{
      if(!rp.clientQueue)throw Error("O Nexus Client não está disponível neste painel.");
      await rp.clientQueue("/alerts/settings",proposed);
      values=proposed;dirty=false;
      rp.toast("Preferências confirmadas e gravadas no Minecraft.");
      await refresh();
    }catch(ex){rp.toast("Falha ao atualizar o mod: "+String(ex.message||ex));}
    finally{pending=false;if(onPage())rp.rerender();}
  },true);
  if(typeof window.addEventListener==="function")
    window.addEventListener("hashchange",()=>{if(onPage())refresh();});
  if(typeof document.addEventListener==="function")
    document.addEventListener("visibilitychange",()=>{if(active())refresh();});
  setInterval(()=>{if(active())refresh();},3000);
  if(onPage())setTimeout(()=>refresh(),0);
  return views;
};
})();
