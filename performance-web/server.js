"use strict";
const http=require("node:http");
const fs=require("node:fs");
const path=require("node:path");
const crypto=require("node:crypto");
const ROOT=path.join(__dirname,"public");
const PORT=Number(process.env.PORT||3000);
const MAX_STREAMS=100,MAX_SAMPLES=720,STALE_MS=15000,EXPIRY_MS=6*3600_000;
const ALLOWED_ACTIONS=new Set(["observe","auto","off","reset"]);
const streams=new Map();
const now=()=>Date.now();
const json=(res,status,body)=>{
 if(res.headersSent)return;
 const data=JSON.stringify(body);
 res.writeHead(status,{"Content-Type":"application/json; charset=utf-8","Content-Length":Buffer.byteLength(data),"Cache-Control":"no-store","X-Content-Type-Options":"nosniff","Referrer-Policy":"no-referrer"});
 res.end(data);
};
const respondError=(res,status,error)=>json(res,status,{ok:false,error});
const keyOk=key=>typeof key==="string"&&/^[a-f0-9]{32}$/i.test(key);
const digest=key=>crypto.createHash("sha256").update(key.toLowerCase()).digest("hex");
const finite=(v,min,max)=>typeof v==="number"&&Number.isFinite(v)&&v>=min&&v<=max;
function validBooster(payload){
 if(!payload||typeof payload!=="object"||Array.isArray(payload))return null;
 const b=payload;
 if(!["off","observe","adaptive"].includes(b.mode)||
    !["idle","observing","baseline","trial","target-met","no-change","kept","reverted"].includes(b.phase)||
    !Number.isInteger(b.targetFps)||b.targetFps<30||b.targetFps>240||
    typeof b.webControlEnabled!=="boolean"||typeof b.changedOption!=="boolean")return null;
 for(const key of ["baselineAvgFps","trialAvgFps","differencePct","secondsInPhase",
                   "originalEntityScale","appliedEntityScale","low1SampledFps"]){
   if(!Number.isFinite(b[key])||Math.abs(b[key])>10000)return null;
 }
 return {
   mode:b.mode,phase:b.phase,targetFps:b.targetFps,
   webControlEnabled:b.webControlEnabled,changedOption:b.changedOption,
   baselineAvgFps:b.baselineAvgFps,trialAvgFps:b.trialAvgFps,
   differencePct:b.differencePct,secondsInPhase:b.secondsInPhase,
   originalEntityScale:b.originalEntityScale,appliedEntityScale:b.appliedEntityScale,
   low1SampledFps:b.low1SampledFps,
   lastAction:String(b.lastAction||"").slice(0,180)
 };
}
function prune(){
 for(const [key,v] of streams)if(now()-v.lastSeen>EXPIRY_MS)streams.delete(key);
 if(streams.size>MAX_STREAMS){const entries=[...streams.entries()].sort((a,b)=>a[1].lastSeen-b[1].lastSeen);for(const [key] of entries.slice(0,streams.size-MAX_STREAMS))streams.delete(key);}
}
async function readJson(req,max=2048){
 let content="";for await(const c of req){content+=c.toString();if(content.length>max)throw Error("payload excessivo");}
 let body;try{body=JSON.parse(content)}catch(e){throw Error("JSON inválido")}
 if(!body||typeof body!=="object"||Array.isArray(body))throw Error("objeto JSON obrigatório");
 return body;
}
function auth(req,res){
 const key=req.headers["x-port-perf-key"];
 if(!keyOk(key)){respondError(res,401,"Informe a chave de pareamento de 32 caracteres gerada no Minecraft.");return null;}
 return digest(key);
}
function staticFile(req,res,pathname){
 const lookup={"/":"/index.html","/index.html":"/index.html","/style.css":"/style.css","/app.js":"/app.js"};
 const file=lookup[pathname];if(!file)return respondError(res,404,"Rota não encontrada.");
 const full=path.join(ROOT,file.slice(1));
 fs.readFile(full,(error,data)=>{
   if(error)return respondError(res,404,"Arquivo ausente.");
   const type=file.endsWith(".html")?"text/html; charset=utf-8":file.endsWith(".css")?"text/css; charset=utf-8":"text/javascript; charset=utf-8";
   res.writeHead(200,{"Content-Type":type,"Cache-Control":"no-store","X-Content-Type-Options":"nosniff","X-Frame-Options":"DENY","Referrer-Policy":"no-referrer","Content-Security-Policy":"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; object-src 'none'; frame-ancestors 'none'","Content-Length":data.length});res.end(data);
 });
}
const server=http.createServer(async(req,res)=>{
 let route;try{route=new URL(req.url,"http://localhost").pathname}catch(e){return respondError(res,400,"URL inválida");}
 if(route==="/api/health"&&req.method==="GET")return json(res,200,{ok:true,status:"online",app:"Port Performance Monitor",version:"2.0.0",streaming:"opt-in",storage:"memory"});
 if(route==="/api/metrics"&&req.method==="POST"){
   const key=auth(req,res);if(!key)return;
   try{
     const body=await readJson(req);
     const fields=["fps","ping","memoryUsedMb","memoryAllocatedMb","memoryMaxMb"];
     for(const k of fields)if(!finite(body[k],k==="ping"?-1:0,k==="fps"?5000:k==="ping"?50000:100000))return respondError(res,422,"Métrica inválida: "+k);
     if(body.memoryUsedMb>body.memoryAllocatedMb||body.memoryAllocatedMb>body.memoryMaxMb+1)return respondError(res,422,"Valores de memória inconsistentes.");
     let stream=streams.get(key);if(stream&&now()-stream.lastSeen<1100)return respondError(res,429,"Máximo de uma atualização por segundo.");
     if(!stream){
       prune();stream={name:"",lastSeen:0,series:[],latest:null,events:[],pendingCommand:null};
       streams.set(key,stream);
     }
     const name=String(body.player||"").slice(0,16).replace(/[^a-z0-9_]/ig,"");
     const t=now();
     const booster=body.booster===undefined?null:validBooster(body.booster);
     if(body.booster!==undefined&&!booster)return respondError(res,422,"Dados do booster inválidos.");
     const lastBooster=stream.latest?.booster;
     if(booster&&(booster.phase!==lastBooster?.phase||booster.mode!==lastBooster?.mode||
         booster.changedOption!==lastBooster?.changedOption)){
       stream.events.push({t,mode:booster.mode,phase:booster.phase,description:booster.lastAction});
       if(stream.events.length>100)stream.events.splice(0,stream.events.length-100);
     }
     const metric={t,fps:Math.round(body.fps),ping:Math.round(body.ping),memoryUsedMb:Math.round(body.memoryUsedMb),memoryAllocatedMb:Math.round(body.memoryAllocatedMb),memoryMaxMb:Math.round(body.memoryMaxMb),booster};
     stream.name=name||"Jogador";stream.lastSeen=t;stream.latest=metric;stream.series.push(metric);if(stream.series.length>MAX_SAMPLES)stream.series.splice(0,stream.series.length-MAX_SAMPLES);
     const pending=stream.pendingCommand;
     const command=pending&&pending.expires>t&&booster?.webControlEnabled?pending.command:null;
     if(command||pending?.expires<=t)stream.pendingCommand=null;
     json(res,200,{ok:true,receivedAt:t,command});
   }catch(e){respondError(res,400,String(e.message).slice(0,160));}
   return;
 }
 if(route==="/api/booster/control"&&req.method==="POST"){
   const key=auth(req,res);if(!key)return;
   try{
     const body=await readJson(req,1024);
     if(!ALLOWED_ACTIONS.has(body.action))return respondError(res,422,"Ação não autorizada.");
     if(body.targetFps!==undefined&&
       (!Number.isInteger(body.targetFps)||body.targetFps<30||body.targetFps>240))
       return respondError(res,422,"Meta de FPS inválida.");
     const stream=streams.get(key);
     if(!stream||now()-stream.lastSeen>STALE_MS||stream.latest?.booster?.webControlEnabled!==true)
       return respondError(res,409,"Ative /portperf stream on e /portperf webcontrol on no Minecraft.");
     const t=now();
     if(stream.lastControl&&t-stream.lastControl<1400)
       return respondError(res,429,"Aguarde antes de enviar outro comando.");
     stream.lastControl=t;
     stream.pendingCommand={command:{action:body.action,
       ...(body.targetFps!==undefined?{targetFps:body.targetFps}:{})},expires:t+90000};
     json(res,200,{ok:true,queued:true,message:"Ação na fila até a próxima amostra HTTPS."});
   }catch(e){respondError(res,400,String(e.message).slice(0,140));}
   return;
 }
 if(route==="/api/snapshot"&&req.method==="GET"){
   const key=auth(req,res);if(!key)return;
   prune();const stream=streams.get(key);
   if(!stream)return json(res,200,{ok:true,online:false,name:"",latest:null,series:[],events:[],lastSeen:null});
   return json(res,200,{ok:true,online:now()-stream.lastSeen<STALE_MS,name:stream.name,latest:stream.latest,series:stream.series,events:stream.events||[],lastSeen:stream.lastSeen});
 }
 if(req.method==="GET"||req.method==="HEAD")return staticFile(req,res,route);
 respondError(res,405,"Método não permitido.");
});
if(require.main===module)server.listen(PORT,"0.0.0.0",()=>console.log("Port Performance Monitor listening on "+PORT));
module.exports={server,streams};
