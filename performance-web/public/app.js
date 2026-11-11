"use strict";
(()=>{
 const $=id=>document.getElementById(id);
 const STORAGE="port-performance-token-v1";
 let token="",online=false,series=[],lastSeen=0,refreshing=false;
 try{token=localStorage.getItem(STORAGE)||sessionStorage.getItem(STORAGE)||"";}catch(e){}
 const notify=(message)=>{$("notice").textContent=message;};
 function updateConnection(){
   $("badge").classList.toggle("online",online);
   $("connection").textContent=online?"MOD CONECTADO":token?"AGUARDANDO TELEMETRIA":"AGUARDANDO CONEXÃO";
   $("disconnect").hidden=!token;
 }
 function metricText(id,value,decimal=0){
   $(id).textContent=Number.isFinite(value)?Number(value).toFixed(decimal):"--";
 }
 function fillMeter(id,fraction){$(id).style.width=Math.max(0,Math.min(100,Math.round(fraction*100)))+"%";}
 function avg(xs){return xs.length?xs.reduce((a,b)=>a+b,0)/xs.length:null;}
 function updateDashboard(data){
   online=!!data.online;series=Array.isArray(data.series)?data.series.slice(-720):[];
   lastSeen=data.lastSeen||0;updateConnection();
   const v=data.latest;
   if(!v){
     for(const n of ["fps","ping","memory","frametime"])metricText(n,null);
     for(const n of ["fpsBar","pingBar","memoryBar","frameBar"])fillMeter(n,0);
     $("lastUpdate").textContent="Sem amostras";
     $("player").textContent="Jogador: --";
     notify("Chave aceita. Execute /portperf stream on dentro do Minecraft.");
   }else{
     metricText("fps",v.fps);
     metricText("ping",v.ping>=0?v.ping:null);
     metricText("memory",v.memoryUsedMb);
     metricText("frametime",v.fps>0?1000/v.fps:null,1);
     fillMeter("fpsBar",v.fps/144);
     fillMeter("pingBar",v.ping<0?0:1-v.ping/300);
     fillMeter("memoryBar",v.memoryUsedMb/Math.max(1,v.memoryMaxMb));
     fillMeter("frameBar",v.fps<=0?0:1-Math.min(1,(1000/v.fps)/40));
     $("fpsNote").textContent=v.fps>=120?"Excelente fluidez":v.fps>=60?"Boa fluidez":v.fps>=30?"Desempenho moderado":"FPS baixo";
     $("pingNote").textContent=v.ping<0?"Indisponível neste mundo":v.ping<80?"Conexão estável":v.ping<180?"Latência moderada":"Latência elevada";
     $("memoryNote").textContent=v.memoryUsedMb+" / "+v.memoryMaxMb+" MB máximos da JVM";
     $("lastUpdate").textContent="Última amostra: "+new Date(lastSeen).toLocaleTimeString("pt-BR");
     $("player").textContent="Jogador: "+(data.name||"--");
     notify(online?"Recebendo dados do mod em tempo real.":"Última amostra recebida. Aguardando o Minecraft retomar a transmissão.");
   }
   const fpsPoints=series.map(s=>s.fps).filter(Number.isFinite);
   const pings=series.map(s=>s.ping).filter(v=>Number.isFinite(v)&&v>=0);
   $("avgFps").textContent="Média: "+(fpsPoints.length?Math.round(avg(fpsPoints))+" FPS":"--");
   $("minFps").textContent="Mínimo: "+(fpsPoints.length?Math.min(...fpsPoints)+" FPS":"--");
   $("maxFps").textContent="Máximo: "+(fpsPoints.length?Math.max(...fpsPoints)+" FPS":"--");
   $("avgPing").textContent="Ping médio: "+(pings.length?Math.round(avg(pings))+" ms":"--");
   plot("fpsChart",fpsPoints,"#dfb56d","FPS");
   plot("pingChart",pings,"#bc6e75","ms");
 }
 function plot(id,data,color,unit){
   const canvas=$(id);if(!canvas)return;
   const rect=canvas.getBoundingClientRect();const dpr=Math.min(2,window.devicePixelRatio||1);
   const width=Math.max(40,Math.floor(rect.width*dpr)),height=Math.max(40,Math.floor(rect.height*dpr));
   if(canvas.width!==width||canvas.height!==height){canvas.width=width;canvas.height=height;}
   const g=canvas.getContext("2d");if(!g)return;
   g.clearRect(0,0,width,height);
   const pad=20*dpr,innerW=width-2*pad,innerH=height-2*pad;
   g.lineWidth=dpr;g.strokeStyle="#343039";
   for(let i=0;i<5;i++){const y=pad+i*(innerH/4);g.beginPath();g.moveTo(pad,y);g.lineTo(width-pad,y);g.stroke();}
   const visible=data.slice(-60);if(!visible.length){
     g.fillStyle="#706a76";g.font=(12*dpr)+"px sans-serif";g.textAlign="center";
     g.fillText("AGUARDANDO TELEMETRIA",width/2,height/2);return;
   }
   const max=Math.max(unit==="FPS"?30:20,...visible);
   const min=Math.min(0,...visible);
   const x=i=>pad+(visible.length===1?innerW/2:(i/(visible.length-1))*innerW);
   const y=v=>height-pad-((v-min)/Math.max(1,max-min))*innerH;
   const gradient=g.createLinearGradient(0,pad,0,height-pad);
   gradient.addColorStop(0,color+"44");gradient.addColorStop(1,color+"00");
   g.beginPath();visible.forEach((v,i)=>i===0?g.moveTo(x(i),y(v)):g.lineTo(x(i),y(v)));
   const lastX=x(visible.length-1);
   g.lineTo(lastX,height-pad);g.lineTo(x(0),height-pad);g.closePath();g.fillStyle=gradient;g.fill();
   g.beginPath();visible.forEach((v,i)=>i===0?g.moveTo(x(i),y(v)):g.lineTo(x(i),y(v)));
   g.strokeStyle=color;g.lineWidth=2*dpr;g.lineJoin="round";g.stroke();
   g.fillStyle=color;g.beginPath();g.arc(lastX,y(visible[visible.length-1]),3.5*dpr,0,Math.PI*2);g.fill();
 }
 async function poll(){
   if(!token||refreshing)return;refreshing=true;
   try{
     const res=await fetch("/api/snapshot",{cache:"no-store",headers:{"X-Port-Perf-Key":token,"Accept":"application/json"},signal:AbortSignal.timeout(8500)});
     const type=res.headers.get("content-type")||"";
     if(!type.includes("application/json"))throw Error("A hospedagem respondeu HTML (HTTP "+res.status+").");
     const data=await res.json();
     if(!res.ok||!data.ok)throw Error(data.error||"HTTP "+res.status);
     updateDashboard(data);
   }catch(e){online=false;updateConnection();notify("Falha na atualização: "+(e.message||e));}
   finally{refreshing=false;}
 }
 $("connectForm").addEventListener("submit",e=>{
   e.preventDefault();const value=$("token").value.trim().replace(/\s/g,"");
   if(!/^[a-f0-9]{32}$/i.test(value)){notify("A chave deve conter exatamente 32 caracteres hexadecimais. Use /portperf key no Minecraft.");return;}
   token=value.toLowerCase();
   try{localStorage.removeItem(STORAGE);sessionStorage.removeItem(STORAGE);
     if($("remember").checked)localStorage.setItem(STORAGE,token);
     else sessionStorage.setItem(STORAGE,token);
   }catch(e){notify("O navegador bloqueou o armazenamento. A chave vale apenas enquanto esta página estiver aberta.");}
   $("token").value="";$("token").type="password";$("reveal").textContent="MOSTRAR";
   updateConnection();poll();
 });
 $("disconnect").addEventListener("click",()=>{
   token="";online=false;series=[];
   try{localStorage.removeItem(STORAGE);sessionStorage.removeItem(STORAGE);}catch(e){}
   $("remember").checked=false;$("token").value="";updateDashboard({online:false,series:[],latest:null,lastSeen:null});notify("Pareamento removido deste navegador. A transmissão do mod continua até usar /portperf stream off.");
 });
 $("reveal").addEventListener("click",()=>{
   const field=$("token"),on=field.type==="password";field.type=on?"text":"password";$("reveal").textContent=on?"OCULTAR":"MOSTRAR";
 });
 if(token){$("remember").checked=!!localStorage.getItem(STORAGE);notify("Chave recuperada deste navegador. Aguardando dados.");}
 updateConnection();updateDashboard({online:false,series:[],latest:null,lastSeen:null});
 if(token)poll();setInterval(poll,5000);
 window.addEventListener("resize",()=>{plot("fpsChart",series.map(s=>s.fps).filter(Number.isFinite),"#dfb56d","FPS");plot("pingChart",series.map(s=>s.ping).filter(v=>Number.isFinite(v)&&v>=0),"#bc6e75","ms");});
})();