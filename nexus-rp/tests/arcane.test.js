"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
function harness(){
 const relay=createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="OWNER",
  limit:()=>true
 });
 return async function call(method,path,headers={},data){
  const req=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
  req.headers=headers;req.method=method;
  const res={status:0,payload:null};
  const parsed=new URL(path,"https://test.invalid");
  await relay.handle(req,res,parsed.pathname,parsed);
  return res;
 };
}
test("Retired magic detector refuses requests from old clients without starting jobs",async()=>{
 const call=harness(),owner={"x-nexus-key":"OWNER"};
 const pair=await call("POST","/api/client/pair/new",owner,{});
 const token=(await call("POST","/api/client/pair/exchange",{},{
  code:pair.payload.code,player:"Port"
 })).payload.token;
 const client={"x-nexus-client":token};
 for(const [method,route,auth] of [
  ["GET","/api/client/arcane",owner],
  ["POST","/api/client/arcane/settings",owner],
  ["POST","/api/client/arcane/state",client]
 ]){
  const res=await call(method,route,auth,{
   enabled:true,reactions:true,scanNearby:true,events:[]
  });
  assert.equal(res.status,410,route);
  assert.equal(res.payload.kind,"magic_detector_retired",route);
  assert.match(res.payload.error,/removid/i);
 }
 const queue=await call("GET","/api/client/poll",client);
 assert.equal(queue.status,200);
 assert.equal(queue.payload.jobs.length,0,"No old magic setting can be queued");
});
test("Armor telemetry and damage alerts survive magic detector retirement",()=>{
 const {readFileSync}=require("node:fs");
 const path=require("node:path");
 const client=readFileSync(path.resolve(__dirname,"../../nexus-client/src/main/java/com/port/nexusclient/ClientLink.java"),"utf8");
 assert.match(client,/NexusVitals\.status\(\)/);
 assert.match(client,/sendTelemetry\(mc\.player\)/);
 assert.doesNotMatch(client,/NexusArcaneSensor/);
 const server=readFileSync(path.resolve(__dirname,"../client_relay.js"),"utf8");
 assert.match(server,/\/api\/client\/monitor\/state/);
 assert.match(server,/\/api\/client\/alerts\/state/);
});
