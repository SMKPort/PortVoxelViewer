"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {route,validateCustomCommand}=require("../chat_channel.js");
const {createClientRelay}=require("../client_relay.js");

function harness(){
  const relay=createClientRelay({
    send:(res,status,payload)=>{res.status=status;res.payload=payload;res.end();},
    verifyUser:req=>req.headers["x-nexus-key"]==="chat-style-test",
    limit:()=>true,
    askAI:async()=>({text:"Desintegrando armadura.",model:"mock-gemini"})
  });
  async function req(method,path,headers={},data=null){
    const r=Readable.from(data===null?[]:[Buffer.from(JSON.stringify(data))]);
    r.method=method;r.headers=headers;
    const response={status:0,payload:null,end(){}};
    await relay.handle(r,response,new URL(path,"https://unit.test").pathname,
      new URL(path,"https://unit.test"));
    return response;
  }
  return req;
}

test("administrator's custom chat prefix need not be on a fixed whitelist",()=>{
  assert.equal(validateCustomCommand("/somake:arcana"),"somake:arcana");
  assert.equal(validateCustomCommand("/f chat"),"f chat");
  assert.equal(validateCustomCommand("/nexusconversa"),"nexusconversa");
  for(const unsafe of ["/op","/minecraft:op","/ban player","/f;op",
    "/f chat;kill","/f @a","/f\nchat","/f | say","/f {json}",
    "/f \"test\"","/f chat \${message}","/f chat && op","/execute run say"]){
    assert.throws(()=>validateCustomCommand(unsafe),undefined,unsafe);
  }
});
test("prefix and body styles are independent; legacy destinations work",()=>{
  assert.deepEqual(route({channel:"local"}),{
    channel:"local",command:"l",format:"&6&l",bodyFormat:""
  });
  assert.deepEqual(route({channel:"custom",command:"/f chat",format:"&6&l",bodyFormat:"&b"}),{
    channel:"custom",command:"f chat",format:"&6&l",bodyFormat:"&b"
  });
  assert.deepEqual(route({channel:"global",format:"&6&l",bodyFormat:"&f"}),{
    channel:"global",command:"",format:"&6&l",bodyFormat:"&f"
  });
  assert.equal(route({channel:"custom",command:"/rp"}).format,"");
  assert.throws(()=>route({channel:"local",bodyFormat:"&4;kick"}));
  assert.throws(()=>route({channel:"custom",command:"/me && op"}));
});
test("regular messages and Gemini voice use the same owner-selected command and formatting",async()=>{
  const run=harness(),user={"x-nexus-key":"chat-style-test"};
  const pair=await run("POST","/api/client/pair/new",user,{});
  assert.equal(pair.status,200);
  const exchange=await run("POST","/api/client/pair/exchange",{},{
    code:pair.payload.code,player:"Port"
  });
  const client={"x-nexus-client":exchange.payload.token};
  const destination={channel:"custom",command:"/somake:arcana",
    format:"&6&l",bodyFormat:"&b"};
  const manual=await run("POST","/api/client/chat",user,{
    message:"Desintegrando armadura.",...destination
  });
  assert.equal(manual.status,200);
  const all=await run("GET","/api/client/poll",client);
  assert.equal(all.payload.jobs.length,1);
  assert.deepEqual(all.payload.jobs[0].payload,{
    message:"[Nexus Aterna] Desintegrando armadura.",
    channel:"custom",command:"somake:arcana",format:"&6&l",bodyFormat:"&b"
  });
  await run("POST","/api/client/ack",client,{
    id:all.payload.jobs[0].id,success:true,output:"Solicitado ao servidor"
  });
  const voice=await run("POST","/api/client/voice",user,{
    transcript:"Nexus, iniciar análise.",...destination
  });
  assert.equal(voice.status,200);
  assert.equal(voice.payload.destination.command,"somake:arcana");
  assert.equal(voice.payload.destination.bodyFormat,"&b");
  const follow=await run("GET","/api/client/poll",client);
  assert.equal(follow.payload.jobs.length,1);
  assert.deepEqual(follow.payload.jobs[0].payload,{
    message:"[Nexus Aterna] Desintegrando armadura.",
    channel:"custom",command:"somake:arcana",format:"&6&l",bodyFormat:"&b"
  });
  const rejected=await run("POST","/api/client/voice",user,{
    transcript:"Nexus, teste.",channel:"custom",command:"/op"
  });
  assert.equal(rejected.status,422);
  assert.match(rejected.payload.error,/comando/i);
});
