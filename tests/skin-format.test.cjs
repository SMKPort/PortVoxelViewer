/* Tests for skin atlas layout, skin model generation and animations; no dependencies. */
const fs=require('node:fs');
const assert=require('node:assert/strict');
const source=fs.readFileSync('app/src/main/assets/skin-studio.js','utf8');
const start=source.indexOf('function skinFaces('),end=source.indexOf('function renderSkin(');
assert(start>0&&end>start,'Skin model constructor not found');
const buildSkin=new Function(source.slice(start,end)+'\nreturn buildSkin;')();
const make=(legacy,slim,ratio=1)=>buildSkin({name:'Test',dataUrl:'data:image/png;base64,AA==',legacy,slim,ratio});
for(const [title,legacy,slim,ratio] of [
 ['classic',false,false,1],['slim',false,true,1],['legacy',true,false,1],['HD',false,true,16]
]){
 const model=make(legacy,slim,ratio),maxX=64*ratio,maxY=(legacy?32:64)*ratio;
 assert.equal(model.elements.length,legacy?7:12,title+' element count');
 assert.equal(model.port_skin.layers.length,legacy?1:6,title+' overlay count');
 assert.equal(model.animations.length,2,title+' animations');
 assert.equal(model.resolution.width,maxX);
 assert.equal(model.resolution.height,maxY);
 for(const e of model.elements){
  assert(e.from.every(Number.isFinite)&&e.to.every(Number.isFinite));
  for(const f of Object.values(e.faces)){
   assert.equal(f.uv.length,4);
   f.uv.forEach((n,i)=>assert(n>=0&&n<=(i%2?maxY:maxX),title+' UV '+e.uuid+' '+n));
  }
 }
 const arm=model.elements.find(x=>x.uuid==='cube-right-arm');
 assert.equal(arm.to[0]-arm.from[0],slim?3:4);
 const animIds=Object.keys(model.animations[0].animators);
 assert(animIds.every(k=>model.groups.some(g=>g.uuid===k)));
 for(const group of model.outliner){
  assert(group.children.every(c=>typeof c==='string'||(c.uuid&&Array.isArray(c.children))),'Nested overlay in main group');
 }
 console.log('PASS '+title+': '+model.elements.length+' pieces, '+model.port_skin.layers.length+' overlays, '+maxX+'x'+maxY);
}
const modern=make(false,false),legacy=make(true,false);
assert.deepEqual(modern.elements.find(e=>e.uuid==='cube-head').faces.north.uv,[8,8,16,16]);
assert.deepEqual(modern.elements.find(e=>e.uuid==='cube-right-arm').faces.north.uv,[44,20,48,32]);
assert.deepEqual(make(false,true).elements.find(e=>e.uuid==='cube-right-arm').faces.north.uv,[44,20,47,32]);
assert(legacy.port_skin.layers.every(id=>id==='skin-head-outer'));
console.log('Skin model unit checks passed.');
