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
 assert.throws(()=>verifyWave(wave(48000,15)),/limite|Formato/);
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
 // Minecraft nicks are case-insensitive on the client; the relay must agree.
 const mixedCase={...payload,speaker:"oThEr",consentedPlayers:["OTHER"]};
 assert.equal((await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},mixedCase)).status,200);
 assert.equal((await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},{
  ...payload,speaker:"Intruder",consentedPlayers:["OTHER"]})).status,403);
 // The synchronized client-state allowlist remains an independent guard.
 assert.equal((await run("POST","/api/client/hearing/state",{"x-nexus-client":token},{
  ready:true,listening:true,memory:true,answer:true,allowedPlayers:["OtHeR"]
 })).status,200);
 assert.equal((await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},mixedCase)).status,200);
 assert.equal((await run("POST","/api/client/hearing/state",{"x-nexus-client":token},{
  ready:true,listening:true,memory:true,answer:true,allowedPlayers:["FriendA"]
 })).status,200);
 assert.equal((await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},mixedCase)).status,403);
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
 assert.equal(poll.payload.jobs[0].payload.sensitivity,55);
 assert.equal(poll.payload.jobs[0].payload.collective,false);
 assert.deepEqual(poll.payload.jobs[0].payload.allowedPlayers,["FriendA"]);
 assert.equal("social" in poll.payload.jobs[0].payload,false);
 assert.equal("speaker" in poll.payload.jobs[0].payload,false);
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


test("Concurrent client ASR requests and memory-grounded wake reply",async()=>{
 const requests=[];
 let release;
 const pending=new Promise(resolve=>{release=resolve;});
 const relay=createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="panel-secret",limit:()=>true,
  transcribe:async data=>{requests.push(data);return pending;},
  askAI:async args=>{requests.push({reply:args});return {text:"Você contou sobre a oficina."};}
 });
 async function call(method,route,headers={},payload){
  const req=Readable.from(payload===undefined?[]:[Buffer.from(JSON.stringify(payload))]);
  req.headers=headers;req.method=method;const res={status:0,payload:null};
  const u=new URL(route,"https://nexus.example");
  await relay.handle(req,res,u.pathname,u);return res;
 }
 const owner={"x-nexus-key":"panel-secret"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{}, {code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 await call("POST","/api/client/hearing/state",client,{
  ready:true,listening:true,memory:true,answer:true,allowedPlayers:["FriendA"],
  memoryCount:345,queued:12,inFlight:3,dropped:2,processed:11,retries:1,
  sensitivity:55,memoryFull:false,lastSpeaker:"FriendA",lastError:""});
 const view=await call("GET","/api/client/hearing",owner);
 assert.equal(view.payload.state.memoryCount,345);
 assert.equal(view.payload.state.queued,12);
 assert.equal(view.payload.state.dropped,2);
 const clip={speaker:"FriendA",audio:wave(),consentedPlayers:["FriendA"],answer:false};
 const p1=call("POST","/api/client/hearing/transcribe",client,clip);
 const p2=call("POST","/api/client/hearing/transcribe",client,clip);
 const p3=call("POST","/api/client/hearing/transcribe",client,clip);
 await new Promise(done=>setImmediate(done));
 assert.equal((await call("POST","/api/client/hearing/transcribe",client,clip)).status,429);
 assert.equal(requests.length,3);
 release({transcript:"Nexus, lembra da oficina?"});
 assert.equal((await p1).status,200);
 assert.equal((await p2).status,200);
 assert.equal((await p3).status,200);
 assert.equal((await call("POST","/api/client/hearing/transcribe",client,{
  ...clip,speaker:"Stranger",consentedPlayers:["Stranger"]})).status,403);
 assert.equal((await call("POST","/api/client/hearing/reply",client,{
  speaker:"FriendA",transcript:"boa tarde",memories:[]})).status,422);
 const reply=await call("POST","/api/client/hearing/reply",client,{
  speaker:"FriendA",transcript:"Nexus, lembra da oficina?",
  memories:["Ontem comecei a construir uma oficina."]});
 assert.equal(reply.status,200);
 assert.match(reply.payload.reply,/oficina/);
 assert.equal(requests.at(-1).reply.voiceSpeaker,"FriendA");
 assert.equal(requests.at(-1).reply.memories.length,1);
});


test("@a collective settings only select previously consenting players",async()=>{
 const relay=createClientRelay({send:(res,status,payload)=>{res.status=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="owner",limit:()=>true});
 async function call(method,url,headers={},payload){
  const req=Readable.from(payload===undefined?[]:[Buffer.from(JSON.stringify(payload))]);
  req.headers=headers;req.method=method;const res={status:0,payload:null};
  const u=new URL(url,"https://test.invalid");
  await relay.handle(req,res,u.pathname,u);return res;
 }
 const owner={"x-nexus-key":"owner"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const client={"x-nexus-client":(await call("POST","/api/client/pair/exchange",{},{
   code,player:"Port"})).payload.token};
 const base={listening:true,memory:true,answer:false,collective:true,
   allowedPlayers:["Alice"],sensitivity:55,consentConfirmed:true};
 assert.equal((await call("POST","/api/client/hearing/settings",owner,{
   ...base,allowedPlayers:[]})).status,422);
 assert.equal((await call("POST","/api/client/hearing/settings",owner,{
   ...base,collective:"@a"})).status,422);
 assert.equal((await call("POST","/api/client/hearing/settings",owner,{
   ...base,consentConfirmed:false})).status,422);
 const accepted=await call("POST","/api/client/hearing/settings",owner,base);
 assert.equal(accepted.status,200);
 const poll=await call("GET","/api/client/poll",client);
 assert.equal(poll.payload.jobs[0].payload.collective,true);
 assert.deepEqual(poll.payload.jobs[0].payload.allowedPlayers,["Alice"]);
 const current=await call("POST","/api/client/hearing/state",client,{
   ready:true,listening:true,memory:true,answer:false,collective:true,
   eligibleOnline:1,allowedPlayers:["Alice"],memoryCount:0});
 assert.equal(current.status,200);
 const view=(await call("GET","/api/client/hearing",owner)).payload;
 assert.equal(view.state.collective,true);
 assert.equal(view.state.eligibleOnline,1);
 assert.equal(JSON.stringify(view).includes("Charlie"),false);
});
