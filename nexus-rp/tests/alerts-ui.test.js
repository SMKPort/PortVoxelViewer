"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const fs=require("node:fs"),vm=require("node:vm"),path=require("node:path");
const code=fs.readFileSync(path.join(__dirname,"../public/alerts.js"),"utf8");
test("Alerts panel waits for real client settings and escapes synced history",async()=>{
 const listeners={};let pulse=()=>{},renders=0;
 const window={location:{hash:"#alertas"},addEventListener(){}};
 const nodes={
  "#content":{addEventListener:(name,fn)=>{listeners[name]=fn;}}
 };
 const document={visibilityState:"visible",querySelector:name=>nodes[name]||null,
   addEventListener(){}};
 const rp={escape:x=>String(x??"").replace(/[&<>"']/g,c=>({
  "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"
 })[c]),date:x=>String(x),rerender(){renders++;},toast(){},
 getAccess:()=>"secret-not-in-html",clientQueue:async()=>({ok:true})};
 const result={ok:true,online:true,settings:{heavy:false,streak:true,structure:true,
  risk:true,recovery:false,critical:true,cooldownSeconds:65},
  history:[{kind:"heavy",text:"<script>alert(1)</script>",at:"2026-11-12T19:00:00.000Z"}],
  syncedAt:"2026-11-12T19:00:00.000Z"};
 const context=vm.createContext({window,document,console,setTimeout(){},
 setInterval(fn){pulse=fn;},fetch:async()=>({ok:true,json:async()=>result}),AbortSignal,
 FormData});
 vm.runInContext(code,context);
 const view=window.NexusRPAlertsUpgrade(rp,{});
 assert.match(view.alertas(),/name="cooldownSeconds"/);
 assert.match(view.alertas(),/type="checkbox"/);
 assert.match(view.alertas(),/SALVAR ALERTAS NO MOD/);
 assert.match(view.alertas(),/disabled/);
 pulse();await new Promise(done=>setImmediate(done));
 assert.equal(renders,1);
 const live=view.alertas();
 assert.ok(live.includes('name="heavy"'));
 assert.ok(!live.includes('name="heavy" type="checkbox" checked'));
 assert.ok(live.includes('name="streak" type="checkbox" checked'));
 assert.ok(live.includes('value="65"'));
 assert.ok(!live.includes("secret-not-in-html"));
 assert.ok(live.includes("&lt;script&gt;alert(1)&lt;/script&gt;"));
 assert.ok(!live.includes("<script>"));
 assert.ok(!live.includes(" disabled"));
});
