"use strict";
/* Nexus Client: ephemeral pairing and authorized Java client relay. */
const crypto=require("node:crypto");
const {getQuestion}=require("./voice_reply.js");
const {askGemini,aiStatus}=require("./ai_conversation.js");
const {transcribeClip}=require("./audio_transcription.js");
const {synthesizeSpeech}=require("./speech_synthesis.js");
const JOB_MAX_AGE=120000;
function createClientRelay({send,verifyUser,limit,askAI=askGemini,getAIStatus=aiStatus,transcribe=transcribeClip,synthesize=synthesizeSpeech}){
  let pair=null,session=null,eventId=0,mind=null,monitor=null,alertsState=null,hearingState=null,arcaneState=null;
  let transcribing=0,ttsBusy=false,lastTtsAt=0;
  const clipTimes=[],replyTimes=new Map();
  const jobs=new Map(),events=[];
  const err=(res,code,message)=>{send(res,code,{ok:false,error:message});return true;};
  const hash=s=>crypto.createHash("sha256").update(String(s||"")).digest("hex");
  function tokenOk(req){
    return session&&session.tokenHash===hash(req.headers["x-nexus-client"]);
  }
  function guardUser(req,res){if(!verifyUser(req)){err(res,401,"Código de acesso do painel incorreto.");return false;}return true;}
  function guardClient(req,res){if(!tokenOk(req)){err(res,401,"Cliente não pareado ou sessão expirada.");return false;}return true;}
  async function body(req,max=5000){
    let raw="";for await(const chunk of req){raw+=chunk.toString("utf8");if(raw.length>max)throw Error("Requisição excede o limite.");}
    try{const json=JSON.parse(raw);if(!json||typeof json!=="object")throw Error();return json;}catch(e){throw Error("JSON inválido.");}
  }
  function health(){
    return {online:!!session&&Date.now()-session.lastSeen<15000,paired:!!session,player:session?.player||null,lastSeen:session?.lastSeen?new Date(session.lastSeen).toISOString():null,events:eventId,pending: [...jobs.values()].filter(j=>!j.done).length,mode:"client"};
  }
  function cleanup(){
    for(const [id,j] of jobs)if(Date.now()-j.createdAt>JOB_MAX_AGE){if(!j.done){j.done=true;j.success=false;j.error="Cliente desconectado ou tempo esgotado.";}if(Date.now()-j.createdAt>240000)jobs.delete(id);}
  }
  function enqueue(type,payload,readyAt=Date.now()){
    cleanup();if(!session||Date.now()-session.lastSeen>15000)throw Error("Nexus Client offline. Abra o Minecraft e verifique o pareamento.");
    const pending=[...jobs.values()].filter(x=>!x.done).length;
    if(pending>=20)throw Error("Fila cheia. Aguarde concluir as ações atuais.");
    const id=crypto.randomUUID(),job={id,type,payload,createdAt:Date.now(),readyAt,claimedAt:0,done:false,success:null,error:"",output:""};
    jobs.set(id,job);return {ok:true,queued:true,jobId:id};
  }
  function numericValue(value,min,max,fallback){
    if(value===null||value===undefined||value==="")return fallback;
    const n=Number(value);
    return Number.isFinite(n)?Math.max(min,Math.min(max,n)):fallback;
  }
  const ALERT_KINDS=new Set(["heavy","streak","structure","risk","recovery","critical","mind"]);
  const ALERT_FLAGS=["heavy","streak","structure","risk","recovery","critical"];
  function alertSettings(data){
    const settings={};
    for(const kind of ALERT_FLAGS){
      if(typeof data[kind]!=="boolean")throw Error("Configuração inválida para "+kind+".");
      settings[kind]=data[kind];
    }
    if(!Number.isInteger(data.cooldownSeconds)||data.cooldownSeconds<15||data.cooldownSeconds>180)
      throw Error("O intervalo entre avisos deve estar entre 15 e 180 segundos.");
    settings.cooldownSeconds=data.cooldownSeconds;
    return settings;
  }
  function alertHistory(data){
    if(!Array.isArray(data))throw Error("Histórico de alertas inválido.");
    return data.slice(-30).map(item=>{
      if(!item||!ALERT_KINDS.has(item.kind)||typeof item.text!=="string"||
        item.text.length>210||item.text.length<1||
        typeof item.at!=="string"||Number.isNaN(Date.parse(item.at)))
        throw Error("Entrada inválida no histórico de alertas.");
      return {
        kind:item.kind,
        text:item.text.replace(/[\u0000-\u001f\u007f]/g," ").slice(0,210),
        at:new Date(item.at).toISOString()
      };
    });
  }
  function splitChatReply(text){
    let remaining=String(text).replace(/[\u0000-\u001f\u007f]/g," ").replace(/\s+/g," ").trim();
    const chunks=[];
    while(remaining&&chunks.length<2){
      if(remaining.length<=194){chunks.push(remaining);break;}
      let cut=remaining.lastIndexOf(" ",194);
      if(cut<105)cut=194;
      chunks.push(remaining.slice(0,cut).trim());
      remaining=remaining.slice(cut).trim();
    }
    return chunks;
  }
  async function handle(req,res,route,url){
    if(!route.startsWith("/api/client/"))return false;
    cleanup();
    if(["/api/client/pair/new","/api/client/pair/exchange","/api/client/chat","/api/client/voice","/api/client/action","/api/client/monitor/settings","/api/client/alerts/settings","/api/client/hearing/settings","/api/client/hearing/fact","/api/client/mind/settings","/api/client/arcane/settings"].includes(route)&&!limit(req))return err(res,429,"Limite de requisições. Reduza a frequência.");
    const userPaths=new Set(["/api/client/pair/new","/api/client/status","/api/client/events","/api/client/chat","/api/client/voice","/api/client/ai/status","/api/client/action","/api/client/job","/api/client/monitor/settings","/api/client/alerts","/api/client/alerts/settings","/api/client/hearing","/api/client/hearing/settings","/api/client/hearing/fact","/api/client/mind/settings","/api/client/arcane","/api/client/arcane/settings"]);
    const clientPaths=new Set(["/api/client/pair/exchange","/api/client/poll","/api/client/telemetry","/api/client/mind","/api/client/ack","/api/client/monitor/state","/api/client/alerts/state","/api/client/hearing/transcribe","/api/client/hearing/state","/api/client/hearing/reply","/api/client/tts","/api/client/arcane/state"]);
    if(!userPaths.has(route)&&!clientPaths.has(route))return err(res,404,"Rota desconhecida.");
    if(userPaths.has(route)&&!guardUser(req,res))return true;
    if(route==="/api/client/pair/new"&&req.method==="POST"){
      // A code is single use, short lived and only usable to bootstrap a strong token.
      pair={code:crypto.randomBytes(5).toString("hex").toUpperCase(),expires:Date.now()+600000};
      send(res,200,{ok:true,code:pair.code,expiresInSeconds:600,instructions:"No Minecraft: /nexusclient pair CODIGO"});return true;
    }
    if(route==="/api/client/pair/exchange"&&req.method==="POST"){
      try{
        const data=await body(req,1000),code=String(data.code||"").trim().toUpperCase();
        if(!pair||Date.now()>pair.expires||code!==pair.code)return err(res,401,"Código inválido ou expirado.");
        const token=crypto.randomBytes(32).toString("hex");pair=null;
        const player=String(data.player||"").slice(0,16);
        session={tokenHash:hash(token),player:/^[A-Za-z0-9_]{3,16}$/.test(player)?player:"",lastSeen:Date.now()};
        jobs.clear();events.splice(0,events.length);eventId=0;mind=null;monitor=null;alertsState=null;hearingState=null;arcaneState=null;transcribing=0;ttsBusy=false;lastTtsAt=0;clipTimes.length=0;replyTimes.clear();
        send(res,200,{ok:true,token,player:session.player,ttl:"until-service-restart-or-repair"});return true;
      }catch(e){return err(res,400,String(e.message));}
    }
    if(route==="/api/client/ai/status"&&req.method==="GET"){send(res,200,{ok:true,...getAIStatus()});return true;}
    if(route==="/api/client/status"&&req.method==="GET"){send(res,200,{ok:true,...health()});return true;}
    if(route==="/api/client/hearing"&&req.method==="GET"){
      send(res,200,{ok:true,...health(),state:hearingState,
        syncedAt:hearingState?.syncedAt||null});
      return true;
    }
    if(route==="/api/client/arcane"&&req.method==="GET"){
      send(res,200,{ok:true,...health(),state:arcaneState,
        syncedAt:arcaneState?.syncedAt||null});return true;
    }
    if(route==="/api/client/alerts"&&req.method==="GET"){
      send(res,200,{ok:true,...health(),source:alertsState?"minecraft-client":"none",
        settings:alertsState?.settings||null,history:alertsState?.history||[],
        syncedAt:alertsState?.syncedAt||null});
      return true;
    }
    if(route==="/api/client/events"&&req.method==="GET"){
      const since=Math.max(0,Number(url.searchParams.get("since")||0)||0);
      // Keep events as an array: health().events is a numeric event counter.
      send(res,200,{ok:true,...health(),events:events.filter(x=>x.id>since).slice(-80),lastId:eventId,monitor});return true;
    }
    if(route==="/api/client/job"&&req.method==="GET"){
      const j=jobs.get(String(url.searchParams.get("id")||""));
      if(!j)return err(res,404,"Comando desconhecido.");
      send(res,200,{ok:true,done:j.done,success:j.success,error:j.error,output:j.output,id:j.id});return true;
    }
    if(route==="/api/client/chat"&&req.method==="POST"){
      try{
        const data=await body(req),raw=String(data.message||"").replace(/[\u0000-\u001f\u007f]/g," ").trim();
        if(!raw||raw.length>230||raw.startsWith("/"))return err(res,422,"Mensagem deve ter 1 a 230 caracteres, sem comandos.");
        // All outgoing Nexus Client messages are unmistakably labeled, including requests from old browser settings.
        const content=raw.replace(/^(?:\[(?:nexus(?:\s+aterna)?)\]\s*)+/i,"").trim();
        const message="[Nexus Aterna] "+content;
        if(!content||message.length>230)return err(res,422,"Mensagem excede o limite de 230 caracteres após incluir [Nexus Aterna].");
        // Restrict commands to configured chat channels; messages cannot contain slash commands.
        const channel=["global","local","custom"].includes(data.channel)?data.channel:"local";
        const custom=String(data.command||"").trim().toLowerCase();
        const root=channel==="local"?"l":channel==="custom"?(custom.startsWith("/")?custom.slice(1):custom):"";
        const allowed=new Set(["l","local","g","global","rp","me","ooc","chat","ch","yell","shout","action"]);
        if(channel==="custom"&&!allowed.has(root))return err(res,422,"Canal personalizado invalido. Escolha um comando de chat permitido.");
        const format=channel==="local"?(data.format===undefined?"&6&l":String(data.format||"").trim().toLowerCase()):"";
        if(!/^(?:&[0-9a-fk-or]){0,8}$/.test(format))return err(res,422,"Código de formatação inválido. Use códigos como &6&l.");
        // Raw formatting is passed only to the chat command; no arbitrary commands.
        send(res,200,enqueue("chat",{message,channel,command:root,format}));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/voice"&&req.method==="POST"){
      try{
        if(!health().online)return err(res,503,"Nexus Client offline. Abra o jogo, pareie o mod e autorize /nexusclient chat on.");
        const data=await body(req,14000);
        if(typeof data.transcript!=="string")return err(res,422,"Transcrição de voz inválida.");
        const transcript=data.transcript.replace(/[\u0000-\u001f\u007f]/g," ").replace(/\s+/g," ").trim();
        if(transcript.length<2||transcript.length>500)return err(res,422,"A mensagem deve ter entre 2 e 500 caracteres.");
        const question=data.followUp===true?transcript:getQuestion(transcript);
        if(question===null)return err(res,422,"Chame Nexus primeiro, ou continue a conversa no modo contínuo.");
        if(!question)return err(res,422,"Depois de chamar Nexus, faça uma pergunta.");
        const generated=await askAI({
          question,history:data.history,online:true,player:session?.player||"",events,mind
        });
        const reply=String(generated?.text||"").trim();
        if(!reply)return err(res,502,"A IA não retornou uma resposta.");
        const parts=splitChatReply(reply);
        const now=Date.now(),jobIds=[];
        for(let i=0;i<parts.length;i++){
          const message="[Nexus Aterna] "+parts[i];
          jobIds.push(enqueue("chat",{message,channel:"local",command:"l",format:"&6&l"},now+i*6500).jobId);
        }
        send(res,200,{ok:true,queued:true,jobId:jobIds[0],jobIds,reply,model:String(generated.model||"Gemini")});
        return true;
      }catch(e){
        return err(res,Number(e.publicStatus)||422,
          e.publicStatus?String(e.message).slice(0,250):"Falha ao preparar resposta. "+String(e.message||"").slice(0,180));
      }
    }
    if(route==="/api/client/arcane/settings"&&req.method==="POST"){
      try{
        const data=await body(req,1000);
        if(typeof data.enabled!=="boolean"||typeof data.reactions!=="boolean"||
          data.scanNearby!==undefined&&typeof data.scanNearby!=="boolean"||
          data.reactions&&!data.enabled)
          return err(res,422,"Configure o Núcleo Arcano antes de ativar reações.");
        send(res,200,enqueue("arcane_settings",{
          enabled:data.enabled,reactions:data.reactions,scanNearby:data.scanNearby===true
        }));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/hearing/settings"&&req.method==="POST"){
      try{
        const data=await body(req,1500);
        const keys=["listening","memory","answer"];
        if(!keys.every(k=>typeof data[k]==="boolean")||!Array.isArray(data.allowedPlayers)||
          data.allowedPlayers.length>20||!data.allowedPlayers.every(n=>
            typeof n==="string"&&/^[A-Za-z0-9_]{3,16}$/.test(n)))
          return err(res,422,"Configuração de audição inválida.");
        if(data.collective!==undefined&&typeof data.collective!=="boolean")
          return err(res,422,"Seletor @a inválido.");
        if(data.listening&&(!data.consentConfirmed||data.allowedPlayers.length===0))
          return err(res,422,"Confirme o consentimento dos jogadores antes de ativar.");
        const allowedPlayers=[...new Set(data.allowedPlayers)];
        const sensitivity=data.sensitivity===undefined?55:data.sensitivity;
        if(!Number.isInteger(sensitivity)||sensitivity<20||sensitivity>100)
          return err(res,422,"Sensibilidade permitida entre 20 e 100.");
        const social=data.social===undefined?null:data.social;
        if(social&&!["diaryEnabled","protectionEnabled","sharePanel"].every(k=>typeof social[k]==="boolean"))
          return err(res,422,"Preferências de memória social inválidas.");
        const speaker=data.speaker===undefined?null:data.speaker;
        if(speaker&&(typeof speaker.enabled!=="boolean"||
          !["auto","Charon","Orus","Kore","Puck","Algenib","Fenrir"].includes(speaker.voice)))
          return err(res,422,"Preferências de voz inválidas.");
        if(social?.sharePanel&&!data.consentConfirmed)
          return err(res,422,"Confirme a autorização para compartilhar o diário com o painel.");
        send(res,200,enqueue("hearing_settings",{
          listening:data.listening,memory:data.memory,answer:data.answer,
          sensitivity,allowedPlayers,collective:data.collective===true,
          ...(social?{social}:{}),...(speaker?{speaker}:{})
        }));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/mind/settings"&&req.method==="POST"){
      try{
        const data=await body(req,1200);
        const social=data.social||{},speaker=data.speaker||{};
        if(!["diaryEnabled","protectionEnabled","sharePanel"].every(k=>typeof social[k]==="boolean")||
          typeof speaker.enabled!=="boolean"||
          !["auto","Charon","Orus","Kore","Puck","Algenib","Fenrir"].includes(speaker.voice))
          return err(res,422,"Preferências do Nexus Mind inválidas.");
        if(social.sharePanel&&data.consentConfirmed!==true)
          return err(res,422,"Confirme o compartilhamento de memórias com o painel.");
        send(res,200,enqueue("mind_settings",{
          social:{diaryEnabled:social.diaryEnabled,
            protectionEnabled:social.protectionEnabled,sharePanel:social.sharePanel},
          speaker:{enabled:speaker.enabled,voice:speaker.voice}
        }));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/hearing/fact"&&req.method==="POST"){
      try{
        const data=await body(req,1100);
        if(!hearingState?.social?.sharePanel)
          return err(res,403,"Habilite a visualização do diário no cliente.");
        const action=String(data.action||"");
        if(!["correct","delete","forget_player","clear"].includes(action))
          return err(res,422,"Ação de memória inválida.");
        const id=Number(data.id);
        if(["correct","delete"].includes(action)&&(!Number.isSafeInteger(id)||id<1))
          return err(res,422,"Identificador de memória inválido.");
        const player=String(data.player||"");
        if(action==="forget_player"&&!/^[A-Za-z0-9_]{3,16}$/.test(player))
          return err(res,422,"Jogador inválido.");
        const corrected=String(data.text||"").trim();
        if(action==="correct"&&(corrected.length<3||corrected.length>250))
          return err(res,422,"Correção de memória inválida.");
        if(action==="clear"&&data.confirm!==true)
          return err(res,422,"Confirmação explícita necessária.");
        send(res,200,enqueue("mind_memory",{
          action,id:Number.isSafeInteger(id)?id:0,player,text:corrected,
          confirm:action==="clear"&&data.confirm===true
        }));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/alerts/settings"&&req.method==="POST"){
      try{
        const settings=alertSettings(await body(req,1500));
        send(res,200,enqueue("alerts_settings",settings));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/monitor/settings"&&req.method==="POST"){
      try{
        const data=await body(req,8000);
        if(typeof data.enabled!=="boolean")return err(res,422,"Informe enabled como true ou false.");
        const numeric=(key,min,max,defaultValue)=>{
          const value=Number(data[key]);
          return Number.isFinite(value)?Math.min(max,Math.max(min,value)):defaultValue;
        };
        const segments=Array.isArray(data.segments)?data.segments.slice(0,48).map((part,i)=>({
          key:String(part?.key||"segment_"+i).slice(0,56),
          integrity:numericValue(part?.integrity,0,100,100),
          integrated:part?.integrated===true
        })):[];
        const settings={
          enabled:data.enabled,damageFactor:numeric("damageFactor",1,10,5),
          healIntegrity:numeric("healIntegrity",0,10,5),
          healEnergy:numeric("healEnergy",0,15,3),
          integrity:numeric("integrity",0,100,100),
          energy:numeric("energy",0,100,85),
          cursor:Math.trunc(numeric("cursor",0,100000,0)),segments
        };
        send(res,200,enqueue("monitor_settings",settings));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/action"&&req.method==="POST"){
      try{
        const data=await body(req),action=String(data.action||"");
        if(!["integrar","desintegrar","remodelar","reparar","ritual","emote","emergencia"].includes(action))return err(res,422,"Ação não autorizada.");
        send(res,200,enqueue("action",{action,form:String(data.form||"").slice(0,28)}));return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(!guardClient(req,res))return true;
    session.lastSeen=Date.now();
    if(route==="/api/client/poll"&&req.method==="GET"){
      const ready=[];
      for(const j of jobs.values()){
        if(j.done||Date.now()<j.readyAt||j.claimedAt&&Date.now()-j.claimedAt<15000)continue;
        j.claimedAt=Date.now();ready.push({id:j.id,type:j.type,payload:j.payload});
        if(ready.length>=5)break;
      }
      send(res,200,{ok:true,jobs:ready,serverTime:new Date().toISOString()});return true;
    }
    if(route==="/api/client/ack"&&req.method==="POST"){
      try{
        const data=await body(req,1800),job=jobs.get(String(data.id||""));
        if(!job)return err(res,404,"Job desconhecido.");
        if(!job.done){job.done=true;job.success=data.success===true;job.error=String(data.error||"").slice(0,250);job.output=String(data.output||"").slice(0,250);}
        send(res,200,{ok:true});return true;
      }catch(e){return err(res,400,String(e.message));}
    }
    if(route==="/api/client/arcane/state"&&req.method==="POST"){
      try{
        const data=await body(req,24000);
        if(typeof data.enabled!=="boolean"||typeof data.reactions!=="boolean"||
          typeof data.present!=="boolean"||typeof data.available!=="boolean"||
          !Array.isArray(data.events)||data.events.length>80)
          return err(res,422,"Estado arcano inválido.");
        const kinds=new Set(["cast_start","cast_end","mana_low","mana_recovered","holy_power","nearby_cast"]);
        const txt=(value,n)=>String(value||"").replace(/[\u0000-\u001f\u007f]/g," ").slice(0,n);
        if(data.events.some(e=>!e||typeof e!=="object"||!kinds.has(e.type)||
          typeof e.message!=="string"||e.message.length>210||
          !Number.isFinite(Number(e.at))||Number(e.at)<=0))
          return err(res,422,"Histórico arcano inválido.");
        const available=data.enabled&&data.present&&data.available;
        const scanNearby=data.enabled&&data.scanNearby===true;
        if(data.nearby!==undefined&&(!Array.isArray(data.nearby)||data.nearby.length>12))
          return err(res,422,"Leituras de proximidade inválidas.");
        arcaneState={
          enabled:data.enabled,reactions:data.reactions,scanNearby,
          nearby:available&&scanNearby?(data.nearby||[]).map(item=>({
            player:/^[A-Za-z0-9_]{3,16}$/.test(String(item?.player||""))?
              String(item.player):"",
            spellId:txt(item?.spellId,90),spellName:txt(item?.spellName,70),
            school:txt(item?.school,90),
            level:numericValue(item?.level,0,100,0),
            distance:numericValue(item?.distance,0,24,0)
          })).filter(item=>item.player):[],
          totalNearby:numericValue(data.totalNearby,0,9999999,0),
          present:data.enabled&&data.present,available,
          source:"Iron's Spells n Spellbooks (cliente)",
          mana:available?numericValue(data.mana,0,1000000,null):null,
          maxMana:available?numericValue(data.maxMana,1,1000000,null):null,
          manaPercent:available?numericValue(data.manaPercent,0,100,null):null,
          casting:available&&data.casting===true,
          castProgress:available?numericValue(data.castProgress,0,1,0):0,
          spellId:available?txt(data.spellId,90):"",
          spellName:available?txt(data.spellName,70):"",
          school:available?txt(data.school,90):"",
          spellLevel:available?numericValue(data.spellLevel,0,100,0):0,
          spellPower:available?numericValue(data.spellPower,0,10000,null):null,
          holyPower:available?numericValue(data.holyPower,0,10000,null):null,
          armAvailable:data.armAvailable===true,
          armIntegrity:data.armAvailable===true?numericValue(data.armIntegrity,0,100,null):null,
          armEnergy:data.armAvailable===true?numericValue(data.armEnergy,0,100,null):null,
          totalCasts:numericValue(data.totalCasts,0,9999999,0),
          lastReadAt:numericValue(data.lastReadAt,0,9999999999999,0),
          lastError:txt(data.lastError,160),
          events:data.events.slice(-40).map(e=>({
            at:numericValue(e.at,1,9999999999999,0),
            type:e.type,message:txt(e.message,210),
            spellId:txt(e.spellId,90),school:txt(e.school,90),
            level:numericValue(e.level,0,100,0)
          })),syncedAt:new Date().toISOString()
        };
        send(res,200,{ok:true,events:arcaneState.events.length});return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/alerts/state"&&req.method==="POST"){
      try{
        const data=await body(req,24000);
        alertsState={
          settings:alertSettings(data),history:alertHistory(data.history),
          syncedAt:new Date().toISOString()
        };
        send(res,200,{ok:true,historyCount:alertsState.history.length});return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/hearing/state"&&req.method==="POST"){
      try{
        const data=await body(req,42000);
        if(typeof data.ready!=="boolean"||typeof data.listening!=="boolean"||
          typeof data.memory!=="boolean"||typeof data.answer!=="boolean"||
          !Array.isArray(data.allowedPlayers)||data.allowedPlayers.length>20||
          !data.allowedPlayers.every(s=>typeof s==="string"&&/^[A-Za-z0-9_]{3,16}$/.test(s)))
          return err(res,422,"Estado de audição inválido.");
        const source=data.social||{},voice=data.speaker||{};
        const counts={};
        for(const k of ["factCount","diaryCount","relationCount","turnCount"])
          counts[k]=numericValue(source[k],0,100000,k==="factCount"?0:0);
        const allowSocial=source.sharePanel===true;
        const safeText=(s,n)=>String(s||"").replace(/[\u0000-\u001f\u007f]/g," ").slice(0,n);
        const permitted=new Set(data.allowedPlayers.map(s=>s.toLowerCase()));
        const sanitizeRows=(rows,max,mapper)=>allowSocial&&Array.isArray(rows)?
          rows.slice(-max).filter(item=>item&&typeof item==="object")
            .map(mapper).filter(item=>item&&
              (item.speaker===undefined||item.speaker==="Nexus"||permitted.has(item.speaker.toLowerCase()))):[];
        const social={
          ...counts,sharePanel:allowSocial,diaryEnabled:source.diaryEnabled===true,
          protectionEnabled:source.protectionEnabled===true,
          facts:sanitizeRows(source.facts,30,item=>({
            id:numericValue(item.id,0,999999999,0),
            at:numericValue(item.at,0,9999999999999,0),
            speaker:safeText(item.speaker,16),kind:safeText(item.kind,30),
            text:safeText(item.text,270),corrected:item.corrected===true
          })),
          diary:sanitizeRows(source.diary,35,item=>({
            id:numericValue(item.id,0,999999999,0),
            at:numericValue(item.at,0,9999999999999,0),
            speaker:safeText(item.speaker,16),kind:safeText(item.kind,30),
            text:safeText(item.text,350)
          })),
          relations:sanitizeRows(source.relations,20,item=>({
            player:safeText(item.player,16),
            familiarity:numericValue(item.familiarity,0,100,0),
            affinity:numericValue(item.affinity,0,100,0),
            interactions:numericValue(item.interactions,0,100000,0)
          })).filter(item=>permitted.has(item.player.toLowerCase())),
          turns:sanitizeRows(source.turns,10,item=>({
            speaker:safeText(item.speaker,16),text:safeText(item.text,270),
            at:numericValue(item.at,0,9999999999999,0)
          }))
        };
        hearingState={ready:data.ready,listening:data.listening,
          memory:data.memory,answer:data.answer,allowedPlayers:data.allowedPlayers,
          collective:data.collective===true,
          eligibleOnline:numericValue(data.eligibleOnline,0,20,0),
          sensitivity:numericValue(data.sensitivity,20,100,55),
          memoryCount:numericValue(data.memoryCount,0,250000,0),
          memoryFull:data.memoryFull===true,queued:numericValue(data.queued,0,36,0),
          inFlight:numericValue(data.inFlight,0,3,0),
          processed:numericValue(data.processed,0,9999999,0),
          dropped:numericValue(data.dropped,0,9999999,0),
          retries:numericValue(data.retries,0,9999999,0),
          lastSpeaker:safeText(data.lastSpeaker,16),
          lastError:safeText(data.lastError,120),
          speaker:{enabled:voice.enabled===true,
            voice:["auto","Charon","Orus","Kore","Puck","Algenib","Fenrir"].includes(voice.voice)?
              voice.voice:"auto",playing:voice.playing===true,
            lastError:safeText(voice.lastError,100)},
          social,syncedAt:new Date().toISOString()};
        send(res,200,{ok:true});return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/hearing/transcribe"&&req.method==="POST"){
      const now=Date.now();
      while(clipTimes.length&&clipTimes[0]<now-60000)clipTimes.shift();
      if(transcribing>=3)return err(res,429,"Transcrição ocupada. Reenviar em instantes.");
      if(clipTimes.length>=90)return err(res,429,"Limite de transcrição por minuto atingido.");
      transcribing++;
      try{
        const data=await body(req,1100000);
        const speaker=String(data.speaker||"");
        if(!/^[A-Za-z0-9_]{3,16}$/.test(speaker))
          return err(res,422,"Nick de participante inválido.");
        if(!Array.isArray(data.consentedPlayers)||!data.consentedPlayers.includes(speaker))
          return err(res,403,"Participante não autorizado para transcrição.");
        if(hearingState&&(!hearingState.ready||!hearingState.listening||
          !hearingState.allowedPlayers.some(s=>s.toLowerCase()===speaker.toLowerCase())))
          return err(res,403,"Audição desativada ou participante revogado.");
        clipTimes.push(now);
        const result=await transcribe({audio:data.audio,answer:false,history:[],speaker});
        send(res,200,{ok:true,speaker,transcript:result.transcript,reply:""});
        return true;
      }catch(e){
        return err(res,Number(e.publicStatus)||422,String(e.message||"Falha na transcrição."));
      }finally{transcribing=Math.max(0,transcribing-1);}
    }
    if(route==="/api/client/hearing/reply"&&req.method==="POST"){
      try{
        const data=await body(req,8000),speaker=String(data.speaker||"");
        if(!/^[A-Za-z0-9_]{3,16}$/.test(speaker))
          return err(res,422,"Nick inválido.");
        if(hearingState&&(!hearingState.listening||!hearingState.answer||
          !hearingState.allowedPlayers.some(s=>s.toLowerCase()===speaker.toLowerCase())))
          return err(res,403,"Respostas de audição desativadas.");
        const transcript=String(data.transcript||"").replace(/[\u0000-\u001f\u007f]/g," ")
          .replace(/\s+/g," ").trim().slice(0,400);
        const question=getQuestion(transcript);
        if(question===null)return err(res,422,"O interlocutor precisa chamar 'Nexus' antes da pergunta.");
        if(!question){
          send(res,200,{ok:true,speaker,reply:"Estou ouvindo. Pode falar."});
          return true;
        }
        if(!Array.isArray(data.memories)||data.memories.length>20||
          !data.memories.every(v=>typeof v==="string"&&v.length<=250))
          return err(res,422,"Contexto de memória inválido.");
        const prev=replyTimes.get(speaker.toLowerCase())||0;
        if(Date.now()-prev<4500)return err(res,429,"Respostas muito próximas. Aguarde.");
        replyTimes.set(speaker.toLowerCase(),Date.now());
        const generated=await askAI({
          question,memories:data.memories,voiceSpeaker:speaker,
          online:true,player:session?.player||"",events,mind
        });
        send(res,200,{ok:true,speaker,reply:String(generated.text||"").slice(0,380)});
        return true;
      }catch(e){
        return err(res,Number(e.publicStatus)||422,
          e.publicStatus?String(e.message).slice(0,250):
          "Falha ao preparar resposta. "+String(e.message||"").slice(0,140));
      }
    }
    if(route==="/api/client/tts"&&req.method==="POST"){
      if(ttsBusy||Date.now()-lastTtsAt<9000)
        return err(res,429,"Aguarde entre as falas do Nexus.");
      if(!hearingState?.speaker?.enabled)
        return err(res,403,"Ative a voz local no Nexus Client.");
      ttsBusy=true;lastTtsAt=Date.now();
      try{
        const data=await body(req,1200);
        const result=await synthesize({
          text:data.text,stage:data.stage,voice:data.voice
        });
        send(res,200,{ok:true,...result});return true;
      }catch(e){return err(res,Number(e.publicStatus)||502,String(e.message).slice(0,180));}
      finally{ttsBusy=false;}
    }
    if(route==="/api/client/monitor/state"&&req.method==="POST"){
      try{
        const data=await body(req,8000);
        if(data.managed!==true||typeof data.enabled!=="boolean")return err(res,422,"Estado do Nexus Client inválido.");
        const segments=Array.isArray(data.segments)?data.segments.slice(0,48).map((part,i)=>({
          key:String(part?.key||"segment_"+i).slice(0,56),
          integrity:numericValue(part?.integrity,0,100,100),
          integrated:part?.integrated===true
        })):[];
        monitor={
          managed:true,source:"minecraft-client",enabled:data.enabled,
          integrity:numericValue(data.integrity,0,100,100),
          energy:numericValue(data.energy,0,100,85),
          segments,damageFactor:numericValue(data.damageFactor,1,10,5),
          healIntegrity:numericValue(data.healIntegrity,0,10,5),
          healEnergy:numericValue(data.healEnergy,0,15,3),
          lastEvent:String(data.lastEvent||"").slice(0,28),
          updatedAt:numericValue(data.updatedAt,0,9999999999999,0),
          receivedAt:new Date().toISOString()
        };
        send(res,200,{ok:true});return true;
      }catch(e){return err(res,422,String(e.message));}
    }
    if(route==="/api/client/mind"&&req.method==="POST"){
      try{
        const data=await body(req,1200);
        const allowed=["PROTOCOLO","OBSERVAÇÃO","ADAPTAÇÃO","IDENTIDADE","INDIVIDUAÇÃO"];
        if(!allowed.includes(data.stage))return err(res,422,"Estágio narrativo inválido.");
        const number=(key,max)=>Number.isFinite(Number(data[key]))?
          Math.min(max,Math.max(0,Math.trunc(Number(data[key])))):0;
        mind={stage:data.stage,experience:number("experience",1000000),
          caution:number("caution",100),attachment:number("attachment",100),
          curiosity:number("curiosity",100),updatedAt:Date.now()};
        send(res,200,{ok:true});return true;
      }catch(e){return err(res,400,String(e.message));}
    }
    if(route==="/api/client/telemetry"&&req.method==="POST"){
      try{
        const data=await body(req,2500),type=String(data.type||"");
        if(!["health","damage","heal","death","respawn","effect","armor","energy","join","leave"].includes(type))return err(res,422,"Tipo de evento desconhecido.");
        const value=Number(data.health),amount=Number(data.amount);
        const player=String(data.player||session.player).slice(0,16);
        if(!/^[A-Za-z0-9_]{3,16}$/.test(player))return err(res,422,"Nick inválido.");
        if(session.player&&player!==session.player)return err(res,403,"Jogador diferente do pareado.");
        session.player=player;
        const item={id:++eventId,player,type,health:Number.isFinite(value)?Math.max(0,Math.min(40,value)):null,amount:Number.isFinite(amount)?Math.max(0,Math.min(40,amount)):0,note:String(data.note||"").slice(0,150),at:new Date().toISOString()};
        events.push(item);if(events.length>250)events.shift();
        send(res,200,{ok:true,id:item.id});return true;
      }catch(e){return err(res,400,String(e.message));}
    }
    return err(res,405,"Método não suportado.");
  }
  return {handle,status:health};
}
module.exports={createClientRelay};
