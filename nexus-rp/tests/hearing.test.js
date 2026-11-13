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
 let req;
 const result=await transcribeClip({audio:wave(),answer:true},{
  key:"development-key-long-enough",model:"gemini-2.5-flash",
  fetcher:async(url,options)=>{
   req=JSON.parse(options.body);
   return {ok:true,json:async()=>({candidates:[{content:{parts:[{text:"Nexus, bom dia."}]}}]})};
  },
  askAI:async()=>({text:"Bom dia. Estou atento."})
 });
 assert.equal(result.transcript,"Nexus, bom dia.");
 assert.equal(result.reply,"Bom dia. Estou atento.");
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
 assert.equal((await run("POST","/api/client/hearing/transcribe",{"x-nexus-client":token},payload)).status,429);
});
