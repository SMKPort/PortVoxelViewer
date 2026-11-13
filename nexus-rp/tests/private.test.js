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
