"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
const {validateCustomCommand}=require("../chat_channel.js");
function harness(){
 const relay=createClientRelay({
   send:(res,status,payload)=>{res.status=status;res.payload=payload;},
   verifyUser:req=>req.headers["x-nexus-key"]==="panel-testing",limit:()=>true
 });
 return async function call(method,endpoint,headers={},data){
   const url=new URL(endpoint,"https://nexus.test");
   const request=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
   request.method=method;request.headers=headers;
   const res={status:0,payload:null};
   await relay.handle(request,res,url.pathname,url);
   return res;
 };
}
const route={
 channel:"custom",command:"/f chat privado roleplay mensagens",
 format:"&6&l",bodyFormat:"&f",announce:true
};
test("Owner command accepts five fixed tokens, not a channel whitelist",()=>{
 assert.equal(validateCustomCommand(route.command),"f chat privado roleplay mensagens");
 for(const blocked of ["/execute run say","/op","/minecraft:op","/say",
   "/f chat @a","/f;stop","/f chat && stop","/f chat $placeholder"])
   assert.throws(()=>validateCustomCommand(blocked));
});
test("User-authenticated route settings reach the paired Java mod",async()=>{
 const call=harness(),user={"x-nexus-key":"panel-testing"};
 assert.equal((await call("POST","/api/client/chat/settings",{},route)).status,401);
 const code=(await call("POST","/api/client/pair/new",user,{})).payload.code;
 const linked=await call("POST","/api/client/pair/exchange",{},{code,player:"Port"});
 const client={"x-nexus-client":linked.payload.token};
 const added=await call("POST","/api/client/chat/settings",user,route);
 assert.equal(added.status,200);
 const poll=await call("GET","/api/client/poll",client);
 assert.equal(poll.payload.jobs[0].type,"chat_settings");
 assert.deepEqual(poll.payload.jobs[0].payload,{
   channel:"custom",command:"f chat privado roleplay mensagens",
   format:"&6&l",bodyFormat:"&f",announce:true
 });
 assert.equal((await call("POST","/api/client/ack",client,
   {id:poll.payload.jobs[0].id,success:true,output:"Saved"})).status,200);
 assert.equal((await call("POST","/api/client/chat/settings",user,
   {...route,command:"/give"})).status,422);
 assert.equal((await call("POST","/api/client/chat/settings",user,
   {...route,announce:"on"})).status,422);
});
test("Alert routes validate HUD, chat, both, off independently",async()=>{
 const call=harness(),user={"x-nexus-key":"panel-testing"};
 const code=(await call("POST","/api/client/pair/new",user,{})).payload.code;
 const linked=await call("POST","/api/client/pair/exchange",{},{code,player:"Port"});
 const client={"x-nexus-client":linked.payload.token};
 const prefs={heavy:true,streak:false,structure:true,risk:true,recovery:true,
   critical:true,cooldownSeconds:45,
   routes:{heavy:"hud",streak:"off",structure:"both",risk:"chat",
     recovery:"hud",critical:"hud",mind:"off",arcane:"chat"}};
 const ok=await call("POST","/api/client/alerts/settings",user,prefs);
 assert.equal(ok.status,200);
 const poll=await call("GET","/api/client/poll",client);
 assert.equal(poll.payload.jobs[0].type,"alerts_settings");
 assert.equal(poll.payload.jobs[0].payload.routes.risk,"chat");
 for(const wrong of ["public","all","server","/rp"]){
   const r=await call("POST","/api/client/alerts/settings",user,{
     ...prefs,routes:{...prefs.routes,risk:wrong}
   });
   assert.equal(r.status,422);
 }
});
