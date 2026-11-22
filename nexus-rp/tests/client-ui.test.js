"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const fs=require("node:fs");
const vm=require("node:vm");
const path=require("node:path");
const root=path.join(__dirname,"../public");
const pageScript=file=>fs.readFileSync(path.join(root,file),"utf8");
function boot(mode,remembered=false){
 const window={};
 const document={
   querySelector:selector=>selector==="#nav"?{querySelector:()=>({before(){}})}:
     selector==="#content"?{addEventListener(){}}:null,
   createElement:()=>({dataset:{}})
 };
 const context=vm.createContext({window,document,setInterval:()=>0});
 for(const file of ["views.js","upgrade.js","evolution.js","client.js","affinity.js"]){
   vm.runInContext(pageScript(file),context,{filename:file});
 }
 const state={
   integrity:100,energy:85,armor:"integrada",form:"Celestial",selectedRitual:"bencao",
   logs:[{kind:"TESTE",text:"ok",at:new Date().toISOString()}],chat:[],rituals:[],
   config:{connectionMode:mode,chatChannel:"local",chatCustomCommand:"rp",
     chatFormatting:"&6&l",chatUsePrefix:true,prefix:"[Nexus Aterna]",
     host:"",port:25575,player:"Port",target:"global",autoAnnounce:true,functions:{},color:"white"}
 };
 const escape=value=>String(value??"").replace(/[&<>"']/g,char=>({
   "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"
 }[char]));
 const rp={
   state,escape,date:v=>String(v),presets:[{id:"bencao",name:"Bênção",cost:16,description:"Teste"}],
   isConnected:()=>false,isHealthy:()=>true,save(){},log(){},toast(){},
   navigate(){},rerender(){},sendToChat(){},armorAction(){},performRitual(){},
   getAccess:()=>remembered?"SENSITIVE_TEST_SECRET_NOT_FOR_HTML":"",isAccessRemembered:()=>remembered,requestEvents:async()=>({events:[],lastId:0})
 };
 let views=window.NexusRPViews(rp);
 views=window.NexusRPUpgrade(rp,views);
 views=window.NexusRPEvolution(rp,views);
 views=window.NexusRPClientUpgrade(rp,views);
 views=window.NexusRPAffinityUpgrade(rp,views);
 return views;
}
test("Nexus Client uses no RCON fields and exposes code pairing",()=>{
 const views=boot("client"),html=views.config(),pair=views.cliente();
 assert.equal((html.match(/value="client"/g)||[]).length,1);
 assert.ok(html.includes('value="client" selected'));
 assert.ok(!html.includes('name="password"'));
 assert.ok(!html.includes('name="port"'));
 assert.ok(html.includes('name="access"'));
 assert.ok(html.includes("SALVAR CÓDIGO E CONFIGURAÇÕES"));
 assert.ok(html.includes("TESTAR NEXUS CLIENT"));
 assert.ok(html.includes("Nenhuma senha RCON é necessária"));
 assert.ok(pair.includes('data-nexus-client="settings"'));
 assert.ok(pair.includes("GERAR CÓDIGO DE PAREAMENTO"));
 assert.ok(pair.includes("TESTAR API E MOSTRAR DETALHES"));
 assert.ok(pair.includes("CONSOLE 4.0"));
 assert.ok(pair.includes("DIAGNÓSTICO DA API"));
 assert.ok(pair.includes("NEXUS 4.0 · PROTOCOLOS NO JOGO"));
 assert.ok(pair.includes('data-v4-copy="/nexusclient situacao"'));
 assert.ok(pair.includes('data-v4-copy="/nexusclient cognition menu"'));
 assert.ok(pair.includes('data-v4-copy="/nexusclient journal menu"'));
 assert.ok(pair.includes('data-v4-copy="/nexusclient cognition insights status"'));
});
test("RCON fields remain available only in RCON mode",()=>{
 const html=boot("public").config();
 assert.ok(html.includes('name="password"'));
 assert.ok(html.includes('name="port"'));
 assert.equal((html.match(/value="client"/g)||[]).length,1);
});
test("app defaults new users to client-only mode and enters client when selected",()=>{
 const s=pageScript("app.js");
 assert.ok(s.includes('config:{connectionMode:"client"'));
 assert.ok(s.includes('if(next==="cliente"){state.config.connectionMode="client"'));
 assert.ok(s.includes('if(state.config.connectionMode==="public"&&!state.config.host)'));
});

