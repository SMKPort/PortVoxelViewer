"use strict";
(function () {
  window.NexusRPUpgrade = function (rp, views) {
    const S = rp.state;
    const E = rp.escape;
    const clamp = (v,a,b) => Math.max(a,Math.min(b,Number(v)||0));
    const id = () => "rp"+Math.random().toString(36).slice(2,9);
    const DEFAULT_SEGMENTS = [
      {key:"ombreira",name:"Ombreira",integrity:100,integrated:true},
      {key:"braco",name:"Braço superior",integrity:100,integrated:true},
      {key:"cotovelo",name:"Articulação do cotovelo",integrity:100,integrated:true},
      {key:"antebraco",name:"Placas do antebraço",integrity:100,integrated:true},
      {key:"manopla",name:"Manopla arcana",integrity:100,integrated:true}
    ];
    const CHAPTERS = [
      {code:"TS-001",title:"Eco de comando",hidden:"Uma instrução antiga foi localizada na memória do braço. Não corresponde a nenhuma ordem do Port.",trace:"ROOT TS / SINAL OBSOLETO",options:["Conter o fragmento","Analisar a assinatura"]},
      {code:"TS-014",title:"Memória fragmentada",hidden:"O Nexus encontrou resíduos do vínculo entre a prótese antiga e a rede dos TS.",trace:"OWNER PORT? // CONTROLE DISPUTADO",options:["Arquivar em isolamento","Decodificar os resíduos"]},
      {code:"TS-026",title:"Retorno do sinal",hidden:"A armadura reconheceu um protocolo que não deveria mais existir. O núcleo aguarda uma decisão.",trace:"VOCÊ NÃO PODE ENTRAR AQUI. NÃO PODE SAIR.",options:["Recusar sincronização","Simular resposta sem conectar"]},
      {code:"TS-041",title:"O último acesso",hidden:"Um arquivo inerte documenta a tentativa final dos TS de recuperar o Nexus.",trace:"PROTOCOLO DE AUTONOMIA / PORT",options:["Selar o arquivo","Preservar como evidência"]},
      {code:"TS-099",title:"Livre do controle",hidden:"O fragmento final não contém novas instruções. ATERNA marca o vínculo como encerrado.",trace:"ACESSO NEGADO AOS TS",options:["Confirmar isolamento","Guardar o registro na oficina"]}
    ];
    const STEP_NAMES = ["Preparação","Canalização","Estabilização","Conclusão"];
    if (!Array.isArray(S.segments) || !S.segments.length) {
      S.segments=DEFAULT_SEGMENTS.map(x=>({...x,integrity:clamp(S.integrity,0,100),integrated:S.armor==="integrada"}));
    }
    S.segments=DEFAULT_SEGMENTS.map((def,i)=>{
      const old=S.segments.find(x=>x?.key===def.key)||S.segments[i]||{};
      return {...def,integrity:clamp(old.integrity??def.integrity,0,100),integrated:Boolean(old.integrated??def.integrated)};
    });
    if (!S.tsEvents || typeof S.tsEvents!=="object") S.tsEvents={enabled:false,chapter:0,pending:false,actions:0,threat:15,journal:[]};
    const TS=S.tsEvents;
    TS.enabled=Boolean(TS.enabled); TS.chapter=clamp(TS.chapter,0,CHAPTERS.length);
    TS.actions=clamp(TS.actions,0,1e8);TS.threat=clamp(TS.threat??15,0,100);TS.pending=Boolean(TS.pending);
    if (!Array.isArray(TS.journal)) TS.journal=[];
    if (!Object.prototype.hasOwnProperty.call(S,"stagedRitual")) S.stagedRitual=null;
    if (!S.statusFlags || typeof S.statusFlags!=="object") S.statusFlags={};
    const phaseDesc=[
      "Reconhecendo runas e ancorando o círculo no ambiente.",
      "O núcleo canaliza energia para os símbolos da matriz ritualística.",
      "Os conduítes se estabilizam e as inscrições se alinham.",
      "O Nexus conclui a sequência e registra o resultado."
    ];
    const card=(title,inner)=>'<section class="card"><div class="card-header"><h2>'+E(title)+'</h2></div>'+inner+'</section>';
    const button=(act,text,css="alt",props="")=>'<button class="btn '+css+'" data-rp="'+E(act)+'" '+props+'>'+text+'</button>';
    const bar=(n,red)=>'<div class="meter '+(red?"red":"")+'"><span style="width:'+clamp(n,0,100)+'%"></span></div>';
    const status=()=>S.integrity<=20?"CRÍTICA":S.integrity<=45?"DANIFICADA":S.integrity<=75?"OPERACIONAL":"ESTÁVEL";
    const log=(text,kind="NEXUS")=>rp.log(text,kind);
    const persist=()=>rp.save();
    const show=()=>rp.rerender();
    const connected=()=>rp.isConnected();
    function sendStage(text){
      if(!connected() || !S.config.autoAnnounce) return;
      rp.sendToChat(text.slice(0,270),"RITUAL").catch(err=>rp.toast("Mensagem da etapa não enviada: "+err.message));
    }
    function alertState(){
      const tier=S.integrity<=20?"critico":S.integrity<=40?"baixo":"normal";
      const energy=S.energy<=15?"critica":S.energy<=30?"baixa":"normal";
      if(S.statusFlags.integrity!==tier){
        if(tier==="critico")log("ALERTA: integridade estrutural crítica. Reparos recomendados.","DIAGNÓSTICO");
        if(tier==="baixo")log("A integridade do Nexus caiu abaixo de 40%.","DIAGNÓSTICO");
        S.statusFlags.integrity=tier;
      }
      if(S.statusFlags.energy!==energy){
        if(energy==="critica")log("ALERTA: reserva arcana crítica. Considere interromper conjurações.","ENERGIA");
        if(energy==="baixa")log("Baixa reserva de energia do núcleo.","ENERGIA");
        S.statusFlags.energy=energy;
      }
      persist();
    }
    function recomputeIntegrity(){
      S.integrity=clamp(Math.round(S.segments.reduce((sum,x)=>sum+x.integrity,0)/S.segments.length),0,100);
      alertState();
    }
    function noteAction(label){
      TS.actions++;
      if(TS.enabled && TS.chapter<CHAPTERS.length && TS.actions%4===0 && !TS.pending){
        TS.pending=true;
        log("Arquivo TS pendente de inspeção. Abra o Protocolo TS quando desejar.","TS / FICÇÃO");
        rp.toast("Novo fragmento TS disponível no painel.");
      }
      alertState();persist();
    }
    // Exposed callbacks let the original RP implementation keep acting as the source
    // of truth for armoring, ritual energy costs, Minecraft RCON and logs.
    rp.afterArmor=(kind)=>{
      if(kind==="integrar"||kind==="desintegrar"){
        const yes=kind==="integrar";
        S.segments.forEach(p=>{p.integrated=yes && p.integrity>0;});
      }
      noteAction("armadura "+kind);
    };
    rp.afterRitual=()=>noteAction("ritual");
    rp.adjustIntegrity=value=>{
      S.segments.forEach(p=>{p.integrity=clamp(value,0,100);});
      alertState();
    };

    function segmentCards(){
      return '<div class="rp-segment-grid">'+S.segments.map((part,i)=>'<article class="rp-segment">'+
      '<div class="rp-segment-title"><strong>0'+(i+1)+' · '+E(part.name)+'</strong><span class="rp-chip '+(part.integrated?"on":"")+'">'+(part.integrated?"INTEGRADO":"DISPERSO")+'</span></div>'+
      '<div class="rp-segment-meter"><span>Integridade</span><strong>'+part.integrity+'%</strong></div>'+bar(part.integrity,part.integrity<=35)+
      '<div class="buttons">'+button("segment-toggle",part.integrated?"DESACOPLAR":"ACOPLAR","alt",'data-key="'+E(part.key)+'"')+
      button("segment-damage","− DANIFICAR","ghost",'data-key="'+E(part.key)+'"')+
      button("segment-fix","+ REPARAR","gold",'data-key="'+E(part.key)+'"')+'</div></article>').join("")+'</div>';
    }
    const oldArmor=views.armadura;
    views.armadura=function(){
      return oldArmor()+'<h2 class="section-title">Controle individual das placas</h2>'+
      card("SEGMENTOS DO BRAÇO",' <p class="hint">Cada peça tem sua própria integridade e estado de acoplamento. Os comandos são narrativos. Caso o servidor esteja conectado e os anúncios estejam ligados, cada alteração é relatada no chat.</p>'+
      segmentCards()+'<div class="rp-status-line"><span>Integridade média</span><strong>'+S.integrity+'%</strong></div>');
    };
    const oldDash=views.painel;
    views.painel=function(){
      return oldDash()+'<h2 class="section-title">Operação em tempo real</h2>'+
      '<div class="grid two">'+card("RP AO VIVO",'<p class="hint">Acesse o comunicador rápido, acione as placas e acompanhe energia e integridade sem menus complexos.</p>'+
      '<div class="rp-quick-metrics"><span>ENERGIA <b>'+S.energy+'%</b></span><span>INTEGRIDADE <b>'+S.integrity+'%</b></span></div>'+
      '<div class="buttons">'+button("live-open","ABRIR MODO AO VIVO","gold")+'</div>')+
      card("PROTOCOLO TS",'<p class="hint">Eventos secretos do universo do Port. Eles ficam desativados por padrão e nunca interferem no servidor sem sua ação.</p>'+
      '<div class="rp-status-line"><span>Monitor narrativo</span><strong>'+ (TS.enabled?"LIGADO":"MANUAL")+'</strong></div>'+
      '<div class="buttons">'+button("ts-open",TS.pending?"INSPECIONAR FRAGMENTO":"ABRIR ARQUIVOS TS","alt")+'</div>')+'</div>';
    };
    const oldRituals=views.rituais;
    views.rituais=function(){
      const intro=oldRituals();
      const ritual=S.stagedRitual;
      if(!ritual){
        return intro+'<h2 class="section-title">Execução por etapas</h2>'+
          card("SEQUÊNCIA DE CONJURAÇÃO",'<p class="hint">Ao clicar em Executar Ritual, o Nexus iniciará as quatro etapas: preparação, canalização, estabilização e conclusão. O gasto de energia é aplicado somente quando você confirmar a última fase.</p>'+
          '<div class="rp-phase-row">'+STEP_NAMES.map((s,i)=>'<span class="rp-phase">'+(i+1)+' · '+s+'</span>').join("")+'</div>');
      }
      const step=clamp(ritual.stage,0,3);
      return intro+'<h2 class="section-title">Ritual em andamento</h2>'+
        card("SEQUÊNCIA: "+ritual.name,
          '<div class="rp-phase-row">'+STEP_NAMES.map((s,i)=>'<span class="rp-phase '+(i<step?"done":i===step?"active":"")+'">'+(i+1)+' · '+s+'</span>').join("")+'</div>'+
          '<div class="rp-ritual-current"><div class="eyebrow">FASE '+(step+1)+' / 4</div><h3>'+STEP_NAMES[step]+'</h3><p>'+phaseDesc[step]+'</p><p class="hint">'+E(ritual.description)+'</p></div>'+
          '<div class="rp-status-line"><span>Energia reservada para conclusão</span><strong>'+ritual.cost+' / '+S.energy+'</strong></div>'+
          '<div class="buttons">'+button("ritual-next",step===3?"✧ CONCLUIR RITUAL":"AVANÇAR ETAPA","gold")+
          button("ritual-cancel","CANCELAR SEM GASTAR ENERGIA","ghost")+'</div>'
        );
    };
    function liveView(){
      const recent=S.chat.slice(-5).reverse().map(m=>'<div class="rp-live-entry '+E(m.status)+'"><small>'+rp.date(m.at)+' · '+E(m.status==="sent"?"ENVIADO":"LOCAL")+'</small>'+E(m.text)+'</div>').join("");
      return '<div class="eyebrow">CONSOLE COMPACTO / MINECRAFT</div><h1 class="heading">RP ao Vivo</h1>'+
      '<p class="subheading">Painel para manter aberto no celular durante as sessões. Sem automação de texto: você decide exatamente o que o Nexus comunica.</p>'+
      '<div class="rp-live-board"><div class="rp-live-stats">'+
      '<div><small>INTEGRIDADE</small><strong>'+S.integrity+'%</strong>'+bar(S.integrity,true)+'</div>'+
      '<div><small>ENERGIA</small><strong>'+S.energy+'%</strong>'+bar(S.energy)+'</div>'+
      '<div><small>ARMADURA</small><strong>'+E(S.armor==="integrada"?"ATIVA":"DISPERSA")+'</strong><span class="muted">'+E(S.form)+'</span></div></div>'+
      '<form id="rpLiveMessage"><label class="field"><span class="field-label">TRANSMITIR AO MINECRAFT</span><textarea name="message" maxlength="280" required rows="2" placeholder="Digite a mensagem do Nexus..."></textarea></label>'+
      '<div class="buttons"><button class="btn gold" type="submit">↗ ENVIAR NO CHAT</button>'+button("live-note","REGISTRAR LOCAL","ghost")+'</div></form>'+
      '<div class="rp-live-actions">'+button("live-integrar","✦ INTEGRAR","gold")+button("live-desintegrar","◈ DESINTEGRAR","alt")+
      button("live-remodelar","⚙ REMODELAR","alt")+button("live-repair","✧ REPARAR","ghost")+
      button("live-rest","⌁ RECUPERAR ENERGIA","ghost")+button("live-ritual","✧ RITUAIS","gold")+'</div>'+
      '<div class="rp-status-line"><span>Conexão</span><strong>'+(connected()?"RCON TESTADO":"MODO LOCAL")+'</strong></div></div>'+
      '<h2 class="section-title">Últimas transmissões</h2>'+card("COMUNICADOR",recent||'<div class="empty">Nenhuma mensagem registrada.</div>');
    }
    views.ao_vivo=liveView;
    function tsView(){
      const current=CHAPTERS[TS.chapter];
      const events=TS.journal.slice(-10).reverse().map(j=>'<div class="rp-ts-log"><small>'+E(j.code)+' · '+rp.date(j.at)+'</small><strong>'+E(j.title)+'</strong><p>'+E(j.choice)+'</p></div>').join("");
      return '<div class="eyebrow">ARQUIVO CLASSIFICADO / TS</div><h1 class="heading">Protocolo TS</h1>'+
      '<p class="subheading">Fragmentos de lore encontrados na antiga prótese. Eventos controlados por você, sem simulação de invasão real.</p>'+
      '<div class="grid two">'+
      card("CENTRAL DE ISOLAMENTO",
      '<div class="rp-ts-header"><span class="light"></span> SISTEMA NARRATIVO · '+(TS.enabled?"AUTOAVISO ATIVO":"MODO MANUAL")+'</div>'+
      '<div class="rp-status-line"><span>Fragmentos decifrados</span><strong>'+TS.chapter+' / '+CHAPTERS.length+'</strong></div>'+
      '<div class="rp-status-line"><span>Pressão fictícia dos TS</span><strong>'+TS.threat+'%</strong></div>'+bar(TS.threat,true)+
      '<label class="field inline"><input type="checkbox" data-rp-change="ts-auto" '+(TS.enabled?"checked":"")+'><span>Oferecer novo fragmento a cada quatro ações do Nexus</span></label>'+
      '<p class="hint">A opção apenas sinaliza um evento. Você decide quando abrir e nunca acontece automaticamente no chat.</p>'+
      (TS.chapter>=CHAPTERS.length?'<div class="notice">ARQUIVOS CONCLUÍDOS. O vínculo narrativo foi encerrado.</div>':
      !TS.pending?'<div class="rp-ts-hidden"><strong>ARQUIVO NÃO REVELADO</strong><span>Próximo fragmento aguardando autorização.</span></div>'+
      '<div class="buttons">'+button("ts-reveal","REVELAR PRÓXIMO FRAGMENTO","gold")+'</div>':
      '<div class="rp-ts-hidden revealed"><small>'+E(current.code)+'</small><h3>'+E(current.title)+'</h3><p>'+E(current.hidden)+'</p><code>'+E(current.trace)+'</code></div>'+
      '<div class="buttons">'+current.options.map((opt,i)=>button("ts-choice",E(opt),i===0?"gold":"alt",'data-choice="'+i+'"')).join("")+'</div>')+
      '<div class="divider"></div>'+button("ts-reset","REINICIAR CAMPANHA TS","ghost")
      )+
      card("DIÁRIO DOS FRAGMENTOS",events||'<div class="empty">Nenhum fragmento analisado ainda.</div>')+'</div>';
    }
    views.ts=tsView;

    const nav=document.querySelector("#nav");
    const config=nav?.querySelector('[data-page="config"]');
    if(config){
      for (const [key,label,icon] of [["ao_vivo","RP ao Vivo","◉"],["ts","Protocolo TS","⊗"]]){
        const el=document.createElement("button");el.className="nav-button";el.dataset.page=key;el.innerHTML="<span>"+icon+"</span> "+label;config.before(el);
      }
    }

    async function sendText(text,kind){
      try{
        await rp.sendToChat(text.slice(0,280),kind||"RP");
        rp.toast("Mensagem entregue ao Minecraft.");
      }catch(err){
        if(!Array.isArray(S.chat))S.chat=[];
        S.chat.push({id:id(),at:new Date().toISOString(),text:text.slice(0,280),status:"failed"});
        persist();rp.toast("Não foi possível enviar: "+err.message);
      }finally{show();}
    }
    async function segmentAction(action,key){
      const part=S.segments.find(x=>x.key===key);if(!part)return;
      let msg="",cost=0;
      if(action==="segment-toggle"){
        const next=!part.integrated;
        cost=next?5:0;
        if(next&&part.integrity===0)return rp.toast("A peça está destruída e precisa de reparo.");
        if(S.energy<cost)return rp.toast("Energia narrativa insuficiente.");
        part.integrated=next;
        msg=part.name+": "+(next?"placa acoplada":"placa desprendida")+" do Nexus.";
      }
      else if(action==="segment-damage"){
        part.integrity=clamp(part.integrity-20,0,100);
        if(part.integrity===0)part.integrated=false;
        msg=part.name+": dano estrutural registrado. Integridade: "+part.integrity+"%.";
      }
      else if(action==="segment-fix"){
        cost=8;
        if(part.integrity>=100)return rp.toast("A peça já está íntegra.");
        if(S.energy<cost)return rp.toast("Energia insuficiente para reparar o segmento.");
        part.integrity=clamp(part.integrity+20,0,100);
        msg=part.name+": reparo concluído. Integridade: "+part.integrity+"%.";
      }
      else return;
      S.energy=clamp(S.energy-cost,0,100);
      recomputeIntegrity();log(msg,"SEGMENTO");noteAction(msg);show();
      if(connected()&&S.config.autoAnnounce)await sendText(msg,"SEGMENTO");
      else rp.toast("Segmento atualizado no Nexus.");
    }

    async function tsChoice(choiceIndex){
      const story=CHAPTERS[TS.chapter];if(!story || !TS.pending)return;
      const index=choiceIndex===1?1:0;
      const choice=story.options[index];
      TS.journal.push({id:id(),code:story.code,title:story.title,choice,at:new Date().toISOString()});
      TS.pending=false;TS.chapter++;
      TS.threat=clamp(TS.threat+(index===1?9:-8),0,100);
      log("Fragmento "+story.code+" processado: "+choice+".","TS / LORE");
      persist();show();rp.toast("Fragmento "+story.code+" arquivado.");
    }
    async function advanceRitual(){
      const r=S.stagedRitual;if(!r)return;
      const current=clamp(r.stage,0,3),name=r.name;
      if(current<3){
        r.stage=current+1;persist();show();
        const message="Ritual "+name+": "+STEP_NAMES[current]+" concluída. "+phaseDesc[current];
        if(S.config.autoAnnounce&&connected())await sendText(message,"RITUAL");
        else rp.toast(STEP_NAMES[current]+" concluída.");
        return;
      }
      if(S.energy<r.cost)return rp.toast("Energia insuficiente para concluir. Ajuste o núcleo ou cancele.");
      const f=document.createElement("form");
      for(const [key,val] of Object.entries({name:r.name,description:r.description,cost:r.cost})){
        const input=document.createElement("input");
        input.name=key;input.value=String(val);f.appendChild(input);
      }
      S.stagedRitual=null;
      persist();
      try{await rp.performRitual(f);noteAction("ritual concluído");}
      catch(err){S.stagedRitual=r;persist();rp.toast(err.message);}
      show();
    }
    const container=document.querySelector("#content");
    container.addEventListener("submit",async e=>{
      if(e.target.id!=="ritualForm"&&e.target.id!=="rpLiveMessage")return;
      e.preventDefault();e.stopImmediatePropagation();
      const f=e.target, data=new FormData(f);
      if(f.id==="rpLiveMessage"){
        const text=String(data.get("message")||"").trim();
        if(text)await sendText(text,"RP AO VIVO");
        return;
      }
      const name=String(data.get("name")||"").trim().slice(0,100);
      const description=String(data.get("description")||"").trim().slice(0,600);
      const cost=clamp(data.get("cost"),0,100);
      if(!name || !description)return rp.toast("Preencha nome e descrição do ritual.");
      if(S.stagedRitual)return rp.toast("Conclua ou cancele o ritual que já está em andamento.");
      if(S.energy<cost)return rp.toast("Energia insuficiente para iniciar este ritual.");
      S.stagedRitual={id:id(),name,description,cost,stage:0,at:new Date().toISOString()};
      persist();log("Ritual "+name+" preparado. Sequência ritualística iniciada.","RITUAL");
      show();rp.toast("Ritual iniciado. Avance pelas quatro etapas.");
    },true);
    container.addEventListener("click",async e=>{
      const el=e.target.closest("[data-rp]");if(!el)return;
      e.preventDefault();e.stopImmediatePropagation();
      const act=el.dataset.rp,key=el.dataset.key;
      if(act==="live-open")return rp.navigate("ao_vivo");
      if(act==="ts-open")return rp.navigate("ts");
      if(act==="live-ritual")return rp.navigate("rituais");
      if(act.startsWith("segment-"))return segmentAction(act,key);
      if(act==="ritual-next")return advanceRitual();
      if(act==="ritual-cancel"){
        if(!S.stagedRitual)return;
        S.stagedRitual=null;persist();log("Execução ritualística cancelada antes do consumo de energia.","RITUAL");show();return;
      }
      if(act==="ts-reveal"){
        if(TS.chapter>=CHAPTERS.length)return;
        TS.pending=true;persist();show();rp.toast("Fragmento disponível para investigação.");return;
      }
      if(act==="ts-choice")return tsChoice(Number(el.dataset.choice));
      if(act==="ts-reset"){
        if(!confirm("Reiniciar toda a campanha TS e seus registros de fragmentos?"))return;
        S.tsEvents={enabled:false,chapter:0,pending:false,actions:0,threat:15,journal:[]};
        // Repurpose outer object reference to preserve event handlers.
        Object.assign(TS,S.tsEvents); S.tsEvents=TS;persist();show();return;
      }
      if(act==="live-note"){
        const field=container.querySelector('#rpLiveMessage textarea');
        const text=field?.value.trim();if(!text)return rp.toast("Digite a mensagem.");
        S.chat.push({id:id(),at:new Date().toISOString(),text,status:"local"});log("Mensagem registrada localmente: "+text,"RP AO VIVO");persist();show();return;
      }
      if(act==="live-rest"){
        const old=S.energy;S.energy=clamp(S.energy+15,0,100);
        log("Núcleo em repouso. Energia recuperada: "+(S.energy-old)+" pontos.","ENERGIA");noteAction("repouso");show();rp.toast("Recuperação narrativa concluída.");return;
      }
      if(act==="live-integrar"||act==="live-desintegrar"||act==="live-repair"){
        const k=act==="live-integrar"?"integrar":act==="live-desintegrar"?"desintegrar":"reparar";
        return rp.armorAction(k);
      }
      if(act==="live-remodelar")return rp.navigate("armadura");
    },true);
    container.addEventListener("change",e=>{
      if(e.target.matches('[data-rp-change="ts-auto"]')){
        TS.enabled=Boolean(e.target.checked);persist();log("Alertas narrativos TS "+(TS.enabled?"ativados":"desativados")+".","TS");show();
      }
    },true);
    return views;
  };
})();