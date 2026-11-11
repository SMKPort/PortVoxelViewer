"use strict";
/**
 * Nexus RP 1.2: outbound bridge for a Minecraft-side agent. No inbound RCON port
 * needs to be exposed when this adapter is used. The agent polls jobs via HTTPS.
 * Data is ephemeral on free hosting and not intended for secrets or server backups.
 */
const crypto=require("node:crypto");
const JOB_TTL=90000;
const MAX_EVENTS=250;
function createBridge(options){
  const {send,verifyUser}=options;
  const AGENT_KEY=String(process.env.NEXUS_RP_AGENT_KEY||"");
  const jobs=new Map(),events=[];
  let eventSeq=0,lastSeen=0,connectedPlayer="",lastAgentError="";
  function sameKey(value){
    const a=Buffer.from(String(value||"")),b=Buffer.from(AGENT_KEY);
    return b.length>=24&&a.length===b.length&&crypto.timingSafeEqual(a,b);
  }
  function authAgent(req,res){
    if(!sameKey(req.headers["x-nexus-agent"])){
      send(res,401,{error:"Agente não autorizado. Configure NEXUS_RP_AGENT_KEY."});return false;
    }
    return true;
  }
  function authUser(req,res){
    if(!verifyUser(req)){
      send(res,401,{error:"Código de acesso do Nexus inválido."});return false;
    }
    return true;
  }
  function cleanup(){
    const now=Date.now();
    for(const [id,j] of jobs){if(now-j.createdAt>JOB_TTL){if(!j.done){j.done=true;j.ok=false;j.error="Tempo limite: agente offline ou comando não respondido.";j.finishedAt=now;}if(now-j.createdAt>240000)jobs.delete(id);}}
  }
  function enqueue(command){
    cleanup();if(jobs.size>100)throw Error("Fila de comandos cheia.");
    const id=crypto.randomUUID();jobs.set(id,{id,command,createdAt:Date.now(),claimedAt:0,finishedAt:0,done:false,ok:null,output:"",error:""});
    return {ok:true,queued:true,jobId:id};
  }
  function readJson(req,max=8192){return new Promise((resolve,reject)=>{
    let raw="";req.on("data",b=>{raw+=b.toString("utf8");if(raw.length>max){reject(Error("Payload excessivo."));req.destroy();}});
    req.on("end",()=>{try{const parsed=JSON.parse(raw);if(!parsed||typeof parsed!=="object")throw Error("JSON inválido.");resolve(parsed);}catch(e){reject(e);}});req.on("error",reject);
  });}
  function status(){
    return {online:Date.now()-lastSeen<15000,lastSeen:lastSeen?new Date(lastSeen).toISOString():null,player:connectedPlayer||null,queue:[...jobs.values()].filter(j=>!j.done).length,eventId:eventSeq,configured:AGENT_KEY.length>=24,agentError:lastAgentError||null};
  }
  async function handle(req,res,route,url){
    const agentRoutes=["/api/bridge/jobs","/api/bridge/ack","/api/bridge/event"];
    const userRoutes=["/api/game/events","/api/bridge/result","/api/bridge/status"];
    if(!agentRoutes.includes(route)&&!userRoutes.includes(route))return false;
    cleanup();
    if(agentRoutes.includes(route)){
      if(!authAgent(req,res))return true;
      lastSeen=Date.now();
      if(route==="/api/bridge/jobs"&&req.method==="GET"){
        const ready=[];
        for(const j of jobs.values()){
          if(j.done||j.claimedAt&&Date.now()-j.claimedAt<18000)continue;
          j.claimedAt=Date.now();
          ready.push({id:j.id,command:j.command});
          if(ready.length>=5)break;
        }
        send(res,200,{ok:true,jobs:ready,time:new Date().toISOString()});return true;
      }
      if(req.method!=="POST"){send(res,405,{error:"Método não suportado."});return true;}
      try{
        const data=await readJson(req);
        if(route==="/api/bridge/ack"){
          const j=jobs.get(String(data.jobId||""));
          if(!j){send(res,404,{error:"Job desconhecido."});return true;}
          if(!j.done){j.done=true;j.ok=data.ok===true;j.error=String(data.error||"").slice(0,250);j.output=String(data.output||"").slice(0,1200);j.finishedAt=Date.now();}
          send(res,200,{ok:true});return true;
        }
        const kind=String(data.type||"").toLowerCase();
        const allowed=new Set(["damage","heal","death","respawn","effect","effect_end","health","join","leave","armor","energy"]);
        const player=String(data.player||"").trim();
        if(!allowed.has(kind)||!(/^[A-Za-z0-9_]{3,16}$/.test(player))){send(res,422,{error:"Evento ou jogador inválido."});return true;}
        const amount=Math.max(0,Math.min(40,Number(data.amount)||0));
        const health=Number.isFinite(Number(data.health))?Math.max(0,Math.min(40,Number(data.health))):null;
        const note=String(data.note||"").slice(0,150);
        const item={id:++eventSeq,type:kind,player,amount,health,note,at:new Date().toISOString()};
        events.push(item);if(events.length>MAX_EVENTS)events.shift();
        connectedPlayer=player;lastAgentError="";
        send(res,200,{ok:true,eventId:item.id});return true;
      }catch(err){lastAgentError=String(err.message).slice(0,200);send(res,400,{error:lastAgentError});return true;}
    }
    if(!authUser(req,res))return true;
    if(req.method!=="GET"){send(res,405,{error:"Método não suportado."});return true;}
    if(route==="/api/bridge/status"){send(res,200,{ok:true,...status()});return true;}
    if(route==="/api/bridge/result"){
      const j=jobs.get(String(url.searchParams.get("id")||""));
      if(!j){send(res,404,{error:"Comando não encontrado."});return true;}
      send(res,200,{ok:true,done:j.done,success:j.ok,output:j.output,error:j.error,jobId:j.id});return true;
    }
    const since=Math.max(0,Number(url.searchParams.get("since")||0)||0);
    const player=String(url.searchParams.get("player")||"").slice(0,16);
    send(res,200,{ok:true,events:events.filter(e=>e.id>since&&(!player||e.player.toLowerCase()===player.toLowerCase())).slice(-80),lastId:eventSeq,bridge:status()});return true;
  }
  return {handle,enqueue,status};
}
module.exports={createBridge};
