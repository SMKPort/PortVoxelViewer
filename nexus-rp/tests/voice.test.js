"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
const {getQuestion,buildReply}=require("../voice_reply.js");
const relay=createClientRelay({send:(res,status,payload)=>{res.status=status;res.payload=payload;res.end();},
 verifyUser:req=>req.headers["x-nexus-key"]==="voice-test-access",limit:()=>true});
async function run(method,path,headers={},data=null){
 const req=Readable.from(data===null?[]:[Buffer.from(JSON.stringify(data))]);
 req.method=method;req.headers=headers;const res={status:0,payload:null,end(){}};
 await relay.handle(req,res,new URL(path,"https://local.test").pathname,new URL(path,"https://local.test"));
 return res;
}
test("replies require wake word and do not fabricate health",()=>{
 assert.equal(getQuestion("Nexus, como estamos?"),"como estamos?");
 assert.equal(getQuestion("oi, como estamos?"),null);
 assert.match(buildReply("Nexus, como estamos?",{online:false}),/não tenho conexão/i);
 assert.match(buildReply("Nexus, como estamos?",{online:true,events:[]}),/telemetria/i);
 assert.match(buildReply("Nexus, como estamos?",{online:true,events:[{type:"damage",health:7,at:new Date().toISOString()}]}),/7 pontos de vida/);
 assert.match(buildReply("Nexus, você está me ouvindo?",{}),/recebendo suas palavras/i);
 assert.equal(buildReply("Port, como estamos?",{}),null);
});
test("microphone transcription is authorized, rate-safe and queued as local Nexus chat",async()=>{
 const owner={"x-nexus-key":"voice-test-access"};
 assert.equal((await run("POST","/api/client/voice",{}, {transcript:"Nexus, como estamos?"})).status,401);
 const pair=await run("POST","/api/client/pair/new",owner,{});
 const token=(await run("POST","/api/client/pair/exchange",{}, {code:pair.payload.code,player:"Port"})).payload.token;
 const c={"x-nexus-client":token};
 assert.equal((await run("POST","/api/client/voice",owner,{transcript:"Como estamos?"})).status,422);
 const voice=await run("POST","/api/client/voice",owner,{transcript:"Nexus, como estamos?"});
 assert.equal(voice.status,200);
 assert.equal(voice.payload.ok,true);
 assert.match(voice.payload.reply,/telemetria/i);
 const polled=await run("GET","/api/client/poll",c);
 assert.equal(polled.payload.jobs.length,1);
 const job=polled.payload.jobs[0];
 assert.equal(job.type,"chat");
 assert.equal(job.payload.channel,"local");
 assert.equal(job.payload.command,"l");
 assert.equal(job.payload.format,"&6&l");
 assert.ok(job.payload.message.startsWith("[Nexus Aterna] "));
 const ack=await run("POST","/api/client/ack",c,{id:job.id,success:true,output:"Enviada"});
 assert.equal(ack.status,200);
 assert.equal((await run("GET","/api/client/job?id="+job.id,owner)).payload.success,true);
});
