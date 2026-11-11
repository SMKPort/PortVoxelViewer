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
 assert.equal((await run("POST","/api/client/ack",client,{id:job.payload.jobId,success:true,output:"ok"})).status,200);
 assert.equal((await run("GET","/api/client/job?id="+job.payload.jobId,user)).payload.success,true);
});
