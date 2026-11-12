"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
function harness(){
 const relay=createClientRelay({
   send:(res,status,payload)=>{res.status=status;res.payload=payload;},
   verifyUser:req=>req.headers["x-nexus-key"]==="panel-key",
   limit:()=>true
 });
 const req=async(method,path,headers={},data)=>{
   const request=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
   request.method=method;request.headers=headers;
   const result={status:0,payload:null};
   await relay.handle(request,result,new URL(path,"https://example.test").pathname,new URL(path,"https://example.test"));
   return result;
 };
 return req;
}
test("Client-owned monitor configuration and state are authenticated",async()=>{
 const req=harness();
 const panel={"x-nexus-key":"panel-key"};
 const pair=await req("POST","/api/client/pair/new",panel,{});
 const token=(await req("POST","/api/client/pair/exchange",{}, {code:pair.payload.code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const config={enabled:true,damageFactor:5,healIntegrity:5,healEnergy:3,
   integrity:80,energy:50,cursor:0,segments:[{key:"arm",integrity:80,integrated:true}]};
 assert.equal((await req("POST","/api/client/monitor/settings",{},config)).status,401);
 assert.equal((await req("POST","/api/client/monitor/state",panel,{managed:true,enabled:true})).status,401);
 const job=await req("POST","/api/client/monitor/settings",panel,config);
 assert.equal(job.status,200);
 const poll=await req("GET","/api/client/poll",client);
 assert.equal(poll.payload.jobs.length,1);
 assert.equal(poll.payload.jobs[0].type,"monitor_settings");
 assert.equal(poll.payload.jobs[0].payload.healIntegrity,5);
 assert.equal((await req("POST","/api/client/ack",client,{id:job.payload.jobId,success:true,output:"Salvo"})).status,200);
 const state=await req("POST","/api/client/monitor/state",client,{managed:true,enabled:true,
   integrity:85,energy:53,lastEvent:"heal",updatedAt:1730000000000,
   segments:[{key:"arm",integrity:85,integrated:true}],damageFactor:5,healIntegrity:5,healEnergy:3});
 assert.equal(state.status,200);
 const feed=await req("GET","/api/client/events?since=0",panel);
 assert.equal(feed.payload.monitor.managed,true);
 assert.equal(feed.payload.monitor.integrity,85);
 assert.equal(feed.payload.monitor.source,"minecraft-client");
 assert.ok(feed.payload.monitor.receivedAt);
});
test("Pairing again resets any monitor snapshot from the old client",async()=>{
 const req=harness(),panel={"x-nexus-key":"panel-key"};
 const pair=await req("POST","/api/client/pair/new",panel,{});
 const token=(await req("POST","/api/client/pair/exchange",{}, {code:pair.payload.code,player:"Port"})).payload.token;
 await req("POST","/api/client/monitor/state",{"x-nexus-client":token},{managed:true,enabled:false,integrity:67});
 assert.equal((await req("GET","/api/client/events",panel)).payload.monitor.integrity,67);
 const next=await req("POST","/api/client/pair/new",panel,{});
 await req("POST","/api/client/pair/exchange",{}, {code:next.payload.code,player:"Another"});
 assert.equal((await req("GET","/api/client/events",panel)).payload.monitor,null);
});
