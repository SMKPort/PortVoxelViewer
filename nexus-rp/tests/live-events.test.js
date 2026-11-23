"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const fs=require("node:fs"),vm=require("node:vm"),path=require("node:path");
const source=fs.readFileSync(path.join(__dirname,"../public/evolution.js"),"utf8");

test("Event feed remains client-only, deduplicated and sanitized without removed armor RP",async()=>{
 const location={hash:"#eventos"};
 const timers=[],nodes={},listener={};
 const document={
   visibilityState:"visible",
   querySelector:selector=>nodes[selector]||null,
   addEventListener:(name,fn)=>{listener[name]=fn;}
 };
 const window={location,addEventListener:(name,fn)=>{listener[name]=fn;}};
 nodes["#content"]={addEventListener(){}};
 for(const name of ["#rpEventsFeed","#rpEventsStatus","#rpEventsSync"])
   nodes[name]={innerHTML:"",textContent:""};
 const events=[{id:1,type:"health",player:"Port",amount:0,
   health:20,note:"Saúde atual",at:"2026-11-23"}];
 const state={config:{connectionMode:"client",player:"Port"},
   integrity:100,energy:85,segments:[{integrity:100,integrated:true}]};
 let saved=0;
 const rp={state,escape:v=>String(v??"").replace(/</g,"&lt;").replace(/>/g,"&gt;"),
   requestEvents:async since=>({online:true,lastId:events.at(-1).id,
     events:events.filter(e=>e.id>since)}),save(){saved++;}};
 const context=vm.createContext({window,document,location,console,Date,
   setInterval:(fn,ms)=>timers.push({fn,ms}),setTimeout(){}});
 vm.runInContext(source,context);
 const views=window.NexusRPEvolution(rp,{});
 assert.ok(views.eventos().includes('id="rpEventsFeed"'));
 assert.ok(!views.eventos().includes("healIntegrity"));
 assert.ok(!views.eventos().includes("armadura"));
 assert.equal(views.emergencia,undefined);
 const pulse=timers.find(t=>t.ms===2000);
 assert.ok(pulse);
 const tick=async()=>{pulse.fn();await new Promise(r=>setImmediate(r));};
 await tick();
 assert.equal(state.gameEvents.feed.length,1);
 assert.match(nodes["#rpEventsFeed"].innerHTML,/Leitura de vida/);
 events.push({id:2,type:"damage",player:"Port",amount:3,
   note:"<img src=x>",at:"2026-11-23"});
 await tick();
 assert.equal(state.gameEvents.feed.length,2);
 assert.match(nodes["#rpEventsFeed"].innerHTML,/Dano recebido/);
 assert.match(nodes["#rpEventsFeed"].innerHTML,/&lt;img src=x&gt;/);
 assert.ok(!nodes["#rpEventsFeed"].innerHTML.includes("<img"));
 assert.equal(state.integrity,100);
 assert.equal(state.segments[0].integrity,100);
 await tick();
 assert.equal(state.gameEvents.feed.length,2);
 assert.ok(saved>=2);
 document.visibilityState="hidden";
 events.push({id:3,type:"heal",player:"Port",amount:2,note:"cura",at:"2026-11-23"});
 await tick();
 assert.equal(state.gameEvents.feed.length,2,"Paused while backgrounded");
});
