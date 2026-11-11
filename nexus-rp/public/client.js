"use strict";
(function(){
window.NexusRPClientUpgrade=function(rp,views){
 const S=rp.state,E=rp.escape,apiRoot="/api/client";
 const button=(action,label,cls="alt")=>'<button type="button" class="btn '+cls+'" data-nexus-client="'+action+'">'+label+'</button>';
 let lastStatus=null,lastPair=null;
 const backend=async(route,options={})=>{
   const key=rp.getAccess?.()||"";
   if(!key)throw Error("Informe e salve o código do painel na aba Configurações.");
   const ctrl=new AbortController(),timer=setTimeout(()=>ctrl.abort(),13000);
   try{
     const response=await fetch(apiRoot+route,{...options,headers:{"X-Nexus-Key":key,...(options.body?{"Content-Type":"application/json"}:{}),...(options.headers||{})},signal:ctrl.signal});
     const data=await response.json();
     if(!response.ok||data.ok===false)throw Error(data.error||"Falha do Nexus Client.");
     return data;
   }finally{clearTimeout(timer);}
 };
 async function getStatus(){const info=await backend("/status");lastStatus=info;return info;}
 async function queue(route,payload){
   const data=await backend(route,{method:"POST",body:JSON.stringify(payload)});
   if(!data.queued||!data.jobId)return data;
   for(let i=0;i<17;i++){
     await new Promise(r=>setTimeout(r,900));
     const job=await backend("/job?id="+encodeURIComponent(data.jobId));
     if(job.done){if(!job.success)throw Error(job.error||"O mod não concluiu esta ação.");return {ok:true,response:job.output};}
   }
   throw Error("O Minecraft não confirmou o comando. Abra o jogo e verifique o mod.");
 }
 rp.clientQueue=queue;
 const oldConfig=views.config;
 views.config=function(){
   let html=oldConfig();
   const needle='</select><small>No modo Agente HTTPS';
   if(html.includes(needle))html=html.replace(needle,'<option value="client" '+(S.config.connectionMode==="client"?"selected":"")+'>NEXUS CLIENT (MOD APENAS NO JOGADOR)</option></select><small>No modo Agente HTTPS');
   else html=html.replace('value="agent"','value="agent"');
   const note='<div class="notice">Sem acesso administrativo ao servidor? Escolha <strong>NEXUS CLIENT</strong>, abra a aba de pareamento e instale o mod NeoForge no seu Minecraft. A porta RCON e a senha não são usadas neste modo.</div>';
   if(S.config.connectionMode==="client")html=html.replaceAll("TESTAR RCON","TESTAR NEXUS CLIENT").replaceAll("Servidor RCON","Mod Minecraft").replaceAll("NÃO TESTADO","AGUARDANDO PAREAMENTO");
   return html+note+'<div class="buttons">'+button("open","ABRIR NEXUS CLIENT","gold")+'</div>';
 };
 views.cliente=function(){
   const on=lastStatus?.online,player=lastStatus?.player||"Não identificado";
   return '<div class="eyebrow">NEOFORGE 1.21.1 · CLIENT SIDE</div><h1 class="heading">Nexus Aterna Client</h1>'+
   '<p class="subheading">Conecte o Nexus ao seu Minecraft sem acesso ao servidor. O mod roda somente no seu cliente e envia telemetria e mensagens pela conta do seu jogador.</p>'+
   '<div class="grid two">'+
   '<section class="card"><div class="card-header"><h2>VINCULAÇÃO SEGURA</h2></div>'+
   '<div class="rp-evo-emergency '+(on?"online":"")+'"><strong>'+(on?"MINECRAFT CONECTADO":"AGUARDANDO MINECRAFT")+'</strong><span>Jogador: '+E(player)+'</span></div>'+
   '<p class="hint">1. Instale o mod Nexus Aterna Client na pasta mods do Minecraft 1.21.1 com NeoForge.</p>'+ 
   '<p><a href="https://github.com/SMKPort/PortVoxelViewer/actions/runs/43069905931" target="_blank" rel="noopener noreferrer">Baixar JAR compilado pelo GitHub Actions</a></p>'+
   '<p class="hint">2. Selecione o modo Nexus Client em Configurações e gere um código temporário.</p>'+
   '<p class="hint">3. No jogo, execute <code>/nexusclient pair SEU_CODIGO</code>. Este comando é executado no cliente, não no servidor.</p>'+
   '<p class="hint">4. Autorize mensagens com <code>/nexusclient chat on</code> e telemetria com <code>/nexusclient telemetry on</code>.</p>'+
   '<div class="buttons">'+button("pair","GERAR CÓDIGO DE PAREAMENTO","gold")+button("status","VERIFICAR CONEXÃO","ghost")+'</div>'+
   (lastPair?'<div class="rp-pair-code"><small>CÓDIGO TEMPORÁRIO (10 MIN)</small><strong>'+E(lastPair.code)+'</strong><p>Digite no chat do Minecraft:</p><code>/nexusclient pair '+E(lastPair.code)+'</code></div>':'<div class="empty" style="margin-top:15px">Nenhum código pendente. Use Gerar Código para vincular seu Minecraft.</div>')+
   '<p class="hint">A sessão pode precisar ser pareada novamente depois que o serviço da Render reiniciar.</p></section>'+
   '<section class="card"><div class="card-header"><h2>CAPACIDADES DO CLIENTE</h2></div>'+
   '<div class="kv"><span>Chat</span><span class="value">Sua própria conta</span></div>'+
   '<div class="kv"><span>Vida e dano</span><span class="value">Leitura do cliente</span></div>'+
   '<div class="kv"><span>Efeitos ativos</span><span class="value">Leitura do cliente</span></div>'+
   '<div class="kv"><span>Armadura / rituais</span><span class="value">Avisos e UI local</span></div>'+
   '<div class="kv"><span>Emotes reais</span><span class="value">Exigem integração com o mod de emotes</span></div>'+
   '<div class="divider"></div><div class="notice">Mensagens enviadas pelo painel são transmitidas como mensagens normais do personagem e ficam sujeitas às regras, filtros e limites do servidor. Não é possível executar comandos de administrador ou modificar outros jogadores.</div>'+
   '<div class="buttons">'+button("open-events","MONITORAR EVENTOS","alt")+button("open-chat","ABRIR COMUNICADOR","gold")+'</div></section></div>';
 };
 const nav=document.querySelector('#nav');
 const cfg=nav?.querySelector('[data-page="config"]');
 if(cfg){const b=document.createElement("button");b.className="nav-button";b.dataset.page="cliente";b.innerHTML='<span>⬡</span> Nexus Client';cfg.before(b);}
 const oldEvents=views.eventos;
 if(oldEvents)views.eventos=function(){
   let html=oldEvents();
   if(S.config.connectionMode==="client")html=html.replaceAll("agente Python","mod Nexus Client").replaceAll("AGENTE NÃO CONFIRMADO","NEXUS CLIENT NÃO CONFIRMADO").replaceAll("AGENTE CONECTADO","NEXUS CLIENT CONECTADO").replaceAll("Agente HTTPS","Nexus Client").replaceAll("AGENTE HTTPS","NEXUS CLIENT");
   return html;
 };
 const root=document.querySelector("#content");
 root.addEventListener("click",async e=>{
   const b=e.target.closest("[data-nexus-client]");if(!b)return;
   e.preventDefault();e.stopImmediatePropagation();
   try{
     if(b.dataset.nexusClient==="open")return rp.navigate("cliente");
     if(b.dataset.nexusClient==="open-chat")return rp.navigate("chat");
     if(b.dataset.nexusClient==="open-events")return rp.navigate("eventos");
     if(b.dataset.nexusClient==="pair"){lastPair=await backend("/pair/new",{method:"POST",body:"{}"});rp.toast("Código criado. Insira-o no comando do Minecraft.");}
     if(b.dataset.nexusClient==="status"){await getStatus();rp.toast(lastStatus.online?"Nexus Client conectado!":"Minecraft ainda não pareado ou offline.");}
     rp.rerender();
   }catch(err){rp.toast(err.message);}
 },true);
 return views;
};
})();