"use strict";
/**
 * Nexus Aterna RP | HTTPS console + Minecraft Java RCON gateway.
 * No dependencies. Requires NODE >=20. See README for security.
 */
const http=require("node:http");
const fs=require("node:fs/promises");
const path=require("node:path");
const net=require("node:net");
const dns=require("node:dns/promises");
const crypto=require("node:crypto");
const {createBridge}=require("./bridge.js");

const ROOT=path.join(__dirname,"public");
const PORT=Number(process.env.PORT)||10000;
const ACCESS=String(process.env.NEXUS_RP_ACCESS_KEY||"");
const BODY_LIMIT=8192;
const limits=new Map();
const types={".html":"text/html; charset=utf-8",".css":"text/css; charset=utf-8",".js":"text/javascript; charset=utf-8",".svg":"image/svg+xml",".json":"application/json; charset=utf-8",".webmanifest":"application/manifest+json"};
const security={
  "X-Content-Type-Options":"nosniff","Referrer-Policy":"no-referrer","X-Frame-Options":"DENY",
  "Permissions-Policy":"camera=(), microphone=(), geolocation=()",
  "Content-Security-Policy":"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"
};
function json(res,status,data){res.writeHead(status,{...security,"Content-Type":"application/json; charset=utf-8","Cache-Control":"no-store"});res.end(JSON.stringify(data));}
function replyError(err){const message=String(err?.message||"Erro de conexão.");return {error:message.slice(0,350)};}
function checkToken(actual){
  const a=Buffer.from(String(actual||"")),b=Buffer.from(ACCESS);
  return b.length>=20&&a.length===b.length&&crypto.timingSafeEqual(a,b);
}
function rateLimit(req){
  const ip=req.socket.remoteAddress||"unknown",now=Date.now();
  let item=limits.get(ip);if(!item||now-item.start>60000){item={start:now,count:0};limits.set(ip,item);}
  return ++item.count<=30;
}
function isPublicIP(address){
  if(net.isIP(address)===4){
    const v=address.split(".").map(Number),[a,b,c]=v;
    if([0,10,127].includes(a)||a>=224)return false;
    if(a===100&&b>=64&&b<=127)return false;
    if(a===169&&b===254)return false;
    if(a===172&&b>=16&&b<=31)return false;
    if(a===192&&([0,168].includes(b)||b===88&&c===99))return false;
    if(a===198&&(b===18||b===19||b===51&&c===100))return false;
    if(a===203&&b===0&&c===113)return false;
    if(a===192&&b===0&&c===2)return false;
    return true;
  }
  if(net.isIP(address)===6){
    const s=address.toLowerCase();
    if(s==="::"||s==="::1"||s.startsWith("fe8")||s.startsWith("fe9")||s.startsWith("fea")||s.startsWith("feb")||s.startsWith("fc")||s.startsWith("fd")||s.startsWith("ff"))return false;
    if(s.includes("::ffff:"))return false;
    return true;
  }
  return false;
}
async function resolveHost(host){
  if(typeof host!=="string"||host.length>253||host.length<3||!/^([a-z0-9.-]+|[0-9a-f:]+)$/i.test(host))throw Error("Endereço do servidor inválido.");
  const h=host.toLowerCase().replace(/\.$/,"");
  if(h==="localhost"||h.endsWith(".local")||h.endsWith(".internal")||h.includes(".."))throw Error("Servidor interno não pode ser acessado pela ponte na nuvem.");
  const records=await dns.lookup(h,{all:true,verbatim:true});
  if(!records.length||records.some(x=>!isPublicIP(x.address)))throw Error("Somente endereços públicos são aceitos pela ponte RCON.");
  return records.find(x=>x.family===4)||records[0];
}
function makePacket(id,type,body=""){
  const text=Buffer.from(body,"utf8"),size=10+text.length;const out=Buffer.alloc(4+size);
  out.writeInt32LE(size,0);out.writeInt32LE(id,4);out.writeInt32LE(type,8);text.copy(out,12);return out;
}
async function rcon(config,command){
  const host=String(config.host||"").trim(),port=Number(config.port||25575),password=String(config.password||"");
  if(!Number.isInteger(port)||port<1024||port>65535)throw Error("Porta RCON inválida (1024–65535).");
  if(password.length<1||password.length>256)throw Error("Informe a senha RCON.");
  const resolved=await resolveHost(host);
  return await new Promise((resolve,reject)=>{
    let finished=false,authorized=false,acc=Buffer.alloc(0);
    const socket=net.createConnection({host:resolved.address,port,family:resolved.family});
    socket.setTimeout(8000);
    const finish=(err,value)=>{if(finished)return;finished=true;socket.destroy();if(err)reject(err);else resolve(String(value||"").slice(0,2000));};
    socket.on("connect",()=>socket.write(makePacket(1101,3,password)));
    socket.on("timeout",()=>finish(Error("Tempo limite RCON. Confirme porta, firewall e servidor ativo.")));
    socket.on("error",e=>finish(Error("Não foi possível alcançar o servidor RCON: "+e.code)));
    socket.on("close",()=>{if(!finished)finish(Error("Conexão RCON encerrada antes da resposta."));});
    socket.on("data",chunk=>{
      acc=Buffer.concat([acc,chunk]);
      while(acc.length>=4){
        const size=acc.readInt32LE(0);
        if(size<10||size>1048576)return finish(Error("Resposta RCON inválida."));
        if(acc.length<size+4)break;
        const msg=acc.subarray(4,size+4);acc=acc.subarray(size+4);
        const id=msg.readInt32LE(0),type=msg.readInt32LE(4),text=msg.subarray(8,msg.length-2).toString("utf8");
        if(id===-1)return finish(Error("Senha RCON rejeitada pelo servidor."));
        if(!authorized&&id===1101&&type===2){authorized=true;socket.write(makePacket(1102,2,command));continue;}
        if(authorized&&id===1102)return finish(null,text);
      }
    });
  });
}
function cleanText(text,max=280){
  if(typeof text!=="string")throw Error("Mensagem inválida.");
  const s=text.replace(/[\r\n\t\u0000-\u001f\u007f]/g," ").replace(/\s{2,}/g," ").trim();
  if(s.length<1||s.length>max)throw Error("Mensagem precisa ter entre 1 e "+max+" caracteres.");
  return s;
}
function buildTellraw(data){
  const message=cleanText(data.message,280);
  const prefix=cleanText(data.prefix||"[NEXUS ATERNA]",38);
  const allowed=["gold","red","dark_red","white","gray","light_purple","yellow","aqua","green"];
  const color=allowed.includes(data.color)?data.color:"white";
  const target=data.target==="player"&&/^[A-Za-z0-9_]{3,16}$/.test(data.player||"")?data.player:"@a";
  const body=[{text:prefix+" ",color:"gold",bold:true},{text:message,color}];
  return {command:"tellraw "+target+" "+JSON.stringify(body),target};
}
function validFunction(val){
  if(typeof val!=="string"||!(/^[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,90}$/.test(val)))throw Error("Use um ID de função, por exemplo nexus:armadura/integrar.");
  return "function "+val;
}
async function parseRequest(req){
  let raw="";
  for await(const block of req){raw+=block.toString("utf8");if(Buffer.byteLength(raw)>BODY_LIMIT)throw Error("Requisição acima do limite permitido.");}
  let body;try{body=JSON.parse(raw);}catch(e){throw Error("JSON inválido.");}
  if(!body||typeof body!=="object")throw Error("Dados inválidos.");
  if(!body.server||typeof body.server!=="object")throw Error("Configuração do servidor ausente.");
  return body;
}
const bridge=createBridge({send:json,verifyUser:req=>checkToken(req.headers["x-nexus-key"])});
async function serve(req,res,route){
  const requested=route==="/"?"index.html":decodeURIComponent(route).slice(1);
  if(!/^(index\.html|styles\.css|upgrade\.css|evolution\.css|views\.js|upgrade\.js|evolution\.js|app\.js|icon\.svg|manifest\.webmanifest)$/.test(requested))return json(res,404,{error:"Arquivo não encontrado."});
  try{
    const file=await fs.readFile(path.join(ROOT,requested));
    res.writeHead(200,{...security,"Content-Type":types[path.extname(requested)]||"application/octet-stream","Cache-Control":requested==="index.html"?"no-cache":"public, max-age=300"});
    res.end(file);
  }catch(e){json(res,500,{error:"Falha ao carregar a interface."});}
}
const server=http.createServer(async(req,res)=>{
  let route,url;try{url=new URL(req.url||"/","http://localhost");route=url.pathname;}catch(e){return json(res,400,{error:"URL inválida."});}
  if(req.method==="GET"&&route==="/api/health")return json(res,200,{status:"online",name:"Nexus Aterna RP",version:"1.2",bridgeReady:ACCESS.length>=20,agent:bridge.status(),adapter:"Minecraft Java RCON + HTTPS agent",configuredOnBrowser:true});
  if(await bridge.handle(req,res,route,url))return;
  if(req.method==="GET")return serve(req,res,route);
  if(req.method!=="POST"||!["/api/rcon/test","/api/rcon/message","/api/rcon/function"].includes(route))return json(res,404,{error:"Rota inexistente."});
  if(!rateLimit(req))return json(res,429,{error:"Limite de requisições. Aguarde um minuto."});
  if(!ACCESS)return json(res,503,{error:"A ponte ainda precisa de NEXUS_RP_ACCESS_KEY nas variáveis privadas da Render."});
  const origin=req.headers.origin;const host=req.headers.host;
  if(origin){let valid=false;try{valid=new URL(origin).host===host;}catch(e){}if(!valid)return json(res,403,{error:"Origem não autorizada."});}
  if(!checkToken(req.headers["x-nexus-key"]))return json(res,401,{error:"Código de acesso do Nexus inválido."});
  try{
    const data=await parseRequest(req);
    if(route==="/api/rcon/test"){if(data.server.mode==="agent")return json(res,200,bridge.enqueue("list"));const response=await rcon(data.server,"list");return json(res,200,{ok:true,response,adapter:"RCON"});}
    if(route==="/api/rcon/message"){const payload=buildTellraw(data);if(data.server.mode==="agent")return json(res,200,bridge.enqueue(payload.command));const response=await rcon(data.server,payload.command);return json(res,200,{ok:true,target:payload.target,response});}
    if(route==="/api/rcon/function"){const command=validFunction(data.functionId);if(data.server.mode==="agent")return json(res,200,bridge.enqueue(command));const response=await rcon(data.server,command);return json(res,200,{ok:true,response});}
  }catch(e){return json(res,400,replyError(e));}
});
server.listen(PORT,"0.0.0.0",()=>console.log("Nexus Aterna RP listening on port "+PORT));