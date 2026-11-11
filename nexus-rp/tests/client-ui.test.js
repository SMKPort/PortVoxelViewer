"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const fs=require("node:fs");
const vm=require("node:vm");
const path=require("node:path");
const root=path.join(__dirname,"../public");
const pageScript=file=>fs.readFileSync(path.join(root,file),"utf8");
function boot(mode){
 const window={};
 const document={
   querySelector:selector=>selector==="#nav"?{querySelector:()=>({before(){}})}:
     selector==="#content"?{addEventListener(){}}:null,
   createElement:()=>({dataset:{}})
 };
 const context=vm.createContext({window,document,setInterval:()=>0});
 for(const file of ["views.js","upgrade.js","evolution.js","client.js"]){
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
   getAccess:()=>"",requestEvents:async()=>({events:[],lastId:0})
 };
 let views=window.NexusRPViews(rp);
 views=window.NexusRPUpgrade(rp,views);
 views=window.NexusRPEvolution(rp,views);
 views=window.NexusRPClientUpgrade(rp,views);
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
