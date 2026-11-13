"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
const {getQuestion}=require("../voice_reply.js");
const requests=[];
const relay=createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;res.end();},
  verifyUser:req=>req.headers["x-nexus-key"]==="voice-test-access",
  limit:()=>true,
  getAIStatus:()=>({provider:"Mock",model:"test-model",configured:true}),
  askAI:async data=>{
    requests.push(data);
    return {text:data.question==="como estamos?"
      ?"Aguarde a telemetria, Port. No momento ainda não há leitura suficiente."
      :"Sim, estou acompanhando nossa conversa, Port.",model:"test-model"};
  }
});
async function run(method,path,headers={},data=null){
 const req=Readable.from(data===null?[]:[Buffer.from(JSON.stringify(data))]);
 req.method=method;req.headers=headers;const res={status:0,payload:null,end(){}};
 await relay.handle(req,res,new URL(path,"https://local.test").pathname,new URL(path,"https://local.test"));
 return res;
}
test("wake word is necessary for first turn, not for authorized follow-ups",()=>{
 assert.equal(getQuestion("Nexus, como estamos?"),"como estamos?");
 assert.equal(getQuestion("Oi, como estamos?"),null);
});
test("Real spoken introductions address Nexus without false positives",()=>{
 const valid=[
  "Nexus, você me ouve?",
  "Ei, Nexus, consegue responder?",
  "Oi, nexo! Você está aí?",
  "Ô, Nexus Aeterna, o que acha?",
  "Então, Nexus, como estamos?",
  "Cara, Nexus, pode ajudar?"
 ];
 for(const line of valid)
  assert.notEqual(getQuestion(line),null,"Esperava reconhecimento: "+line);
 for(const line of ["Oi, tudo bem?", "O mod Nexus está funcionando", "Gostei do Nexus", "Nexuses"])
  assert.equal(getQuestion(line),null,"Não deve responder: "+line);
});

test("AI replies go to Minecraft only when authorized; preserve context",async()=>{
 const owner={"x-nexus-key":"voice-test-access"};
 assert.equal((await run("POST","/api/client/voice",{}, {transcript:"Nexus, como estamos?"})).status,401);
 const pair=await run("POST","/api/client/pair/new",owner,{});
 const token=(await run("POST","/api/client/pair/exchange",{}, {code:pair.payload.code,player:"Port"})).payload.token;
 const c={"x-nexus-client":token};
 assert.equal((await run("GET","/api/client/ai/status",owner)).payload.configured,true);
 assert.equal((await run("POST","/api/client/voice",owner,{transcript:"Como estamos?"})).status,422);
 const voice=await run("POST","/api/client/voice",owner,{transcript:"Nexus, como estamos?"});
 assert.equal(voice.status,200);
 assert.equal(voice.payload.jobIds.length,1);
 assert.match(voice.payload.reply,/telemetria/i);
 assert.equal(requests[0].question,"como estamos?");
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
 const follow=await run("POST","/api/client/voice",owner,{transcript:"E por quê?",followUp:true,
    history:[{role:"user",text:"como estamos?"},{role:"model",text:"Aguardando dados."}]});
 assert.equal(follow.status,200);
 assert.equal(requests[1].question,"E por quê?");
 assert.equal(requests[1].history.length,2);
 assert.equal((await run("GET","/api/client/job?id="+job.id,owner)).payload.success,true);
 const mind=await run("POST","/api/client/mind",c,{stage:"ADAPTAÇÃO",experience:100,caution:30,attachment:40,curiosity:50});
 assert.equal(mind.status,200);
 const third=await run("POST","/api/client/voice",owner,{transcript:"Nexus, me explique."});
 assert.equal(third.status,200);
 assert.equal(requests[2].mind.stage,"ADAPTAÇÃO");
});
test("AI response segments never exceed the chat limit",async()=>{
 const relayLong=createClientRelay({
   send:(res,status,payload)=>{res.status=status;res.payload=payload;res.end();},
   verifyUser:req=>req.headers["x-nexus-key"]==="voice-test-access",limit:()=>true,
   askAI:async()=>({text:"Uma resposta sobre o Nexus e a oficina arcana. ".repeat(9),model:"Mock"})
 });
 async function invoke(method,path,headers={},data=null){
   const req=Readable.from(data===null?[]:[Buffer.from(JSON.stringify(data))]);
   req.method=method;req.headers=headers;const res={status:0,payload:null,end(){}};
   await relayLong.handle(req,res,new URL(path,"https://local.test").pathname,new URL(path,"https://local.test"));
   return res;
 }
 const user={"x-nexus-key":"voice-test-access"};
 const pair=await invoke("POST","/api/client/pair/new",user,{});
 const clientToken=(await invoke("POST","/api/client/pair/exchange",{}, {code:pair.payload.code,player:"Port"})).payload.token;
 const result=await invoke("POST","/api/client/voice",user,{transcript:"Nexus, me explica."});
 assert.equal(result.status,200);
 assert.equal(result.payload.jobIds.length,2);
 const poll=await invoke("GET","/api/client/poll",{"x-nexus-client":clientToken});
 assert.equal(poll.payload.jobs.length,1,"A segunda mensagem só será liberada após o intervalo de chat.");
 assert.ok(poll.payload.jobs[0].payload.message.length<=230);
});
