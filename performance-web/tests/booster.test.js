"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {server}=require("../server");
const KEY="9999aaaabbbbccccddddeeeeffff0000";
const OTHER="12121212121212121212121212121212";
let base="";
const metrics={player:"Port",fps:52,ping:42,memoryUsedMb:980,memoryAllocatedMb:1800,memoryMaxMb:4096};
const booster={
 mode:"observe",phase:"observing",targetFps:60,webControlEnabled:true,
 baselineAvgFps:0,trialAvgFps:0,differencePct:0,secondsInPhase:0,
 originalEntityScale:1,appliedEntityScale:-1,changedOption:false,
 low1SampledFps:38,lastAction:"Nenhuma alteração."
};
async function ask(path,method="GET",key="",body){
 const headers={"Accept":"application/json"};
 if(key)headers["X-Port-Perf-Key"]=key;
 if(body!==undefined)headers["Content-Type"]="application/json";
 const response=await fetch(base+path,{
   method,headers,body:body===undefined?undefined:JSON.stringify(body)
 });
 return {status:response.status,data:await response.json()};
}
test.before(async()=>{await new Promise(r=>server.listen(0,"127.0.0.1",r));base="http://127.0.0.1:"+server.address().port;});
test.after(async()=>{await new Promise((res,rej)=>server.close(e=>e?rej(e):res()));});
test("controle remoto requer chave e consentimento dentro do Minecraft",async()=>{
 assert.equal((await ask("/api/booster/control","POST",KEY,{action:"auto"})).status,409);
 assert.equal((await ask("/api/metrics","POST",KEY,{...metrics,booster:{...booster,webControlEnabled:false}})).status,200);
 assert.equal((await ask("/api/booster/control","POST",KEY,{action:"auto"})).status,409);
 assert.equal((await ask("/api/booster/control","POST",OTHER,{action:"auto"})).status,409);
 await new Promise(r=>setTimeout(r,1150));
 assert.equal((await ask("/api/metrics","POST",KEY,{...metrics,booster})).status,200);
 assert.equal((await ask("/api/booster/control","POST",KEY,{action:"auto",targetFps:75})).status,200);
 await new Promise(r=>setTimeout(r,1150));
 const ack=await ask("/api/metrics","POST",KEY,{...metrics,fps:55,
   booster:{...booster,mode:"adaptive",phase:"baseline",lastAction:"Coletando referência."}});
 assert.equal(ack.status,200);
 assert.deepEqual(ack.data.command,{action:"auto",targetFps:75});
 await new Promise(r=>setTimeout(r,1150));
 const again=await ask("/api/metrics","POST",KEY,{...metrics,booster});
 assert.equal(again.status,200);
 assert.equal(again.data.command,null);
 const snap=await ask("/api/snapshot","GET",KEY);
 assert.equal(snap.status,200);
 assert.equal(snap.data.latest.booster.targetFps,60);
 assert.ok(Array.isArray(snap.data.events));
 assert.ok(snap.data.events.some(e=>e.phase==="baseline"));
 assert.equal((await ask("/api/snapshot","GET",OTHER)).data.latest,null);
});
test("rejeita alterações inválidas mesmo com chave",async()=>{
 assert.equal((await ask("/api/booster/control","POST",KEY,{action:"arbitrary-command"})).status,422);
 assert.equal((await ask("/api/booster/control","POST",KEY,{action:"auto",targetFps:999})).status,422);
 await new Promise(r=>setTimeout(r,1150));
 assert.equal((await ask("/api/metrics","POST",KEY,{...metrics,booster:{...booster,targetFps:"90"}})).status,422);
});
