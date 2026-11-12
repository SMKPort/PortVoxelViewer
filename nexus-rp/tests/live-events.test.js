"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const fs=require("node:fs"),vm=require("node:vm"),path=require("node:path");
const code=fs.readFileSync(path.join(__dirname,"../public/evolution.js"),"utf8");
test("Events auto-refresh without buttons, preserving RP changes opt-in",async()=>{
 const timers=[],nodes={},listeners={};
 const document={visibilityState:"visible",querySelector:selector=>nodes[selector]||null,
   addEventListener:(name,fn)=>listeners[name]=fn};
 const window={location:{hash:"#eventos"},addEventListener:(name,fn)=>listeners[name]=fn};
 nodes["#content"]={addEventListener(){}};
 for(const id of ["#rpEventsFeed","#rpEventsStatus","#rpEventsSync"])nodes[id]={innerHTML:"",textContent:""};
 let events=[{id:1,type:"health",player:"Port",amount:0,health:20,note:"Vida",at:"2026-11-12"}],renders=0;
 const state={config:{connectionMode:"client",player:"Port"},integrity:100,energy:85,
   segments:[{integrity:100,integrated:true}],gameEvents:{enabled:false,lastId:0,bootstrapped:false,damageFactor:5,healEnergy:3,cursor:0,feed:[],lastSync:"",error:""}};
 const rp={state,escape:x=>String(x??""),date:x=>String(x),getAccess:()=>"test-key",
   requestEvents:async since=>({online:true,lastSeen:"now",lastId:events.at(-1)?.id||0,events:events.filter(e=>e.id>since)}),
   rerender(){renders++;},save(){},log(){},toast(){},navigate(){}};
 const context=vm.createContext({window,document,setInterval:(fn,ms)=>timers.push({fn,ms}),setTimeout(){},console,Date});
 vm.runInContext(code,context);
 const views=window.NexusRPEvolution(rp,{armadura:()=>"",ao_vivo:()=>"",config:()=>""});
 assert.ok(views.eventos().includes('id="rpEventsFeed"'));
 assert.ok(!views.eventos().includes("ATUALIZAR EVENTOS"));
 const pulse=timers.find(t=>t.ms===2000);
 assert.ok(pulse);
 const tick=async()=>{pulse.fn();await new Promise(r=>setImmediate(r));};
 await tick();
 assert.equal(state.gameEvents.feed.length,1);
 assert.match(nodes["#rpEventsFeed"].innerHTML,/Leitura de vida/);
 assert.equal(renders,0);
 events.push({id:2,type:"damage",player:"Port",amount:2,health:18,note:"Dano",at:"2026-11-12"});
 await tick();
 assert.equal(state.gameEvents.feed.length,2);
 assert.equal(state.integrity,100,"Monitoring alone must not modify the RP");
 assert.match(nodes["#rpEventsFeed"].innerHTML,/Dano recebido/);
 await tick();assert.equal(state.gameEvents.feed.length,2,"No duplicate events");
 document.visibilityState="hidden";await tick();assert.equal(state.gameEvents.feed.length,2);
});
