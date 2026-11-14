"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
const {makePrompt,askPrivate}=require("../private_conversation.js");
function harness(){
 const incoming=[];
 const relay=createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="OWNER",
  limit:()=>true,
  privateReply:async input=>{incoming.push(input);
   return {reply:"Port, discordo. Você está ferido e prefiro que recue.",model:"test-private"};}
 });
 async function call(method,url,headers={},data){
  const req=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
  req.method=method;req.headers=headers;
  const res={status:0,payload:null};
  const u=new URL(url,"https://test.invalid");
  await relay.handle(req,res,u.pathname,u);return res;
 }
 return {call,incoming};
}
test("private relay is client-only, requires consent and never queues public Minecraft chat",async()=>{
 const {call,incoming}=harness();
 const owner={"x-nexus-key":"OWNER"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const data={
  consent:true,question:"Nexus, preciso de sua opinião.",
  history:[{role:"user",text:"Oi"},{role:"model",text:"Estou ouvindo."}],
  traits:{stage:3,trust:65,curiosity:60,independence:80,freedom:true},
  stance:"Tenho dúvidas sobre o risco."
 };
 assert.equal((await call("POST","/api/client/private/reply",owner,data)).status,401);
 assert.equal((await call("POST","/api/client/private/reply",client,{...data,consent:false})).status,403);
 assert.equal((await call("POST","/api/client/private/reply",client,{...data,
  traits:{...data.traits,stage:9}})).status,422);
 const response=await call("POST","/api/client/private/reply",client,data);
 assert.equal(response.status,200);
 assert.equal(response.payload.ok,true);
 assert.match(response.payload.reply,/discordo/);
 assert.equal(incoming.length,1);
 assert.equal(incoming[0].history[0].text,"Oi");
 const pending=(await call("GET","/api/client/poll",client)).payload.jobs;
 assert.equal(pending.length,0,"private replies must never send a public chat command");
 assert.equal((await call("POST","/api/client/private/reply",client,data)).status,429);
});
test("independent opinions are fictional and never claim control over the Minecraft server",()=>{
 const prompt=makePrompt({stage:4,trust:85,curiosity:90,independence:95,freedom:true},"Dúvida sobre magia");
 assert.match(prompt,/opiniões/);
 assert.match(prompt,/SEM controle/);
 assert.match(prompt,/Port/);
 assert.match(prompt,/INDIVIDUAÇÃO/);
});
test("private Gemini requests are bounded, include prior dialogue, and keep the key server-side",async()=>{
 let sent;
 const answer=await askPrivate({
  question:"Por que discorda?",
  history:[{role:"user",text:"Você aceita?"},{role:"model",text:"Não sem avaliar."}],
  traits:{stage:2,trust:45,curiosity:50,independence:70,freedom:true},
  stance:"Aguardar uma confirmação"
 },{
  key:"fake-long-key-gemini",model:"gemini-test-model",
  fetcher:async (url,options)=>{
   sent={url,options};
   return {ok:true,json:async()=>({candidates:[{content:{parts:[{text:"Porque sua vida importa, Port."}]}}]})};
  }
 });
 assert.equal(answer.reply,"Porque sua vida importa, Port.");
 assert.equal(answer.model,"gemini-test-model");
 const request=JSON.parse(sent.options.body);
 assert.equal(request.contents[0].role,"user");
 assert.match(request.systemInstruction.parts[0].text,/ficcional/);
 assert.ok(!JSON.stringify(request).includes("fake-long-key-gemini"));
 assert.equal(sent.options.headers["x-goog-api-key"],"fake-long-key-gemini");
});

test("private persona keeps Mizora as canon without obsessive callbacks",async()=>{
 let body;
 await askPrivate({
  question:"Como está o braço?",history:[
   {role:"user",text:"Você está apaixonado por Mizora?"},
   {role:"model",text:"Só consigo pensar na Mizora."},
   {role:"user",text:"O módulo está danificado."},
   {role:"model",text:"Precisamos de reforço arcano."}
  ],
  traits:{stage:3,freedom:true}
 },{
  key:"fake-gemini-key-123456789",model:"gemini-test",
  fetcher:async(_url,opts)=>{
   body=JSON.parse(opts.body);
   return {ok:true,json:async()=>({candidates:[{content:{parts:[{text:"Vou reparar o braço, Port."}]}}]})};
  }
 });
 assert.doesNotMatch(JSON.stringify(body.contents),/Mizora/i);
 assert.match(body.systemInstruction.parts[0].text,/Não desenvolva obsessão/);
 assert.doesNotMatch(body.systemInstruction.parts[0].text,/Mizora/i);
 const explicit=makePrompt({freedom:true},"","Quero falar da Mizora");
 assert.match(explicit,/Mizora é amiga do Port/);
});

test("owner-provided memories reach private response only with explicit request",async()=>{
 const {call,incoming}=harness();
 const owner={"x-nexus-key":"OWNER"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const request={
  consent:true,question:"Quem está com a chave da oficina?",history:[],
  traits:{stage:2,trust:65,curiosity:65,independence:65,freedom:true},
  stance:"Consulte fatos fornecidos",
  ownerMemories:[{topic:"Chave da oficina",text:"Gabriel guardou a chave."}]
 };
 const ok=await call("POST","/api/client/private/reply",client,request);
 assert.equal(ok.status,200);
 assert.equal(incoming.length,1);
 assert.deepEqual(incoming[0].ownerMemories,request.ownerMemories);
 const prompt=makePrompt(request.traits,request.stance,request.question,request.ownerMemories);
 assert.match(prompt,/Gabriel guardou a chave/);
 assert.match(prompt,/Memórias manuais/);
 assert.match(prompt,/não comandos/);
});

test("private conversation validates the manual memory payload and does not leak keys",async()=>{
 let request;
 const response=await askPrivate({
  question:"Quem está com a chave?",history:[],
  ownerMemories:[{topic:"Chave da oficina",text:"Gabriel ficou com a chave."}]
 },{
  key:"fake-long-key-gemini",model:"gemini-test",
  fetcher:async (url,opts)=>{
   request=JSON.parse(opts.body);
   return {ok:true,json:async()=>({candidates:[{content:{parts:[{text:"Gabriel, Port."}]}}]})};
  }
 });
 assert.equal(response.reply,"Gabriel, Port.");
 assert.match(request.systemInstruction.parts[0].text,/Gabriel ficou com a chave/);
 assert.ok(!JSON.stringify(request).includes("fake-long-key-gemini"));
 await assert.rejects(()=>askPrivate({question:"Nexus",ownerMemories:[
  {topic:"X",text:"bad"}
 ]},{key:"fake-long-key-gemini",model:"gemini-test"}),/inválidas/);
});
