/* Port Elétrica - Motor matemático offline, sem dependências. */
(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Electrical = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  const EPS = 1e-12;
  const pt = x => Number(x).toLocaleString('pt-BR', {maximumFractionDigits: 6});
  function number(value) {
    if (value === '' || value === null || value === undefined) return null;
    const n = Number(String(value).trim().replace(/\s/g, '').replace(',', '.'));
    return Number.isFinite(n) ? n : null;
  }
  function mustPositive(n, name, zeroAllowed = false) {
    if (n == null || !Number.isFinite(n) || (zeroAllowed ? n < 0 : n <= 0)) {
      throw new Error('Informe um valor válido ' + (zeroAllowed ? '(≥ 0)' : '(> 0)') + ' para ' + name + '.');
    }
  }
  const out = (title, value, unit, steps, extra = '') => ({ title, value, unit, answer: `${pt(value)} ${unit}`.trim(), steps, extra });
  function ohm({v=null, i=null, r=null}) {
    const known = [v,i,r].filter(x => x != null);
    if (known.length !== 2) throw new Error('Preencha exatamente dois campos: tensão, corrente ou resistência.');
    if (v != null) mustPositive(v, 'tensão');
    if (i != null) mustPositive(i, 'corrente');
    if (r != null) mustPositive(r, 'resistência');
    if (v == null) return out('Tensão elétrica', r*i, 'V', ['Lei de Ohm: V = R × I',`V = ${pt(r)} × ${pt(i)}`,`V = ${pt(r*i)} V`]);
    if (i == null) return out('Corrente elétrica', v/r, 'A', ['Lei de Ohm: I = V ÷ R',`I = ${pt(v)} ÷ ${pt(r)}`,`I = ${pt(v/r)} A`]);
    return out('Resistência elétrica', v/i, 'Ω', ['Lei de Ohm: R = V ÷ I',`R = ${pt(v)} ÷ ${pt(i)}`,`R = ${pt(v/i)} Ω`]);
  }
  function power({v=null, i=null, r=null, p=null, goal=null}) {
    const known = Object.entries({v,i,r,p}).filter(([k,x])=>x!=null);
    if (known.length !== 2) throw new Error('Preencha exatamente dois campos conhecidos.');
    for (const [k,x] of known) mustPositive(x, ({v:'tensão',i:'corrente',r:'resistência',p:'potência'})[k]);
    const key = known.map(([k])=>k).sort().join('');
    let V=v,I=i,R=r,P=p,steps=[];
    switch (key) {
      case 'iv': R=V/I; P=V*I; steps=[`R = V ÷ I = ${pt(V)} ÷ ${pt(I)} = ${pt(R)} Ω`,`P = V × I = ${pt(V)} × ${pt(I)} = ${pt(P)} W`]; break;
      case 'rv': I=V/R; P=V*I; steps=[`I = V ÷ R = ${pt(V)} ÷ ${pt(R)} = ${pt(I)} A`,`P = V² ÷ R = ${pt(V)}² ÷ ${pt(R)} = ${pt(P)} W`]; break;
      case 'ir': V=I*R; P=V*I; steps=[`V = I × R = ${pt(I)} × ${pt(R)} = ${pt(V)} V`,`P = R × I² = ${pt(R)} × ${pt(I)}² = ${pt(P)} W`]; break;
      case 'pv': I=P/V; R=V/I; steps=[`I = P ÷ V = ${pt(P)} ÷ ${pt(V)} = ${pt(I)} A`,`R = V² ÷ P = ${pt(R)} Ω`]; break;
      case 'ip': V=P/I; R=V/I; steps=[`V = P ÷ I = ${pt(P)} ÷ ${pt(I)} = ${pt(V)} V`,`R = P ÷ I² = ${pt(R)} Ω`]; break;
      case 'pr': I=Math.sqrt(P/R); V=I*R; steps=[`I = √(P ÷ R) = √(${pt(P)} ÷ ${pt(R)}) = ${pt(I)} A`,`V = √(P × R) = ${pt(V)} V`]; break;
      default: throw new Error('Combinação de dados não suportada.');
    }
    const targetKey = p == null ? 'p' : ((['v','i','r'].includes(goal) && ({v,i,r})[goal]==null) ? goal : (i==null ? 'i' : v==null ? 'v' : 'r'));
    const labels = {p:['Potência elétrica','W'],v:['Tensão elétrica','V'],i:['Corrente elétrica','A'],r:['Resistência elétrica','Ω']};
    return out(labels[targetKey][0], {v:V,i:I,r:R,p:P}[targetKey], labels[targetKey][1], ['Fórmulas: P = V × I = R × I² = V² ÷ R', ...steps], `V = ${pt(V)} V  •  I = ${pt(I)} A  •  R = ${pt(R)} Ω  •  P = ${pt(P)} W`);
  }
  function energy({p=null, t=null, unit='h', tariff=null}) {
    mustPositive(p, 'potência'); mustPositive(t,'tempo');
    if (tariff != null) mustPositive(tariff,'tarifa',true);
    const factor = unit === 'min' ? 1/60 : unit === 's' ? 1/3600 : 1;
    const hours = t*factor;
    const wh = p*hours, kwh=wh/1000, cost=tariff == null?null:kwh*tariff;
    return out('Energia consumida', kwh, 'kWh', [
      `Converta o tempo para horas: t = ${pt(hours)} h`,
      `E = P × t = ${pt(p)} W × ${pt(hours)} h = ${pt(wh)} Wh`,
      `E = ${pt(wh)} ÷ 1.000 = ${pt(kwh)} kWh`,
      ...(cost==null?[]:[`Custo = ${pt(kwh)} kWh × R$ ${pt(tariff)}/kWh = R$ ${pt(cost)}`])
    ], `Equivalente: ${pt(wh*3600)} J${cost==null?'':`  •  Custo: R$ ${pt(cost)}`}`);
  }
  function resistors(raw) {
    const arr = Array.isArray(raw) ? raw : String(raw||'').split(/[;\s]+/).filter(Boolean).map(number);
    if (arr.length < 2) throw new Error('Informe pelo menos duas resistências, separadas por ponto e vírgula ou espaço.');
    arr.forEach((r,idx)=>mustPositive(r,`R${idx+1}`));
    return arr;
  }
  function association({type, values, v=null}) {
    const arr = resistors(values);
    if (v != null) mustPositive(v,'tensão aplicada');
    const isSeries=type==='series';
    const req = isSeries ? arr.reduce((a,b)=>a+b,0) : 1/arr.reduce((a,b)=>a+1/b,0);
    const steps=[`Resistores: ${arr.map((x,j)=>`R${j+1} = ${pt(x)} Ω`).join(' · ')}`];
    if (isSeries) steps.push(`Rₑq = R₁ + R₂ + … = ${arr.map(pt).join(' + ')} = ${pt(req)} Ω`);
    else steps.push(`1/Rₑq = 1/R₁ + 1/R₂ + … = ${arr.map(x=>`1/${pt(x)}`).join(' + ')}`,`Rₑq = ${pt(req)} Ω`);
    if (v != null) {
      const totalI=v/req;
      steps.push(`I total = V ÷ Rₑq = ${pt(v)} ÷ ${pt(req)} = ${pt(totalI)} A`);
      arr.forEach((r,j)=>steps.push(isSeries ? `V${j+1} = I × R${j+1} = ${pt(totalI*r)} V` : `I${j+1} = V ÷ R${j+1} = ${pt(v/r)} A`));
      steps.push(`P total = V × I = ${pt(v*totalI)} W`);
    }
    return out(isSeries?'Associação em série':'Associação em paralelo',req,'Ω',steps,v==null?'':`I total = ${pt(v/req)} A  •  P total = ${pt(v*v/req)} W`);
  }
  function charge({q=null, i=null, t=null}) {
    if ([q,i,t].filter(x=>x!=null).length!==2) throw new Error('Preencha exatamente dois campos: carga, corrente e tempo.');
    if(q!=null) mustPositive(q,'carga',true);
    if(i!=null) mustPositive(i,'corrente',true);
    if(t!=null) mustPositive(t,'tempo');
    if (q==null) return out('Carga elétrica',i*t,'C',[`Q = I × t = ${pt(i)} × ${pt(t)} = ${pt(i*t)} C`]);
    if (i==null) return out('Corrente elétrica',q/t,'A',[`I = Q ÷ t = ${pt(q)} ÷ ${pt(t)} = ${pt(q/t)} A`]);
    if (i===0) throw new Error('A corrente não pode ser zero para calcular o tempo.');
    return out('Tempo',q/i,'s',[`t = Q ÷ I = ${pt(q)} ÷ ${pt(i)} = ${pt(q/i)} s`]);
  }
  function resistivity({rho=null,l=null,a=null}) {
    mustPositive(rho,'resistividade'); mustPositive(l,'comprimento'); mustPositive(a,'área');
    const area=a*1e-6, r=rho*l/area;
    return out('Resistência do condutor',r,'Ω',[`Área: ${pt(a)} mm² = ${a.toExponential(3)} × 10⁻⁶ m²`, 'Fórmula: R = ρ × L ÷ A', `R = ${rho} × ${pt(l)} ÷ ${area.toExponential(3)} = ${pt(r)} Ω`]);
  }
  function mixed(expression, voltage=null) {
    if (voltage != null) mustPositive(voltage,'tensão aplicada');
    const s=String(expression||'').replace(/\s+/g,'').replace(/,/g,'.');
    if (s.length>250) throw new Error('Expressão muito longa.');
    let pos=0, count=0; const steps=[];
    function parse(depth=0) {
      if(depth>16 || ++count>60) throw new Error('Circuito muito profundo.');
      const ch=s[pos]?.toUpperCase();
      if(ch==='S'||ch==='P'){
        pos++; if(s[pos++]!=='(') throw new Error('Use S(...) ou P(...).');
        const children=[]; children.push(parse(depth+1));
        while(s[pos]===';'){pos++; children.push(parse(depth+1));}
        if(children.length<2) throw new Error('Cada associação precisa de pelo menos dois componentes. Use ; como separador.');
        if(s[pos++]!==')') throw new Error('Feche os parênteses da associação.');
        const r=ch==='S'?children.reduce((a,b)=>a+b,0):1/children.reduce((a,b)=>a+1/b,0);
        steps.push(`${ch==='S'?'Série':'Paralelo'} (${children.map(pt).join('; ')}): Rₑq = ${pt(r)} Ω`);
        return r;
      }
      const m=s.slice(pos).match(/^(?:\d+\.?\d*|\.\d+)(?:e[+-]?\d+)?/i);
      if(!m) throw new Error('Use somente resistências positivas e S(...) ou P(...).');
      pos+=m[0].length; const r=Number(m[0]); mustPositive(r,'resistência'); return r;
    }
    // Separador obrigatório para evitar ambiguidade entre decimal e componentes.
    const req=parse(); if(pos!==s.length) throw new Error('Expressão inválida perto de: '+s.slice(pos,Math.min(pos+12,s.length)));
    steps.push(`Resistência equivalente total: ${pt(req)} Ω`);
    if(voltage!=null) steps.push(`I total = ${pt(voltage)} ÷ ${pt(req)} = ${pt(voltage/req)} A`,`P total = V² ÷ Rₑq = ${pt(voltage*voltage/req)} W`);
    return out('Circuito misto',req,'Ω',steps,voltage==null?'':`I total = ${pt(voltage/req)} A`);
  }
  function tokenizeStatement(input) {
    const text=String(input||'');
    const clean=text.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');
    const all=[];
    const pattern=/(-?\d+(?:[.,]\d+)?)\s*(kω|mω|ω|kohms?|ohms?|kwh|kw|mw|wh|watts?|w|ma|amp[eè]res?|amps?|a|volts?|v|horas?|hrs?|h|minutos?|mins?|min|segundos?|segs?|s)(?=$|[\s,.!?;:)])/giu;
    for(const match of text.matchAll(pattern)) {
      const value=number(match[1]); let unit=match[2].toLowerCase();
      let kind=null, val=value; const originalUnit=match[2];
      if(/^(kω|kohm)/.test(unit)){kind='r';val*=1000;}
      else if(/^mω/.test(unit)){kind='r';val*=originalUnit[0]==='M'?1e6:1e-3;}
      else if(/^(ω|ohm)/.test(unit)){kind='r';}
      else if(unit==='kw'){kind='p';val*=1000;}
      else if(unit==='mw'){kind='p';val*=1e6;}
      else if(/^w|watt/.test(unit)){kind='p';}
      else if(unit==='ma'){kind='i';val/=1000;}
      else if(/^a|amp/.test(unit)){kind='i';}
      else if(/^v|volt/.test(unit)){kind='v';}
      else if(/^(hora|hr|h$)/.test(unit)){kind='t';val=val;}
      else if(/^(minut|min)/.test(unit)){kind='t';val/=60;}
      else if(/^(seg|s$)/.test(unit)){kind='t';val/=3600;}
      if(kind) all.push({kind,value:val,raw:match[0]});
    }
    return { clean, all };
  }
  function solveStatement(text) {
    if (!String(text||'').trim()) throw new Error('Digite o enunciado da questão.');
    const {clean,all}=tokenizeStatement(text);
    const group = kind => all.filter(x=>x.kind===kind).map(x=>x.value);
    const v=group('v')[0]??null,i=group('i')[0]??null,r=group('r')[0]??null,p=group('p')[0]??null,t=group('t')[0]??null;
    const isEnergy=/(consumo|energia|conta de luz|kwh|gast[ao]|durante|por\s+\d+\s*(?:hora|minut|segund))/.test(clean) && p!=null && t!=null;
    const isParallel=/(paralel)/.test(clean),isSeries=/(serie)/.test(clean);
    try {
      if(isEnergy) return {...energy({p,t,unit:'h'}), detection:all};
      if((isParallel||isSeries) && group('r').length>=2) return {...association({type:isParallel?'parallel':'series',values:group('r'),v}), detection:all};
      const vals={v,i,r,p}, known=Object.entries(vals).filter(([k,x])=>x!=null);
      if(known.length >= 2) {
        // Sem indicação de potência, favorece Ohm quando V, I, R são os únicos dados.
        const elec=Object.entries({v,i,r}).filter(([k,x])=>x!=null);
        if(p==null && elec.length===2 && !/(potencia|watts?|consumo)/.test(clean)) return {...ohm({v,i,r}), detection:all};
        if(known.length===2) { const goal=/(corrente)/.test(clean)?'i':/(resistencia)/.test(clean)?'r':/(tensao|voltagem)/.test(clean)?'v':'p'; return {...power({...vals,goal}), detection:all}; }
      }
    } catch(e) { throw new Error(e.message); }
    const detected = all.map(x=>x.raw).join(', ');
    throw new Error('Não foi possível determinar uma solução única para este enunciado.'+(detected?` Valores identificados: ${detected}.`:'')+' Use a calculadora guiada ou informe mais dados.');
  }
  return { number, pt, ohm, power, energy, association, charge, resistivity, mixed, tokenizeStatement, solveStatement };
});
