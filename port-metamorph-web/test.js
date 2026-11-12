'use strict';
const test=require('node:test');
const assert=require('node:assert/strict');
const {spawn}=require('node:child_process');
const {randomUUID,randomBytes}=require('node:crypto');
const path=require('node:path');

test('pareamento, sessão, controle visual e entrega ao cliente',async()=>{
 const port=19000+Math.floor(Math.random()*40000);
 const root=path.resolve(__dirname);
 const child=spawn(process.execPath,['server.js'],{
  cwd:root,env:{...process.env,PORT:String(port)},stdio:['ignore','pipe','pipe']
 });
 const url='http://127.0.0.1:'+port;
 async function call(endpoint,body,token){
  const h={'Accept':'application/json'};
  if(body!==undefined)h['Content-Type']='application/json';
  if(token)h.Authorization='Bearer '+token;
  const response=await fetch(url+endpoint,{
   method:body===undefined?'GET':'POST',headers:h,
   body:body===undefined?undefined:JSON.stringify(body)
  });
  return {status:response.status,data:await response.json()};
 }
 try{
  let ready=false;
  for(let i=0;i<75;i++){
   try{const result=await call('/health');if(result.status===200){ready=true;break}}catch{}
   await new Promise(r=>setTimeout(r,100));
  }
  assert.equal(ready,true,'servidor deve iniciar');
  const deviceId=randomUUID(),deviceToken=randomBytes(32).toString('hex'),code='F0E1D2C3';
  let r=await call('/api/device/register',{deviceId,deviceToken,code});
  assert.equal(r.status,200);assert.equal(r.data.ok,true);
  r=await call('/api/pair',{code});
  assert.equal(r.status,200);assert.match(r.data.token,/^[0-9a-f]{64}$/);
  const browserToken=r.data.token;
  r=await call('/api/pair',{code});
  assert.equal(r.status,404,'código não pode ser reutilizado');
  r=await call('/api/me',undefined,browserToken);
  assert.equal(r.status,200);assert.equal(r.data.deviceId,deviceId);
  const config={profile:'hybrid',name:'Forma de teste',enabled:true,wings:true,nexus:true,horns:true,
   wingScale:1.4,wingSpeed:2.1,wingMotion:30,armScale:1,transitionSeconds:1,material:'gold',cubes:[]};
  r=await call('/api/control',{config});
  assert.equal(r.status,401,'controle exige sessão');
  r=await call('/api/control',{config},browserToken);
  assert.equal(r.status,200);assert.equal(r.data.queued,1);
  r=await call('/api/device/poll',{deviceId,config:{...config,enabled:false}},deviceToken);
  assert.equal(r.status,200);assert.equal(r.data.commands.length,1);
  assert.equal(r.data.commands[0].config.profile,'hybrid');
  r=await call('/api/device/poll',{deviceId,config},deviceToken);
  assert.equal(r.status,200);assert.equal(r.data.commands.length,0);
  r=await call('/api/me',undefined,browserToken);
  assert.equal(r.status,200);assert.equal(r.data.config.enabled,true);
  r=await call('/api/unpair',{},browserToken);
  assert.equal(r.status,200);
  r=await call('/api/me',undefined,browserToken);
  assert.equal(r.status,401);
 }finally{
  child.kill('SIGTERM');
 }
});
