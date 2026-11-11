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
 assert.equal(health.code,200);assert.equal(health.json.version,"1.0.0");
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
