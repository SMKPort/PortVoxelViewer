"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {spawn}=require("node:child_process");
const http=require("node:http");
const net=require("node:net");
const path=require("node:path");
const KEY="integration-test-access-key-123456789";
async function ephemeralPort(){
  return new Promise((resolve,reject)=>{
    const server=net.createServer();
    server.once("error",reject);
    server.listen(0,"127.0.0.1",()=>{
      const port=server.address().port;
      server.close(()=>resolve(port));
    });
  });
}
async function request(url,method="GET",key="",body=null,clientToken=""){
  const headers={"accept":"application/json"};
  if(key)headers["X-Nexus-Key"]=key;
  if(clientToken)headers["X-Nexus-Client"]=clientToken;
  if(body!==null)headers["content-type"]="application/json";
  const res=await fetch(url,{method,headers,body:body===null?undefined:JSON.stringify(body),signal:AbortSignal.timeout(3500)});
  assert.match(res.headers.get("content-type")||"",/application\/json/i,
    "A ponte deve sempre responder JSON, mesmo em erros.");
  return {status:res.status,body:await res.json()};
}
test("HTTP real: pareamento, mensagem e ACK não derrubam o servidor",async()=>{
  const port=await ephemeralPort(),base="http://127.0.0.1:"+port;
  const child=spawn(process.execPath,[path.join(__dirname,"../server.js")],{
    env:{...process.env,PORT:String(port),NEXUS_RP_ACCESS_KEY:KEY},
    stdio:["ignore","pipe","pipe"]
  });
  let output="";
  child.stdout.on("data",b=>output+=b.toString());
  child.stderr.on("data",b=>output+=b.toString());
  try{
    let ready=false;
    for(let attempt=0;attempt<40;attempt++){
      if(child.exitCode!==null)throw Error("Servidor encerrou prematuramente: "+output);
      try{
        const h=await request(base+"/api/health");
        if(h.status===200){ready=true;break;}
      }catch(e){}
      await new Promise(r=>setTimeout(r,75));
    }
    assert.ok(ready,"HTTP server não iniciou: "+output);
    const unauthorized=await request(base+"/api/client/pair/new","POST","wrong",{});
    assert.equal(unauthorized.status,401);
    const created=await request(base+"/api/client/pair/new","POST",KEY,{});
    assert.equal(created.status,200);
    assert.match(created.body.code,/^[A-F0-9]{10}$/);
    const exchanged=await request(base+"/api/client/pair/exchange","POST","",{code:created.body.code,player:"Port"});
    assert.equal(exchanged.status,200);
    const token=exchanged.body.token;
    assert.equal(token.length,64);
    const chat=await request(base+"/api/client/chat","POST",KEY,{message:"Desintegrando armadura",channel:"local",format:"&6&l"});
    assert.equal(chat.status,200);
    const jobs=await request(base+"/api/client/poll","GET","",null,token);
    assert.equal(jobs.status,200);
    assert.equal(jobs.body.jobs.length,1);
    assert.equal(jobs.body.jobs[0].payload.message,"[Nexus Aterna] Desintegrando armadura");
    const ack=await request(base+"/api/client/ack","POST","",{id:chat.body.jobId,success:true,output:"ok"},token);
    assert.equal(ack.status,200);
    const result=await request(base+"/api/client/job?id="+chat.body.jobId,"GET",KEY);
    assert.equal(result.body.done,true);
    assert.equal(result.body.success,true);
    const another=await request(base+"/api/health");
    assert.equal(another.status,200,"A API deve permanecer de pé depois de atender a fila.");
    assert.equal(child.exitCode,null,"O servidor não pode cair ao enviar mensagens.");
  }finally{
    child.kill();
    if(output.includes("ERR_HTTP_HEADERS_SENT"))throw Error("Header duplicado detectado: "+output);
  }
});
