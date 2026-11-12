"use strict";
(function(){
window.NexusRPClientUpgrade=function(rp,views){
 const S=rp.state,E=rp.escape,apiRoot="/api/client";
 const button=(action,label,cls="alt")=>'<button type="button" class="btn '+cls+'" data-nexus-client="'+action+'">'+label+'</button>';
 let lastStatus=null,lastPair=null,diagnostics=null;
 async function runDiagnostics(){
   const origin=location.origin;
   const inspect=async(path,key)=>{
     try{
       const response=await fetch(path,{method:"GET",cache:"no-store",
         headers:{"Accept":"application/json",...(key?{"X-Nexus-Key":key}:{})},
         signal:AbortSignal.timeout(10000)});
       const type=String(response.headers.get("content-type")||"").toLowerCase();
       if(!type.includes("application/json"))
         return path+": HTTP "+response.status+" · resposta HTML/não JSON ("+(type||"sem Content-Type")+")";
       const data=await response.json();
       if(response.status===401)return path+": HTTP 401 · código HTTPS incorreto ou expirado nesta aba";
       if(!response.ok)return path+": HTTP "+response.status+" · "+String(data.error||"falha da API").slice(0,120);
       return path+": HTTP "+response.status+" · JSON OK · "+(data.version?"versão "+data.version+" · ":"")+
         (data.status||"")+(path.includes("/status")?(data.online?" · MOD ONLINE":" · aguardando mod"):"");
     }catch(e){return path+": ERRO · "+(e?.name==="TimeoutError"?"tempo esgotado":String(e?.message||e)).slice(0,140);}
   };
   const health=await inspect("/api/health","");
   const key=rp.getAccess?.()||"";
   const status=key?await inspect("/api/client/status",key):"/api/client/status: não testado (informe a chave HTTPS)";
   diagnostics={origin,health,status};
   rp.rerender();
 }
 const backend=async(route,options={})=>{
   const key=rp.getAccess?.()||"";
   if(!key)throw Error("Informe a NEXUS_RP_ACCESS_KEY em Configurações, toque em SALVAR CÓDIGO e volte ao Nexus Client. Nenhuma senha RCON é necessária.");
   const ctrl=new AbortController(),timer=setTimeout(()=>ctrl.abort(),route==="/voice"?34000:13000);
   try{
     const response=await fetch(apiRoot+route,{...options,headers:{"X-Nexus-Key":key,...(options.body?{"Content-Type":"application/json"}:{}),...(options.headers||{})},signal:ctrl.signal});
     const type=response.headers.get("content-type")||"";
     if(!type.toLowerCase().includes("application/json")){
       const hint=response.status===502||response.status===503||response.status===504
         ?"O servidor da Render está indisponível ou reiniciando."
         :"A API respondeu com uma página HTML em vez de JSON.";
       throw Error(hint+" Verifique o endereço do Nexus RP e atualize a página. HTTP "+response.status+".");
     }
     let data;
     try{data=await response.json();}catch(error){throw Error("Resposta JSON incompleta da Render. Atualize a página e tente novamente.");}
     if(!response.ok||data.ok===false)throw Error(data.error||"Falha do Nexus Client (HTTP "+response.status+").");
     return data;
   }finally{clearTimeout(timer);}
 };
 async function getStatus(){const info=await backend("/status");lastStatus=info;return info;}
 async function queue(route,payload){
   const data=await backend(route,{method:"POST",body:JSON.stringify(payload)});
   if(!data.queued||!data.jobId)return data;
   const ids=Array.isArray(data.jobIds)&&data.jobIds.length?data.jobIds.slice(0,3):[data.jobId];
   let output="";
   for(const id of ids){
     let done=false;
     for(let i=0;i<30;i++){
       await new Promise(r=>setTimeout(r,800));
       const job=await backend("/job?id="+encodeURIComponent(id));
       if(job.done){
         if(!job.success)throw Error(job.error||"O mod não concluiu esta mensagem.");
         output=job.output;done=true;break;
       }
     }
     if(!done)throw Error("O Minecraft não confirmou a mensagem. Verifique o mod e o chat local.");
   }
   return {ok:true,response:output,...(data.reply?{reply:data.reply}:{}),model:data.model};
 }
 rp.clientQueue=queue;
 const oldConfig=views.config;
 views.config=function(){
   let html=oldConfig();
   // Client mode is now a first-class option in Evolution's connection selector.
   // Keep fallback for older cached assets without duplicating the option.
   if(!html.includes('value="client"')){
     html=html.replace('<select id="connectionMode" name="connectionMode">','<select id="connectionMode" name="connectionMode"><option value="client" '+(S.config.connectionMode==="client"?"selected":"")+'>NEXUS CLIENT (SEM RCON)</option>');
   }
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
   const keyReady=!!rp.getAccess?.();
   const note=S.config.connectionMode==="client"
     ?'<div class="notice">'+(keyReady?'<strong>Código HTTPS pronto.</strong> '+(rp.isAccessRemembered?.()?'A chave foi lembrada neste dispositivo. ':'')+'Você já pode gerar o código temporário na aba Nexus Client.':'<strong>Falta o código HTTPS.</strong> Insira o valor de NEXUS_RP_ACCESS_KEY da Render, salve e depois gere o código temporário de pareamento.')+' Nenhuma senha RCON é necessária.</div>'
     :'<div class="notice">Não tem acesso ao servidor Minecraft? Escolha <strong>NEXUS CLIENT</strong>. O modo funciona sem RCON.</div>';
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
   return '<div class="eyebrow">NEOFORGE 1.21.1 · CLIENT SIDE · PAINEL 2.0</div><h1 class="heading">Nexus Aterna Client</h1>'+
   '<p class="subheading">Conecte o Nexus ao seu Minecraft sem acesso ao servidor. O mod roda somente no seu cliente e envia telemetria e mensagens pela conta do seu jogador.</p>'+
   '<div class="grid two">'+
   '<section class="card"><div class="card-header"><h2>VINCULAÇÃO SEGURA</h2></div>'+
   '<div class="rp-evo-emergency '+(on?"online":"")+'"><strong>'+(on?"MINECRAFT CONECTADO":"AGUARDANDO MINECRAFT")+'</strong><span>Jogador: '+E(player)+'</span></div>'+
   '<p class="hint">1. Instale o mod Nexus Aterna Client na pasta mods do Minecraft 1.21.1 com NeoForge.</p>'+ 
   '<p><a href="https://github.com/SMKPort/PortVoxelViewer/actions/runs/43080065875" target="_blank" rel="noopener noreferrer">Baixar Nexus Aterna Client 1.2.0 (GitHub Actions)</a></p>'+
   '<p class="hint">2. Nas Configurações, informe a chave HTTPS da Render uma vez. Marque <strong>Lembrar neste dispositivo</strong> para não precisar digitá-la novamente. Não precisa de senha RCON.</p>'+
   '<p class="hint">3. No jogo, execute <code>/nexusclient pair SEU_CODIGO</code>. Este comando é executado no cliente, não no servidor.</p>'+
   '<p class="hint">4. Autorize mensagens com <code>/nexusclient chat on</code> e telemetria com <code>/nexusclient telemetry on</code>.</p>'+
   '<div class="buttons">'+button("settings","CONFIGURAR CÓDIGO HTTPS","alt")+button("pair","GERAR CÓDIGO DE PAREAMENTO","gold")+button("status","VERIFICAR CONEXÃO","ghost")+'</div>'+
   (lastPair?'<div class="rp-pair-code"><small>CÓDIGO TEMPORÁRIO (10 MIN)</small><strong>'+E(lastPair.code)+'</strong><p>Digite no chat do Minecraft:</p><code>/nexusclient pair '+E(lastPair.code)+'</code></div>':'<div class="empty" style="margin-top:15px">Nenhum código pendente. Use Gerar Código para vincular seu Minecraft.</div>')+
   '<p class="hint">A sessão pode precisar ser pareada novamente depois que o serviço da Render reiniciar.</p></section>'+
   '<section class="card"><div class="card-header"><h2>CAPACIDADES DO CLIENTE</h2></div>'+
   '<div class="kv"><span>Chat</span><span class="value">Sua própria conta</span></div>'+
   '<div class="kv"><span>Vida e dano</span><span class="value">Leitura do cliente</span></div>'+
   '<div class="kv"><span>Efeitos ativos</span><span class="value">Leitura do cliente</span></div>'+
   '<div class="kv"><span>Armadura / rituais</span><span class="value">Avisos e UI local</span></div>'+
   '<div class="kv"><span>Emotes reais</span><span class="value">Exigem integração com o mod de emotes</span></div>'+
   '<div class="divider"></div><div class="notice">Mensagens enviadas pelo painel são transmitidas como mensagens normais do personagem e ficam sujeitas às regras, filtros e limites do servidor. Não é possível executar comandos de administrador ou modificar outros jogadores.</div>'+
   '<div class="buttons">'+button("open-events","MONITORAR EVENTOS","alt")+button("open-chat","ABRIR COMUNICADOR","gold")+'</div></section></div>'+
   '<section class="card" style="margin-top:16px"><div class="card-header"><h2>DIAGNÓSTICO DA API</h2></div>'+
   '<p class="hint">Verifica se seu navegador consegue acessar a Render e se as rotas estão respondendo JSON. Não exibe sua senha nem o código de acesso.</p>'+
   button("diagnostic","TESTAR API E MOSTRAR DETALHES","gold")+
   (diagnostics?'<div class="notice" style="margin-top:12px;overflow-wrap:anywhere">'+
     '<strong>Origem:</strong> '+E(diagnostics.origin)+'<p>'+E(diagnostics.health)+'</p><p>'+E(diagnostics.status)+'</p></div>':
     '<p class="hint">Nenhum diagnóstico executado nesta aba.</p>')+'</section>';
 };
 const nav=document.querySelector('#nav');
 const cfg=nav?.querySelector('[data-page="config"]');
 if(cfg&&!nav.querySelector('[data-page="cliente"]')){const b=document.createElement("button");b.className="nav-button";b.dataset.page="cliente";b.innerHTML='<span>⬡</span> Nexus Client';cfg.before(b);}
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
     if(b.dataset.nexusClient==="settings")return rp.navigate("config");
     if(b.dataset.nexusClient==="open-chat")return rp.navigate("chat");
     if(b.dataset.nexusClient==="open-events")return rp.navigate("eventos");
     if(b.dataset.nexusClient==="pair"){lastPair=await backend("/pair/new",{method:"POST",body:"{}"});rp.toast("Código criado. Insira-o no comando do Minecraft.");}
     if(b.dataset.nexusClient==="status"){await getStatus();rp.toast(lastStatus.online?"Nexus Client conectado!":"Minecraft ainda não pareado ou offline.");}
     if(b.dataset.nexusClient==="diagnostic"){await runDiagnostics();rp.toast("Diagnóstico exibido no rodapé da aba Nexus Client.");}
     rp.rerender();
   }catch(err){rp.toast(err.message);}
 },true);
 return views;
};
})();