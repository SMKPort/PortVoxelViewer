"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {server}=require("../server.js");
let origin;
const KEY="11112222333344445555666677778888";
const OTHER="9999aaaabbbbccccddddeeeeffff0000";
const payload={player:"Port",fps:123,ping:44,memoryUsedMb:1150,memoryAllocatedMb:2048,memoryMaxMb:4096};
async function ask(path,method="GET",key="",body){
 const headers={accept:"application/json"};
 if(key)headers["X-Port-Perf-Key"]=key;
 if(body)headers["Content-Type"]="application/json";
 const res=await fetch(origin+path,{method,headers,body:body?JSON.stringify(body):undefined});
 const type=res.headers.get("content-type");
 return {code:res.status,type,json:type?.includes("application/json")?await res.json():null};
}
test.before(async()=>{await new Promise(r=>server.listen(0,"127.0.0.1",r));origin="http://127.0.0.1:"+server.address().port;});
test.after(async()=>{await new Promise((resolve,reject)=>server.close(error=>error?reject(error):resolve()));});
test("site and health respond",async()=>{
 const page=await fetch(origin+"/");
 assert.equal(page.status,200);
 assert.match(await page.text(),/Port Performance Monitor/i);
 const health=await ask("/api/health");
 assert.equal(health.code,200);assert.equal(health.json.version,"4.0.0");
});
test("unknown device has no shared data",async()=>{
 assert.equal((await ask("/api/snapshot")).code,401);
 const unknown=await ask("/api/snapshot","GET",OTHER);
 assert.equal(unknown.code,200);
 assert.equal(unknown.json.online,false);
 assert.equal(unknown.json.latest,null);
});
test("sender metrics require token and enforce types",async()=>{
 assert.equal((await ask("/api/metrics","POST","invalid",payload)).code,401);
 assert.equal((await ask("/api/metrics","POST",KEY,{...payload,fps:"123"})).code,422);
 const result=await ask("/api/metrics","POST",KEY,payload);
 assert.equal(result.code,200);
 const latest=await ask("/api/snapshot","GET",KEY);
 assert.equal(latest.code,200);
 assert.equal(latest.json.online,true);
 assert.equal(latest.json.latest.fps,123);
 assert.equal(latest.json.latest.ping,44);
 assert.equal(latest.json.series.length,1);
 const foreign=await ask("/api/snapshot","GET",OTHER);
 assert.equal(foreign.json.latest,null);
 assert.equal((await ask("/api/metrics","POST",KEY,payload)).code,429);
});
test("bad routes do not expose files",async()=>{
 const x=await ask("/../../server.js");assert.equal(x.code,404);
 const r=await fetch(origin+"/app.js");assert.equal(r.status,200);assert.match(r.headers.get("content-security-policy")||"",/default-src/);
});

test("v3 real frame timing and adaptive chunk telemetry are accepted",async()=>{
 const key="abcdefabcdefabcdefabcdefabcdefab";
 const booster={
   mode:"adaptive",phase:"trial",targetFps:60,webControlEnabled:false,
   baselineAvgFps:38.2,trialAvgFps:42.9,differencePct:12.3,secondsInPhase:5,
   originalEntityScale:1,appliedEntityScale:-1,changedOption:true,
   low1SampledFps:22.5,realLow1Fps:22.5,frameP95Ms:38.4,frameP99Ms:62.0,
   stutterPercent:2.5,measuredAvgFps:42.9,
   renderDistance:10,originalRenderDistance:12,changedRenderDistance:true,
   allowChunkScaling:true,allowEntityScaling:false,lastAction:"Ajuste reversível de chunks."
 };
 assert.equal((await ask("/api/metrics","POST",key,{...payload,booster})).code,200);
 const snap=await ask("/api/snapshot","GET",key);
 assert.equal(snap.json.latest.booster.realLow1Fps,22.5);
 assert.equal(snap.json.latest.booster.frameP95Ms,38.4);
 assert.equal(snap.json.latest.booster.renderDistance,10);
 assert.equal(snap.json.latest.booster.changedRenderDistance,true);
 const invalidKey="abababababababababababababababab";
 assert.equal((await ask("/api/metrics","POST",invalidKey,{
   ...payload,booster:{...booster,frameP95Ms:"unexpected"}
 })).code,422);
});

test("v4 iGPU profile and CPU diagnostics retain validated type and token isolation",async()=>{
 const key="fedcbafedcbafedcbafedcbafedcbafe";
 const baseBooster={
   mode:"adaptive",phase:"trial",targetFps:60,webControlEnabled:false,
   baselineAvgFps:42,trialAvgFps:48,differencePct:14,secondsInPhase:10,
   originalEntityScale:1,appliedEntityScale:-1,changedOption:true,
   low1SampledFps:25,realLow1Fps:25,frameP95Ms:33,frameP99Ms:58,
   stutterPercent:3,measuredAvgFps:48,renderDistance:10,
   originalRenderDistance:12,changedRenderDistance:true,allowChunkScaling:true,
   allowEntityScaling:false,profile:"igpu",logicalProcessors:12,
   clientThreadCpuPct:75.2,processCpuPct:18.3,freeSystemMemoryMb:3200,
   totalSystemMemoryMb:16384,hardwareHint:"sem gargalo confirmado",
   appliedBiomeBlend:2,appliedClouds:"none",allowBiomeBlend:true,allowClouds:false,
   lastAction:"Ajuste de chunks, teste."
 };
 assert.equal((await ask("/api/metrics","POST",key,{...payload,booster:baseBooster})).code,200);
 const snapshot=await ask("/api/snapshot","GET",key);
 assert.equal(snapshot.json.latest.booster.clientThreadCpuPct,75.2);
 assert.equal(snapshot.json.latest.booster.profile,"igpu");
 assert.equal(snapshot.json.latest.booster.appliedBiomeBlend,2);
 assert.equal((await ask("/api/snapshot","GET",OTHER)).json.latest,null);
 const badKey="edededededededededededededededed";
 assert.equal((await ask("/api/metrics","POST",badKey,{
   ...payload,booster:{...baseBooster,logicalProcessors:"12"}
 })).code,422);
});
