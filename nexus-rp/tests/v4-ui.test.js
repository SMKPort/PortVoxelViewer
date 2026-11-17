"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const fs=require("node:fs");
const vm=require("node:vm");
const path=require("node:path");
const root=path.join(__dirname,"../public");
const read=file=>fs.readFileSync(path.join(root,file),"utf8");
const escape=value=>String(value??"").replace(/[&<>"']/g,c=>({
  "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"
})[c]);

function overview(connected=false,logText="SISTEMA OK"){
  const context=vm.createContext({window:{}});
  vm.runInContext(read("views.js"),context,{filename:"views.js"});
  return context.window.NexusRPViews({
    state:{
      integrity:87,energy:64,armor:"integrada",form:"Celestial",
      logs:[{kind:"TESTE",at:"2026-11-17T12:00:00Z",text:logText}],
      chat:[],rituals:[],selectedRitual:"bencao",config:{
        connectionMode:"client",prefix:"[Nexus Aterna]",chatChannel:"local",
        chatFormatting:"&6&l",functions:{}
      }
    },escape,date:()=> "17/11",presets:[],
    isConnected:()=>connected
  }).painel();
}

test("4.0 keeps core navigation and offers an accessible search dialog",()=>{
  const html=read("index.html");
  assert.match(html,/NEXUS ATERNA 4\.0/);
  assert.match(html,/v4-shell\.css\?v=4\.0\.[0-9]+/);
  assert.match(html,/v4-shell\.js\?v=4\.0\.[0-9]+/);
  assert.match(html,/id="v4CommandDialog"[^>]*role="dialog"/);
  assert.match(html,/id="v4CommandInput"/);
  assert.match(html,/id="v4QuickLaunch"/);
  assert.match(html,/class="v4-mobile-dock"/);
  for(const page of ["painel","voz","chat","cliente","config","armadura","rituais","alertas","mente"])
    assert.ok(html.includes('data-page="'+page+'"'),"Retain navigation "+page);
  assert.ok(!html.includes("name=\"access\" value="),"No HTTPS secrets in markup");
});

test("Nexus 4.0 overview exposes only local roleplay metrics and in-game shortcuts",()=>{
  const html=overview(false);
  assert.ok(html.includes('class="v4-hero"'));
  assert.ok(html.includes("Inteligência")||html.includes("próximo estágio"));
  assert.ok(html.includes("TELEMETRIA NARRATIVA"));
  assert.ok(html.includes("INTEGRIDADE RP"));
  assert.ok(html.includes("ENERGIA ARCANA RP"));
  assert.ok(html.includes("Não verificada nesta sessão"));
  assert.ok(html.includes("Os indicadores de energia e integridade"));
  for(const cmd of [
    "/nexusclient situacao","/nexusclient cognition menu",
    "/nexusclient journal menu","/nexusclient cognition insights status"
  ])assert.ok(html.includes('data-v4-copy="'+cmd+'"'));
  assert.ok(!html.includes("Minecraft RCON"),"No stale mode label in Client mode");
  assert.ok(!html.includes("ENTREGUE AO MINECRAFT"),"No fabricated transmission");
  const connected=overview(true);
  assert.ok(connected.includes("ONLINE"));
  assert.ok(connected.includes("Nexus Client verificado"));
});

test("dynamic narrative log text remains escaped in the new overview",()=>{
  const html=overview(false,'<img src=x onerror=alert(1)>');
  assert.ok(!html.includes('<img src=x'));
  assert.ok(html.includes("&lt;img src=x"));
});

test("command palette navigates only declared sections and copies client commands",()=>{
  const source=read("v4-shell.js");
  assert.ok(source.includes('location.hash="#"+item.route'));
  assert.ok(source.includes('navigator.clipboard.writeText(text)'));
  assert.ok(source.includes('event.target.closest("[data-v4-copy]")'));
  assert.ok(source.includes('if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==="k")'));
  assert.ok(source.includes('if(e.key==="Escape"&&!dialog.hidden)'));
  assert.ok(source.includes('window.addEventListener("hashchange",sync)'));
  assert.ok(!source.includes("X-Nexus-Key"),"Quick launcher never handles HTTP secrets");
  assert.ok(!source.includes("fetch("),"Quick launcher must not send network requests");
  assert.ok(!source.includes("localStorage"),"Quick launcher must not persist secrets");
});

test("responsive shell retains reduced-motion and mobile navigation",()=>{
  const css=read("v4-shell.css");
  assert.ok(css.includes("@media(max-width:820px)"));
  assert.ok(css.includes("@media(max-width:600px)"));
  assert.ok(css.includes("prefers-reduced-motion:reduce"));
  assert.ok(css.includes(".v4-mobile-dock"));
  assert.ok(css.includes(".v4-dialog[hidden]{display:none}"));
});
