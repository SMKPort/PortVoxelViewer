(function () {
'use strict';
const E=window.Electrical;
const $=id=>document.getElementById(id);
const escapeHtml=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const views=['resolver','circuitos','treinar','formulas','historico'];
function nav(id) { if(!views.includes(id))return; document.querySelectorAll('.view').forEach(x=>x.classList.toggle('active',x.id===id)); document.querySelectorAll('[data-nav]').forEach(x=>x.classList.toggle('active',x.dataset.nav===id)); window.scrollTo({top:0,behavior:'smooth'}); if(id==='historico')renderHistory(); }
document.querySelectorAll('[data-nav]').forEach(btn=>btn.addEventListener('click',()=>nav(btn.dataset.nav)));
function error(place,msg){$(place).innerHTML=`<div class="error-card"><strong>Não foi possível resolver.</strong><div>${escapeHtml(msg)}</div></div>`;}
function show(place,result,origin,save=true){$(place).innerHTML=`<div class="result-card"><h3>${escapeHtml(result.title)}</h3><div class="result-value">${escapeHtml(result.answer)}</div>${result.extra?`<div class="result-extra">${escapeHtml(result.extra)}</div>`:''}<ol class="steps">${result.steps.map(step=>`<li>${escapeHtml(step)}</li>`).join('')}</ol></div>`; if(save)saveHistory(origin,result);}
function saveHistory(origin,result){try{const hist=JSON.parse(localStorage.getItem('port-eletrica-history')||'[]');hist.unshift({date:new Date().toISOString(),origin,title:result.title,answer:result.answer,steps:result.steps});localStorage.setItem('port-eletrica-history',JSON.stringify(hist.slice(0,30)));}catch(e){/* modo de armazenamento restrito */}}
function renderHistory(){const node=$('history-content');let hist=[];try{hist=JSON.parse(localStorage.getItem('port-eletrica-history')||'[]');}catch(e){}if(!hist.length){node.innerHTML='<div class="empty-state">⚡ Nenhuma questão resolvida por enquanto.</div>';return;}node.innerHTML=hist.map(x=>`<div class="history-item"><strong>${escapeHtml(x.title)}</strong><p>${escapeHtml(x.answer)}<br>${escapeHtml(x.origin)}</p><small>${escapeHtml(new Date(x.date).toLocaleString('pt-BR'))}</small></div>`).join('');}
$('clear-history').addEventListener('click',()=>{if(window.confirm('Apagar todas as soluções salvas neste aparelho?')){try{localStorage.removeItem('port-eletrica-history');}catch(e){}renderHistory();}});
const samples={ohm:'Um resistor de 12 Ω está ligado a uma tensão de 24 V. Qual é a corrente elétrica?',energy:'Um aparelho de 1000 W permanece ligado durante 2 horas. Qual é a energia consumida em kWh?',parallel:'Dois resistores de 10 Ω e 20 Ω estão ligados em paralelo a uma fonte de 12 V. Qual a resistência equivalente?'};
document.querySelectorAll('[data-example]').forEach(btn=>btn.addEventListener('click',()=>{$('statement').value=samples[btn.dataset.example];$('statement').focus();}));
$('solve-statement').addEventListener('click',()=>{try{const statement=$('statement').value;const result=E.solveStatement(statement);show('statement-result',result,statement);}catch(e){error('statement-result',e.message);}});
const field=(key,label,placeholder='',initial='')=>({key,label,placeholder,initial});
const configs={
 ohm:{hint:'Preencha exatamente dois dos três campos. Deixe o que deseja descobrir vazio.',fields:[field('v','Tensão (V)','Ex.: 24'),field('i','Corrente (A)','Ex.: 2'),field('r','Resistência (Ω)','Ex.: 12')]},
 power:{hint:'Preencha exatamente dois campos quaisquer para calcular as grandezas relacionadas.',fields:[field('v','Tensão (V)'),field('i','Corrente (A)'),field('r','Resistência (Ω)'),field('p','Potência (W)')]},
 energy:{hint:'Informe potência, tempo e, opcionalmente, a tarifa em R$/kWh.',fields:[field('p','Potência (W)'),field('t','Tempo'),field('tariff','Tarifa (R$/kWh), opcional')]},
 series:{hint:'Digite ao menos duas resistências positivas, separadas por ponto e vírgula. Tensão opcional.',fields:[field('values','Resistências (Ω)','10; 20; 30'),field('v','Tensão da fonte (V), opcional')]},
 parallel:{hint:'Digite ao menos duas resistências positivas, separadas por ponto e vírgula. Tensão opcional.',fields:[field('values','Resistências (Ω)','10; 20; 30'),field('v','Tensão da fonte (V), opcional')]},
 charge:{hint:'Preencha dois dos três valores para descobrir o restante.',fields:[field('q','Carga (C)'),field('i','Corrente (A)'),field('t','Tempo (s)')]},
 resistivity:{hint:'Use a resistividade em Ω·m, o comprimento em metros e a área em mm².',fields:[field('rho','Resistividade (Ω·m)','0,0000000172'),field('l','Comprimento (m)','Ex.: 10'),field('a','Área (mm²)','Ex.: 2,5')]}
};
function renderFields(){const mode=$('calc-mode').value,conf=configs[mode];$('calc-tip').textContent=conf.hint;$('calc-fields').innerHTML=conf.fields.map(f=>`<label for="field-${f.key}"><span>${escapeHtml(f.label)}</span><input id="field-${f.key}" type="text" inputmode="decimal" value="${escapeHtml(f.initial)}" placeholder="${escapeHtml(f.placeholder)}"></label>`).join('')+(mode==='energy'?`<label for="field-unit"><span>Unidade de tempo</span><select id="field-unit"><option value="h">Horas</option><option value="min">Minutos</option><option value="s">Segundos</option></select></label>`:'');$('calc-result').innerHTML='';}
$('calc-mode').addEventListener('change',renderFields);renderFields();
$('solve-calc').addEventListener('click',()=>{const mode=$('calc-mode').value;const input={};for(const f of configs[mode].fields){const raw=$('field-'+f.key).value.trim();input[f.key]=f.key==='values'?raw:E.number(raw);if(raw!==''&&f.key!=='values'&&input[f.key]===null){error('calc-result',`Número inválido no campo ${f.label}.`);return;}}try{let res;switch(mode){case 'ohm':res=E.ohm(input);break;case 'power':res=E.power(input);break;case 'energy':res=E.energy({...input,unit:$('field-unit').value});break;case 'series':case 'parallel':res=E.association({...input,type:mode});break;case 'charge':res=E.charge(input);break;case 'resistivity':res=E.resistivity(input);break;}show('calc-result',res,'Calculadora: '+$('calc-mode').selectedOptions[0].textContent);}catch(e){error('calc-result',e.message);}});
document.querySelectorAll('[data-circuit]').forEach(btn=>btn.addEventListener('click',()=>{$('mixed-expression').value=btn.dataset.circuit;}));
$('solve-mixed').addEventListener('click',()=>{try{const exp=$('mixed-expression').value, v=E.number($('mixed-voltage').value);if($('mixed-voltage').value.trim()!==''&&v===null)throw new Error('Informe uma tensão válida.');show('mixed-result',E.mixed(exp,v),'Circuito: '+exp);}catch(e){error('mixed-result',e.message);}});
const formulas=[
 {ico:'Ω',title:'Lei de Ohm',equations:['V = R × I','I = V ÷ R','R = V ÷ I'],desc:'V: tensão (volts), I: corrente (ampères), R: resistência (ohms).'},
 {ico:'⚡',title:'Potência elétrica',equations:['P = V × I','P = R × I²','P = V² ÷ R'],desc:'P: potência (watts). As expressões equivalentes usam a Lei de Ohm.'},
 {ico:'◷',title:'Energia e consumo',equations:['E (Wh) = P (W) × t (h)','E (kWh) = Wh ÷ 1.000','Custo = kWh × tarifa'],desc:'1 Wh = 3.600 J. Converta o tempo para horas antes de calcular.'},
 {ico:'⤳',title:'Resistores em série',equations:['Rₑq = R₁ + R₂ + …','I₁ = I₂ = Iₜ'],desc:'A corrente é a mesma em todos os resistores. As quedas de tensão se somam.'},
 {ico:'⑂',title:'Resistores em paralelo',equations:['1/Rₑq = 1/R₁ + 1/R₂ + …','V₁ = V₂ = Vₜ'],desc:'A tensão é a mesma em cada ramo. As correntes dos ramos se somam.'},
 {ico:'Q',title:'Carga e corrente',equations:['Q = I × t','I = Q ÷ t'],desc:'Q em coulombs (C), I em ampères (A) e tempo em segundos (s).'},
 {ico:'ρ',title:'Resistividade',equations:['R = ρ × L ÷ A'],desc:'ρ em Ω·m, L em metros, A em m². Para mm², multiplique por 10⁻⁶.'},
 {ico:'↔',title:'Conversões úteis',equations:['1 kΩ = 1.000 Ω','1 kW = 1.000 W','1 mA = 0,001 A','1 kWh = 3,6 × 10⁶ J'],desc:'Considere as unidades antes de substituir valores nas fórmulas.'},
];
$('formulas-grid').innerHTML=formulas.map(x=>`<article class="formula-card"><div class="formula-icon">${escapeHtml(x.ico)}</div><h2>${escapeHtml(x.title)}</h2>${x.equations.map(eq=>`<div class="formula-eq">${escapeHtml(eq)}</div>`).join('')}<p>${escapeHtml(x.desc)}</p></article>`).join('');
let quiz=null,correct=0,attempts=0;
const rand=(min,max)=>Math.floor(Math.random()*(max-min+1))+min;
function createQuiz(topic){const t=topic==='mixed'?['ohm','power','energy','series','parallel'][rand(0,4)]:topic;let question,result,unit;switch(t){
case 'ohm':{const r=rand(2,20),i=rand(1,8);question=`Um resistor de ${r} Ω é atravessado por ${i} A. Qual é a tensão sobre ele, em volts?`;result=E.ohm({i,r});unit='V';break;}
case 'power':{const v=[12,24,110,127,220][rand(0,4)],i=rand(1,10);question=`Um equipamento funciona com ${v} V e consome ${i} A. Qual é a potência elétrica, em watts?`;result=E.power({v,i});unit='W';break;}
case 'energy':{const p=[100,200,500,1000,1500][rand(0,4)],t=rand(1,8);question=`Um aparelho de ${p} W fica ligado por ${t} horas. Qual é o consumo em kWh?`;result=E.energy({p,t});unit='kWh';break;}
case 'series':{const a=rand(2,20),b=rand(2,20),c=rand(1,4);question=`${c===1?'Dois':'Três'} resistores de ${a} Ω e ${b} Ω${c===1?'':` e ${c*10} Ω`} estão associados em série. Qual é a resistência equivalente em ohms?`;result=E.association({type:'series',values:c===1?[a,b]:[a,b,c*10]});unit='Ω';break;}
case 'parallel':{const r=rand(1,10)*4;question=`Dois resistores idênticos de ${r} Ω estão associados em paralelo. Qual é a resistência equivalente em ohms?`;result=E.association({type:'parallel',values:[r,r]});unit='Ω';break;}
}return {question,result,unit,answered:false};}
function newQuiz() {quiz=createQuiz($('quiz-topic').value);$('quiz-question').textContent=quiz.question;$('quiz-answer').value='';$('quiz-result').innerHTML='';$('quiz-answer').disabled=false;$('check-answer').disabled=false;}
function updateScore(){$('correct-count').textContent=correct;$('attempt-count').textContent=attempts;$('score-rate').textContent=attempts?Math.round(100*correct/attempts)+'%':'0%';}
$('next-question').addEventListener('click',newQuiz);$('quiz-topic').addEventListener('change',newQuiz);
$('check-answer').addEventListener('click',()=>{if(!quiz||quiz.answered)return;const raw=$('quiz-answer').value,answer=E.number(raw);if(answer===null){error('quiz-result','Digite um número para verificar a resposta.');return;}attempts++;const expected=quiz.result.value;const correctAnswer=Math.abs(answer-expected)<=Math.max(0.01,Math.abs(expected)*0.01);if(correctAnswer)correct++;quiz.answered=true;updateScore();show('quiz-result',{...quiz.result,title:correctAnswer?'✅ Resposta correta!':'📘 Confira a resolução'},'Treino: '+quiz.question,false);$('quiz-answer').disabled=true;$('check-answer').disabled=true;});
newQuiz();
})();
