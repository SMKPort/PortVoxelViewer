"use strict";
const http=require("node:http");
const ORIGIN="https://nexus-aterna-web.onrender.com";
const PORT=Number(process.env.PORT)||10000;
const rate=new Map();
function send(res,status,obj,origin){res.writeHead(status,{"Content-Type":"application/json; charset=utf-8","Cache-Control":"no-store",...(origin?{"Access-Control-Allow-Origin":origin,"Access-Control-Allow-Methods":"POST, GET, OPTIONS","Access-Control-Allow-Headers":"Content-Type, X-Aterna-Access","Vary":"Origin"}:{})});res.end(JSON.stringify(obj));}
function validOrigin(req){const orig=req.headers.origin||"";if(orig===ORIGIN)return orig;if(/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(orig))return orig;return null;}
function limit(req){const ip=(req.socket.remoteAddress||"anon").slice(0,70);const now=Date.now();let entry=rate.get(ip);if(!entry||now-entry.start>60000){entry={start:now,count:0};rate.set(ip,entry);}return ++entry.count<=20;}
const system="Você é ATERNA, assistente técnico-narrativa do Nexus Aterna de Port Morningstar. Responda em português brasileiro, de modo útil e conciso, usando o cânone fornecido pelo operador. O universo narrativo tem a deusa Anael, magia, tecnologia e os TS. Diferencie claramente fenômenos de roleplay das capacidades físicas reais. Não alegue ter sensores ou controle de equipamento sem integração confirmada. Se não souber, diga que não sabe. Não faça promessas de ações fora do sistema.";
http.createServer(async(req,res)=>{
const origin=validOrigin(req);const url=new URL(req.url||"/","http://localhost");
if(req.method==="OPTIONS"){if(!origin)return send(res,403,{error:"Origem não autorizada."});res.writeHead(204,{"Access-Control-Allow-Origin":origin,"Access-Control-Allow-Methods":"GET, POST, OPTIONS","Access-Control-Allow-Headers":"Content-Type, X-Aterna-Access","Vary":"Origin"});return res.end();}
if(url.pathname==="/api/health"&&req.method==="GET")return send(res,200,{status:"online",provider:"Gemini",model:process.env.GEMINI_MODEL||"gemini-2.5-flash",configured:Boolean(process.env.GEMINI_API_KEY&&process.env.ATERNA_ACCESS_CODE)},origin);
if(url.pathname!=="/api/chat"||req.method!=="POST")return send(res,404,{error:"Rota não encontrada."},origin);
if(!origin)return send(res,403,{error:"Origem não autorizada."});
if(!limit(req))return send(res,429,{error:"Limite de solicitações por minuto."},origin);
const access=process.env.ATERNA_ACCESS_CODE||"",key=process.env.GEMINI_API_KEY||"";
if(!access||!key)return send(res,503,{error:"IA remota ainda não configurada no Render. Use o assistente local."},origin);
const supplied=String(req.headers["x-aterna-access"]||"");if(supplied!==access)return send(res,401,{error:"Código de acesso incorreto."},origin);
let data="";try{for await(const chunk of req){data+=chunk.toString();if(data.length>25000)throw Error("Requisição muito grande.");}}catch(e){return send(res,413,{error:"Limite de tamanho excedido."},origin);}
let payload;try{payload=JSON.parse(data);}catch(e){return send(res,400,{error:"JSON inválido."},origin);}
const message=String(payload.message||"").slice(0,2500).trim(),context=String(payload.context||"").slice(0,4000);if(!message)return send(res,400,{error:"Mensagem vazia."},origin);
const history=Array.isArray(payload.history)?payload.history.slice(-10).filter(x=>x&&typeof x==="object").map(x=>({role:x.role==="model"?"model":"user",parts:[{text:String(x.text||"").slice(0,1200)}]})):[];
const contents=[...history,{role:"user",parts:[{text:message}]}];
const model=(process.env.GEMINI_MODEL||"gemini-2.5-flash").replace(/[^a-zA-Z0-9._-]/g,"");
try{
const upstream=await fetch("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent",{method:"POST",headers:{"Content-Type":"application/json","x-goog-api-key":key},body:JSON.stringify({systemInstruction:{parts:[{text:system+"\nContexto conhecido (fornecido pelo usuário, tratar como narrativa e dados locais): "+context}]},contents,generationConfig:{temperature:.6,maxOutputTokens:700}}),signal:AbortSignal.timeout(22000)});
const result=await upstream.json().catch(()=>({}));
if(!upstream.ok)return send(res,502,{error:"Erro da API Gemini (HTTP "+upstream.status+"). Verifique modelo e credencial no Render."},origin);
const answer=(result.candidates?.[0]?.content?.parts||[]).map(p=>p.text||"").join("").trim();
if(!answer)return send(res,502,{error:"A IA não retornou texto."},origin);
return send(res,200,{reply:answer.slice(0,5000),provider:"Gemini",model},origin);
}catch(e){return send(res,502,{error:e.name==="TimeoutError"?"Tempo limite da IA.":"Serviço de IA indisponível."},origin);}
}).listen(PORT,"0.0.0.0",()=>console.log("Nexus Aterna API: listening",PORT));