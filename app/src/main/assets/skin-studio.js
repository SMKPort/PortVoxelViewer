/* Port Voxel Viewer 2.1: Minecraft skin reader (offline) */
(()=>{'use strict';
const api=window.PortVoxelViewer,el=id=>document.getElementById(id);
if(!api)return;
const safe=s=>String(s||'skin').replace(/\.[^.]+$/,'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z0-9_-]+/g,'_').slice(0,72)||'skin';
function toast(s){const t=el('toast');if(!t)return;t.textContent=s;t.classList.remove('hidden');clearTimeout(toast.timer);toast.timer=setTimeout(()=>t.classList.add('hidden'),4500);}
const section=document.createElement('section');section.className='skin-studio';
section.innerHTML=[
'<h2>⬡ SKIN STUDIO <small>MINECRAFT</small></h2>',
'<p>Abra skins PNG e visualize seu personagem em 3D com roupas e animações.</p>',
'<input id="skinFile" type="file" accept=".png,image/png" hidden>',
'<button id="skinOpen" class="skin-primary">▣ Abrir skin PNG</button>',
'<div class="skin-line"><label>Braços<select id="skinArms"><option value="classic">Clássico / Steve (4 px)</option><option value="slim">Slim / Alex (3 px)</option></select></label>',
'<label>Vista<select id="skinPose"><option value="none">Livre</option><option value="front">Frente</option><option value="back">Costas</option><option value="side">Lateral</option></select></label></div>',
'<label class="skin-check"><input type="checkbox" id="skinLayers" checked> Segunda camada (roupas, cabelo, mangas)</label>',
'<div class="skin-line"><button id="skinSavePng">↓ PNG atual</button><button id="skinCapture">▧ Imagem 3D</button></div>',
'<div id="skinStatus" class="skin-status">64×64, 64×32 e HD até 1024×1024. Para animações e GIF, use a aba Animações.</div>'
].join('');
const css=document.createElement('style');
css.textContent='.skin-studio{padding:18px 20px 24px;border-top:1px solid #765c50;background:linear-gradient(155deg,#292230,#151826);color:#e9e2d6}.skin-studio h2{color:#f6d69d;font-size:17px;margin:0 0 8px}.skin-studio h2 small{font-size:9px;color:#bcb5cc}.skin-studio p,.skin-status{font-size:11px;line-height:1.55;color:#b1adc2;margin:0 0 12px}.skin-studio button{background:#352d40;color:#f6ddbd;border:1px solid #80616a;border-radius:8px;padding:11px 8px;font-size:12px;min-height:39px}.skin-studio .skin-primary{width:100%;background:linear-gradient(110deg,#b35f59,#654464);font-weight:800;border-color:#e6b079;margin:2px 0 12px}.skin-line{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin:10px 0}.skin-studio label{font-size:11px;color:#ddd1c3;display:block}.skin-studio select{display:block;width:100%;min-width:0;margin-top:6px;background:#151725;color:#f5e5d0;border:1px solid #4a4259;padding:9px 4px;border-radius:7px;font-size:11px}.skin-check{display:flex!important;align-items:center;gap:8px;padding:8px 0}.skin-check input{accent-color:#dfad75}.skin-status{padding:9px;border:1px solid #3e4055;border-radius:7px;background:#171c29;margin-top:10px;white-space:pre-line}';
document.head.append(css);
const sidebar=document.querySelector('.control-panel'),v2=document.querySelector('.v2-tools');
if(v2)sidebar.insertBefore(section,v2);else sidebar.append(section);
let imageUrl='',lastName='',ratio=1,legacy=false,baseModel=null;
function skinFaces(x,y,w,h,d,mirror){
 const r=(a,b,c,f)=>mirror?[c,b,a,f]:[a,b,c,f];
 const faces={west:r(x,y+d,x+d,y+d+h),north:r(x+d,y+d,x+d+w,y+d+h),east:r(x+d+w,y+d,x+d+w+d,y+d+h),south:r(x+2*d+w,y+d,x+2*d+2*w,y+d+h),up:r(x+d,y,x+d+w,y+d),down:r(x+d+w,y,x+d+2*w,y+d)};
 if(mirror){const tmp=faces.west;faces.west=faces.east;faces.east=tmp;}return faces;
}
function buildSkin(opts){
 const dataUrl=opts.dataUrl,name=opts.name,legacy=opts.legacy,ratio=opts.ratio,slim=opts.slim;
 const elements=[],groups=[],outliner=[],outerIds=[],arm=slim?3:4;
 function cube(id,title,from,to,uv,size,outer,mirror){
   const [u,v]=uv,[w,h,d]=size,faces=Object.fromEntries(Object.entries(skinFaces(u,v,w,h,d,mirror)).map(([key,rect])=>[key,{uv:rect.map(n=>n*ratio),texture:0}]));
   const item={uuid:'cube-'+id,type:'cube',name:title,from,to,origin:[0,0,0],rotation:[0,0,0],inflate:outer?(id.includes('head')?.5:.25):0,color:'#ffffff',faces};
   elements.push(item);return item.uuid;
 }
 function part(id,label,from,to,pivot,uv,whd,over,mirror){
   const g={uuid:'skin-'+id,name:label,origin:pivot,rotation:[0,0,0],children:[]};
   g.children.push(cube(id,label,from,to,uv,whd,false,mirror));
   if(over){
     const outer={uuid:g.uuid+'-outer',name:label+' (segunda camada)',origin:pivot,rotation:[0,0,0],children:[]};
     outer.children.push(cube(id+'-overlay',label+' (overlay)',from,to,over,whd,true,false));
     g.children.push(outer);groups.push(outer);outerIds.push(outer.uuid);
   }
   groups.push(g);outliner.push(g);
 }
 part('head','Cabeça',[-4,24,-4],[4,32,4],[0,24,0],[0,0],[8,8,8],[32,0],false);
 part('body','Tronco',[-4,12,-2],[4,24,2],[0,12,0],[16,16],[8,12,4],legacy?null:[16,32],false);
 part('right-arm','Braço direito',[-4-arm,12,-2],[-4,24,2],[-4-arm/2,22,0],[40,16],[arm,12,4],legacy?null:[40,32],false);
 part('left-arm','Braço esquerdo',[4,12,-2],[4+arm,24,2],[4+arm/2,22,0],legacy?[40,16]:[32,48],[arm,12,4],legacy?null:[48,48],legacy);
 part('right-leg','Perna direita',[-4,0,-2],[0,12,2],[-2,12,0],[0,16],[4,12,4],legacy?null:[0,32],false);
 part('left-leg','Perna esquerda',[0,0,-2],[4,12,2],[2,12,0],legacy?[0,16]:[16,48],[4,12,4],legacy?null:[0,48],legacy);
 function animation(name,duration,tracks){
   const animators={};for(const [key,kfs] of tracks){animators['skin-'+key]={name:key,type:'bone',keyframes:kfs.map(([time,x,y,z])=>({time,channel:'rotation',interpolation:'linear',data_points:[{x,y,z}]}))};}
   return {name,length:duration,loop:'loop',snapping:20,animators};
 }
 const idle=animation('Respiração',2.4,[['head',[[0,0,-5,0],[1.2,2,5,0],[2.4,0,-5,0]]],['right-arm',[[0,3,0,-3],[1.2,-3,0,2],[2.4,3,0,-3]]],['left-arm',[[0,-3,0,3],[1.2,3,0,-2],[2.4,-3,0,3]]]]);
 const walk=animation('Caminhada',1.2,[['right-arm',[[0,26,0,0],[.3,0,0,0],[.6,-26,0,0],[.9,0,0,0],[1.2,26,0,0]]],['left-arm',[[0,-26,0,0],[.3,0,0,0],[.6,26,0,0],[.9,0,0,0],[1.2,-26,0,0]]],['right-leg',[[0,-24,0,0],[.3,0,0,0],[.6,24,0,0],[.9,0,0,0],[1.2,-24,0,0]]],['left-leg',[[0,24,0,0],[.3,0,0,0],[.6,-24,0,0],[.9,0,0,0],[1.2,24,0,0]]]]);
 return {name,meta:{format_version:'5.0',model_format:'free',box_uv:false},resolution:{width:64*ratio,height:(legacy?32:64)*ratio},textures:[{uuid:'skin-texture',name:'skin.png',source:dataUrl,uv_width:64*ratio,uv_height:(legacy?32:64)*ratio}],elements,groups,outliner,animations:[idle,walk],port_skin:{version:1,format:legacy?'legacy':'modern',model:slim?'slim':'classic',layers:outerIds}};
}
function renderSkin(){
 if(!imageUrl)return;
 const slim=el('skinArms').value==='slim';
 const data=buildSkin({dataUrl:imageUrl,name:lastName,legacy,ratio,slim});
 api.openModel(data,lastName+'.png',false);
 api.setGroupVisible(data.port_skin.layers,el('skinLayers').checked);
 api.setCamera({yaw:.33,pitch:.12,dist:72,target:[0,16,0]});
 baseModel=data;
 el('skinStatus').textContent='Skin: '+lastName+'.png\n'+data.resolution.width+'×'+data.resolution.height+' px • '+(slim?'Slim (Alex)':'Clássico (Steve)')+' • '+(legacy?'Legado':'Moderno')+'\nUse Animações para caminhada, respiração e exportar GIF.';
}
async function openImage(dataUrl,name){
 const match=/^data:[^,]*;base64,(.+)$/is.exec(dataUrl);if(!match)throw Error('Selecione um PNG válido.');const prefix=atob(match[1].slice(0,16));if(prefix.charCodeAt(0)!==137||prefix.slice(1,4)!=='PNG')throw Error('Arquivo selecionado não é PNG.');dataUrl='data:image/png;base64,'+match[1];
 const img=new Image();await new Promise((ok,no)=>{img.onload=ok;img.onerror=()=>no(Error('PNG inválido ou ilegível'));img.src=dataUrl;});
 const w=img.naturalWidth,h=img.naturalHeight,k=w/64;
 if(!Number.isInteger(k)||k<1||k>16||!(h===w||h===w/2))throw Error('Dimensões '+w+'×'+h+' inválidas. Use 64×64, 64×32 ou HD proporcional até 1024 px.');
 imageUrl=dataUrl;lastName=safe(name);ratio=k;legacy=h===w/2;
 el('skinArms').disabled=legacy;if(legacy)el('skinArms').value='classic';
 renderSkin();return true;
}
async function openFile(file){
 if(!file)return false;if(file.size>12*1024*1024)throw Error('PNG excede 12 MB.');
 const data=await new Promise((ok,no)=>{const r=new FileReader();r.onload=()=>ok(r.result);r.onerror=()=>no(Error('Falha ao ler PNG'));r.readAsDataURL(file);});
 return openImage(data,file.name);
}
el('skinOpen').onclick=()=>el('skinFile').click();
el('skinFile').onchange=async e=>{try{await openFile(e.target.files?.[0]);}catch(err){toast('Skin: '+err.message);}finally{e.target.value='';}};
el('skinArms').onchange=()=>{try{renderSkin();}catch(e){toast(e.message);}};
el('skinLayers').onchange=e=>{if(baseModel)api.setGroupVisible(baseModel.port_skin.layers,e.target.checked);};
el('skinPose').onchange=e=>{const v=e.target.value,angles={front:0,back:Math.PI,side:Math.PI/2};if(angles[v]!==undefined)api.setCamera({yaw:angles[v],pitch:0,dist:72,target:[0,16,0]});};
el('skinSavePng').onclick=()=>{if(!imageUrl)return toast('Abra uma skin primeiro.');if(window.PortAndroid?.savePNG){window.PortAndroid.savePNG(imageUrl,lastName+'.png');toast('Escolha a pasta para salvar a skin.');}else{const a=document.createElement('a');a.href=imageUrl;a.download=lastName+'.png';a.click();}};
el('skinCapture').onclick=()=>api.saveScreenshot();
window.PortVoxelSkin={openFile,openAndroidPng:(url,name)=>openImage(url,name).catch(e=>toast(e.message)),buildSkin,updateEditedPng:async(url)=>{if(!baseModel||!/^data:image\/png;base64,/.test(url))throw Error('Skin não disponível');imageUrl=url;baseModel.textures[0].source=url;return api.updateSkinTexture(url);}};
window.addEventListener('port-model-opened',()=>{
 const current=api.getModel();
 if(!current?.port_skin){baseModel=null;imageUrl='';el('skinStatus').textContent='Abra uma skin PNG para visualizar o personagem em 3D.';return;}
 baseModel=current;const png=current.textures?.[0]?.source;
 if(typeof png==='string'&&png.startsWith('data:image/png;base64,'))imageUrl=png;
 lastName=safe(current.name);legacy=current.port_skin.format==='legacy';ratio=current.resolution.width/64;
 el('skinArms').disabled=legacy;el('skinArms').value=current.port_skin.model==='slim'?'slim':'classic';
});
})();