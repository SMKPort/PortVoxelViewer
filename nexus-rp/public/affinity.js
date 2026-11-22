"use strict";
(function(root){
  const fold=v=>String(v||"").normalize("NFD").replace(/[\u0300-\u036f]/g,"")
    .toLowerCase().replace(/[^a-z0-9]+/g," ").trim();
  function relevant(question,entries,max=6){
    if(!Array.isArray(entries))return [];
    const q=fold(question);
    if(!q)return [];
    const broad=/\b(afinidade|afinidades|relacoes|relacionamentos|parentesco|parentes|familia|familiares|jogadores)\b/.test(q);
    const ranked=[];
    for(const e of entries){
      if(!e||typeof e.player!=="string"||typeof e.nickname!=="string"||
         typeof e.kinship!=="string"||!Number.isInteger(e.score)||e.score<0||e.score>10)continue;
      const player=fold(e.player),nickname=fold(e.nickname),kinship=fold(e.kinship);
      const named=[player,nickname].filter(x=>x.length>=2&&(" "+q+" ").includes(" "+x+" "));
      // Only matching people or explicit requests about relationships; never
      // routinely share every stored affinity with Gemini.
      const score=named.length?30+(named[0]===player?4:0):
        broad?(q.includes(kinship)&&kinship?14:5):0;
      if(score>0)ranked.push({score,record:{
        player:e.player.slice(0,32),nickname:e.nickname.slice(0,48),
        kinship:e.kinship.slice(0,70),score:e.score}});
    }
    ranked.sort((a,b)=>b.score-a.score);
    return ranked.slice(0,Math.min(6,Math.max(0,max))).map(x=>x.record);
  }
  root.NexusRPAffinitySelect=relevant;
  root.NexusRPAffinityUpgrade=function(rp,views){
    const E=rp.escape;
    if(!Array.isArray(rp.state.affinities))rp.state.affinities=[];
    let editingId="";
    const content=document.querySelector("#content");
    function cards(){
      const rows=rp.state.affinities;
      if(!rows.length)return '<p class="hint">Nenhum jogador cadastrado. Comece pelo nome de Minecraft dele.</p>';
      return rows.map((a,i)=>'<div class="notice" style="margin-top:10px">'+
        '<strong style="color:#e9bb68">'+E(a.player)+'</strong> '+
        '<span style="color:#cbc3ce">('+E(a.nickname||"sem apelido")+')</span>'+
        '<div><small>Parentesco: '+E(a.kinship||"nenhum informado")+'</small></div>'+
        '<div>❤ Afinidade: <strong>'+E(a.score)+'/10</strong></div>'+
        '<div class="buttons" style="margin-top:10px">'+
        '<button type="button" class="btn ghost" data-affinity-edit="'+E(a.id)+'">EDITAR</button>'+
        '<button type="button" class="btn ghost" data-affinity-delete="'+E(a.id)+'">EXCLUIR</button>'+
        '</div></div>').join("");
    }
    views.afinidade=function(){
      const edit=rp.state.affinities.find(a=>a.id===editingId)||null;
      return '<div class="page-head"><div><span class="eyebrow">NEXUS ATERNA / RELAÇÕES</span>'+
        '<h1>Afinidades</h1><p>Registre como Port enxerga cada jogador. As notas são subjetivas, de 0 a 10.</p></div></div>'+
        '<div class="grid two"><section class="card"><div class="card-header"><h2>'+
        (edit?'EDITAR JOGADOR':'NOVO VÍNCULO')+'</h2></div>'+
        '<form id="affinityForm">'+
        '<div class="field"><label for="affinityPlayer">NOME NO MINECRAFT</label>'+
        '<input id="affinityPlayer" name="player" maxlength="32" minlength="2" required value="'+E(edit?.player||"")+'" placeholder="Ex.: Gabriel"></div>'+
        '<div class="field"><label for="affinityNickname">APELIDO QUE O PORT USA</label>'+
        '<input id="affinityNickname" name="nickname" maxlength="48" value="'+E(edit?.nickname||"")+'" placeholder="Ex.: Gabi"></div>'+
        '<div class="field"><label for="affinityKinship">PARENTESCO / VÍNCULO</label>'+
        '<input id="affinityKinship" name="kinship" maxlength="70" value="'+E(edit?.kinship||"")+'" placeholder="Ex.: filho, irmã, mãe adotiva, amigo"></div>'+
        '<div class="field"><label for="affinityScore">AFINIDADE (0 A 10)</label>'+
        '<input id="affinityScore" name="score" type="number" min="0" max="10" step="1" required value="'+E(edit?.score??5)+'"></div>'+
        '<div class="buttons"><button class="btn gold" type="submit">'+
        (edit?'SALVAR ALTERAÇÕES':'CADASTRAR JOGADOR')+'</button>'+
        (edit?'<button class="btn ghost" type="button" data-affinity-cancel>CANCELAR</button>':'')+
        '</div></form><p class="hint">Guardado apenas neste navegador, junto aos dados locais do Nexus. O backup JSON inclui as fichas.</p>'+
        '</section><section class="card"><div class="card-header"><h2>FICHAS REGISTRADAS ('+
        rowsCount()+')</h2></div>'+
        '<label style="display:flex;gap:10px;align-items:center;margin-bottom:12px">'+
        '<input type="checkbox" id="shareAffinitiesAI" '+(rp.state.config.shareAffinitiesAI===true?'checked':'')+'>'+
        '<span>Permitir que o Gemini consulte afinidades relevantes nas conversas</span></label>'+
        '<p class="hint">Desligado por padrão. Somente fichas relacionadas à pergunta são enviadas à IA. Nunca altera parentescos ou notas automaticamente.</p>'+
        cards()+'</section></div>';
    };
    function rowsCount(){return rp.state.affinities.length;}
    content?.addEventListener("submit",e=>{
      if(e.target.id!=="affinityForm")return;
      e.preventDefault();
      try{
        const d=new FormData(e.target);
        const player=String(d.get("player")||"").replace(/[\u0000-\u001f<>]/g,"").trim().slice(0,32);
        const nickname=String(d.get("nickname")||"").replace(/[\u0000-\u001f<>]/g,"").trim().slice(0,48);
        const kinship=String(d.get("kinship")||"").replace(/[\u0000-\u001f<>]/g,"").trim().slice(0,70);
        const score=Number(d.get("score"));
        if(player.length<2||!Number.isInteger(score)||score<0||score>10)
          throw Error("Informe um nome válido e afinidade inteira entre 0 e 10.");
        const duplicate=rp.state.affinities.some(a=>fold(a.player)===fold(player)&&a.id!==editingId);
        if(duplicate)throw Error("Já existe uma ficha com esse nome de jogador. Edite a ficha existente.");
        const previous=rp.state.affinities.find(a=>a.id===editingId);
        const item={id:previous?.id||"aff-"+Date.now()+"-"+Math.random().toString(36).slice(2,8),
          player,nickname,kinship,score};
        if(previous)Object.assign(previous,item);
        else{
          if(rp.state.affinities.length>=150)throw Error("Limite de 150 fichas atingido.");
          rp.state.affinities.push(item);
        }
        editingId="";rp.save();rp.rerender();rp.toast("Afinidade salva no navegador.");
      }catch(error){rp.toast(error.message);}
    });
    content?.addEventListener("change",e=>{
      if(e.target?.id!=="shareAffinitiesAI")return;
      rp.state.config.shareAffinitiesAI=e.target.checked===true;
      rp.save();rp.toast(rp.state.config.shareAffinitiesAI?
        "Afinidades relevantes autorizadas para o Gemini.":
        "Compartilhamento de afinidades desligado.");
    });
    content?.addEventListener("click",e=>{
      const edit=e.target.closest("[data-affinity-edit]");
      const del=e.target.closest("[data-affinity-delete]");
      if(edit){editingId=edit.dataset.affinityEdit;rp.rerender();}
      else if(del){
        const record=rp.state.affinities.find(x=>x.id===del.dataset.affinityDelete);
        if(!record||!confirm("Excluir a ficha de "+record.player+"?"))return;
        rp.state.affinities=rp.state.affinities.filter(x=>x.id!==record.id);
        if(editingId===record.id)editingId="";
        rp.save();rp.rerender();rp.toast("Ficha excluída.");
      }else if(e.target.closest("[data-affinity-cancel]")){
        editingId="";rp.rerender();
      }
    });
    return views;
  };
})(window);
