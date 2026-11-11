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
const c=S.config,f=c.functions;
return '<form id="configForm"><div class="form-grid">'+field("host","HOST PÚBLICO MINECRAFT",c.host,"text","placeholder='mc.exemplo.com'")+field("port","PORTA RCON",c.port,"number","min='1024' max='65535' required")+field("player","SEU NOME NO MINECRAFT",c.player,"text","maxlength='16'")+field("prefix","PREFIXO NO CHAT",c.prefix,"text","maxlength='38'")+'</div>'+
field("password","SENHA RCON (NÃO É SALVA)","","password","placeholder='Informe a senha RCON' autocomplete='off'")+
field("access","CÓDIGO DE ACESSO HTTPS (NÃO É SALVO)","","password","placeholder='Código da ponte Render' autocomplete='off'")+
'<div class="form-grid"><div class="field"><label for="target">DESTINO</label><select name="target" id="target"><option value="global" '+(c.target==="global"?"selected":"")+'>Todos os jogadores</option><option value="player" '+(c.target==="player"?"selected":"")+'>Jogador específico</option></select></div>'+
field("recipient","JOGADOR DE DESTINO",c.recipient,"text","maxlength='16'")+
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
painel(){return h("INTERFACE DE ROLEPLAY / NÚCLEO","Nexus Aterna","O sistema narrativo do braço direito de Port. Energia, integridade e comandos que podem ser transmitidos ao Minecraft.")+
'<div class="grid four">'+
'<div class="card metric"><small>INTEGRIDADE</small><strong>'+S.integrity+'%</strong><p>Estrutura do braço</p></div>'+
'<div class="card metric"><small>ENERGIA</small><strong>'+S.energy+'%</strong><p>Reserva arcana</p></div>'+
'<div class="card metric"><small>ARMADURA</small><strong>'+(S.armor==="integrada"?"ATIVA":"DISPERSA")+'</strong><p>Placas do Nexus</p></div>'+
'<div class="card metric"><small>CONEXÃO</small><strong>'+(rp.isConnected()?"ONLINE":"LOCAL")+'</strong><p>'+E(rp.isConnected()?"Minecraft RCON":"Modo narrativa")+'</p></div></div>'+
'<h2 class="section-title">Centro de operações</h2><div class="hero-columns">'+core+card("REGISTROS DA OFICINA",logView())+'</div>'+
'<h2 class="section-title">Comandos imediatos</h2><div class="buttons">'+b("go-chat","▣ MENSAGEM NO CHAT","gold")+b("go-armadura","⚙ ARMADURA")+b("go-rituais","✧ RITUAIS")+b("go-config","⛭ CONFIGURAR","ghost")+'</div>'+
'<h2 class="section-title">Monitor narrativo</h2>'+card("INTEGRIDADE E ENERGIA",'<div class="grid two">'+
'<div><div class="stat-edit"><span>INTEGRIDADE</span><strong id="integrityLabel">'+S.integrity+'%</strong></div><input id="integritySlider" type="range" min="0" max="100" value="'+S.integrity+'">'+meter(S.integrity,true)+'</div>'+
'<div><div class="stat-edit"><span>ENERGIA ARCANA</span><strong id="energyLabel">'+S.energy+'%</strong></div><input id="energySlider" type="range" min="0" max="100" value="'+S.energy+'">'+meter(S.energy)+'</div></div><p class="hint">Os valores são fictícios e podem ser ajustados conforme os acontecimentos do roleplay.</p>')+
'<div class="notice">Este painel é uma nova interface separada do Nexus 2.0 completo. Alterações feitas aqui não afetam o aplicativo anterior.</div>';}
,
chat(){const messages=S.chat.slice(-30).map(x=>'<div class="chat-entry '+E(x.status)+'"><small>'+E(x.status==="sent"?"ENTREGUE AO MINECRAFT":x.status==="failed"?"FALHOU":"NARRATIVA LOCAL")+' · '+rp.date(x.at)+'</small><p>'+E(x.text)+'</p></div>').join("");
const t=[["Ativação","Núcleo Aterna sincronizado. Prótese pronta para operação."],["Alerta","Oscilação identificada na integridade do braço direito."],["Engenharia","Iniciando reorganização das placas arcanas do Nexus."],["Ritual","Canalização ritualística autorizada. Núcleo ativo."]];
return h("MÓDULO 01 / CANAL MINECRAFT","Comunicador","Escreva uma mensagem do Nexus e publique no chat do seu servidor Java por meio da ponte RCON.")+
'<div class="grid two">'+card("TRANSMISSOR DE MENSAGENS",'<div class="chat-feed" id="chatFeed">'+(messages||'<div class="empty">Nenhuma mensagem registrada.</div>')+'</div><form id="chatForm"><div class="field"><label for="chatMessage">MENSAGEM DO NEXUS</label><textarea name="message" id="chatMessage" maxlength="280" required placeholder="Ex.: A armadura se fragmenta em placas douradas..."></textarea></div><div class="chat-preview"><strong>'+E(S.config.prefix)+'</strong> <span id="chatPreview">Sua mensagem aparecerá aqui.</span></div><div class="buttons" style="margin-top:14px"><button type="submit" class="btn gold">↗ ENVIAR AO MINECRAFT</button>'+b("chat-local","REGISTRAR LOCALMENTE","ghost")+'</div></form><p class="hint">O envio real precisa de RCON ativo e senhas configuradas. Registrar localmente não publica no jogo.</p>')+
card("MENSAGENS RÁPIDAS",'<div class="recent-template">'+t.map((item,i)=>'<button type="button" class="template-btn" data-action="template" data-index="'+i+'"><strong>'+E(item[0])+'</strong>'+E(item[1])+'</button>').join("")+'</div><div class="divider"></div>'+kv("Destino",S.config.target==="global"?"Todos os jogadores":S.config.recipient||"Não definido")+kv("Ponte",rp.isConnected()?"Conexão testada":"Não testada")+b("go-config","CONFIGURAR SERVIDOR","ghost"))+'</div>';}
,
armadura(){return h("MÓDULO 02 / PRÓTESE ARCANA","Armadura do Nexus","Integre, desintegre e remodele as placas do braço. O efeito visual acontece neste painel, e funções opcionais podem atuar no jogo.")+
'<div class="grid two">'+card("MANIFESTAÇÃO DA ARMADURA",armorView()+'<p class="hint">Representação visual narrativa. Animações reais no Minecraft exigem datapack ou mod compatível.</p>')+
card("ORDENS DE RECONFIGURAÇÃO",kv("Estado",S.armor==="integrada"?"INTEGRADA":"DISPERSA")+kv("Forma atual",S.form)+
'<div class="divider"></div><div class="buttons">'+b("armor-integrar","✦ INTEGRAR","gold")+b("armor-desintegrar","◈ DESINTEGRAR")+'</div>'+
'<div class="field" style="margin-top:18px"><label for="armorForm">FORMA A ASSUMIR</label><select id="armorForm">'+["Celestial","Combate","Ritual","Repouso"].map(x=>'<option '+(S.form===x?"selected":"")+'>'+x+'</option>').join("")+'</select></div>'+
'<div class="buttons">'+b("armor-remodelar","⚙ REMODELAR","gold")+b("armor-reparar","✧ REPARAR","ghost")+'</div>'+
'<div class="notice">Integrar consome 12 de energia. Remodelar consome 9. Reparar consome 14 e recupera 18 de integridade. Desintegrar devolve 5 pontos de energia.</div>'+
kv("Efeito no Minecraft",rp.isConnected()?"Via função configurada":"Somente nesta interface")+b("go-config","CONFIGURAR COMANDOS DO MINECRAFT","ghost"))+'</div>';}
,
rituais(){const preset=P.find(x=>x.id===S.selectedRitual)||P[0],last=S.rituals[0];
return h("MÓDULO 03 / MAGIA ARCANA","Rituais e Bênçãos","Execute rituais do universo do Port, use energia narrativa e transmita os efeitos para o chat do Minecraft.")+
'<div class="grid two">'+card("CÍRCULOS RITUALÍSTICOS",'<div class="ritual-preset">'+P.map(p=>'<button data-action="ritual-preset" data-id="'+E(p.id)+'" class="'+(p.id===preset.id?"active":"")+'">'+E(p.name)+'<small>'+E(p.school)+' · '+p.cost+' energia</small></button>').join("")+'</div><div class="divider"></div>'+kv("Selecionado",preset.name)+kv("Energia atual",S.energy+" / 100")+meter(S.energy)+'<div class="notice">Os rituais são fictícios, para uso no roleplay. Nenhuma ação sobrenatural real é executada.</div>')+
card("CONJURAÇÃO",'<form id="ritualForm"><div class="field"><label for="ritualName">NOME DO RITUAL</label><input id="ritualName" name="name" maxlength="100" required value="'+E(preset.name)+'"></div><div class="field"><label for="ritualDescription">DESCRIÇÃO</label><textarea name="description" id="ritualDescription" maxlength="600" required>'+E(preset.description)+'</textarea></div><div class="field"><label for="ritualCost">CUSTO DE ENERGIA (0–100)</label><input type="number" name="cost" id="ritualCost" min="0" max="100" value="'+preset.cost+'" required></div><button type="submit" class="btn gold">✧ EXECUTAR RITUAL</button></form>'+(last?'<div class="divider"></div><div class="ritual-result"><strong>'+E(last.name)+'</strong><p>'+E(last.description)+'</p><p>Estabilidade narrativa '+last.score+'% · Energia consumida '+last.cost+'</p><small class="hint">'+rp.date(last.at)+'</small></div>':'<div class="empty" style="margin-top:16px">Nenhum ritual realizado ainda.</div>'))+'</div>';}
,
config(){return h("MÓDULO 04 / CONEXÕES","Configurar Minecraft","Defina o servidor, como as mensagens aparecem e quais funções do datapack comandam efeitos opcionais.")+
'<div class="grid two">'+card("PONTE MINECRAFT JAVA RCON",form()+'<p class="input-note">Host, porta e preferências são salvos neste navegador. Senha RCON e código de acesso ficam somente na memória da aba, sem entrar no backup.</p><div class="divider"></div>'+kv("Ponte HTTPS",rp.isHealthy()?"ONLINE":"VERIFICANDO")+kv("Servidor RCON",rp.isConnected()?"AUTENTICADO":"NÃO TESTADO"))+
card("FUNÇÕES DO SERVIDOR",'<p class="hint">Opcional: configure funções de um datapack ou mod. Exemplo: <code>nexus:armadura/integrar</code>. O servidor executará a função correspondente somente se ela existir.</p>'+settingsFunctions()+
'<div class="notice warn"><strong>Segurança:</strong> RCON não cifra o tráfego TCP por conta própria. Evite expor a porta diretamente à internet. Proteja-a com firewall e, idealmente, um túnel privado. A ponte HTTPS usa código de acesso. Servidores Minecraft Java compatíveis precisam de <code>enable-rcon=true</code>.</div>')+'</div>'+
'<h2 class="section-title">Registros locais</h2>'+card("DIÁRIO E BACKUP",'<div class="buttons">'+b("export","↓ EXPORTAR JSON","gold")+b("clear","REINICIAR REGISTROS","ghost")+'</div><div class="divider"></div>'+logView(12));}
};
};
})();