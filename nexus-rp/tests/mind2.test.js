"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
function harness(){
 const relay=createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="SECRET",limit:()=>true,
 });
 async function call(method,url,headers={},payload){
  const request=Readable.from(payload===undefined?[]:[Buffer.from(JSON.stringify(payload))]);
  request.method=method;request.headers=headers;
  const response={status:0,payload:null};const u=new URL(url,"https://test.invalid");
  await relay.handle(request,response,u.pathname,u);
  return response;
 }
 return call;
}
test("Memory & voice opt-in settings require owner authorization and consent to share",async()=>{
 const call=harness(),owner={"x-nexus-key":"SECRET"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const settings={social:{diaryEnabled:true,protectionEnabled:true,sharePanel:true},
   speaker:{enabled:true,voice:"Kore"}};
 assert.equal((await call("POST","/api/client/mind/settings",client,settings)).status,401);
 assert.equal((await call("POST","/api/client/mind/settings",owner,settings)).status,422);
 const queued=await call("POST","/api/client/mind/settings",owner,{...settings,consentConfirmed:true});
 assert.equal(queued.status,200);
 const tasks=await call("GET","/api/client/poll",client);
 assert.equal(tasks.payload.jobs[0].type,"mind_settings");
 assert.equal(tasks.payload.jobs[0].payload.speaker.voice,"Kore");
 assert.equal((await call("POST","/api/client/ack",client,{id:queued.payload.jobId,success:true})).status,200);
});
test("Social journal and relationships only leave the mod with explicit share on",async()=>{
 const call=harness(),owner={"x-nexus-key":"SECRET"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const base={ready:true,listening:true,memory:true,answer:true,sensitivity:55,
  allowedPlayers:["FriendA"],memoryCount:21,queued:0,inFlight:0,
  speaker:{enabled:false,voice:"auto"},social:{
   diaryEnabled:true,protectionEnabled:true,sharePanel:false,
   factCount:2,diaryCount:1,relationCount:1,
   facts:[{id:12,at:1794561000000,speaker:"FriendA",kind:"projeto",text:"Oficina."}],
   diary:[{id:13,at:1794561000000,speaker:"FriendA",kind:"projeto",text:"Diário privado"}],
   relations:[{player:"FriendA",interactions:3,familiarity:3,affinity:2}]
  }};
 assert.equal((await call("POST","/api/client/hearing/state",client,base)).status,200);
 let view=(await call("GET","/api/client/hearing",owner)).payload;
 assert.equal(view.state.social.factCount,2);
 assert.deepEqual(view.state.social.diary,[]);
 assert.deepEqual(view.state.social.facts,[]);
 assert.equal((await call("POST","/api/client/hearing/fact",owner,{action:"delete",id:12})).status,403);
 base.social.sharePanel=true;
 assert.equal((await call("POST","/api/client/hearing/state",client,base)).status,200);
 view=(await call("GET","/api/client/hearing",owner)).payload;
 assert.equal(view.state.social.diary[0].text,"Diário privado");
 assert.equal(view.state.social.facts[0].speaker,"FriendA");
 const job=await call("POST","/api/client/hearing/fact",owner,{
   action:"correct",id:12,text:"Oficina começou em novembro."});
 assert.equal(job.status,200);
 assert.equal((await call("POST","/api/client/hearing/fact",owner,{
   action:"clear"})).status,422);
 assert.equal((await call("POST","/api/client/hearing/fact",owner,{
   action:"forget_player",player:"FriendA"})).status,200);
});
