"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {verifyWave,transcribeClip}=require("../audio_transcription.js");
const {createClientRelay}=require("../client_relay.js");
function wave(rate=16000,seconds=1){
 const bytes=rate*2*seconds,b=Buffer.alloc(bytes+44);
 b.write("RIFF",0,"ascii");b.writeUInt32LE(bytes+36,4);b.write("WAVEfmt ",8,"ascii");
 b.writeUInt32LE(16,16);b.writeUInt16LE(1,20);b.writeUInt16LE(1,22);
 b.writeUInt32LE(rate,24);b.writeUInt32LE(rate*2,28);b.writeUInt16LE(2,32);
 b.writeUInt16LE(16,34);b.write("data",36,"ascii");b.writeUInt32LE(bytes,40);
 return b.toString("base64");
}
test("Rejects malformed WAV and transcribes without writing audio",async()=>{
 assert.throws(()=>verifyWave("bad"));
 assert.throws(()=>verifyWave(wave(48000,7)),/limite|Formato/);
 let req,modelRequest;
 const result=await transcribeClip({audio:wave(),answer:true,speaker:"PlayerA",history:[{role:"user",text:"Oficina do jogador em construção."}]},{
  key:"development-key-long-enough",model:"gemini-2.5-flash",
  fetcher:async(url,options)=>{
   req=JSON.parse(options.body);
   return {ok:true,json:async()=>({candidates:[{content:{parts:[{text:"Nexus, bom dia."}]}}]})};
  },
  askAI:async data=>{modelRequest=data;return {text:"Bom dia. Estou atento."};}
 });
 assert.equal(result.transcript,"Nexus, bom dia.");
 assert.equal(result.reply,"Bom dia. Estou atento.");
 assert.equal(modelRequest.memories[0],"Oficina do jogador em construção.");
 assert.equal(modelRequest.voiceSpeaker,"PlayerA");
 assert.equal(req.contents[0].parts[1].inlineData.mimeType,"audio/wav");
});
test("Client-only hearing route checks consent list and auth",async()=>{
 const calls=[];
 const relay=createClientRelay({
  send:(res,code,payload)=>{res.status=code;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="key",limit:()=>true,
  transcribe:async data=>{calls.push(data);return {transcript:"Olá Nexus",reply:""};}
 });
 async function run(method,route,headers={},body){
  const req=Readable.from(body===undefined?[]:[Buffer.from(JSON.stringify(body))]);
  req.headers=headers;req.method=method;const res={status:0,payload:null};
  const u=new URL(route,"https://test.invalid");await relay.handle(req,res,u.pathname,u);
  return res;
 }
 const user={"x-nexus-key":"key"};
 const code=(await run("POST","/api/client/pair/new",user,{})).payload.code;
 const token=(await run("POST","/api/client/pair/exchange",{}, {code,player:"Port"})).payload.token;
 const payload={speaker:"Other",audio:wave(),consentedPlayers:["Other"],answer:false};
 assert.equal((await run("POST","/api/client/hearing/transcribe",user,payload)).status,401);
 assert.equal((await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},
  {...payload,consentedPlayers:[]})).status,403);
 const ok=await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},payload);
 assert.equal(ok.status,200);assert.equal(ok.payload.transcript,"Olá Nexus");assert.equal(calls.length,1);
 assert.equal((await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},payload)).status,200);
});

test("Hearing preferences are panel-authenticated, require consent, and sync no transcripts",async()=>{
 const relay=createClientRelay({send:(res,status,payload)=>{res.status=status;res.payload=payload;},
   verifyUser:req=>req.headers["x-nexus-key"]==="key",limit:()=>true});
 async function call(method,route,headers={},body){
  const req=Readable.from(body===undefined?[]:[Buffer.from(JSON.stringify(body))]);
  req.headers=headers;req.method=method;const res={status:0,payload:null};
  const u=new URL(route,"https://test.invalid");
  await relay.handle(req,res,u.pathname,u);return res;
 }
 const owner={"x-nexus-key":"key"};
 const pair=await call("POST","/api/client/pair/new",owner,{});
 const token=(await call("POST","/api/client/pair/exchange",{},{
  code:pair.payload.code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const payload={listening:true,memory:true,answer:true,
   allowedPlayers:["FriendA"],consentConfirmed:false};
 assert.equal((await call("POST","/api/client/hearing/settings",owner,payload)).status,422);
 assert.equal((await call("POST","/api/client/hearing/settings",client,{
   ...payload,consentConfirmed:true})).status,401);
 const saved=await call("POST","/api/client/hearing/settings",owner,{
   ...payload,consentConfirmed:true});
 assert.equal(saved.status,200);
 const poll=await call("GET","/api/client/poll",client);
 assert.equal(poll.payload.jobs[0].type,"hearing_settings");
 assert.deepEqual(poll.payload.jobs[0].payload.allowedPlayers,["FriendA"]);
 const synced=await call("POST","/api/client/hearing/state",client,{
   ready:true,listening:true,memory:true,answer:true,
   allowedPlayers:["FriendA"],memoryCount:28});
 assert.equal(synced.status,200);
 const view=await call("GET","/api/client/hearing",owner);
 assert.equal(view.payload.state.memoryCount,28);
 assert.equal(view.payload.state.ready,true);
 assert.equal(JSON.stringify(view.payload).includes("transcript"),false);
 assert.equal((await call("GET","/api/client/hearing")).status,401);
});
