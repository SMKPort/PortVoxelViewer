"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createBridge}=require("../bridge.js");

process.env.NEXUS_RP_AGENT_KEY="unit-test-agent-key-with-40-chars-safe";
const bridge=createBridge({
  send(res,status,payload){res.status=status;res.payload=payload;res.end();},
  verifyUser:req=>req.headers["x-nexus-key"]==="user-test-access"
});
async function call(method,path,headers={},payload=null){
  const req=Readable.from(payload===null?[]:[Buffer.from(JSON.stringify(payload))]);
  req.method=method;req.headers=headers;
  const res={status:0,payload:null,end(){}};
  const handled=await bridge.handle(req,res,new URL(path,"https://example.com").pathname,new URL(path,"https://example.com"));
  return {handled,...res};
}
test("Unauthenticated user cannot inspect events",async()=>{
  const r=await call("GET","/api/game/events");
  assert.equal(r.handled,true);assert.equal(r.status,401);
});
test("Unauthenticated agent cannot push fake events",async()=>{
  const r=await call("POST","/api/bridge/event",{}, {type:"damage",player:"Port",amount:7});
  assert.equal(r.status,401);
});
test("Agent can send event and user can receive it",async()=>{
  const agent={"x-nexus-agent":process.env.NEXUS_RP_AGENT_KEY};
  let r=await call("POST","/api/bridge/event",agent,{type:"damage",player:"Port",amount:3,health:17});
  assert.equal(r.status,200);assert.equal(r.payload.eventId,1);
  r=await call("GET","/api/game/events?since=0&player=Port",{"x-nexus-key":"user-test-access"});
  assert.equal(r.status,200);assert.equal(r.payload.events.length,1);
  assert.equal(r.payload.events[0].amount,3);
});
test("Invalid event is rejected",async()=>{
  const r=await call("POST","/api/bridge/event",{"x-nexus-agent":process.env.NEXUS_RP_AGENT_KEY},{type:"arbitrary_command",player:"Port",amount:1});
  assert.equal(r.status,422);
});
test("Job queue requests agent acknowledgement",async()=>{
  const queued=bridge.enqueue('tellraw @a [{"text":"Teste"}]');
  const agent={"x-nexus-agent":process.env.NEXUS_RP_AGENT_KEY};
  let r=await call("GET","/api/bridge/jobs",agent);
  assert.equal(r.status,200);
  assert.equal(r.payload.jobs.length,1);
  assert.equal(r.payload.jobs[0].id,queued.jobId);
  r=await call("POST","/api/bridge/ack",agent,{jobId:queued.jobId,ok:true,output:"Comando executado"});
  assert.equal(r.status,200);
  r=await call("GET","/api/bridge/result?id="+queued.jobId,{"x-nexus-key":"user-test-access"});
  assert.equal(r.status,200);assert.equal(r.payload.done,true);assert.equal(r.payload.success,true);
});