test("remembered key is recognized without injecting it into HTML",()=>{
 const empty=boot("client").config();
 const remembered=boot("client",true).config();
 assert.ok(empty.includes('name="rememberAccess"'));
 assert.ok(!empty.includes('name="rememberAccess" checked'));
 assert.ok(remembered.includes('name="rememberAccess" checked'));
 assert.ok(remembered.includes("Chave lembrada neste navegador"));
 assert.ok(remembered.includes("ESQUECER CHAVE SALVA"));
 assert.ok(!remembered.includes("SENSITIVE_TEST_SECRET_NOT_FOR_HTML"));
 const js=pageScript("app.js");
 assert.ok(js.includes("ACCESS_REMEMBER_KEY"));
 assert.ok(js.includes("localStorage.setItem(ACCESS_REMEMBER_KEY,secrets.access)"));
 assert.ok(js.includes("localStorage.removeItem(ACCESS_REMEMBER_KEY)"));
});

test("compact navigation keeps core pages visible and advanced pages accessible",()=>{
 const index=pageScript("index.html");
 const mainNav=index.match(/<nav id="nav">([\s\S]*?)<details id="advancedNav"/);
 assert.ok(mainNav,"Navigation needs a collapsible advanced group");
 for(const page of ["painel","voz","chat","cliente","config"])
   assert.ok(mainNav[1].includes('data-page="'+page+'"'),"Core navigation missing "+page);
 for(const page of ["armadura","rituais","alertas","mente","afinidade"])
   assert.ok(index.includes('data-page="'+page+'"'),"Advanced tool lost "+page);
 assert.ok(!index.includes('data-page="audicao"'),"Retired paid hearing must not be navigable");
 assert.ok(!index.includes('src="/hearing.js'),"Retired paid hearing script must not load");
 assert.ok(!index.includes('data-page="arcano"'),"Removed magic scanner cannot appear in navigation");
 assert.ok(!index.includes('src="/arcane.js'),"Removed scanner script cannot load");
 assert.ok(index.includes('href="#voz"'),"Voice must be directly accessible from mobile topbar");
 assert.ok(/app\.js\?v=\d+\.\d+(?:\.\d+)?/.test(index),"Cache busting prevents stale pages after deployment");
 assert.ok(pageScript("app.js").includes("advancedItems.appendChild(item)"));
 assert.ok(pageScript("client.js").includes('!nav.querySelector(\'[data-page="cliente"]\')'));
});


test("Protocolo TS, Sequenciador e Emotes desaparecem de todas as páginas",()=>{
 const index=pageScript("index.html");
 const app=pageScript("app.js");
 const upgrade=pageScript("upgrade.js");
 const evolution=pageScript("evolution.js");
 const client=pageScript("client.js");
 const views=boot("client");
 for(const retired of ["ts","sequencias","emotes"]){
   assert.equal(Object.hasOwn(views,retired),false,"A página "+retired+" não pode existir");
   assert.ok(!app.includes(retired+':"'),"A rota "+retired+" não pode existir");
   assert.ok(!index.includes('data-page="'+retired+'"'),"Menu antigo "+retired);
 }
 const screens=[views.painel(),views.config(),views.ao_vivo(),views.armadura(),views.cliente()];
 for(const html of screens){
   assert.doesNotMatch(html,/Protocolo TS|SEQUENCIADOR|EDITAR EMOTES|ABRIR SEQUENCIADOR|Emotes reais/i);
   assert.doesNotMatch(html,/data-(?:evo|rp)="(?:open-sequences|open-emotes|ts-open)"/);
 }
 assert.doesNotMatch(upgrade,/ts-open|ts-choice|ts-reveal|ts-reset|\btsEvents\b/);
 assert.doesNotMatch(evolution,/rpEmotesForm|rpNewSequence|rpNewStep|triggerEmote|seq-run/);
 assert.doesNotMatch(client,/Emotes reais/);
 assert.ok(views.emergencia(),"O protocolo de emergência deve continuar funcionando");
 assert.ok(views.rituais(),"Rituais devem continuar funcionando");
 assert.ok(views.eventos(),"O painel de eventos deve continuar funcionando");
});

test("Afinidades navigation opens a real page with fields, without losing old records",()=>{
 const index=pageScript("index.html"),app=pageScript("app.js");
 assert.ok(index.includes('data-page="afinidade"'),"Affinity button is visible");
 assert.ok(index.includes('src="/affinity.js?v='),"Affinity module is loaded");
 assert.ok(app.includes('afinidade:"AFINIDADES"'),"Router recognizes affinity route");
 assert.ok(app.includes("NexusRPAffinityUpgrade(rp,views)"),"Page module is registered");
 const views=boot("client");
 assert.equal(typeof views.afinidade,"function","No blank fallback page");
 const html=views.afinidade();
 for(const field of ['name="player"','name="nickname"','name="kinship"',
   'name="race"','name="score"'])
   assert.ok(html.includes(field),"Missing affinity field "+field);
 assert.ok(html.includes("FICHAS REGISTRADAS"));
});
