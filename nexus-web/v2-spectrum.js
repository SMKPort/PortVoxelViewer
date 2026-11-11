"use strict";
(function(){
const V=window.NexusV2,S=V.state,E=V.esc,q=s=>document.querySelector(s);
let raf=0,active=false,frame=0;
const d=S.spectro;d.snapshots=Array.isArray(d.snapshots)?d.snapshots:[];d.csvSamples=Array.isArray(d.csvSamples)?d.csvSamples:[];
d.csvRate=Number(d.csvRate)||512;
function wave(time){const arr=[];for(let i=0;i<256;i++){const t=i/512+time;const noise=(Math.sin(i*32.4+t*19)+Math.cos(i*72.12+t*7))*.005*d.noise;arr.push((Math.sin(2*Math.PI*d.frequency*t)+.2*Math.sin(4*Math.PI*d.frequency*t))*(d.amplitude/100)*.83+noise);}return arr;}
function measure(values,rate){
const n=values.length||1,mean=values.reduce((a,b)=>a+b,0)/n;
const rms=Math.sqrt(values.reduce((a,b)=>a+b*b,0)/n),peak=Math.max(0,...values.map(x=>Math.abs(x)));
let max=0,dominant=0;
const limit=Math.min(128,Math.floor(n/2));
for(let k=1;k<limit;k++){let re=0,im=0;for(let i=0;i<n;i++){const a=2*Math.PI*i*k/n;re+=values[i]*Math.cos(a);im+=values[i]*Math.sin(a);}const amplitude=Math.hypot(re,im);if(amplitude>max){max=amplitude;dominant=k*rate/n;}}
return {mean,rms,peak,dominant};
}
function metrics(m){return '<div class="grid two v2-mini-kpis">'+[["MÉDIA",m.mean.toFixed(3)],["RMS",m.rms.toFixed(3)],["PICO",m.peak.toFixed(3)],["FREQ. PRINCIPAL",m.dominant.toFixed(1)+" Hz"]].map(x=>'<div><small>'+x[0]+'</small><strong>'+x[1]+'</strong></div>').join("")+'</div>';}
function view(){
let recent=d.snapshots.slice(-15).reverse().map(x=>'<div class="kv"><span>'+E(x.mode)+' · '+new Date(x.at).toLocaleString("pt-BR")+'</span><span class="value">RMS '+E(x.rms)+'</span></div>').join("");
if(!recent)recent='<div class="empty">Nenhuma amostra arquivada.</div>';
const graph='<div class="v2-spectrum"><canvas id="nexusWave" aria-label="Gráfico do sinal"></canvas><span class="v2-plot-label">FORMA DE ONDA</span></div><div class="v2-spectrum"><canvas id="nexusFFT" aria-label="Frequências discretas"></canvas><span class="v2-plot-label">COMPONENTES DE FREQUÊNCIA (DFT)</span></div><div class="actions">'+V.button("spec-pause",d.live?"Ⅱ PAUSAR":"▶ REPRODUZIR","gold")+V.button("spec-store","+ SALVAR LEITURA","ghost")+V.button("spec-export","↓ HISTÓRICO JSON","ghost")+'</div>';
const panel='<form class="stack" data-v2-form="spec-tune"><label class="field">FREQUÊNCIA DE TESTE: <b id="specHz">'+d.frequency+' Hz</b><input id="specFreq" name="freq" type="range" min="2" max="90" value="'+d.frequency+'"></label><label class="field">AMPLITUDE: <b id="specAmp">'+d.amplitude+'%</b><input id="specAmplitude" name="amp" type="range" min="0" max="100" value="'+d.amplitude+'"></label><label class="field">RUÍDO SINTÉTICO: <b id="specNoise">'+d.noise+'%</b><input id="specNoiseRange" name="noise" type="range" min="0" max="70" value="'+d.noise+'"></label><button class="btn gold" type="submit">APLICAR SINAL SINTÉTICO</button></form><div class="divider"></div><label class="field">IMPORTAR CSV DE SENSOR<input type="file" id="specCSVFile" data-v2-change="spec-csv" accept=".csv,.txt,text/csv"></label><label class="field">TAXA DE AMOSTRAGEM DO CSV (Hz)<input id="specSampleRate" type="number" value="'+d.csvRate+'" min="1" max="100000"></label><p class="panel-caption">É necessário indicar a taxa real do seu sensor. O CSV deve conter pelo menos 16 valores numéricos.</p>'+V.button("spec-demo","VOLTAR AO SINAL SINTÉTICO","ghost")+'<div class="divider"></div><div class="eyebrow">INDICADORES</div><div id="nexusMetrics">'+metrics(measure([0,0],512))+'</div>';
return V.intro("MÓDULO 13 · ANÁLISE MATEMÁTICA","Espectrômetro","Analise ondas e frequências usando um gerador sintético ou dados CSV fornecidos por sensores reais.","OSCILOSCÓPIO + DFT")+
'<div class="grid two">'+V.card("SINAL E ESPECTRO",graph)+V.card("PARÂMETROS E AQUISIÇÃO",panel)+'</div><h2 class="section-heading">Registro de amostras</h2>'+V.card("HISTÓRICO",recent)+
'<div class="notice" style="margin-top:18px">O modo sintético produz ondas matemáticas. O CSV é tratado como dados físicos fornecidos pelo usuário. Este módulo não detecta magia ou energia sobrenatural.</div>';
}
V.register("espectrometro",view,start);
function draw(canvas,values,frequencyMode){
if(!canvas||!values?.length)return;
const ctx=canvas.getContext("2d");if(!ctx)return;
const w=Math.max(100,canvas.clientWidth||600),h=Math.max(140,canvas.clientHeight||180),pixel=Math.min(devicePixelRatio||1,2);
if(canvas.width!==Math.round(w*pixel)||canvas.height!==Math.round(h*pixel)){canvas.width=w*pixel;canvas.height=h*pixel;}
ctx.setTransform(pixel,0,0,pixel,0,0);ctx.fillStyle="#10131a";ctx.fillRect(0,0,w,h);ctx.strokeStyle="#39313e";ctx.lineWidth=.7;
for(let i=0;i<=10;i++){ctx.beginPath();ctx.moveTo(i*w/10,0);ctx.lineTo(i*w/10,h);ctx.stroke();}
for(let i=0;i<=4;i++){ctx.beginPath();ctx.moveTo(0,i*h/4);ctx.lineTo(w,i*h/4);ctx.stroke();}
ctx.beginPath();ctx.lineWidth=2;ctx.strokeStyle=frequencyMode?"#e5bb7d":"#d65c69";
if(!frequencyMode){const max=Math.max(1,...values.map(Math.abs));values.forEach((v,i)=>{const x=w*i/(values.length-1),y=h/2-(v/max)*(h*.43);if(i===0)ctx.moveTo(x,y);else ctx.lineTo(x,y);});}
else{const bins=64,n=values.length;for(let k=0;k<bins;k++){let re=0,im=0;for(let i=0;i<n;i++){const a=2*Math.PI*k*i/n;re+=values[i]*Math.cos(a);im+=values[i]*Math.sin(a);}const magnitude=Math.hypot(re,im)/n*2,x=w*k/(bins-1),y=h-Math.min(1,magnitude)*h*.9-5;if(k===0)ctx.moveTo(x,y);else ctx.lineTo(x,y);}}
ctx.stroke();
}
function samples(now){if(d.mode==="csv"&&d.csvSamples.length)return d.csvSamples;return wave(now/1000);}
function start(){active=true;frame=0;V.addDisposer(()=>{active=false;cancelAnimationFrame(raf);});
function tick(now){if(!active)return;raf=requestAnimationFrame(tick);if(++frame%6!==0||!d.live)return;const vals=samples(now),rate=d.mode==="csv"?d.csvRate:512;draw(q("#nexusWave"),vals,false);draw(q("#nexusFFT"),vals,true);if(frame%30===0){const el=q("#nexusMetrics");if(el)el.innerHTML=metrics(measure(vals,rate));}}tick(0);}
document.addEventListener("input",e=>{const ids={specFreq:["specHz"," Hz"],specAmplitude:["specAmp","%"],specNoiseRange:["specNoise","%"]};const item=ids[e.target.id];if(item&&q("#"+item[0]))q("#"+item[0]).textContent=e.target.value+item[1];});
V.forms["spec-tune"]=f=>{const form=new FormData(f);d.frequency=V.clamp(Number(form.get("freq")),2,90);d.amplitude=V.clamp(Number(form.get("amp")),0,100);d.noise=V.clamp(Number(form.get("noise")),0,70);d.mode="simulado";d.live=true;V.save();V.rerender();};
V.actions["spec-pause"]=()=>{d.live=!d.live;V.save();V.rerender();};
V.actions["spec-demo"]=()=>{d.mode="simulado";d.live=true;V.save();V.rerender();};
V.actions["spec-store"]=()=>{const vals=samples(performance.now()),m=measure(vals,d.mode==="csv"?d.csvRate:512);d.snapshots.push({id:V.id(),mode:d.mode==="csv"?"CSV físico":"Simulação",rms:m.rms.toFixed(3),peak:m.peak.toFixed(3),dominant:m.dominant.toFixed(1),at:Date.now()});d.snapshots=d.snapshots.slice(-60);V.save();V.log("Espectro arquivado ("+d.mode+").","energia");V.rerender();};
V.actions["spec-export"]=()=>{const blob=new Blob([JSON.stringify({format:"nexus-aterna-spectrum-v2",at:new Date().toISOString(),snapshots:d.snapshots},null,2)],{type:"application/json"}),url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download="nexus-espectrometro.json";a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
V.changes["spec-csv"]=async el=>{
const file=el.files?.[0];if(!file)return;if(file.size>2000000)throw Error("Limite de 2MB.");
const lines=(await file.text()).split(/\r?\n/).slice(0,3000),arr=[];
for(const line of lines){const tokens=line.trim().split(/[\t;,]/);let v;for(const t of tokens){const n=Number(t.trim());if(t.trim()!==""&&Number.isFinite(n)){v=n;break;}}if(v!==undefined)arr.push(v);}
if(arr.length<16)throw Error("O CSV precisa conter 16 ou mais valores numéricos.");
const chosen=arr.slice(0,256),avg=chosen.reduce((a,b)=>a+b,0)/chosen.length,peak=Math.max(.000001,...chosen.map(x=>Math.abs(x-avg)));
d.csvSamples=chosen.map(x=>(x-avg)/peak);d.csvRate=V.clamp(Number(q("#specSampleRate")?.value)||512,1,100000);d.mode="csv";d.live=true;V.save();V.rerender();V.toast("CSV importado e normalizado: "+arr.length+" valores.");
};
})();