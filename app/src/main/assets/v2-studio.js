/* Port Voxel Viewer v2.0: cameras e exportacoes opcionais */
(()=>{'use strict';
const v=window.PortV2;if(!v)return;
const a=v.api,el=id=>document.getElementById(id);
const section=document.createElement('div');
section.innerHTML=[
'<details><summary>◉ Estúdio e câmeras</summary><div class="v2-body">',
'<label>Iluminação <span id="v2LightText">100%</span><input id="v2Light" type="range" min="40" max="160" step="5" value="100"></label>',
'<label>Qualidade gráfica <span id="v2QualityText">100%</span><input id="v2Quality" type="range" min="50" max="150" step="25" value="100"></label>',
'<label>Fundo<select id="v2Background"><option value="night">Noite</option><option value="wine">Vinho</option><option value="stone">Cinza neutro</option><option value="black">Preto</option></select></label>',
'<label>Nome da câmera<input id="v2CameraName" placeholder="Ex.: Detalhes das correntes" maxlength="60"></label>',
'<button id="v2AddCamera">Salvar câmera atual</button><div id="v2Cameras"></div>',
'<small>As vistas salvas permanecem neste dispositivo.</small>',
'</div></details>',
'<details><summary>▣ Exportar GIF, WebP e MP4</summary><div class="v2-body">',
'<button id="v2StillWebP">↓ Salvar imagem WebP</button>',
'<button id="v2AnimWebP">↓ Salvar WebP animado</button>',
'<button id="v2CancelWebP" style="display:none">Cancelar exportação WebP</button>',
'<button id="v2MP4">↓ Gravar MP4</button>',
'<p id="v2Status" style="font-size:11px;color:#f1c889;margin:2px 0" aria-live="polite"></p>',
'<small id="v2Note">GIF original permanece disponível na aba Animações. WebP animado usa a resolução e FPS escolhidos para GIF.</small>',
'</div></details>'
].join('');
el('v2Extra').append(section);
const presets={night:'radial-gradient(circle at 54% 40%,#262234,#0a0e18 75%)',wine:'radial-gradient(circle at 55% 40%,#502632,#0c0812 75%)',stone:'radial-gradient(circle at 55% 40%,#62616a,#171a25 75%)',black:'#050505'};
el('v2Background').onchange=e=>{el('viewport').style.background=presets[e.target.value]||presets.night;};
el('v2Light').oninput=e=>{a.setLight(Number(e.target.value)/100);el('v2LightText').textContent=e.target.value+'%';};
el('v2Quality').oninput=e=>{a.setQuality(Number(e.target.value)/100);el('v2QualityText').textContent=e.target.value+'%';};
const cameraKey='port-voxel-v2-cameras',getCameras=()=>{try{return JSON.parse(localStorage.getItem(cameraKey)||'[]');}catch(e){return [];}};
function paintCameras(){const root=el('v2Cameras');root.replaceChildren();for(const c of getCameras()){const row=document.createElement('div');row.className='v2-item';const span=document.createElement('b');span.textContent=c.name;const use=document.createElement('button');use.textContent='Ver';use.onclick=()=>{a.setCamera(c.camera);const intensity=Math.round(a.getCamera().light*100);el('v2Light').value=intensity;el('v2LightText').textContent=intensity+'%';};const del=document.createElement('button');del.textContent='×';del.onclick=()=>{localStorage.setItem(cameraKey,JSON.stringify(getCameras().filter(x=>x.id!==c.id)));paintCameras();};row.append(span,use,del);root.append(row);}if(!root.children.length)root.textContent='Nenhuma câmera salva.';}
el('v2AddCamera').onclick=()=>{const list=getCameras();if(list.length>=24)return v.notify('Limite de 24 câmeras');const name=el('v2CameraName').value.trim()||'Câmera '+(list.length+1);list.push({id:Date.now()+'-'+Math.random(),name,camera:a.getCamera()});try{localStorage.setItem(cameraKey,JSON.stringify(list));paintCameras();el('v2CameraName').value='';v.notify('Câmera salva.');}catch(e){v.notify('Falha ao salvar câmera: '+e.message);}};
paintCameras();
const status=s=>el('v2Status').textContent=s;
const still=()=>{const src=a.getCanvas(),out=document.createElement('canvas');out.width=Math.max(1,src.width);out.height=Math.max(1,src.height);const c=out.getContext('2d');c.fillStyle='#161724';c.fillRect(0,0,out.width,out.height);c.drawImage(src,0,0);return out;};
el('v2StillWebP').onclick=async()=>{try{const url=still().toDataURL('image/webp',.92);if(!url.startsWith('data:image/webp'))throw Error('WebP indisponível');const b=await(await fetch(url)).blob();await v.saveBlob(b,'PortVoxel_'+v.safe(a.getModel()?.name)+'.webp','image/webp');}catch(e){status('WebP: '+e.message);}};
const encode=s=>Uint8Array.from([...s].map(c=>c.charCodeAt(0))),u32=(n,bytes)=>Uint8Array.from({length:bytes},(_,i)=>n/Math.pow(256,i)&255);
const concat=items=>{let size=0;for(const x of items)size+=x.length;const out=new Uint8Array(size);let pos=0;for(const x of items){out.set(x,pos);pos+=x.length;}return out;};
const chunk=(four,data)=>concat([encode(four),u32(data.length,4),data,data.length%2?Uint8Array.of(0):new Uint8Array(0)]);
function parseFrame(input){const raw=atob(input.split(',')[1]),bytes=new Uint8Array(raw.length);for(let i=0;i<raw.length;i++)bytes[i]=raw.charCodeAt(i);
if(String.fromCharCode(...bytes.slice(0,4))!=='RIFF'||String.fromCharCode(...bytes.slice(8,12))!=='WEBP')throw Error('Imagem WebP inválida');
const chunks=[];for(let i=12;i+8<=bytes.length;){const id=String.fromCharCode(...bytes.slice(i,i+4));const len=(bytes[i+4]|(bytes[i+5]<<8)|(bytes[i+6]<<16)|(bytes[i+7]<<24))>>>0;if(len>bytes.length-i-8)break;if(['VP8 ','VP8L','ALPH'].includes(id))chunks.push(chunk(id,bytes.slice(i+8,i+8+len)));i+=8+len+(len&1);}
if(!chunks.some(c=>String.fromCharCode(...c.slice(0,3))==='VP8'))throw Error('Quadro WebP não compatível');return chunks;
}
function muxWebP(frames,w,h,fps,repeat){const alpha=frames.some(f=>f.some(c=>String.fromCharCode(...c.slice(0,4))==='ALPH')),head=new Uint8Array(10);
head[0]=2|(alpha?16:0);head.set(u32(w-1,3),4);head.set(u32(h-1,3),7);
const chunks=[chunk('VP8X',head),chunk('ANIM',concat([u32(0,4),u32(repeat?0:1,2)]))];
for(const frame of frames){const fhead=concat([u32(0,3),u32(0,3),u32(w-1,3),u32(h-1,3),u32(Math.round(1000/fps),3),Uint8Array.of(0)]);chunks.push(chunk('ANMF',concat([fhead,...frame])));}
const body=concat([encode('WEBP'),...chunks]);return new Blob([concat([encode('RIFF'),u32(body.length,4),body])],{type:'image/webp'});
}
let busy=false,cancelWebP=false;
el('v2CancelWebP').onclick=()=>{cancelWebP=true;status('Cancelando exportação WebP…');};
el('v2AnimWebP').onclick=async()=>{if(busy)return;const anim=a.getAnimation(),fps=Number(el('gifFps').value),size=Number(el('gifResolution').value);
if(anim.index<0||anim.length<=0)return v.notify('Selecione uma animação.');
const total=Math.ceil(anim.length*fps);if(total>240)return v.notify('Máximo de 240 quadros. Reduza o FPS.');
busy=true;cancelWebP=false;el('v2CancelWebP').style.display='block';el('v2AnimWebP').disabled=true;try{a.setAnimation(anim.index,0,false);const src=a.getCanvas(),tmp=document.createElement('canvas');tmp.width=size;tmp.height=size;const ctx=tmp.getContext('2d');const frames=[];
for(let i=0;i<total;i++){if(cancelWebP)throw Error('Exportação cancelada');a.setAnimation(anim.index,i/fps,false);a.renderNow();const c=a.getCanvas();ctx.fillStyle='#141525';ctx.fillRect(0,0,size,size);const factor=Math.min(size/Math.max(c.width,1),size/Math.max(c.height,1)),w=c.width*factor,h=c.height*factor;ctx.drawImage(c,(size-w)/2,(size-h)/2,w,h);const url=tmp.toDataURL('image/webp',.83);if(!url.startsWith('data:image/webp'))throw Error('WebP não suportado');frames.push(parseFrame(url));if(i%3===0){status('WebP: '+(i+1)+' / '+total);await new Promise(resolve=>setTimeout(resolve,0));}}
const blob=muxWebP(frames,size,size,fps,el('gifLoop').checked);if(blob.size>28*1024*1024)throw Error('WebP acima de 28 MB. Use 256 px.');await v.saveBlob(blob,'PortVoxel_'+v.safe(a.getModel()?.name)+'_animado.webp','image/webp');status('WebP animado pronto, '+(blob.size/1048576).toFixed(1)+' MB');}
catch(e){status('Falha no WebP animado: '+e.message);console.error(e);}
finally{a.setAnimation(anim.index,anim.time,anim.playing);el('v2CancelWebP').style.display='none';el('v2AnimWebP').disabled=false;busy=false;}};
const compatibleMP4=()=>{const canvas=a.getCanvas();if(typeof MediaRecorder==='undefined'||!canvas.captureStream)return '';return ['video/mp4;codecs="avc1.42E01E"','video/mp4;codecs="avc1"','video/mp4'].find(t=>MediaRecorder.isTypeSupported(t))||'';};
if(!compatibleMP4()){el('v2MP4').disabled=true;el('v2Note').textContent='GIF e WebP animado disponíveis. A gravação MP4 só é habilitada em WebViews que aceitam MediaRecorder video/mp4. MP4 nativo universal ainda precisa ser implementado.';}
el('v2MP4').onclick=async()=>{if(busy)return;const typ=compatibleMP4(),old=a.getAnimation();if(!typ)return v.notify('MP4 indisponível neste aparelho');if(old.index<0||old.length<=0)return v.notify('Selecione uma animação.');busy=true;let rec,stream;try{stream=a.getCanvas().captureStream(20);const parts=[];rec=new MediaRecorder(stream,{mimeType:typ,videoBitsPerSecond:2500000});const done=new Promise((resolve,reject)=>{rec.ondataavailable=e=>{if(e.data.size)parts.push(e.data);};rec.onstop=resolve;rec.onerror=e=>reject(e.error||Error('Gravação falhou'));});a.setAnimation(old.index,0,true);rec.start(200);status('Gravando MP4…');await new Promise(r=>setTimeout(r,Math.round(old.length*1000)));rec.stop();await done;const blob=new Blob(parts,{type:'video/mp4'});if(blob.size<100)throw Error('MP4 vazio');await v.saveBlob(blob,'PortVoxel_'+v.safe(a.getModel()?.name)+'_animado.mp4','video/mp4');status('MP4 gerado.');}catch(e){status('Falha ao gravar MP4: '+e.message);}finally{if(rec?.state==='recording')rec.stop();if(stream)stream.getTracks().forEach(t=>t.stop());a.setAnimation(old.index,old.time,old.playing);busy=false;}};
})();