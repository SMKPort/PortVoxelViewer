"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const fs=require("node:fs");
const vm=require("node:vm");
const path=require("node:path");
const root=path.join(__dirname,"../public");
const source=name=>fs.readFileSync(path.join(root,name),"utf8");
function boot(mode="client",remembered=false){
 const location={hash:"#painel"};
 const window={location,addEventListener(){}};
 const nav={querySelector:()=>({before(){}})};
 const document={
   visibilityState:"visible",
   querySelector:selector=>selector==="#nav"?nav:
     selector==="#content"?{addEventListener(){}}:null,
   createElement:()=>({dataset:{}}),addEventListener(){}
 };
 const context=vm.createContext({window,document,location,setInterval(){},setTimeout(){}});
 for(const file of ["views.js","evolution.js","client.js","affinity.js"]){
   vm.runInContext(source(file),context,{filename:file});
 }
 const state={integrity:100,energy:85,armor:"integrada",form:"Celestial",
   logs:[{kind:"TESTE",text:"ok",at:"2026-11-23"}],
   chat:[],rituals:[],affinities:[],config:{
     connectionMode:mode,chatChannel:"custom",chatCustomCommand:"/f chat privado",
     chatFormatting:"&6&l",chatBodyFormatting:"&f",chatUsePrefix:true,
     prefix:"[Nexus Aterna]",host:"",port:25575,player:"Port",
     target:"global",autoAnnounce:true,functions:{},color:"white"
   }};
 const escape=value=>String(value??"").replace(/[&<>"']/g,ch=>({
   "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"
 }[ch]));
 const rp={state,escape,date:String,presets:[],
   isConnected:()=>false,isHealthy:()=>true,save(){},log(){},toast(){},
   navigate(){},rerender(){},sendToChat(){},
   getAccess:()=>remembered?"SENSITIVE_TEST_SECRET_NOT_FOR_HTML":"",
   isAccessRemembered:()=>remembered,requestEvents:async()=>({events:[],lastId:0})};
 let views=window.NexusRPViews(rp);
 views=window.NexusRPEvolution(rp,views);
 views=window.NexusRPClientUpgrade(rp,views);
 views=window.NexusRPAffinityUpgrade(rp,views);
 return {views,rp};
}
test("Client-only config retains mode, custom channel, independent styling and security",()=>{
 const {views}=boot(),html=views.config();
 assert.match(html,/name="connectionMode" value="client"/);
 assert.match(html,/name="chatCustomCommand"/);
 assert.match(html,/name="chatBodyFormatting"/);
 assert.match(html,/f chat privado/);
 assert.ok(html.includes('name="access"'));
 assert.ok(!html.includes('name="password"'));
 assert.ok(!html.includes('name="port"'));
 assert.ok(!html.includes("SENSITIVE_TEST_SECRET_NOT_FOR_HTML"));
 const client=views.cliente();
 assert.ok(client.includes("GERAR CÓDIGO DE PAREAMENTO"));
 assert.ok(client.includes("DIAGNÓSTICO DA API"));
 for(const command of [
   "/nexusclient situacao","/nexusclient awakening status",
   "/nexusclient awakening journal recent","/nexusclient macro status"
 ])assert.ok(client.includes('data-v4-copy="'+command+'"'));
});
test("Custom chat settings sync to mod and voice follows browser configuration",()=>{
 const c=source("client.js"),v=source("voice.js"),app=source("app.js");
 assert.match(c,/rp\.syncChatRouting=async/);
 assert.ok(c.includes('queue("/chat/settings"'));
 assert.match(app,/rp\.syncChatRouting\(\)/);
 assert.match(v,/channel,command:config\.chatCustomCommand/);
 assert.match(v,/bodyFormat:config\.chatBodyFormatting/);
 assert.match(app,/selectedMode==null\?c\.connectionMode/);
 assert.ok(!app.includes('c.connectionMode=["agent","client"].includes(d.get("connectionMode"))'));
});
test("Retired website pages and legacy links cannot be opened",()=>{
 const {views}=boot();
 const html=source("index.html"),app=source("app.js"),palette=source("v4-shell.js");
 for(const page of ["armadura","rituais","ao_vivo","emergencia"]){
   assert.ok(!html.includes('data-page="'+page+'"'));
   assert.ok(!app.includes(page+':"'));
   assert.equal(Object.hasOwn(views,page),false);
   assert.ok(!palette.includes('route:"'+page+'"'));
 }
 assert.ok(!html.includes('src="/upgrade.js'));
 for(const page of ["painel","voz","chat","cliente","config","alertas","mente","afinidade"])
   assert.ok(html.includes('data-page="'+page+'"'));
 assert.ok(views.eventos().includes("Eventos do Jogo"));
});
test("Affinity browser remains available and keeps all desired fields",()=>{
 const {views}=boot();
 const html=views.afinidade();
 for(const name of ["player","nickname","kinship","race","score"])
   assert.ok(html.includes('name="'+name+'"'));
 assert.ok(source("index.html").includes('src="/affinity.js?v='));
});
test("Remembered HTTPS key does not appear in HTML",()=>{
 const plain=boot().views.config();
 const saved=boot("client",true).views.config();
 assert.ok(!plain.includes("SENSITIVE_TEST_SECRET_NOT_FOR_HTML"));
 assert.ok(!saved.includes("SENSITIVE_TEST_SECRET_NOT_FOR_HTML"));
 assert.ok(saved.includes("ESQUECER CHAVE SALVA"));
});
