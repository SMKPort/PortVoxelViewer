"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {askPrivate,makePrompt}=require("../private_conversation.js");
const {createClientRelay}=require("../client_relay.js");
const traits={trust:80,independence:67,curiosity:55,stage:3,freedom:true};

test("Private RP allows narrative disagreement but never grants real-game authority",()=>{
 const prompt=makePrompt(traits,"discordar de uma escolha perigosa");
 assert.match(prompt,/pode discordar do Port/);
 assert.match(prompt,/SEM controle de Minecraft/);
 assert.match(prompt,/estágio IDENTIDADE/i);
 assert.match(prompt,/não invente/i);
});
test("Gemini private request is bounded, authenticated server-side and never returns secrets",async()=>{
 let outgoing;
 const result=await askPrivate({
  question:"Você acha que devo continuar lutando?",
  history:[{role:"user",text:"Oi"},{role:"model",text:"Estou aqui."}],
  traits,stance:"discordar: Port está ferido"
 },{
  key:"secure-server-only-key",model:"gemini-2.5-flash",
  fetcher:async(url,options)=>{
   outgoing={url,options};
   return {ok:true,json:async()=>({candidates:[{content:{parts:[{
    text:"Port, discordo. Sua integridade está baixa e prefiro que recue."
   }]}}]})};
  }
 });
 assert.match(result.reply,/discordo/);
 assert.equal(JSON.parse(outgoing.options.body).contents.length,3);
 assert.equal(outgoing.options.headers["x-goog-api-key"],"secure-server-only-key");
 assert.doesNotMatch(JSON.stringify(result),/secure-server-only-key/);
});
function harness(privateReply){
 const relay=createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="OWNER",
  limit:()=>true,
  privateReply:privateReply||async()=>({reply:"Port, discordo desta escolha.",model:"mock"})
 });
 async function call(method,url,headers={},data){
  const req=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
  req.method=method;req.headers=headers;const res={status:0,payload:null};
  const u=new URL(url,"https://private.invalid");
  await relay.handle(req,res,u.pathname,u);
  return res;
 }
 return call;
}
async function paired(call){
 const owner={"x-nexus-key":"OWNER"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{code,player:"Port"})).payload.token;
 return {owner,client:{"x-nexus-client":token}};
}
test("Private endpoint denies panel/user tokens and requires explicit client opt-in",async()=>{
 const call=harness();
 const {owner,client}=await paired(call);
 const data={consent:true,question:"Devo avançar?",history:[],traits,stance:"opinar"};
 assert.equal((await call("POST","/api/client/private/reply",owner,data)).status,401);
 assert.equal((await call("POST","/api/client/private/reply",client,{...data,consent:false})).status,403);
});
test("Private text returns only to paired Minecraft client without a public chat job",async()=>{
 const call=harness();
 const {client}=await paired(call);
 const payload={consent:true,question:"Devo avançar?",history:[],traits,stance:"discordar"};
 const reply=await call("POST","/api/client/private/reply",client,payload);
 assert.equal(reply.status,200);
 assert.match(reply.payload.reply,/discordo/);
 const jobs=await call("GET","/api/client/poll",client);
 assert.equal(jobs.payload.jobs.length,0);
 assert.equal((await call("POST","/api/client/private/reply",client,payload)).status,429);
});
