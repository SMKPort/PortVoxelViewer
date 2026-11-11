"use strict";
(function () {
 const V=window.NexusV2, S=V.state, E=V.esc, q=x=>document.querySelector(x);
 const TYPES=[["Sol","☼"],["Lua","☽"],["Selo","✧"],["Âncora","◇"],["Vínculo","⌘"],["Ciclo","⊗"],["Olho","◉"],["Proteção","⛨"]];
 const SPOTS=[[260,105],[405,260],[260,415],[115,260],[145,145],[375,145],[375,375],[145,375]];
 function diagram() {
   const lines='<circle cx="260" cy="260" r="215"/><circle cx="260" cy="260" r="195"/><circle cx="260" cy="260" r="155"/><circle cx="260" cy="260" r="105"/><path d="M260 40 L450 370 H70 Z M260 480 L70 150 H450 Z"/><path d="M260 105 L415 260 L260 415 L105 260 Z"/>';
   const glyphs=S.ritualCanvas.runes.map(r=>{
      const symbol=TYPES.find(x=>x[0]===r.type)?.[1]||"✧";
      return '<g data-glyph="'+E(r.id)+'" transform="translate('+r.x+','+r.y+')"><circle r="24" fill="#21141c" stroke="#d5b275"/><text x="0" y="10" text-anchor="middle" font-size="28" fill="#efcb8f" stroke="none" style="pointer-events:none">'+symbol+'</text></g>';
   }).join("");
   return '<svg id="nexusRuneBoard" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 520"><rect width="520" height="520" fill="#110f17"/><g stroke="#ae765d" fill="none" stroke-width="1.5">'+lines+'</g><g>'+glyphs+'</g></svg>';
 }
 function view() {
   const d=S.ritualCanvas;
   const palette=TYPES.map(x=>'<button type="button" class="v2-rune-chip" data-v2="rune-add" data-type="'+E(x[0])+'"><span>'+x[1]+'</span>'+E(x[0])+'</button>').join("");
   const left=diagram()+'<p class="panel-caption">Arraste os símbolos sobre o círculo. Use os botões abaixo para inserir novas runas.</p><div class="v2-rune-palette">'+palette+'</div><div class="actions">'+V.button("rune-undo","↶ REMOVER","ghost")+V.button("rune-clear","LIMPAR","ghost")+V.button("rune-export","↓ SVG","gold")+'</div>';
   const form='<form data-v2-form="rune-sim" class="stack"><label class="field">NOME DO RITUAL<input name="name" required maxlength="120" value="'+E(d.name)+'"></label><label class="field">ESCOLA ARCANA<select name="school">'+["Divina","Demoníaca","Arcana","Ancestral","Híbrida"].map(x=>'<option>'+x+'</option>').join("")+'</select></label><label class="field">DEMANDA FICTÍCIA: <span id="runeEnergyLabel">'+d.complexity+'%</span><input id="runeEnergy" name="energy" type="range" min="0" max="100" value="'+d.complexity+'"></label><label class="field">COMPONENTES<textarea name="components">'+E(d.components||"")+'</textarea></label><label class="field">ETAPAS<textarea name="steps">'+E(d.steps||"")+'</textarea></label><button type="submit" class="btn gold">✧ SIMULAR E SALVAR</button></form>';
   let report='<div class="notice">Adicione runas e simule para obter uma análise narrativa.</div>';
   if(d.last){const l=d.last;report='<div class="v2-analysis"><strong>ANÁLISE RITUALÍSTICA</strong><div class="big-number">'+l.score+'%</div><p>'+E(l.assessment)+'</p><p class="panel-caption">Simetria '+l.symmetry+'% · Custo fictício '+l.cost+'</p></div>';}
   const right=form+report+'<div class="actions">'+V.button("rune-original","ABRIR RITUAIS ORIGINAIS","ghost")+'</div>';
   return V.intro("MÓDULO 12 · COMPOSIÇÃO RÚNICA","Rituais Avançados","Monte círculos rúnicos com símbolos móveis, simule estabilidade e arquive no acervo original.","LORE INTERATIVA")+'<div class="grid two v2-ritual-grid">'+V.card("EDITOR VISUAL",'<div class="v2-ritual-board">'+left+'</div>')+V.card("SIMULADOR RITUALÍSTICO",right)+'</div><div class="notice">Percentuais são regras fictícias de roleplay. Nenhum ritual é executado fisicamente.</div>';
 }
 V.register("rituais2",view,setup);
 let dragged=null;
 function setup(){
   const svg=q("#nexusRuneBoard");if(!svg)return;
   function point(e){const p=svg.createSVGPoint();p.x=e.clientX;p.y=e.clientY;const m=svg.getScreenCTM();if(!m)return {x:260,y:260};const n=p.matrixTransform(m.inverse());return {x:V.clamp(n.x,25,495),y:V.clamp(n.y,25,495)};}
   function down(e){const g=e.target.closest?.("[data-glyph]");if(!g)return;dragged=g.dataset.glyph;svg.setPointerCapture(e.pointerId);}
   function move(e){if(!dragged)return;const item=S.ritualCanvas.runes.find(x=>x.id===dragged);if(!item)return;const p=point(e);item.x=Math.round(p.x);item.y=Math.round(p.y);const el=[...svg.querySelectorAll("[data-glyph]")].find(x=>x.dataset.glyph===dragged);if(el)el.setAttribute("transform","translate("+item.x+","+item.y+")");}
   function up(){if(dragged){dragged=null;V.save();}}
   svg.addEventListener("pointerdown",down);svg.addEventListener("pointermove",move);svg.addEventListener("pointerup",up);svg.addEventListener("pointercancel",up);
   V.addDisposer(()=>{svg.removeEventListener("pointerdown",down);svg.removeEventListener("pointermove",move);svg.removeEventListener("pointerup",up);svg.removeEventListener("pointercancel",up);dragged=null;});
 }
 V.actions["rune-add"]=el=>{const type=el.dataset.type;const i=S.ritualCanvas.runes.length;if(i>=24)return V.toast("Limite de 24 runas.");const p=SPOTS[i%8];S.ritualCanvas.runes.push({id:V.id(),type,x:p[0]+Math.floor(i/8)*12,y:p[1]+Math.floor(i/8)*12});V.save();V.rerender();};
 V.actions["rune-undo"]=()=>{S.ritualCanvas.runes.pop();V.save();V.rerender();};
 V.actions["rune-clear"]=()=>{if(confirm("Apagar o círculo?")){S.ritualCanvas.runes=[];S.ritualCanvas.last=null;V.save();V.rerender();}};
 V.actions["rune-original"]=()=>V.navigate("rituais");
 V.actions["rune-export"]=()=>{const svg=q("#nexusRuneBoard");if(!svg)return;const blob=new Blob([new XMLSerializer().serializeToString(svg)],{type:"image/svg+xml"});const url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download="nexus-circulo.svg";a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
 document.addEventListener("input",e=>{if(e.target.id==="runeEnergy"&&q("#runeEnergyLabel"))q("#runeEnergyLabel").textContent=e.target.value+"%";});
 V.forms["rune-sim"]=f=>{
   const data=new FormData(f),d=S.ritualCanvas;
   d.name=String(data.get("name")||"").trim().slice(0,120);d.school=String(data.get("school")||"Arcana");d.complexity=V.clamp(Number(data.get("energy")),0,100);d.components=String(data.get("components")||"").slice(0,2000);d.steps=String(data.get("steps")||"").slice(0,2500);
   const count=d.runes.length;
   const error=count?d.runes.reduce((n,r)=>n+Math.abs(Math.hypot(r.x-260,r.y-260)-195),0)/count:195;
   const symmetry=V.clamp(Math.round(100-error*.35-(count<3?35:0)),0,100);
   const types=new Set(d.runes.map(x=>x.type)).size;
   const score=V.clamp(Math.round(symmetry*.58+Math.min(count,12)*2+types*2-d.complexity*.16),0,99);
   const cost=Math.round(d.complexity*.8+count*3+types*2);
   const assessment=count<3?"Adicione símbolos para melhorar a sustentação do círculo.":score>=70?"Boa estabilidade conforme as regras do universo.":"A geometria ou a demanda energética podem ser revistas.";
   d.last={score,symmetry,cost,assessment,at:Date.now()};V.save();
   const old=V.getOld();if(Array.isArray(old.rituals)){old.rituals.unshift({id:V.id(),name:d.name,school:d.school,components:d.components,energy:d.complexity,symmetry,steps:d.steps,score,at:new Date().toISOString()});V.persistOld?.();}
   V.log("Ritual avançado "+d.name+" registrado.","rituais");V.rerender();V.toast("Ritual salvo no acervo.");
 };
})();