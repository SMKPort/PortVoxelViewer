"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const {Readable} = require("node:stream");
const {createClientRelay} = require("../client_relay.js");
const relay = createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;res.end();},
  verifyUser:req=>req.headers["x-nexus-key"]==="unit-test-user",
  limit:()=>true
});
async function run(method,path,headers={},data=null){
 const req=Readable.from(data===null?[]:[Buffer.from(JSON.stringify(data))]);
 req.method=method;req.headers=headers;const res={status:0,payload:null,end(){}};
 await relay.handle(req,res,new URL(path,"https://example.test").pathname,new URL(path,"https://example.test"));
 return res;
}
test("pairing requires user authorization and a one time code",async()=>{
 assert.equal((await run("POST","/api/client/pair/new")).status,401);
 const user={"x-nexus-key":"unit-test-user"};
 const init=await run("POST","/api/client/pair/new",user,{});
 assert.equal(init.status,200);
 const pair=await run("POST","/api/client/pair/exchange",{}, {code:init.payload.code,player:"Port"});
 assert.equal(pair.status,200);
 assert.equal(pair.payload.token.length,64);
 assert.equal((await run("POST","/api/client/pair/exchange",{}, {code:init.payload.code,player:"Port"})).status,401);
 const client={"x-nexus-client":pair.payload.token};
 const job=await run("POST","/api/client/chat",user,{message:"O Nexus esta online."});
 assert.equal(job.status,200);
 const polled=await run("GET","/api/client/poll",client);
 assert.equal(polled.payload.jobs[0].id,job.payload.jobId);
 assert.equal(polled.payload.jobs[0].payload.channel,"local");
 assert.equal(polled.payload.jobs[0].payload.command,"l");
 assert.equal(polled.payload.jobs[0].payload.format,"&6&l");
 assert.equal((await run("POST","/api/client/ack",client,{id:job.payload.jobId,success:true,output:"ok"})).status,200);
 assert.equal((await run("GET","/api/client/job?id="+job.payload.jobId,user)).payload.success,true);
 const global=await run("POST","/api/client/chat",user,{message:"Ola, geral!",channel:"global"});
 assert.equal(global.status,200);
 const custom=await run("POST","/api/client/chat",user,{message:"Estou em RP.",channel:"custom",command:"/rp"});
 assert.equal(custom.status,200);
 const unsafe=await run("POST","/api/client/chat",user,{message:"teste",channel:"custom",command:"op"});
 assert.equal(unsafe.status,422);
 const pending=await run("GET","/api/client/poll",client);
 assert.equal(pending.payload.jobs.length,2);
 assert.equal(pending.payload.jobs[0].payload.channel,"global");
 assert.equal(pending.payload.jobs[0].payload.command,"");
 assert.equal(pending.payload.jobs[0].payload.format,"");
 assert.equal(pending.payload.jobs[1].payload.channel,"custom");
 assert.equal(pending.payload.jobs[1].payload.command,"rp");
 assert.equal(pending.payload.jobs[1].payload.format,"");
 const changed=await run("POST","/api/client/chat",user,{message:"Teste de cor",channel:"local",format:"&a&l"});
 assert.equal(changed.status,200);
 const changedPoll=await run("GET","/api/client/poll",client);
 assert.equal(changedPoll.payload.jobs.length,1);
 assert.equal(changedPoll.payload.jobs[0].payload.format,"&a&l");
 const invalidFormat=await run("POST","/api/client/chat",user,{message:"Texto",channel:"local",format:"&6/kill"});
 assert.equal(invalidFormat.status,422);
 const noStyle=await run("POST","/api/client/chat",user,{message:"Sem estilo",channel:"local",format:""});
 assert.equal(noStyle.status,200);
 const noStylePoll=await run("GET","/api/client/poll",client);
 assert.equal(noStylePoll.payload.jobs[0].payload.format,"");
});
