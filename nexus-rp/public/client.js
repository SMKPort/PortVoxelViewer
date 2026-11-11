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
   if(S.config.connectionMode==="client"){
     const c=S.config;
     html=html.replace(/(<input id="prefix"[^>]*)(>)/,'$1 readonly aria-readonly="true"$2');
     const selected=["local","global","custom"].includes(c.chatChannel)?c.chatChannel:"local";
     const options=[["local","Local (/l) - padrão"],["global","Global (sem comando)"],["custom","Personalizado (comando de chat)"]]
       .map(item=>'<option value="'+item[0]+'" '+(selected===item[0]?"selected":"")+'>'+item[1]+'</option>').join("");
     const channelForm='<div class="divider"></div><div class="eyebrow">COMUNICADOR DO JOGADOR</div>'+
       '<div class="field"><label for="chatChannel">CANAL DAS MENSAGENS</label><select name="chatChannel" id="chatChannel">'+options+'</select>'+
       '<small>Local envia /l seguido da sua mensagem. O mod usa a conexão normal do Minecraft, sem simular teclado.</small></div>'+
       '<div class="field"><label for="chatFormatting">CÓDIGOS DE FORMATAÇÃO DO CHAT LOCAL</label>'+ 
       '<input name="chatFormatting" id="chatFormatting" maxlength="16" value="'+E(c.chatFormatting??"&6&l")+
       '" placeholder="&6&l"><small><code>&6</code> = dourado. <code>&l</code> = negrito. O padrão envia /l &6&l seguido diretamente do texto. Deixe vazio para desativar.</small></div>'+ 
       '<div class="field"><label for="chatCustomCommand">COMANDO PERSONALIZADO (SOMENTE CANAIS DE CHAT)</label>'+
       '<input name="chatCustomCommand" id="chatCustomCommand" maxlength="24" value="'+E(c.chatCustomCommand||"")+
       '" placeholder="/rp"><small>Exemplos: /rp, /me, /ooc. Comandos administrativos são bloqueados.</small></div>'+
       '<div class="notice"><strong>EMISSOR: [Nexus Aterna]</strong>. Identificação permanente de todas as mensagens enviadas pelo painel (inclusive em configurações antigas).</div>'+
       '<div class="notice">Exemplo do comando: <strong>/l &6&l[Nexus Aterna] Desintegrando armadura</strong>. O identificador do Nexus é aplicado mesmo se o perfil antigo tiver desativado o prefixo.</div>';
     html=html.replace('<form id="configForm">','<form id="configForm">'+channelForm);
   }
   const note='<div class="notice">Sem acesso administrativo ao servidor? Escolha <strong>NEXUS CLIENT</strong>, abra a aba de pareamento e instale o mod NeoForge no seu Minecraft. A porta RCON e a senha não são usadas neste modo.</div>';
   if(S.config.connectionMode==="client")html=html.replaceAll("TESTAR RCON","TESTAR NEXUS CLIENT").replaceAll("Servidor RCON","Mod Minecraft").replaceAll("NÃO TESTADO","AGUARDANDO PAREAMENTO");
   return html+note+'<div class="buttons">'+button("open","ABRIR NEXUS CLIENT","gold")+'</div>';
 };
 const oldChat=views.chat;
 views.chat=function(){
   let html=oldChat();
   if(S.config.connectionMode!=="client")return html;
   const c=S.config,channel=["global","local","custom"].includes(c.chatChannel)?c.chatChannel:"local";
   const label=channel==="global"?"GLOBAL":channel==="local"?"/l":"/"+String(c.chatCustomCommand||"rp").replace(/^\//,"");
   const displayPrefix=channel==="local"?" "+String(c.chatFormatting??"&6&l"):"";
   const previous='<div class="chat-preview"><strong>'+E(c.prefix)+'</strong>';
   const preview='<div class="chat-preview"><strong>'+E(label+displayPrefix+(channel==="local"?"":" ")+(c.prefix||"[Nexus Aterna]"))+'</strong>';
   html=html.replace(previous,preview);
   const info='<div class="notice">CANAL CONFIGURADO: <strong>'+E(label)+'</strong> · EMISSOR: <strong>[Nexus Aterna]</strong>. O servidor ainda identifica sua conta como origem técnica do envio.</div>';
   return info+html;
 };
 const oldLive=views.ao_vivo;
 if(oldLive)views.ao_vivo=function(){
   const html=oldLive();
   if(S.config.connectionMode!=="client")return html;
   const label=S.config.chatChannel==="global"?"GLOBAL":S.config.chatChannel==="custom"?"/"+String(S.config.chatCustomCommand||"rp"):"/l "+String(S.config.chatFormatting??"&6&l");
   return '<div class="notice">Chat do Nexus Client: <strong>'+E(label)+'</strong> · identidade <strong>[Nexus Aterna]</strong> aplicada automaticamente. Envie apenas o texto.</div>'+html;
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