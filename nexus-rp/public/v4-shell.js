"use strict";
(()=>{
  /* Progressive interface enhancements. Never accesses secret fields or sends game commands. */
  const dialog=document.getElementById("v4CommandDialog");
  const input=document.getElementById("v4CommandInput");
  const results=document.getElementById("v4CommandResults");
  const opener=document.getElementById("v4QuickLaunch");
  const closer=document.getElementById("v4CommandClose");
  if(!dialog||!input||!results||!opener)return;

  const entries=[
    {name:"Visão geral",detail:"Painel e indicadores narrativos",route:"painel",tags:"inicio dashboard nucleo"},
    {name:"Voz do Nexus",detail:"Comandos e respostas por voz",route:"voz",tags:"microfone tts"},
    {name:"Comunicador",detail:"Mensagens e chat do jogo",route:"chat",tags:"mensagens"},
    {name:"Nexus Client",detail:"Pareamento e status do mod",route:"cliente",tags:"minecraft neoforge"},
    {name:"Nexus Mind",detail:"Memórias e painel de IA",route:"mente",tags:"inteligencia"},
    {name:"Alertas do Nexus",detail:"Configurar notificações",route:"alertas",tags:"avisos"},
    {name:"Armadura arcana",detail:"Placas e remodelagem",route:"armadura",tags:"nexus braco"},
    {name:"Rituais",detail:"Laboratório narrativo",route:"rituais",tags:"magia"},
    {name:"RP ao vivo",detail:"Controles rápidos de interpretação",route:"ao_vivo",tags:"live"},
    {name:"Eventos do jogo",detail:"Registros observáveis do cliente",route:"eventos",tags:"historico"},
    {name:"Emergência RP",detail:"Painel narrativo de emergência",route:"emergencia",tags:"protocolo"},
    {name:"Configurações",detail:"Conexão e permissões",route:"config",tags:"seguranca"},
    {name:"Nexus 4.0 no jogo",detail:"Copiar comando para abrir a central do mod",copy:"/nexusclient cognition menu",tags:"4.0 contexto"},
    {name:"Situação atual no jogo",detail:"Copiar comando de diagnóstico do mod",copy:"/nexusclient situacao",tags:"vida mana contextual"},
    {name:"Histórico de macros",detail:"Copiar comando para análise no mod",copy:"/nexusclient cognition insights status",tags:"aprendizado combate"}
  ];

  let matches=entries.slice(),selected=0,lastFocus=null,toastTimer=null;
  const fold=text=>String(text||"").normalize("NFD").replace(/[\u0300-\u036f]/g,"")
    .toLowerCase().replace(/[^a-z0-9]+/g," ").trim();
  const norm=text=>fold(text).split(" ").filter(Boolean);
  function search(q){
    const words=norm(q);
    return entries.map((item,index)=>{
      const label=fold(item.name),haystack=fold([item.name,item.detail,item.tags].join(" "));
      let score=0;
      for(const word of words){
        if(!haystack.includes(word))return null;
        score+=label.startsWith(word)?8:label.includes(word)?5:2;
      }
      return {item,index,score};
    }).filter(Boolean).sort((a,b)=>b.score-a.score||a.index-b.index).map(x=>x.item);
  }
  function draw(){
    matches=search(input.value);
    selected=Math.max(0,Math.min(selected,matches.length-1));
    results.replaceChildren();
    if(!matches.length){
      const empty=document.createElement("p");
      empty.className="v4-fineprint";
      empty.textContent="Nenhum comando encontrado. Tente outro termo.";
      results.appendChild(empty);
      return;
    }
    matches.forEach((item,i)=>{
      const button=document.createElement("button");
      button.type="button";button.className="v4-result";
      button.setAttribute("role","option");
      button.setAttribute("aria-selected",String(i===selected));
      const block=document.createElement("div");
      block.textContent=item.name;
      const detail=document.createElement("span");detail.textContent=item.detail;
      block.appendChild(detail);
      const icon=document.createElement("span");icon.className="v4-result-key";
      icon.textContent=item.copy?"COPIAR":"ABRIR";
      button.append(block,icon);
      button.addEventListener("mouseenter",()=>{
        selected=i;
        results.querySelectorAll(".v4-result").forEach((el,n)=>
          el.setAttribute("aria-selected",String(n===selected)));
      });
      button.addEventListener("click",()=>run(item));
      results.appendChild(button);
    });
  }
  function open(){
    if(!dialog.hidden)return;
    lastFocus=document.activeElement;
    dialog.hidden=false;
    document.body.classList.add("v4-palette-open");
    input.value="";selected=0;draw();input.focus();
  }
  function close(){
    if(dialog.hidden)return;
    dialog.hidden=true;
    document.body.classList.remove("v4-palette-open");
    if(lastFocus&&typeof lastFocus.focus==="function")lastFocus.focus();
  }
  function flash(message){
    let element=document.getElementById("v4Toast");
    if(!element){
      element=document.createElement("div");
      element.className="v4-toast";element.id="v4Toast";
      element.setAttribute("role","status");
      document.body.appendChild(element);
    }
    element.textContent=message;element.hidden=false;
    clearTimeout(toastTimer);
    toastTimer=setTimeout(()=>{element.hidden=true;},3200);
  }
  async function copy(text){
    if(!/^\/nexusclient [a-z0-9_ .-]{1,130}$/i.test(text)){
      flash("Comando não reconhecido.");return;
    }
    try{
      if(!navigator.clipboard?.writeText)throw Error("Clipboard unavailable");
      await navigator.clipboard.writeText(text);
      flash("Comando copiado. Cole no chat do Minecraft.");
    }catch(error){
      const temp=document.createElement("textarea");
      temp.value=text;temp.setAttribute("readonly","");
      temp.style.position="fixed";temp.style.opacity="0";
      document.body.appendChild(temp);temp.select();
      const copied=document.execCommand?.("copy")||false;
      temp.remove();
      flash(copied?"Comando copiado.":"Copie pelo chat: "+text);
    }
  }
  function run(item){
    close();
    if(item.copy){void copy(item.copy);return;}
    if(item.route){
      location.hash="#"+item.route;
      sync();
      document.getElementById("content")?.focus?.({preventScroll:true});
      window.scrollTo?.({top:0,behavior:"instant"});
    }
  }
  function sync(){
    const active=location.hash.slice(1)||"painel";
    document.querySelectorAll(".v4-mobile-dock [data-v4-route]").forEach(button=>{
      const selectedRoute=button.dataset.v4Route===active;
      button.classList.toggle("active",selectedRoute);
      button.setAttribute("aria-current",selectedRoute?"page":"false");
    });
  }
  opener.addEventListener("click",open);
  closer?.addEventListener("click",close);
  dialog.querySelector(".v4-dialog-backdrop")?.addEventListener("click",close);
  input.addEventListener("input",()=>{selected=0;draw();});
  input.addEventListener("keydown",e=>{
    if(e.key==="ArrowDown"||e.key==="ArrowUp"){
      e.preventDefault();
      if(matches.length)selected=(selected+(e.key==="ArrowDown"?1:-1)+matches.length)%matches.length;
      draw();results.querySelectorAll(".v4-result")[selected]?.scrollIntoView?.({block:"nearest"});
    }else if(e.key==="Enter"){
      e.preventDefault();if(matches[selected])run(matches[selected]);
    }
  });
  document.addEventListener("keydown",e=>{
    if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==="k"){
      e.preventDefault();dialog.hidden?open():close();return;
    }
    if(e.key==="Escape"&&!dialog.hidden){e.preventDefault();close();return;}
    const tag=document.activeElement?.tagName||"";
    const editing=["INPUT","TEXTAREA","SELECT"].includes(tag)||
      document.activeElement?.isContentEditable;
    if(e.key==="/"&&!editing&&dialog.hidden&&!e.altKey&&!e.ctrlKey&&!e.metaKey){
      e.preventDefault();open();
    }
    if(e.key==="Tab"&&!dialog.hidden){
      const focusable=[input,...dialog.querySelectorAll(".v4-result"),closer].filter(Boolean);
      const current=focusable.indexOf(document.activeElement);
      if(e.shiftKey&&current<=0){e.preventDefault();focusable.at(-1).focus();}
      if(!e.shiftKey&&current===focusable.length-1){e.preventDefault();input.focus();}
    }
  });
  document.querySelectorAll(".v4-mobile-dock [data-v4-route]").forEach(button=>{
    button.addEventListener("click",()=>run({route:button.dataset.v4Route}));
  });
  document.querySelectorAll("[data-v4-open]").forEach(button=>button.addEventListener("click",open));
  document.addEventListener("click",event=>{
    const action=event.target.closest("[data-v4-copy]");
    if(action){event.preventDefault();void copy(action.dataset.v4Copy||"");}
  });
  window.addEventListener("hashchange",sync);
  sync();
})();
