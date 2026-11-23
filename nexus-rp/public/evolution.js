"use strict";
/* Live event observation. Narrative armor, rituals, live RP and containment
 * were retired in Nexus 4.0.6. No legacy side effects or server actions. */
(function(){
window.NexusRPEvolution=function(rp,views){
  const S=rp.state,E=rp.escape;
  if(!S.gameEvents||typeof S.gameEvents!=="object")S.gameEvents={feed:[],lastId:0};
  const G=S.gameEvents;
  if(!Array.isArray(G.feed))G.feed=[];
  G.lastId=Math.max(0,Number(G.lastId)||0);
  let error="",lastSync="",online=false,polling=false;
  const TYPES={
    health:"Leitura de vida",damage:"Dano recebido",heal:"Cura",
    death:"Morte",respawn:"Renascimento",effect:"Efeito atualizado",
    armor:"Armadura",energy:"Energia",join:"Conexão",leave:"Desconexão"
  };
  const onPage=()=>location.hash==="#eventos";
  const active=()=>onPage()&&document.visibilityState!=="hidden";
  function feedHtml(){
    return G.feed.slice(-50).reverse().map(item=>
      '<article class="rp-evo-event"><strong>'+E(TYPES[item.type]||item.type)+
      '</strong><small>'+E(item.at||"")+' · '+E(item.player||"")+'</small>'+
      '<p>'+E(item.note||((item.amount||"")+(item.amount?" pontos":"")))+'</p></article>'
    ).join("")||'<div class="empty">Aguardando eventos do Minecraft.</div>';
  }
  views.eventos=()=>'<div class="eyebrow">NEXUS CLIENT / TELEMETRIA</div>'+
    '<h1 class="heading">Eventos do Jogo</h1>'+
    '<p class="subheading">Registros observáveis da sua sessão. Ative '+
    '<code>/nexusclient telemetry on</code> dentro do Minecraft.</p>'+
    '<div class="grid two"><section class="card">'+
    '<div class="card-header"><h2>CONEXÃO E MONITORAMENTO</h2></div>'+
    '<div class="notice" id="rpEventsStatus">'+E(error?"Falha: "+error:
      online?"Minecraft conectado":"Aguardando Minecraft")+'</div>'+
    '<p id="rpEventsSync" class="hint">'+E(lastSync?
      "Sincronizado em "+lastSync:"Nenhuma sincronização nesta sessão.")+'</p>'+
    '<p class="hint">A leitura da vida, dos efeitos e das magias depende do '+
    'cliente instalado. Os dados do jogo são separados dos antigos valores narrativos.</p>'+
    '<button type="button" class="btn ghost" data-events-refresh>ATUALIZAR</button>'+
    '</section><section class="card"><div class="card-header">'+
    '<h2>ÚLTIMOS ACONTECIMENTOS</h2></div>'+
    '<div id="rpEventsFeed">'+feedHtml()+'</div></section></div>';
  function paint(){
    const status=document.querySelector("#rpEventsStatus");
    if(status)status.textContent=error?"Falha: "+error:
      online?"Minecraft conectado":"Aguardando Minecraft";
    const sync=document.querySelector("#rpEventsSync");
    if(sync)sync.textContent=lastSync?"Sincronizado em "+lastSync:"Nenhuma sincronização.";
    const feed=document.querySelector("#rpEventsFeed");
    if(feed)feed.innerHTML=feedHtml();
  }
  async function refresh(){
    if(polling||!active())return;
    polling=true;
    try{
      let result=await rp.requestEvents(G.lastId,S.config.player);
      if(!Array.isArray(result.events))throw Error("Dados de telemetria inválidos.");
      if(Number(result.lastId)<G.lastId){
        G.lastId=0;G.feed.length=0;
        result=await rp.requestEvents(0,S.config.player);
      }
      online=result.online===true||result.bridge?.online===true;
      for(const item of result.events){
        if(!item||typeof item!=="object"||!Number.isInteger(Number(item.id))||
          Number(item.id)<=G.lastId)continue;
        G.feed.push(item);
        G.lastId=Number(item.id);
      }
      G.lastId=Math.max(G.lastId,Number(result.lastId)||0);
      G.feed=G.feed.slice(-80);
      error="";lastSync=new Date().toLocaleTimeString("pt-BR");
      rp.save();
    }catch(e){error=String(e.message||e).slice(0,180);}
    finally{polling=false;paint();}
  }
  const container=document.querySelector("#content");
  container.addEventListener("click",e=>{
    if(e.target.closest?.("[data-events-refresh]"))refresh();
  });
  const advanced=document.querySelector("#advancedItems");
  if(advanced&&!advanced.querySelector('[data-page="eventos"]')){
    const nav=document.createElement("button");
    nav.className="nav-button";nav.dataset.page="eventos";
    nav.innerHTML='<span>◉</span> Eventos do jogo';
    advanced.appendChild(nav);
  }
  window.addEventListener("hashchange",()=>{if(onPage())refresh();});
  document.addEventListener("visibilitychange",()=>{if(active())refresh();});
  setInterval(()=>{if(active())refresh();},2000);
  if(onPage())setTimeout(refresh,0);
  return views;
};
})();