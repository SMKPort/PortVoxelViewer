"use strict";
(function(){
window.NexusRPViews=function(rp){
const S=rp.state,E=rp.escape,P=rp.presets;
const h=(tag,title,desc)=>'<div class="eyebrow">'+E(tag)+'</div><h1 class="heading">'+E(title)+'</h1><p class="subheading">'+E(desc)+'</p>';
const card=(title,body)=>'<section class="card"><div class="card-header"><h2>'+E(title)+'</h2></div>'+body+'</section>';
const b=(action,label,style="alt")=>'<button type="button" class="btn '+style+'" data-action="'+E(action)+'">'+label+'</button>';
const meter=(n,red=false)=>'<div class="meter '+(red?"red":"")+'"><span style="width:'+Math.min(100,Math.max(0,n))+'%"></span></div>';
const kv=(label,value)=>'<div class="kv"><span>'+E(label)+'</span><span class="value">'+E(value)+'</span></div>';
const logView=(count=8)=>'<div class="hero-logs">'+(S.logs.slice(0,count).map(x=>'<div class="log-row"><strong>'+E(x.kind)+' · '+rp.date(x.at)+'</strong><span>'+E(x.text)+'</span></div>').join("")||'<div class="empty">Nenhum registro ainda.</div>')+'</div>';
const core='<div class="nexus-stage"><div class="stage-grid"></div><svg viewBox="0 0 400 400" role="img" aria-label="Núcleo arcano giratório"><defs><radialGradient id="gl"><stop stop-color="#f1c58f" stop-opacity=".3"/><stop offset="1" stop-color="#7c203b" stop-opacity="0"/></radialGradient></defs><circle cx="200" cy="200" r="190" fill="url(#gl)"/><g class="stage-orbit" stroke="#bb916a" fill="none"><circle cx="200" cy="200" r="162" stroke-dasharray="13 12"/><path d="M200 24V58 M200 342V376 M24 200H58 M342 200H376" stroke-width="3"/></g><g class="stage-orbit rev" stroke="#ad424b" stroke-width="2" fill="none"><circle cx="200" cy="200" r="134" stroke-dasharray="35 8"/><path d="M200 75L325 200L200 325L75 200Z"/></g><g stroke="#e3bf81" fill="none"><circle cx="200" cy="200" r="102"/><path d="M200 112L277 200L200 288L123 200Z M200 137L251 200L200 263L149 200Z"/></g><circle class="pulse" cx="200" cy="200" r="31" stroke="#f5dbb0" fill="#a96a59" fill-opacity=".25"/><path d="M200 172L218 200L200 228L182 200Z" fill="#ffe3a6"/><circle cx="200" cy="200" r="6" fill="#fff2d8"/></svg><div class="stage-caption">NEXUS ATERNA · ARCANE CORE<small>PORT MORNINGSTAR HERNANDES</small></div></div>';
const armorView=()=>'<div class="armor-stage"><div class="armor-visual '+(S.armor==="desintegrada"?"desintegrada ":"")+E(S.form.toLowerCase())+'"><div class="armor-core"></div><div class="armor-piece shoulder"></div><div class="armor-piece upper"></div><div class="armor-piece joint"></div><div class="armor-piece lower"></div><div class="armor-piece hand"></div></div><div class="armor-status">'+E(S.armor==="integrada"?"PLACAS INTEGRADAS":"PLACAS DESINTEGRADAS")+' · '+E(S.form.toUpperCase())+'</div></div>';
const field=(id,title,value="",type="text",attr="")=>'<div class="field"><label for="'+id+'">'+E(title)+'</label><input id="'+id+'" name="'+id+'" type="'+type+'" value="'+E(value)+'" '+attr+'></div>';
const form=function(){
const c=S.config;
if(c.connectionMode==="client"){
 return '<form id="configForm"><input type="hidden" name="connectionMode" value="client">'+
 '<div class="notice"><strong>MODO NEXUS CLIENT:</strong> não precisa de senha RCON, host, porta nem acesso aos arquivos do servidor Minecraft.</div>'+
 '<div class="form-grid">'+field("player","SEU NOME NO MINECRAFT",c.player,"text","maxlength=\'16\'")+
 field("prefix","EMISSOR DAS MENSAGENS","[Nexus Aterna]","text","readonly aria-readonly=\'true\' maxlength=\'38\'")+'</div>'+
 field("access","CÓDIGO DE ACESSO HTTPS DO PAINEL","","password","placeholder=\'Chave HTTPS da Render (somente na primeira vez)\' autocomplete=\'new-password\'")+
 '<div class="notice">'+(rp.isAccessRemembered?.()
  ?'<strong>Chave lembrada neste navegador.</strong> Não precisa digitá-la novamente. Ela não é exibida nesta página.'
  :'<strong>Primeiro acesso:</strong> informe a chave NEXUS_RP_ACCESS_KEY da Render uma vez. O endereço do serviço já está configurado.')+'</div>'+
 '<label class="field inline"><input type="checkbox" name="rememberAccess" '+(rp.isAccessRemembered?.()?"checked":"")+'><span>Lembrar a chave HTTPS apenas neste dispositivo</span></label>'+
 '<p class="hint">Opcional. O navegador guarda a chave localmente caso você marque a opção. Não use em dispositivo compartilhado. Ela não é gravada no repositório ou no HTML público.</p>'+
 (rp.isAccessRemembered?.()?'<div class="buttons">'+b("forget-access","ESQUECER CHAVE SALVA","ghost")+'</div>':"")+
 '<label class="field inline"><input type="checkbox" name="autoAnnounce" '+(c.autoAnnounce?"checked":"")+'><span>Enviar avisos automáticos do Nexus pelo chat local</span></label>'+
 '<div class="buttons"><button class="btn gold" type="submit">SALVAR CÓDIGO E CONFIGURAÇÕES</button>'+b("test-connection","◉ TESTAR NEXUS CLIENT","gold")+'</div></form>';
}
return '<form id="configForm"><div class="form-grid">'+field("host","HOST PÚBLICO MINECRAFT",c.host,"text","placeholder=\'mc.exemplo.com\'")+field("port","PORTA RCON",c.port,"number","min=\'1024\' max=\'65535\' required")+field("player","SEU NOME NO MINECRAFT",c.player,"text","maxlength=\'16\'")+field("prefix","PREFIXO NO CHAT",c.prefix,"text","maxlength=\'38\'")+'</div>'+
field("password","SENHA RCON (NÃO É SALVA)","","password","placeholder=\'Informe a senha RCON\' autocomplete=\'off\'")+
field("access","CÓDIGO DE ACESSO HTTPS (NÃO É SALVO)","","password","placeholder=\'Código da ponte Render\' autocomplete=\'off\'")+
'<div class="form-grid"><div class="field"><label for="target">DESTINO</label><select name="target" id="target"><option value="global" '+(c.target==="global"?"selected":"")+'>Todos os jogadores</option><option value="player" '+(c.target==="player"?"selected":"")+'>Jogador específico</option></select></div>'+
field("recipient","JOGADOR DE DESTINO",c.recipient,"text","maxlength=\'16\'")+
'<div class="field"><label for="color">COR DA MENSAGEM</label><select name="color" id="color">'+["white","gold","red","dark_red","aqua","light_purple","green","yellow","gray"].map(x=>'<option '+(c.color===x?"selected":"")+'>'+x+'</option>').join("")+'</select></div>'+
'<div class="field"><span class="field-label">ANÚNCIOS AUTOMÁTICOS</span><label class="field inline"><input type="checkbox" name="autoAnnounce" '+(c.autoAnnounce?"checked":"")+'><span>Transmitir ações quando conectado</span></label></div></div>'+
'<div class="buttons"><button class="btn" type="submit">SALVAR CONFIGURAÇÕES</button>'+b("test-connection","◉ TESTAR RCON","gold")+'</div></form>';
};
function settingsFunctions(){
const f=S.config.functions;
return '<form id="functionForm">'+[
["integrar","INTEGRAR ARMADURA"],["desintegrar","DESINTEGRAR ARMADURA"],["remodelar","REMODELAR ARMADURA"],["ritual","EXECUTAR RITUAL"]
].map(([key,label])=>field(key,label,f[key]||"","text","placeholder='nexus:armadura/"+key+"'")).join("")+'<div class="buttons"><button class="btn gold" type="submit">SALVAR FUNÇÕES</button>'+b("datapack-preset","USAR FUNÇÕES DO DATAPACK","ghost")+'</div></form>';
}
return {
painel(){
const connected=rp.isConnected();
const command=(title,detail,value)=>
  '<article class="v4-feature"><h3>'+E(title)+'</h3><p>'+E(detail)+'</p>'+
  '<button type="button" data-v4-copy="'+E(value)+'">COPIAR COMANDO</button></article>';
return '<section class="v4-hero"><div class="v4-hero-content">'+
  '<div class="v4-edition">NÚCLEO ARCANO / NEXUS 4.0</div>'+
  '<h1>O próximo estágio<br>do <em>Nexus Aterna.</em></h1>'+
  '<p>Central de inteligência, percepção e comunicação do Minecraft.</p>'+
  '<div class="v4-hero-actions">'+b("go-cliente","⬡ NEXUS CLIENT","gold")+
  b("go-voz","🎙 VOZ DO NEXUS","ghost")+'</div>'+
  '<div class="v4-hero-meta"><span>NEOFORGE 1.21.1</span>'+
  '<span>CONSCIÊNCIA CONTEXTUAL</span><span>AD AETERNUM</span></div></div>'+
  '<div class="v4-insignia" aria-hidden="true"><span class="v4-ring"></span>'+
  '<span class="v4-ring"></span><span class="v4-glyph">✦</span></div></section>'+
  '<div class="v4-section-head"><div><small>01 / SITUAÇÃO</small>'+
  '<h2>Conexão e permissões</h2></div></div>'+
  '<div class="grid two">'+card("MINECRAFT",kv("Nexus Client",
    connected?"CONECTADO":"AGUARDANDO PAREAMENTO")+
    kv("Mecanismo","Minecraft 1.21.1 · NeoForge")+
    b("go-cliente","TESTAR CLIENTE","gold"))+
    card("COMUNICAÇÃO",kv("Identificação","[Nexus Aterna]")+
    kv("Canal",S.config.chatChannel==="custom"?"/"+E(S.config.chatCustomCommand||"rp"):
      S.config.chatChannel==="global"?"Global":"Local (/l)")+
    b("go-config","CONFIGURAR CANAL","gold"))+'</div>'+
  '<div class="v4-section-head"><div><small>02 / PROTOCOLOS</small>'+
  '<h2>Funções disponíveis no jogo</h2></div></div>'+
  '<div class="v4-feature-grid">'+
  command("Situação atual","Vida, mana e protocolos observados.","/nexusclient situacao")+
  command("Proteção contextual","Estado do protocolo que suspende macros em vida crítica.",
    "/nexusclient awakening status")+
  command("Diário narrativo","Consultar eventos pessoais salvos no jogo.",
    "/nexusclient awakening journal recent")+
  command("Macros","Verificar sequências e gatilhos ativos.",
    "/nexusclient macro status")+
  command("Visão Arcana","Abrir a análise de tela por Gemini.",
    "/nexusclient vision menu")+
  command("Editor do HUD","Configurar o tamanho e a posição.",
    "/nexusclient hud edit")+'</div>'+
  '<div class="v4-section-head"><div><small>03 / MONITORAMENTO</small>'+
  '<h2>Eventos e alertas</h2></div></div>'+
  '<div class="buttons">'+b("go-eventos","EVENTOS DO JOGO","gold")+
  b("go-alertas","DESTINO DOS ALERTAS","ghost")+
  b("go-mente","NEXUS MIND","ghost")+
  b("go-chat","COMUNICADOR","ghost")+'</div>'+
  card("ÚLTIMAS ATIVIDADES",logView(12));
}
,
chat(){const messages=S.chat.slice(-30).map(x=>'<div class="chat-entry '+E(x.status)+'"><small>'+E(x.status==="sent"?"SOLICITADO AO MINECRAFT":x.status==="failed"?"FALHOU":"NARRATIVA LOCAL")+' · '+rp.date(x.at)+'</small><p>'+E(x.text)+'</p></div>').join("");
const t=[["Status","Nexus Aterna conectado"],["Alerta","Detectei uma situação de risco"],["Percepção","Análise situacional concluída"],["Combate","Recomendo cautela durante o combate"]];
return h("MÓDULO 01 / CANAL MINECRAFT","Comunicador","Envie mensagens identificadas como Nexus Aterna pelo canal configurado no Minecraft.")+
'<div class="grid two">'+card("TRANSMISSOR DE MENSAGENS",'<div class="chat-feed" id="chatFeed">'+(messages||'<div class="empty">Nenhuma mensagem registrada.</div>')+'</div><form id="chatForm"><div class="field"><label for="chatMessage">MENSAGEM DO NEXUS</label><textarea name="message" id="chatMessage" maxlength="280" required placeholder="Ex.: Desintegrando armadura"></textarea></div><div class="chat-preview"><strong>'+E(S.config.prefix)+'</strong> <span id="chatPreview">Sua mensagem aparecerá aqui.</span></div><div class="buttons" style="margin-top:14px"><button type="submit" class="btn gold">↗ ENVIAR AO MINECRAFT</button>'+b("chat-local","REGISTRAR LOCALMENTE","ghost")+'</div></form><p class="hint">O envio requer uma conexão ativa com o Minecraft. Registrar localmente não publica a mensagem no jogo.</p>')+
card("MENSAGENS RÁPIDAS",'<div class="recent-template">'+t.map((item,i)=>'<button type="button" class="template-btn" data-action="template" data-index="'+i+'"><strong>'+E(item[0])+'</strong>'+E(item[1])+'</button>').join("")+'</div><div class="divider"></div>'+kv("Destino",S.config.connectionMode==="client"?(S.config.chatChannel==="local"?"Local (/l)":S.config.chatChannel==="custom"?"Comando personalizado":"Global"):S.config.target==="global"?"Todos os jogadores":S.config.recipient||"Não definido")+kv("Ponte",rp.isConnected()?"Conexão testada":"Não testada")+b("go-config","CONFIGURAR SERVIDOR","ghost"))+'</div>';}
,
config(){const client=S.config.connectionMode==="client";
return h("CONEXÃO / NEXUS","Configurações de comunicação",
  "Defina o canal do chat e o pareamento do mod sem precisar acessar o servidor.")+
  '<div class="grid two">'+
    card("NEXUS CLIENT E CANAL PERSONALIZADO",form()+
      '<p class="hint">O comando personalizado não precisa pertencer à lista de atalhos. '+
      'Ele deve existir no servidor e ser um comando de mensagens, com até cinco argumentos fixos.</p>')+
    card("INSTRUÇÕES",'<p class="hint">1. Salve o acesso HTTPS. 2. Gere um código na aba Nexus Client.'+
      ' 3. No jogo execute /nexusclient pair CODIGO e /nexusclient chat on.'+
      ' 4. Configure individualmente o destino dos avisos na aba Alertas.</p>'+
      b("go-cliente","VERIFICAR PAREAMENTO","gold")+
      b("go-alertas","CONFIGURAR AVISOS","ghost"))+'</div>'+
  '<h2 class="section-title">Armazenamento local</h2>'+
  card("BACKUP E REGISTROS",'<div class="buttons">'+
    b("export","EXPORTAR JSON","gold")+
    b("clear","REINICIAR REGISTROS","ghost")+'</div>'+logView(12));
}
};
};
})();