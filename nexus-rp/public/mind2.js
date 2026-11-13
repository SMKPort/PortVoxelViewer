"use strict";
(function(){
window.NexusRPMind2Upgrade=function(rp,views){
  const E=rp.escape;
  let state=null,loading=false,busy=false,dirty=false,error="";
  let config={social:{diaryEnabled:true,protectionEnabled:true,sharePanel:false},
    speaker:{enabled:false,voice:"auto"}};
  const current=()=>location.hash==="#mente"&&document.visibilityState!=="hidden";
  const KEY=()=>rp.getAccess?.();
  const connected=()=>state?.online===true&&state.state!==null;
  const share=()=>state?.state?.social?.sharePanel===true;
  function saneDate(time){
    const n=Number(time);
    return Number.isFinite(n)&&n>0?rp.date(new Date(n).toISOString()):"Data não disponível";
  }
  function controls(){
    const disabled=!connected()||busy?" disabled":"";
    const bool=(name,title,detail,checked)=>
      '<label class="nexus-alert-option"><input type="checkbox" name="'+name+'"'+
      (checked?" checked":"")+disabled+'><span><strong>'+E(title)+'</strong><small>'+
      E(detail)+'</small></span></label>';
    const opts=["auto","Charon","Orus","Kore","Puck","Algenib","Fenrir"];
    return '<section class="card"><div class="card-header"><h2>AJUSTES DA CONSCIÊNCIA</h2></div>'+
      '<form id="nexusMind2Form" class="nexus-alert-form">'+
      bool("diaryEnabled","Diário autônomo",
        "Registra acontecimentos e evolução do personagem a partir de eventos observados.",config.social.diaryEnabled)+
      bool("protectionEnabled","Proteção contextual",
        "Combina avisos de perigo com dano confirmado no Minecraft antes de comentar.",config.social.protectionEnabled)+
      bool("sharePanel","Mostrar diário e vínculos neste painel",
        "Permite que a Render receba cópias temporárias de lembranças curtas e perfis narrativos. Desligado por padrão.",config.social.sharePanel)+
      bool("speakerEnabled","Voz própria no seu computador",
        "Gera áudio pelo Gemini ao enviar falas RP. Somente você escuta; não transmite pelo Plasmo Voice.",config.speaker.enabled)+
      '<label class="field"><span class="field-label">VOZ DO NEXUS</span><select name="voice"'+disabled+'>'+
      opts.map(v=>'<option value="'+E(v)+'"'+(config.speaker.voice===v?" selected":"")+'>'+
        E(v==="auto"?"Evolutiva (muda com o estágio)":v)+'</option>').join("")+
      '</select><small>As vozes disponíveis são predefinidas. A opção Evolutiva troca o timbre de acordo com o estágio narrativo.</small></label>'+
      '<label class="nexus-alert-option"><input name="consentConfirmed" type="checkbox"'+disabled+'><span><strong>Confirmo a autorização para exibir memórias no painel</strong><small>Necessária quando o compartilhamento está ativado. Somente as falas dos participantes previamente autorizados entram na memória.</small></span></label>'+
      '<button type="submit" class="btn gold"'+disabled+'>SALVAR MIND 2.0 NO MOD</button>'+
      '</form><p class="hint">O diário permanece no computador mesmo com o site fechado. Desativar a publicação deixa o histórico privado novamente.</p></section>';
  }
  function facts(){
    const rows=state?.state?.social?.facts||[];
    if(!share())return '<div class="empty">Para consultar ou corrigir lembranças aqui, habilite o compartilhamento na configuração acima.</div>';
    if(!rows.length)return '<div class="empty">Nenhuma lembrança semântica registrada ainda.</div>';
    return rows.slice(-22).reverse().map(item=>
      '<div class="rp-evo-event"><div><strong>'+E(item.speaker)+' · '+E(item.kind)+'</strong>'+
      '<small>'+E(saneDate(item.at))+(item.corrected?" · Corrigida":"")+'</small></div>'+
      '<p>'+E(item.text)+'</p>'+
      '<div class="buttons"><button class="btn ghost" type="button" data-mind-action="correct" data-memory-id="'+Number(item.id)+'">CORRIGIR</button>'+
      '<button class="btn ghost" type="button" data-mind-action="delete" data-memory-id="'+Number(item.id)+'">ESQUECER</button></div></div>'
    ).join("");
  }
  function diary(){
    if(!share())return '<div class="empty">Diário privado no Minecraft. Você escolhe quando o painel pode visualizá-lo.</div>';
    const entries=state?.state?.social?.diary||[];
    if(!entries.length)return '<div class="empty">O Nexus ainda não escreveu uma página. As primeiras surgirão após acontecimentos relevantes.</div>';
    return entries.slice(-20).reverse().map(item=>
      '<div class="rp-evo-event"><small>'+E(saneDate(item.at))+' · '+E(item.kind)+'</small>'+
      '<p>'+E(item.text)+'</p></div>').join("");
  }
  function relations(){
    if(!share())return '<div class="empty">Vínculos privados, armazenados somente no cliente.</div>';
    const rows=state?.state?.social?.relations||[];
    if(!rows.length)return '<div class="empty">Nenhum vínculo narrativo formado até agora.</div>';
    return rows.map(item=>'<div class="rp-evo-event"><strong>'+E(item.player)+'</strong>'+
      '<div class="kv"><span>Interações</span><strong>'+Number(item.interactions)+'</strong></div>'+
      '<div class="kv"><span>Familiaridade (RP)</span><strong>'+Number(item.familiarity)+'/100</strong></div>'+
      '<div class="kv"><span>Afinidade narrativa</span><strong>'+Number(item.affinity)+'/100</strong></div>'+
      '<button type="button" class="btn ghost" data-mind-action="forget_player" data-player="'+E(item.player)+'">'+
      'APAGAR REGISTROS DESTE JOGADOR</button></div>').join("");
  }
  function summary(){
    if(error)return "Erro de conexão: "+error;
    if(!state?.online)return "Minecraft não conectado. Abra o jogo e confira o pareamento.";
    if(!state?.state)return "Aguardando Nexus Client 2.0.0 sincronizar o estado.";
    return "Nexus Mind 2.0 conectado · última sincronização "+rp.date(state.syncedAt);
  }
  views.mente=()=>'<div class="eyebrow">NEXUS ATERNA / AUTONOMIA NARRATIVA</div>'+
    '<h1 class="heading">Nexus Mind 2.0</h1>'+
    '<p class="subheading">Lembranças atribuídas, vínculos de roleplay, proteção contextual, diário e voz local. Os registros não provam que uma declaração de outro jogador seja verdadeira.</p>'+
    '<div class="notice" id="nexusMindStatus">'+E(summary())+'</div>'+
    '<div class="grid two">'+controls()+
    '<section class="card"><div class="card-header"><h2>RESUMO DA MEMÓRIA</h2></div>'+
    '<div class="kv"><span>Declarações importantes</span><strong id="nexusMindFacts">'+(state?.state?.social?.factCount||0)+'</strong></div>'+
    '<div class="kv"><span>Vínculos narrativos</span><strong id="nexusMindRelations">'+(state?.state?.social?.relationCount||0)+'</strong></div>'+
    '<div class="kv"><span>Páginas do diário</span><strong id="nexusMindDiaryCount">'+(state?.state?.social?.diaryCount||0)+'</strong></div>'+
    '<div class="kv"><span>Voz local</span><strong id="nexusMindVoice">'+(state?.state?.speaker?.enabled?"ATIVADA":"DESLIGADA")+'</strong></div>'+
    '<p class="hint">As transcrições completas continuam em arquivos locais e não são exibidas nem exportadas nesta página.</p>'+
    '<button type="button" class="btn ghost" data-mind-action="clear">APAGAR MEMÓRIA SOCIAL E TRANSCRIÇÕES</button>'+
    '<p class="hint">Confirme com atenção. Essa ação elimina os registros de audição do Nexus.</p></section></div>'+
    '<div class="grid two"><section class="card"><div class="card-header"><h2>DIÁRIO AUTÔNOMO</h2></div>'+
    '<div id="nexusMindDiary">'+diary()+'</div></section>'+
    '<section class="card"><div class="card-header"><h2>RELACIONAMENTOS DO RP</h2></div>'+
    '<div id="nexusMindRelationsList">'+relations()+'</div></section></div>'+
    '<section class="card"><div class="card-header"><h2>MEMÓRIA SEMÂNTICA</h2></div>'+
    '<p class="hint">Lembranças são trechos atribuídos aos participantes, não fatos verificados. Você pode corrigir ou apagar qualquer entrada.</p>'+
    '<div id="nexusMindFactsList">'+facts()+'</div></section>';
  function update(){
    const status=document.querySelector("#nexusMindStatus");
    if(status)status.textContent=summary();
    for(const [selector,value] of [
      ["#nexusMindFacts",state?.state?.social?.factCount||0],
      ["#nexusMindRelations",state?.state?.social?.relationCount||0],
      ["#nexusMindDiaryCount",state?.state?.social?.diaryCount||0],
      ["#nexusMindVoice",state?.state?.speaker?.enabled?"ATIVADA":"DESLIGADA"]]){
      const el=document.querySelector(selector);if(el)el.textContent=String(value);
    }
    for(const [selector,markup] of [
      ["#nexusMindDiary",diary()],["#nexusMindRelationsList",relations()],
      ["#nexusMindFactsList",facts()]]){
      const el=document.querySelector(selector);if(el&&el.innerHTML!==markup)el.innerHTML=markup;
    }
  }
  async function refresh(){
    if(loading||busy||!current())return;
    if(!KEY()){error="Informe o código HTTPS em Configurações.";update();return;}
    loading=true;
    try{
      const res=await fetch("/api/client/hearing",{
        headers:{"X-Nexus-Key":KEY()},cache:"no-store",signal:AbortSignal.timeout(9000)});
      const data=await res.json();
      if(!res.ok||data.ok===false)throw Error(data.error||"Falha ao consultar Nexus.");
      error="";state=data;
      if(!dirty&&data.state){
        const next={social:{
          diaryEnabled:data.state.social?.diaryEnabled??true,
          protectionEnabled:data.state.social?.protectionEnabled??true,
          sharePanel:data.state.social?.sharePanel??false
        },speaker:{enabled:data.state.speaker?.enabled??false,voice:data.state.speaker?.voice||"auto"}};
        if(JSON.stringify(next)!==JSON.stringify(config)){config=next;rp.rerender();}
      }
      update();
    }catch(e){error=String(e.message||e);update();}
    finally{loading=false;}
  }
  const root=document.querySelector("#content");
  root.addEventListener("input",e=>{if(e.target.closest?.("#nexusMind2Form"))dirty=true;});
  root.addEventListener("change",e=>{if(e.target.closest?.("#nexusMind2Form"))dirty=true;});
  root.addEventListener("submit",async e=>{
    if(e.target.id!=="nexusMind2Form")return;
    e.preventDefault();e.stopImmediatePropagation();
    if(!connected()||busy)return;
    const form=new FormData(e.target);
    const next={social:{
      diaryEnabled:form.has("diaryEnabled"),protectionEnabled:form.has("protectionEnabled"),
      sharePanel:form.has("sharePanel")
    },speaker:{enabled:form.has("speakerEnabled"),voice:String(form.get("voice")||"auto")},
      consentConfirmed:form.has("consentConfirmed")};
    if(next.social.sharePanel&&!next.consentConfirmed)
      return rp.toast("Confirme a autorização para mostrar memórias neste painel.");
    busy=true;rp.rerender();
    try{
      await rp.clientQueue("/mind/settings",next);
      config={social:next.social,speaker:next.speaker};dirty=false;
      rp.toast("Preferências do Nexus Mind 2.0 salvas no Minecraft.");
      state=null;
    }catch(err){rp.toast("Falha: "+String(err.message||err));}
    finally{busy=false;rp.rerender();refresh();}
  },true);
  root.addEventListener("click",async e=>{
    const button=e.target.closest?.("[data-mind-action]");
    if(!button||busy)return;
    const action=button.dataset.mindAction;
    if(!connected()||!share()){
      if(action!=="clear")rp.toast("Ative a visualização privada do diário antes de editar.");
      return;
    }
    const id=Number(button.dataset.memoryId);
    const player=String(button.dataset.player||"");
    let payload={action};
    if(action==="delete"||action==="correct"){
      if(!Number.isSafeInteger(id)||id<1)return;
      payload.id=id;
      if(action==="correct"){
        const source=(state.state.social.facts||[]).find(x=>Number(x.id)===id);
        const corrected=window.prompt("Corrigir a lembrança atribuída:",source?.text||"");
        if(corrected===null)return;
        payload.text=corrected.trim().slice(0,250);
        if(payload.text.length<3)return rp.toast("A correção precisa ter pelo menos três caracteres.");
      }else if(!window.confirm("Apagar permanentemente esta lembrança?"))return;
    }else if(action==="forget_player"){
      if(!/^[A-Za-z0-9_]{3,16}$/.test(player))return;
      if(!window.confirm("Apagar todas as memórias de "+player+"?"))return;
      payload.player=player;
    }else if(action==="clear"){
      if(!window.confirm("Apagar TODAS as transcrições e memórias sociais? Essa ação não pode ser desfeita."))
        return;
      payload.confirm=true;
    }else return;
    busy=true;
    try{
      await rp.clientQueue("/hearing/fact",payload);
      rp.toast("Alteração confirmada pelo Nexus Client.");
      state=null;
    }catch(err){rp.toast("Falha: "+String(err.message||err));}
    finally{busy=false;refresh();}
  });
  window.addEventListener("hashchange",()=>{if(current())refresh();});
  document.addEventListener("visibilitychange",()=>{if(current())refresh();});
  setInterval(()=>{if(current())refresh();},4000);
  if(current())setTimeout(refresh,0);
  return views;
};
})();
