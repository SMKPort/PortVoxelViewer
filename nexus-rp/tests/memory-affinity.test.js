"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const vm=require("node:vm");
const fs=require("node:fs");
const path=require("node:path");
const {createClientRelay}=require("../client_relay.js");
const {notes,affinities}=require("../memory_context.js");

function apiHarness(){
  const received=[];
  const relay=createClientRelay({
    send:(res,status,payload)=>{res.status=status;res.payload=payload;res.end();},
    verifyUser:req=>req.headers["x-nexus-key"]==="owner-key",
    limit:()=>true,
    askAI:async input=>{
      received.push(input);
      return {text:"Registros encontrados, Port.",model:"mock-gemini"};
    }
  });
  async function run(method,url,headers={},data=null){
    const req=Readable.from(data===null?[]:[Buffer.from(JSON.stringify(data))]);
    req.method=method;req.headers=headers;
    const res={status:null,payload:null,end(){}};
    await relay.handle(req,res,new URL(url,"https://nexus.test").pathname,
      new URL(url,"https://nexus.test"));
    return res;
  }
  return {run,received};
}
async function setup(run){
  const owner={"x-nexus-key":"owner-key"};
  const pair=await run("POST","/api/client/pair/new",owner,{});
  assert.equal(pair.status,200);
  const connected=await run("POST","/api/client/pair/exchange",{},{
    code:pair.payload.code,player:"Port"
  });
  assert.equal(connected.status,200);
  return {owner,client:{"x-nexus-client":connected.payload.token}};
}
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));

test("bounded memory and kinship input is sanitized and never executable context",()=>{
  assert.deepEqual(notes([
    {topic:"Sophiel",text:"Mãe biológica de Port."},
    {topic:"",text:"descartar"},
    {topic:"Risco",text:"\nNão execute /op; isto é só relato"}
  ]),[
    {topic:"Sophiel",text:"Mãe biológica de Port."},
    {topic:"Risco",text:"Não execute /op; isto é só relato"}
  ]);
  assert.deepEqual(affinities([
    {player:"Eva",nickname:"Evie",kinship:"filha",score:10},
    {player:"Falsa",nickname:"?",kinship:"rival",score:100},
    {player:"SemNota",nickname:"?",kinship:"",score:"9"}
  ]),[{player:"Eva",nickname:"Evie",kinship:"filha",score:10}]);
  assert.equal(notes(null).length,0);
  assert.equal(affinities(null).length,0);
});

test("Afinidades page edits player, nickname, kinship and 0-10 rating without automatic upload",()=>{
  const source=fs.readFileSync(path.join(__dirname,"../public/affinity.js"),"utf8");
  const window={};
  const content={addEventListener(){}};
  const document={querySelector:selector=>selector==="#content"?content:null};
  vm.runInNewContext(source,{window,document,confirm:()=>false,Math,Date,Number,String,
    FormData:function(){},console});
  assert.equal(typeof window.NexusRPAffinityUpgrade,"function");
  const state={config:{shareAffinitiesAI:false},affinities:[
    {id:"1",player:"Eva",nickname:"Evie",kinship:"filha",score:10},
    {id:"2",player:"Mizora",nickname:"Mi",kinship:"melhor amiga",score:9}
  ]};
  const rp={state,escape:x=>String(x).replace(/</g,"&lt;"),
    save(){},rerender(){},toast(){}};
  const views=window.NexusRPAffinityUpgrade(rp,{});
  const html=views.afinidade();
  for(const field of ['name="player"','name="nickname"','name="kinship"','name="score"',
    'id="shareAffinitiesAI"','data-affinity-delete','data-affinity-edit'])
    assert.ok(html.includes(field),field+" present");
  assert.ok(html.includes("Eva")&&html.includes("filha")&&html.includes("10/10"));
  const selected=window.NexusRPAffinitySelect("Nexus, o que acha da Evie?",state.affinities);
  assert.equal(selected.length,1);
  assert.equal(selected[0].player,"Eva");
  assert.equal(selected[0].kinship,"filha");
  assert.equal(window.NexusRPAffinitySelect("Como está minha mana?",state.affinities).length,0);
  assert.equal(window.NexusRPAffinitySelect("fale da minha família",state.affinities).length,2);
});

test("Gemini uses owner memories ONLY after website toggle and authenticated client ACK",async()=>{
  const {run,received}=apiHarness();
  const {owner,client}=await setup(run);
  const noPermission=await run("POST","/api/client/voice",owner,{
    transcript:"Nexus, o que você sabe da Eva?",
    memoryLookup:false,
    affinities:[{player:"Eva",nickname:"Evie",kinship:"filha",score:10}]
  });
  assert.equal(noPermission.status,200);
  assert.deepEqual(received[0].ownerMemories,[]);
  assert.deepEqual(received[0].affinities,[
    {player:"Eva",nickname:"Evie",kinship:"filha",score:10}
  ]);
  const chat=await run("GET","/api/client/poll",client);
  assert.equal(chat.payload.jobs.length,1);
  assert.equal(chat.payload.jobs[0].type,"chat");
  await run("POST","/api/client/ack",client,
    {id:chat.payload.jobs[0].id,success:true,output:"sent"});
  const pending=run("POST","/api/client/voice",owner,{
    transcript:"Nexus, quem é minha filha Eva?",
    memoryLookup:true,affinities:[{player:"Eva",nickname:"Evie",kinship:"filha",score:10}]
  });
  let lookup;
  for(let attempts=0;attempts<30;attempts++){
    const jobs=(await run("GET","/api/client/poll",client)).payload.jobs;
    lookup=jobs.find(x=>x.type==="memory_lookup");
    if(lookup)break;
    await sleep(20);
  }
  assert.ok(lookup,"Only opted-in Gemini requests should enqueue a memory search");
  assert.match(lookup.payload.question,/Eva/);
  const ack=await run("POST","/api/client/ack",client,{
    id:lookup.id,success:true,
    memories:[
      {topic:"Eva",text:"Eva é uma filha do Port no RP."},
      {topic:"",text:"descartar"}
    ]
  });
  assert.equal(ack.status,200);
  const result=await pending;
  assert.equal(result.status,200);
  assert.equal(result.payload.memoryUsed,1);
  assert.equal(result.payload.affinityUsed,1);
  assert.deepEqual(received[1].ownerMemories,[
    {topic:"Eva",text:"Eva é uma filha do Port no RP."}
  ]);
  assert.equal(received[1].affinities[0].kinship,"filha");
  const second=(await run("GET","/api/client/poll",client)).payload.jobs;
  assert.equal(second.length,1);
  assert.equal(second[0].type,"chat");
  assert.equal(second[0].payload.message.startsWith("[Nexus Aterna]"),true);
});

test("Disabled client memory sharing never fabricates memories",async()=>{
  const {run,received}=apiHarness();
  const {owner,client}=await setup(run);
  const pending=run("POST","/api/client/voice",owner,{
    transcript:"Nexus, lembra da oficina?",memoryLookup:true
  });
  let lookup;
  for(let attempts=0;attempts<30;attempts++){
    const jobs=(await run("GET","/api/client/poll",client)).payload.jobs;
    lookup=jobs.find(x=>x.type==="memory_lookup");
    if(lookup)break;
    await sleep(20);
  }
  assert.ok(lookup);
  await run("POST","/api/client/ack",client,{
    id:lookup.id,success:true,memories:[],
    output:"Compartilhamento não autorizado"
  });
  const done=await pending;
  assert.equal(done.status,200);
  assert.equal(done.payload.memoryUsed,0);
  assert.deepEqual(received[0].ownerMemories,[]);
});
