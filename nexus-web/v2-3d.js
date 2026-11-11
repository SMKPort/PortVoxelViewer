"use strict";
(function(){
const V=window.NexusV2,S=V.state,E=V.esc,B=V.button,C=V.card,clamp=V.clamp;
const q=s=>document.querySelector(s);
let scene=null,root=null,renderer=null,camera=null,controls=null,elbow=null,plates=[],moving=false,exploded=false,mixer=null,frame=0,session=0;
function viewerHTML(){
return V.intro("MÓDULO 09 · GÊMEO DIGITAL","Braço Nexus em 3D","Rotacione e inspecione o braço, movimente o cotovelo e importe GLB, GLTF autossuficiente ou cubos BBMODEL.","WEBGL INTERATIVO")+
'<div class="grid two v2-model-grid">'+
C("VISUALIZADOR TRIDIMENSIONAL",'<div class="v2-viewport" id="nexus3d"><div class="v2-load-msg">Inicializando WebGL...</div></div><p class="panel-caption">Arraste para girar. Role ou aproxime os dedos para zoom.</p><div class="actions">'+B("3d-reset","⟳ CENTRALIZAR","ghost")+B("3d-plates","✧ PLACAS","gold")+B("3d-animate","▶ COTOVELO","ghost")+'</div><label class="field">ÂNGULO DO COTOVELO <b id="nexusAngleText">'+(S.blueprint.hinge||0)+'°</b><input id="nexusAngle" type="range" min="-25" max="115" value="'+(S.blueprint.hinge||0)+'"></label>')+
C("COMPONENTES E IMPORTAÇÃO",'<div class="v2-kpis"><div><small>PROTÓTIPO</small><strong>ARCANO MK-II</strong></div><div><small>GEOMETRIA</small><strong>3D</strong></div></div><label class="field">IMPORTAR MODELO<input id="nexus3dFile" type="file" data-v2-change="3d-file" accept=".glb,.gltf,.bbmodel"></label><p class="panel-caption">Modelos BBMODEL são reconstruídos por cubos, sem todas as texturas e hierarquias de bones. Para fidelidade, exporte GLB no Blockbench.</p><div class="actions">'+B("3d-default","MODELO DE REFERÊNCIA","gold")+B("3d-photo","CAPTURA PNG","ghost")+'</div><div class="divider"></div><div class="item-card"><strong>ESTRUTURA PRÓPRIA DO NEXUS</strong><p>Ombreira, núcleo de energia dourada, placas retráteis, articulação de cotovelo e manopla sem dedos.</p></div><div class="notice">O modelo inicial é uma reconstrução geométrica conceitual. Importe seu arquivo real para inspecionar o Nexus fielmente.</div>')+'</div>';}
V.register("gemeo",viewerHTML,()=>mount3D());
function material(T,color,emissive){return new T.MeshStandardMaterial({color,metalness:.8,roughness:.27,emissive:emissive||"#000000",emissiveIntensity:emissive?.6:0});}
function makeArm(T){
if(root)scene.remove(root);
root=new T.Group();scene.add(root);root.position.y=.35;elbow=null;plates=[];exploded=false;mixer=null;
const dark=material(T,"#242532"),gold=material(T,"#d8b878"),red=material(T,"#943843"),light=material(T,"#edcb91","#d58e48");
function cube(group,name,w,h,d,x,y,z,mat){const o=new T.Mesh(new T.BoxGeometry(w,h,d),mat);o.position.set(x,y,z);o.name=name;group.add(o);return o;}
cube(root,"Ombro",.85,.4,.75,0,1.9,0,dark);cube(root,"Acoplamento",.9,.23,.85,0,1.8,0,gold);cube(root,"Braço superior",.62,1.45,.65,0,1,0,dark);
[1.6,1.14,.69].forEach((y,i)=>{const g=new T.Group();g.position.set(0,y,0);cube(g,"Placa superior "+i,.78,.32,.15,0,0,.4,i%2?gold:red);root.add(g);plates.push({group:g,base:g.position.clone(),direction:new T.Vector3(i%2?-1:1,.1,1)});});
const joint=new T.Group();joint.position.y=.24;root.add(joint);elbow=joint;
const ball=new T.Mesh(new T.SphereGeometry(.3,20,16),gold);joint.add(ball);
const low=new T.Group();low.position.y=-.86;joint.add(low);cube(low,"Estrutura antebraço",.66,1.7,.7,0,0,0,dark);
[.57,.21,-.15,-.51].forEach((y,i)=>{const g=new T.Group();g.position.y=y;cube(g,"Placa do Nexus "+i,.78,.3,.18,0,0,.43,i%2?red:gold);low.add(g);plates.push({group:g,base:g.position.clone(),direction:new T.Vector3(i%2?1:-1,.2,1)});});
cube(low,"Canal de energia",.37,.6,.15,0,.1,.52,light);
cube(low,"Manopla sem dedos",.72,.52,.71,0,-1.12,0,dark);
cube(low,"Armadura da mão",.76,.21,.73,0,-.9,0,gold);
const core=new T.Mesh(new T.OctahedronGeometry(.23),light);core.position.set(0,-.25,.57);low.add(core);
elbow.rotation.z=(S.blueprint.hinge||0)*Math.PI/180;
}
async function mount3D(){
const host=q("#nexus3d");if(!host)return;
const my=++session;V.addDisposer(()=>{session++;dispose3D();});
try{
const [T,Orbit,GLTF]=await Promise.all([import("three"),import("three/addons/controls/OrbitControls.js"),import("three/addons/loaders/GLTFLoader.js")]);
if(my!==session||!host.isConnected)return;
V.three={T,GLTF:GLTF.GLTFLoader};scene=new T.Scene();scene.background=new T.Color("#0b1017");
camera=new T.PerspectiveCamera(45,1,.01,100);camera.position.set(4,2,6.2);
renderer=new T.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});renderer.setPixelRatio(Math.min(devicePixelRatio||1,2));renderer.outputColorSpace=T.SRGBColorSpace;
host.replaceChildren(renderer.domElement);renderer.domElement.style.cssText="width:100%;height:100%;display:block";
controls=new Orbit.OrbitControls(camera,renderer.domElement);controls.enableDamping=true;controls.target.set(0,.25,0);controls.minDistance=2;controls.maxDistance=14;
scene.add(new T.AmbientLight("#cad5e4",1.2));
const direct=new T.DirectionalLight("#ffd8a3",2.4);direct.position.set(4,5,6);scene.add(direct);
const red=new T.PointLight("#df5b68",40,10);red.position.set(-3,1,2);scene.add(red);
const grid=new T.GridHelper(8,16,"#5e4148","#392e38");grid.position.y=-2.25;scene.add(grid);
makeArm(T);let last=0;
function loop(now){
if(session!==my)return;frame=requestAnimationFrame(loop);if(moving&&elbow){elbow.rotation.z=(35+Math.sin(now*.002)*45)*Math.PI/180;const label=q("#nexusAngleText");if(label)label.textContent=Math.round(elbow.rotation.z*180/Math.PI)+"°";}
if(mixer)mixer.update(Math.min((now-last)/1000,.05));last=now;
const w=Math.round(host.clientWidth)||550,h=Math.round(host.clientHeight)||430;
if(renderer.domElement.clientWidth!==w||renderer.domElement.clientHeight!==h){renderer.setSize(w,h,false);camera.aspect=w/h;camera.updateProjectionMatrix();}
controls.update();renderer.render(scene,camera);
}loop(0);
}catch(err){if(my===session)host.innerHTML='<div class="v2-load-msg">Visualizador indisponível: '+V.esc(err.message)+'. Verifique WebGL e o carregamento de Three.js.</div>';}
}
function dispose3D(){moving=false;cancelAnimationFrame(frame);if(controls)controls.dispose();if(renderer){renderer.dispose();renderer.forceContextLoss?.();renderer.domElement.remove();}if(root)root.traverse(x=>{x.geometry?.dispose?.();if(x.material){(Array.isArray(x.material)?x.material:[x.material]).forEach(m=>m.dispose?.());}});root=null;elbow=null;plates=[];scene=null;renderer=null;controls=null;mixer=null;}
V.actions["3d-default"]=()=>{if(scene&&V.three)makeArm(V.three.T);S.blueprint.parts=[];V.save();V.toast("Modelo geométrico de referência carregado.");};
V.actions["3d-plates"]=()=>{if(!plates.length)return V.toast("Disponível no protótipo de referência.");exploded=!exploded;plates.forEach(p=>p.group.position.copy(p.base).addScaledVector(p.direction,exploded?.38:0));V.toast(exploded?"Placas separadas.":"Placas recolhidas.");};
V.actions["3d-animate"]=()=>{if(!elbow)return V.toast("Use um modelo de referência para testar a articulação.");moving=!moving;V.toast(moving?"Articulação em andamento.":"Articulação pausada.");};
V.actions["3d-reset"]=()=>{if(camera&&controls){camera.position.set(4,2,6.2);controls.target.set(0,.25,0);controls.update();}};
V.actions["3d-photo"]=()=>{if(!renderer)return;const a=document.createElement("a");a.download="nexus-aterna-3d.png";a.href=renderer.domElement.toDataURL("image/png");a.click();};
document.addEventListener("input",e=>{if(e.target.id==="nexusAngle"){S.blueprint.hinge=Number(e.target.value);V.save();if(elbow){moving=false;elbow.rotation.z=S.blueprint.hinge*Math.PI/180;}const label=q("#nexusAngleText");if(label)label.textContent=S.blueprint.hinge+"°";}});
V.changes["3d-file"]=async el=>{
const file=el.files?.[0];if(!file||!scene||!V.three)return;if(file.size>20*1024*1024)throw Error("Arquivo maior que 20 MB.");
const T=V.three.T,ext=file.name.split(".").pop().toLowerCase();let model;
if(ext==="bbmodel"){const json=JSON.parse(await file.text());if(!Array.isArray(json.elements))throw Error("BBMODEL sem elementos.");
model=new T.Group();let count=0;for(const e of json.elements.slice(0,3000)){if(!Array.isArray(e.from)||!Array.isArray(e.to))continue;const f=e.from,t=e.to;const dims=f.map((v,i)=>Math.abs(t[i]-v)/16);if(dims.some(v=>v<=0))continue;const c=new T.Mesh(new T.BoxGeometry(...dims),material(T,"#bf975f"));c.position.set((f[0]+t[0])/32,(f[1]+t[1])/32,(f[2]+t[2])/32);c.name=e.name||"Cubo";if(Array.isArray(e.rotation))c.rotation.set(...e.rotation.map(v=>v*Math.PI/180));model.add(c);count++;}if(!count)throw Error("Sem cubos válidos para reconstrução.");}
else if(ext==="glb"||ext==="gltf"){const url=URL.createObjectURL(file);try{const data=await new Promise((resolve,reject)=>new V.three.GLTF().load(url,resolve,undefined,reject));model=data.scene;if(data.animations?.length){mixer=new T.AnimationMixer(model);data.animations.forEach(clip=>mixer.clipAction(clip).play());}}finally{URL.revokeObjectURL(url);}}
else throw Error("Use .glb, .gltf ou .bbmodel.");
const bounds=new T.Box3().setFromObject(model);const size=bounds.getSize(new T.Vector3()),center=bounds.getCenter(new T.Vector3());model.position.sub(center);model.scale.setScalar(3.5/Math.max(.1,size.x,size.y,size.z));if(root)scene.remove(root);root=model;scene.add(root);elbow=null;plates=[];S.blueprint.parts=[{name:file.name,objects:model.children.length}];V.save();V.toast("Modelo "+file.name+" carregado. Geometria: "+model.children.length+" objetos.");
};
})();