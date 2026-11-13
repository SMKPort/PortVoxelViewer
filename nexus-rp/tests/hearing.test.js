"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");

function createTestClient(){
 const relay=createClientRelay({
  send:(res,status,data)=>{res.status=status;res.payload=data;res.end();},
  verifyUser:req=>req.headers["x-nexus-key"]==="owner",
  limit:()=>true,
  askAI:async()=>{throw Error("Retired speech path called AI");}
 });
 async function call(method,route,headers={},data){
  const req=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
  req.method=method;req.headers=headers;
  const res={status:0,payload:null,end(){}};
  const url=new URL(route,"https://test.invalid");
  await relay.handle(req,res,url.pathname,url);
  return res;
 }
 return {call};
}

test("Deprecated speech endpoints always return 410 and cannot call Gemini",async()=>{
 const {call}=createTestClient();
 for(const route of [
  "/api/client/hearing/transcribe",
  "/api/client/hearing/reply",
  "/api/client/hearing/settings"
 ]){
  const res=await call("POST",route,{},{
   speaker:"ConsentedPlayer",audio:"FAKE_AUDIO_BYTES",
   allowedPlayers:["ConsentedPlayer"],transcript:"Nexus, me escute."
  });
  assert.equal(res.status,410,route);
  assert.equal(res.payload.kind,"audio_retired",route);
  assert.equal(res.payload.ok,false,route);
  assert.match(res.payload.error,/removid/i);
 }
});

test("An old client cannot enable or report active Plasmo capture",async()=>{
 const {call}=createTestClient();
 const code=await call("POST","/api/client/pair/new",{"x-nexus-key":"owner"},{});
 const token=(await call("POST","/api/client/pair/exchange",{},{
  code:code.payload.code,player:"Port"
 })).payload.token;
 const client={"x-nexus-client":token};
 assert.equal((await call("POST","/api/client/hearing/transcribe",client,{
  speaker:"FriendA",consentedPlayers:["FriendA"],audio:"ANY"
 })).status,410);
 const synced=await call("POST","/api/client/hearing/state",client,{
  ready:true,listening:true,memory:true,answer:true,
  allowedPlayers:["FriendA"],memoryCount:2
 });
 assert.equal(synced.status,200);
 const state=await call("GET","/api/client/hearing",{"x-nexus-key":"owner"});
 assert.equal(state.status,200);
 assert.equal(state.payload.state.ready,false);
 assert.equal(state.payload.state.listening,false);
 assert.equal(state.payload.state.memoryCount,2);
});

test("Legacy social memory remains accessible without transcribing",async()=>{
 const {call}=createTestClient();
 const code=await call("POST","/api/client/pair/new",{"x-nexus-key":"owner"},{});
 const token=(await call("POST","/api/client/pair/exchange",{},{
  code:code.payload.code,player:"Port"
 })).payload.token;
 const client={"x-nexus-client":token};
 const result=await call("POST","/api/client/hearing/state",client,{
  ready:false,listening:false,memory:false,answer:false,
  allowedPlayers:[],memoryCount:7,
  social:{sharePanel:false,diaryEnabled:true,protectionEnabled:true}
 });
 assert.equal(result.status,200);
 const view=await call("GET","/api/client/hearing",{"x-nexus-key":"owner"});
 assert.equal(view.payload.state.memoryCount,7);
 assert.equal(view.payload.state.listening,false);
 assert.equal((await call("GET","/api/client/hearing",{})).status,401);
});
