'use strict';
/*
 * Port Metamorph web relay.
 * Public site with code pairing; browser receives a scoped random session token,
 * game device polls using a separate random device token.
 * Only validated cosmetic configs, no server commands or uploaded code.
 * In-memory state is intentionally temporary on free hosting.
 */
const http=require('node:http');
const crypto=require('node:crypto');
const fs=require('node:fs');
const path=require('node:path');
const {URL}=require('node:url');

const PORT=Number(process.env.PORT||3000);
const ROOT=path.resolve(__dirname,'public');
const MAX_BODY=30000, PAIR_TTL=5*60*1000, SESSION_TTL=7*24*60*60*1000;
const devices=new Map(), sessions=new Map(), rate=new Map();
const HEX=/^[0-9a-f]{64}$/, ID=/^[0-9a-f-]{36}$/, CODE=/^[0-9A-F]{8}$/;
const ALLOWED_MATERIALS=new Set(['gold','iron','netherite','amethyst','redstone','copper']);
const ALLOWED_PROFILES=new Set(['normal','nexus','angel','hybrid','custom']);
const ANCHORS=new Set(['body','head','right_arm','left_arm']);
const privateToken=()=>crypto.randomBytes(32).toString('hex');
function json(res,status,obj){
 res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'});
 res.end(JSON.stringify(obj));
}
function limit(req,res){
 const ip=(req.socket.remoteAddress||'unknown');
 const now=Date.now();
 let record=rate.get(ip);
 if(!record||now-record.start>60000)record={start:now,count:0};
 record.count++;rate.set(ip,record);
 if(record.count>90){json(res,429,{error:'Limite de requisições excedido.'});return false;}
 return true;
}
async function body(req){
 let chunks=[],size=0;
 for await(const chunk of req){
  size+=chunk.length;
  if(size>MAX_BODY)throw new Error('too_large');
  chunks.push(chunk);
 }
 const text=Buffer.concat(chunks).toString('utf8');
 const parsed=JSON.parse(text||'{}');
 if(!parsed||Array.isArray(parsed)||typeof parsed!=='object')throw new Error('bad_json');
 return parsed;
}
function bearer(req){
 const h=req.headers.authorization||'';
 return /^Bearer [0-9a-f]{64}$/.test(h)?h.substring(7):'';
}
function session(req,res){
 const key=bearer(req),entry=sessions.get(key);
 if(!entry||entry.expires<Date.now()){
  json(res,401,{error:'Sessão expirada. Pareie novamente.'});return null;
 }
 const device=devices.get(entry.deviceId);
 if(!device||!device.paired){
  json(res,401,{error:'Dispositivo desvinculado. Pareie novamente.'});return null;
 }
 return device;
}
function deviceAuth(req,res,data){
 const dev=devices.get(data.deviceId);
 const token=bearer(req);
 if(!dev||!token||!ID.test(data.deviceId||'')||
    !crypto.timingSafeEqual(Buffer.from(dev.token),Buffer.from(token))){
  json(res,401,{error:'Dispositivo não autorizado.'});return null;
 }
 return dev;
}
function str(v,def,max){
 return typeof v==='string'&&v.trim()?v.trim().slice(0,max):def;
}
function num(v,lo,hi,def){
 const n=Number(v);return Number.isFinite(n)?Math.max(lo,Math.min(hi,n)):def;
}
function bool(v){return v===true;}
function normalizeConfig(input){
 if(!input||typeof input!=='object'||Array.isArray(input))throw new Error('Configuração inválida.');
 const profile=ALLOWED_PROFILES.has(input.profile)?input.profile:'custom';
 const material=ALLOWED_MATERIALS.has(input.material)?input.material:'gold';
 const cubes=Array.isArray(input.cubes)?input.cubes.slice(0,24).map(c=>{
  if(!c||typeof c!=='object')return null;
  return {
   anchor:ANCHORS.has(c.anchor)?c.anchor:'body',
   material:ALLOWED_MATERIALS.has(c.material)?c.material:'gold',
   x:num(c.x,-1.7,1.7,0),y:num(c.y,0,2.8,1),z:num(c.z,-1.7,1.7,0),
   sx:num(c.sx,.02,.9,.15),sy:num(c.sy,.02,.9,.15),sz:num(c.sz,.02,.9,.15)
  };
 }).filter(Boolean):[];
 return {
  name:str(input.name,'Forma personalizada',40).replace(/[\x00-\x1f]/g,' '),
  profile,enabled:bool(input.enabled),wings:bool(input.wings),
  nexus:bool(input.nexus),horns:bool(input.horns),
  wingScale:num(input.wingScale,.3,2.2,1),wingSpeed:num(input.wingSpeed,0,8,2),
  wingMotion:num(input.wingMotion,0,70,25),
  armScale:num(input.armScale,.4,1.8,1),
  transitionSeconds:num(input.transitionSeconds,.2,6,1),
  material,cubes
 };
}
function accessOrigin(req){
 const origin=req.headers.origin;
 if(!origin)return true;
 try{
  const u=new URL(origin);
  return u.host===req.headers.host&&['https:','http:'].includes(u.protocol);
 }catch{return false;}
}
function staticFile(req,res,pathname){
 const files={'/':'index.html','/app.js':'app.js','/styles.css':'styles.css'};
 const file=files[pathname];
 if(!file){res.writeHead(404);res.end('Not found');return;}
 const mime=file.endsWith('.js')?'text/javascript; charset=utf-8':
  file.endsWith('.css')?'text/css; charset=utf-8':'text/html; charset=utf-8';
 res.writeHead(200,{'Content-Type':mime,'Cache-Control':file==='index.html'?'no-cache':'public, max-age=180',
  'X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer',
  'Content-Security-Policy':"default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'"});
 fs.createReadStream(path.join(ROOT,file)).pipe(res);
}
async function handle(req,res){
 let pathname;
 try{pathname=new URL(req.url,'http://localhost').pathname;}catch{json(res,400,{error:'URL inválida'});return;}
 if(req.method==='GET'&&pathname==='/health'){
  json(res,200,{status:'ok',service:'port-metamorph',devices:devices.size});return;
 }
 if(req.method==='GET'&&!pathname.startsWith('/api/')){
  staticFile(req,res,pathname);return;
 }
 if(!pathname.startsWith('/api/')||!['POST','GET'].includes(req.method)){
  json(res,404,{error:'Rota inexistente.'});return;
 }
 if(!accessOrigin(req)){json(res,403,{error:'Origem não autorizada.'});return;}
 if(!limit(req,res))return;
 let input={};
 if(req.method==='POST'){
  if(!String(req.headers['content-type']||'').startsWith('application/json')){
   json(res,415,{error:'JSON obrigatório.'});return;
  }
  try{input=await body(req);}catch{json(res,400,{error:'JSON inválido ou grande demais.'});return;}
 }
 const now=Date.now();
 if(pathname==='/api/device/register'&&req.method==='POST'){
  const {deviceId,deviceToken,code}=input;
  if(typeof deviceId!=='string'||!ID.test(deviceId)||typeof deviceToken!=='string'||
   !HEX.test(deviceToken)||typeof code!=='string'||!CODE.test(code)){
    json(res,400,{error:'Identificação de dispositivo inválida.'});return;
  }
  const collision=[...devices.values()].some(d=>d.code===code&&d.id!==deviceId&&d.codeExpiry>now);
  if(collision){json(res,409,{error:'Código já utilizado. Tente novamente.'});return;}
  devices.set(deviceId,{
   id:deviceId,token:deviceToken,code,codeExpiry:now+PAIR_TTL,paired:false,
   lastSeen:now,config:null,queue:[],lastCommand:0
  });
  json(res,200,{ok:true,expiresIn:300});return;
 }
 if(pathname==='/api/device/poll'&&req.method==='POST'){
  const dev=deviceAuth(req,res,input);if(!dev)return;
  dev.lastSeen=now;
  if(input.config){
   try{dev.config=normalizeConfig(input.config);}catch{}
  }
  const commands=dev.paired?dev.queue.splice(0,10):[];
  json(res,200,{ok:true,paired:dev.paired,commands});return;
 }
 if(pathname==='/api/pair'&&req.method==='POST'){
  if(typeof input.code!=='string'||!CODE.test(input.code)){
   json(res,400,{error:'Use o código de oito caracteres exibido no Minecraft.'});return;
  }
  const dev=[...devices.values()].find(d=>d.code===input.code&&d.codeExpiry>now&&!d.paired);
  if(!dev){json(res,404,{error:'Código inválido, expirado ou já utilizado.'});return;}
  dev.paired=true;dev.code=null;dev.codeExpiry=0;
  const token=privateToken();
  sessions.set(token,{deviceId:dev.id,expires:now+SESSION_TTL});
  json(res,200,{ok:true,token,deviceId:dev.id});return;
 }
 if(pathname==='/api/me'&&req.method==='GET'){
  const dev=session(req,res);if(!dev)return;
  json(res,200,{ok:true,online:now-dev.lastSeen<18000,lastSeen:dev.lastSeen,
   config:dev.config,lastCommand:dev.lastCommand,
   queued:dev.queue.length,deviceId:dev.id});return;
 }
 if(pathname==='/api/control'&&req.method==='POST'){
  const dev=session(req,res);if(!dev)return;
  let config;
  try{config=normalizeConfig(input.config);}catch{json(res,400,{error:'Configuração inválida.'});return;}
  dev.queue=dev.queue.filter(c=>c.type!=='configure');
  dev.queue.push({type:'configure',config});
  dev.lastCommand=now;
  json(res,200,{ok:true,queued:dev.queue.length});return;
 }
 if(pathname==='/api/unpair'&&req.method==='POST'){
  const dev=session(req,res);if(!dev)return;
  dev.paired=false;dev.queue=[];
  for(const [key,value] of sessions)if(value.deviceId===dev.id)sessions.delete(key);
  json(res,200,{ok:true});return;
 }
 json(res,404,{error:'Endpoint não encontrado.'});
}
setInterval(()=>{
 const now=Date.now();
 for(const [id,dev] of devices){
  if(now-dev.lastSeen>2*60*60*1000)devices.delete(id);
 }
 for(const [token,s] of sessions)if(s.expires<now||!devices.has(s.deviceId))sessions.delete(token);
 for(const [ip,record] of rate)if(now-record.start>120000)rate.delete(ip);
},60000).unref();
http.createServer((req,res)=>{
 Promise.resolve(handle(req,res)).catch(e=>{
  console.error('Request error',e.message);
  if(!res.headersSent)json(res,500,{error:'Erro interno.'});
  else res.destroy();
 });
}).listen(PORT,'0.0.0.0',()=>console.log('Port Metamorph web on '+PORT));
