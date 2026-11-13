"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),fs=require("node:fs");
const vm=require("node:vm"),path=require("node:path");
const code=fs.readFileSync(path.join(__dirname,"../public/mind2.js"),"utf8");
const escapeHtml=x=>String(x??"").replace(/[&<>"']/g,c=>({
 "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"
})[c]);
test("Mind 2.0 view hides private memories and renders consented text safely",async()=>{
 let refresh,updates=0;const listeners={};const content={
  addEventListener(name,handler){listeners[name]=handler;}
 };
 const document={visibilityState:"visible",querySelector:q=>q==="#content"?content:null,
   addEventListener(){}};
 const window={addEventListener(){},prompt:()=>null,confirm:()=>false};
 const location={hash:"#mente"};
 const payload={ok:true,online:true,syncedAt:"2026-11-13T11:00:00Z",state:{
  social:{diaryEnabled:true,protectionEnabled:true,sharePanel:false,
   factCount:1,diaryCount:1,relationCount:1},
  speaker:{enabled:true,voice:"Kore"}
 }};
 const rp={escape:escapeHtml,date:x=>String(x),getAccess:()=>"SECRET_MUST_NEVER_APPEAR",
  rerender(){updates++;},toast(){},clientQueue:async()=>({ok:true})};
 const context=vm.createContext({window,document,location,console,AbortSignal,FormData,
  fetch:async()=>({ok:true,json:async()=>payload}),setInterval(fn){refresh=fn;},
  setTimeout(fn){},Date});
 vm.runInContext(code,context);
 const views=window.NexusRPMind2Upgrade(rp,{});
 assert.ok(views.mente().includes("Nexus Mind 2.0"));
 assert.ok(!views.mente().includes("SECRET_MUST_NEVER_APPEAR"));
 assert.ok(views.mente().includes("privado"));
 await refresh();
 await new Promise(done=>setImmediate(done));
 assert.ok(updates>=1);
 assert.ok(!views.mente().includes("Segredo não autorizado"));
 payload.state.social={diaryEnabled:true,protectionEnabled:true,sharePanel:true,
  factCount:1,diaryCount:1,relationCount:1,
  facts:[{id:7,at:1794570000000,speaker:"FriendA",kind:"projeto",
    text:"<script>alert(1)</script>",corrected:false}],
  diary:[{id:8,at:1794570000000,speaker:"FriendA",kind:"projeto",
    text:"Hoje ouvi a palavra <script>."}],
  relations:[{player:"FriendA",interactions:5,familiarity:5,affinity:0}]};
 await refresh();await new Promise(done=>setImmediate(done));
 const rendered=views.mente();
 assert.ok(rendered.includes("&lt;script&gt;"));
 assert.ok(!rendered.includes("<script>"));
 assert.ok(rendered.includes('data-memory-id="7"'));
 assert.ok(rendered.includes("APAGAR REGISTROS DESTE JOGADOR"));
});
