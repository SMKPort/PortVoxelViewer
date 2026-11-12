"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
function harness(){
 const relay=createClientRelay({send:(r,code,data)=>{r.code=code;r.payload=data;},
   verifyUser:req=>req.headers["x-nexus-key"]==="panel-test-key",limit:()=>true});
 return async function call(method,endpoint,headers={},body){
   const url=new URL(endpoint,"https://nexus.test");
   const request=Readable.from(body===undefined?[]:[Buffer.from(JSON.stringify(body))]);
   request.method=method;request.headers=headers;
   const response={code:0,payload:null};
   await relay.handle(request,response,url.pathname,url);
   return response;
 };
}
const prefs={heavy:true,streak:false,structure:true,risk:true,
  recovery:false,critical:true,cooldownSeconds:75};
test("Alerts settings require panel key, are queued, and history requires client token",async()=>{
 const call=harness(),user={"x-nexus-key":"panel-test-key"};
 assert.equal((await call("GET","/api/client/alerts")).code,401);
 assert.equal((await call("POST","/api/client/alerts/settings",{},prefs)).code,401);
 assert.equal((await call("POST","/api/client/alerts/state",user,{...prefs,history:[]})).code,401);
 const code=(await call("POST","/api/client/pair/new",user,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const initial=await call("GET","/api/client/alerts",user);
 assert.equal(initial.code,200);assert.equal(initial.payload.settings,null);
 const queued=await call("POST","/api/client/alerts/settings",user,prefs);
 assert.equal(queued.code,200);
 const jobs=await call("GET","/api/client/poll",client);
 assert.equal(jobs.payload.jobs[0].type,"alerts_settings");
 assert.deepEqual(jobs.payload.jobs[0].payload,prefs);
 assert.equal((await call("POST","/api/client/ack",client,
   {id:queued.payload.jobId,success:true,output:"OK"})).code,200);
 const sync=await call("POST","/api/client/alerts/state",client,{
   ...prefs,history:[{kind:"heavy",text:"Port, recomendo recuar.",at:"2026-11-12T18:00:00Z"},
    {kind:"mind",text:"Estou aprendendo.",at:"2026-11-12T18:01:00Z"}]
 });
 assert.equal(sync.code,200);
 const view=await call("GET","/api/client/alerts",user);
 assert.equal(view.payload.settings.cooldownSeconds,75);
 assert.equal(view.payload.history.length,2);
 assert.equal(view.payload.source,"minecraft-client");
 assert.ok(view.payload.syncedAt);
});
test("Reject malformed settings and invalid history; new pair clears previous state",async()=>{
 const call=harness(),user={"x-nexus-key":"panel-test-key"};
 const pair=await call("POST","/api/client/pair/new",user,{});
 const token=(await call("POST","/api/client/pair/exchange",{},
   {code:pair.payload.code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 for(const bad of [{...prefs,cooldownSeconds:1},{...prefs,cooldownSeconds:999},
   {...prefs,heavy:"true"},{...prefs,critical:null}]){
   assert.equal((await call("POST","/api/client/alerts/settings",user,bad)).code,422);
 }
 assert.equal((await call("POST","/api/client/alerts/state",client,{
   ...prefs,history:[{kind:"heavy",text:"<script>",at:"garbage"}]
 })).code,422);
 assert.equal((await call("POST","/api/client/alerts/state",client,
   {...prefs,history:[]})).code,200);
 const next=await call("POST","/api/client/pair/new",user,{});
 await call("POST","/api/client/pair/exchange",{},
   {code:next.payload.code,player:"Other"});
 assert.equal((await call("GET","/api/client/alerts",user)).payload.history.length,0);
 assert.equal((await call("GET","/api/client/alerts",user)).payload.settings,null);
});
