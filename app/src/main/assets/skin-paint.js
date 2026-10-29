/* Minecraft Skin Painter v2.2 - 2D atlas and 3D touchscreen painting */
(()=>{'use strict';
const api=window.PortVoxelViewer,core=window.PortSkinPaintCore,skin=window.PortVoxelSkin;
if(!api||!core||!skin)return;
const $=id=>document.getElementById(id);
const notify=s=>{const t=$('toast');t.textContent=s;t.classList.remove('hidden');clearTimeout(notify.timer);notify.timer=setTimeout(()=>t.classList.add('hidden'),4000);};
const pane=document.createElement('details');pane.className='skin-painter';
pane.innerHTML=[
'<summary>✎ EDITAR SKIN 2D + 3D</summary><div class="skin-painter-body">',
'<p>Pinte no mapa UV ou diretamente sobre o personagem em 3D.</p>',
'<div class="skin-painter-row"><label>Ferramenta<select id="spTool"><option value="brush">Pincel</option><option value="erase">Borracha</option><option value="pick">Conta-gotas</option><option value="fill">Balde</option></select></label>',
'<label>Cor<input id="spColor" type="color" value="#f1bb76"></label>',
'<label>Pincel<select id="spSize"><option>1</option><option>2</option><option>3</option><option>5</option><option>8</option></select></label></div>',
'<label class="sp-check"><input id="sp3d" type="checkbox"> Ativar pintura no modelo 3D</label>',
'<label class="sp-check"><input id="spOuter" type="checkbox"> Pintar camada externa (roupas e cabelo)</label>',
'<div class="skin-painter-row"><button id="spUndo">↶ Desfazer</button><button id="spRedo">↷ Refazer</button><button id="spReset">Restaurar</button></div>',
'<label>Zoom mapa UV <b id="spZoomText">6×</b><input id="spZoom" type="range" min="1" max="12" value="6"></label>',
'<div id="spScroll" class="sp-scroll"><div id="spWrap" class="sp-wrap"><canvas id="spCanvas" aria-label="Mapa de pixels da skin"></canvas><div id="spHighlight" class="sp-highlight" hidden></div></div></div>',
'<div id="spInfo" class="sp-info">Abra uma skin PNG para editar.</div>',
'<h3>Corrigir partes invertidas</h3>',
'<label>Peça / camada<select id="spPart"></select></label>',
'<label>Face<select id="spFace"><option value="north">Frente</option><option value="south">Costas</option><option value="east">Direita</option><option value="west">Esquerda</option><option value="up">Topo</option><option value="down">Baixo</option></select></label>',
'<div class="skin-painter-row"><button id="spFlipH">⇄ Horizontal</button><button id="spFlipV">⇅ Vertical</button><button id="spFlip180">⤾ 180°</button></div>',
'<div class="skin-painter-row sp-two"><button id="spSave" class="sp-primary">↓ Salvar PNG</button><button id="spBB">↓ Salvar .bbmodel</button></div>',
'<p>A skin original pode ser restaurada. Para girar a câmera, desative a pintura 3D. As ferramentas corrigem os pixels, sem alterar o UV padrão Minecraft.</p>',
'</div>'
].join('');
const css=document.createElement('style');css.textContent=[
'.skin-painter{margin:12px 0;border:1px solid #846050;border-radius:11px;background:#171925;color:#e9dacf}',
'.skin-painter summary{font-size:12px;font-weight:800;color:#f0cb91;cursor:pointer;padding:13px}',
'.skin-painter-body{display:flex;flex-direction:column;gap:10px;padding:12px}',
'.skin-painter-body p{font-size:10px;color:#aba7bc;line-height:1.6;margin:0}',
'.skin-painter-body h3{font-size:12px;color:#eec48c;margin:4px 0 0}',
'.skin-painter-body label{font-size:11px;color:#d0c3ca;display:flex;flex-direction:column;gap:5px;min-width:0}',
'.skin-painter-body button{background:#493749;color:#ffeddd;border:1px solid #815e60;border-radius:7px;padding:9px 3px;font-size:11px;min-height:35px}',
'.skin-painter-body button:disabled{opacity:.45}',
'.skin-painter-body .sp-primary{background:linear-gradient(130deg,#af6152,#633a60);border-color:#e6aa69}',
'.skin-painter-body input,.skin-painter-body select{width:100%;min-width:0;box-sizing:border-box;background:#101522;color:#f8e6d5;border:1px solid #51445a;border-radius:6px;padding:8px;font-size:12px}',
'.skin-painter-body input[type=color]{padding:3px;height:36px}',
'.skin-painter-body input[type=range]{padding:0;accent-color:#dda16f}',
'.skin-painter-body .sp-check{flex-direction:row;align-items:center;gap:8px}',
'.skin-painter-body .sp-check input{width:18px;height:18px;flex:none;accent-color:#dca66e}',
'.skin-painter-row{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:7px}',
'.skin-painter-row.sp-two{grid-template-columns:1fr 1fr}',
'.sp-scroll{max-height:340px;min-height:130px;width:100%;overflow:auto;border:1px solid #515168;background-color:#232634;background-image:linear-gradient(45deg,#33313d 25%,transparent 25%),linear-gradient(-45deg,#33313d 25%,transparent 25%);background-size:16px 16px}',
'.sp-wrap{position:relative;width:max-content;height:max-content;line-height:0}',
'#spCanvas{display:block;touch-action:none;cursor:crosshair;image-rendering:pixelated}',
'.sp-highlight{position:absolute;pointer-events:none;border:2px solid #f6cc69;box-shadow:0 0 8px #ebb45e}',
'.sp-info{font:11px ui-monospace,monospace;color:#dbb892;white-space:pre-wrap}'
].join('');
document.head.append(css);
document.querySelector('.skin-studio').append(pane);
const canvas=$('spCanvas'),ctx=canvas.getContext('2d',{willReadFrequently:true}),scene=api.getCanvas();
let ready=false,original='',fileName='skin',history=[],future=[],active=null,liveImage=null,last=null,zoom=6,paintTimer=null,serial=0;
function info(s){$('spInfo').textContent=s;}
function current(){return ctx.getImageData(0,0,canvas.width,canvas.height);}
function snapshot(){history.push(current());const max=canvas.width>512?6:20;if(history.length>max)history.shift();future=[];}
function buttons(){$('spUndo').disabled=!history.length;$('spRedo').disabled=!future.length;}
async function updateTexture(immediate=false){
 if(!ready)return;
 if(!immediate){if(paintTimer)return;paintTimer=setTimeout(()=>{paintTimer=null;updateTexture(true);},130);return;}
 if(paintTimer){clearTimeout(paintTimer);paintTimer=null;}
 try{await skin.updateEditedPng(canvas.toDataURL('image/png'));}catch(e){console.error('Skin preview:',e);}
}
function setImage(data){ctx.putImageData(data,0,0);updateTexture(true);}
function color(){const c=$('spColor').value;return [parseInt(c.slice(1,3),16),parseInt(c.slice(3,5),16),parseInt(c.slice(5,7),16),255];}
function getPos2d(e){const r=canvas.getBoundingClientRect();if(!r.width||!r.height)return null;const x=Math.floor((e.clientX-r.left)/r.width*canvas.width),y=Math.floor((e.clientY-r.top)/r.height*canvas.height);return x<0||y<0||x>=canvas.width||y>=canvas.height?null:{x,y};}
function getPos3d(e){return api.pickSkinUV(e.clientX,e.clientY,$('spOuter').checked?'outer':'base');}
function at(p){if(!p)return;const tool=$('spTool').value;if(tool==='pick'){const d=ctx.getImageData(p.x,p.y,1,1).data;$('spColor').value='#'+[d[0],d[1],d[2]].map(n=>n.toString(16).padStart(2,'0')).join('');info('Conta-gotas '+$('spColor').value+' · alfa '+d[3]);return;}
 const c=tool==='erase'?[0,0,0,0]:color(),sz=Number($('spSize').value);
 if(tool==='fill')core.floodFill(liveImage,p.x,p.y,c);
 else if(last)core.drawLine(liveImage,last.x,last.y,p.x,p.y,c,sz);
 else core.putPixel(liveImage,p.x,p.y,c,sz);
 last=p;ctx.putImageData(liveImage,0,0);info('Pixel '+p.x+', '+p.y+' | '+canvas.width+'×'+canvas.height);
 updateTexture(false);
}
function down(e,mode){
 if(!ready||mode==='3d'&&!$('sp3d').checked)return;
 if(e.pointerType==='mouse'&&e.button!==0)return;
 if(mode==='3d'){e.preventDefault();e.stopImmediatePropagation();}
 if(active!==null)return;
 const p=mode==='3d'?getPos3d(e):getPos2d(e);if(!p)return;
 const tool=$('spTool').value;
 if(tool==='pick'){at(p);return;}
 snapshot();liveImage=current();last=null;active=e.pointerId;e.currentTarget.setPointerCapture?.(e.pointerId);at(p);buttons();
 if(mode==='3d'){const anim=api.getAnimation();api.setAnimation(null,anim.time,false);}
}
function move(e,mode){if(mode==='3d'&&$('sp3d').checked){e.preventDefault();e.stopImmediatePropagation();}
 if(active!==e.pointerId||!ready||$('spTool').value==='fill')return;
 const p=mode==='3d'?getPos3d(e):getPos2d(e);if(p)at(p);
}
function up(e,mode){if(mode==='3d'&&$('sp3d').checked){e.preventDefault();e.stopImmediatePropagation();}
 if(active!==e.pointerId)return;active=null;liveImage=null;last=null;updateTexture(true);buttons();}
for(const [node,mode] of [[canvas,'2d'],[scene,'3d']]){
 node.addEventListener('pointerdown',e=>down(e,mode),true);
 node.addEventListener('pointermove',e=>move(e,mode),true);
 node.addEventListener('pointerup',e=>up(e,mode),true);
 node.addEventListener('pointercancel',e=>up(e,mode),true);
}
$('spUndo').onclick=()=>{if(!history.length)return;future.push(current());setImage(history.pop());buttons();};
$('spRedo').onclick=()=>{if(!future.length)return;history.push(current());setImage(future.pop());buttons();};
$('spReset').onclick=()=>{if(!ready)return;if(!confirm('Restaurar todos os pixels originais?'))return;snapshot();load(original,false);};
function face(){const model=api.getModel(),part=model?.elements?.find(e=>e.uuid===$('spPart').value),uv=part?.faces?.[$('spFace').value]?.uv;return uv?core.region(uv,canvas.width,canvas.height):null;}
function highlight(){const r=ready&&face(),h=$('spHighlight');if(!r){h.hidden=true;return;}h.hidden=false;h.style.left=r.x*zoom+'px';h.style.top=r.y*zoom+'px';h.style.width=r.w*zoom+'px';h.style.height=r.h*zoom+'px';}
function showParts(){const select=$('spPart'),old=select.value;select.replaceChildren();for(const e of api.getModel()?.elements||[]){if(!e.uuid?.startsWith('cube-'))continue;const opt=document.createElement('option');opt.value=e.uuid;opt.textContent=e.name;select.append(opt);}if([...select.options].some(o=>o.value===old))select.value=old;highlight();}
function zoomAtlas(){zoom=Number($('spZoom').value);canvas.style.width=canvas.width*zoom+'px';canvas.style.height=canvas.height*zoom+'px';$('spZoomText').textContent=zoom+'×';highlight();}
$('spPart').onchange=highlight;$('spFace').onchange=highlight;$('spZoom').oninput=zoomAtlas;
$('spOuter').onchange=e=>{if(e.target.checked&&$('skinLayers')&&!$('skinLayers').checked){$('skinLayers').checked=true;$('skinLayers').dispatchEvent(new Event('change'));}};
function invert(type){if(!ready)return notify('Abra uma skin primeiro.');try{const r=face();if(!r)throw Error('Selecione uma face');snapshot();const data=current();core.flipFace(data,[r.x,r.y,r.x+r.w,r.y+r.h],type);setImage(data);buttons();notify('Face corrigida. Salve o PNG editado.');}catch(e){notify(e.message);}}
$('spFlipH').onclick=()=>invert('horizontal');$('spFlipV').onclick=()=>invert('vertical');$('spFlip180').onclick=()=>invert('rotate180');
$('spSave').onclick=async()=>{if(!ready)return notify('Abra uma skin.');try{await updateTexture(true);const url=canvas.toDataURL('image/png'),name=fileName+'_editada.png';if(window.PortAndroid?.savePNG)window.PortAndroid.savePNG(url,name);else{const a=document.createElement('a');a.download=name;a.href=url;document.body.append(a);a.click();a.remove();}notify('Escolha onde salvar o PNG.');}catch(e){notify('PNG: '+e.message);}};
$('spBB').onclick=async()=>{if(!ready)return notify('Abra uma skin.');try{await updateTexture(true);const blob=new Blob([JSON.stringify(api.getModel(),null,2)],{type:'application/json'});await window.PortV2.saveBlob(blob,fileName+'_editada.bbmodel','application/json');}catch(e){notify(e.message);}};
async function load(url,clearHistory=true){
 const id=++serial,img=new Image();await new Promise((ok,fail)=>{img.onload=ok;img.onerror=()=>fail(Error('Textura PNG não carregada'));img.src=url;});
 if(id!==serial)return;canvas.width=img.naturalWidth;canvas.height=img.naturalHeight;ctx.imageSmoothingEnabled=false;ctx.clearRect(0,0,canvas.width,canvas.height);ctx.drawImage(img,0,0);
 ready=true;if(clearHistory){original=url;history=[];future=[];fileName=String(api.getModel()?.name||'skin').replace(/[^A-Za-z0-9_-]+/g,'_').slice(0,65);}
 if(clearHistory){$('spZoom').value=String(Math.max(1,Math.min(6,Math.floor(350/canvas.width))));}zoomAtlas();showParts();buttons();info('Mapa UV '+canvas.width+'×'+canvas.height+' · toque para pintar');await updateTexture(true);
}
async function onModel(){if(paintTimer){clearTimeout(paintTimer);paintTimer=null;}active=null;liveImage=null;const model=api.getModel();if(!model?.port_skin){ready=false;history=[];future=[];buttons();$('spHighlight').hidden=true;info('Abra uma skin PNG para editar.');return;}
 const url=model.textures?.[0]?.source;if(!url?.startsWith('data:image/png;base64,'))return;
 try{await load(url,true);}catch(e){notify('Mapa UV: '+e.message);}
}
window.addEventListener('port-model-opened',onModel);
window.PortVoxelSkinPainter={onModel,savePNG:()=> $('spSave').click(),atlas:canvas};
buttons();onModel();
})();