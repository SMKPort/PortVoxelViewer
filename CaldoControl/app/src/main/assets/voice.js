/* CaldoControl Voice Parser v2.7.
 * Pure, deterministic Portuguese pH/Brix extractor. Never persists or auto-applies measurements.
 * Stage and measurement name MUST both be present. Uncertain/conflicting values are rejected.
 */
(function(root,factory){
  const api=factory();
  if(typeof module==='object'&&module.exports)module.exports=api;
  if(root)root.CaldoVoiceParser=api;
})(typeof window!=='undefined'?window:(typeof globalThis!=='undefined'?globalThis:null),function(){
  'use strict';
  const units={
    zero:0,um:1,uma:1,dois:2,duas:2,tres:3,quatro:4,cinco:5,seis:6,sete:7,
    oito:8,nove:9,dez:10,onze:11,doze:12,treze:13,quatorze:14,catorze:14,
    quinze:15,dezesseis:16,dezasseis:16,dezessete:17,dezoito:18,dezenove:19,
    vinte:20,trinta:30,quarenta:40,cinquenta:50,sessenta:60,setenta:70,
    oitenta:80,noventa:90,cem:100,cento:100
  };
  const fields={
    caleado:{ph:'phCalado',brix:'brixCalado'},
    clarificado:{ph:'phDec',brix:'brixClarificado'},
    xarope:{ph:'phXarope',brix:'brixXarope'},
    torre:{ph:'phTorre'}
  };
  const labels={
    phCalado:'pH caleado/dosado',brixCalado:'Brix caleado/dosado',
    phDec:'pH clarificado',brixClarificado:'Brix clarificado',
    phXarope:'pH do xarope',brixXarope:'Brix do xarope',phTorre:'pH da torre'
  };
  const filler=new Set(['do','da','de','no','na','ao','a','o','valor','medida',
    'medicao','esta','estava','igual','em','foi','e','caldo','com','aqui','registrado']);
  function normalize(text){
    return String(text||'').slice(0,1200).toLocaleLowerCase('pt-BR')
      .normalize('NFD').replace(/[\u0300-\u036f]/g,'')
      .replace(/\bpe[\s-]+aga\b/g,' ph ')
      .replace(/\bp[\s-]+h\b/g,' ph ')
      .replace(/\b(brics|briks|bricks|brits|brichs|briqs|bricx)\b/g,' brix ');
  }
  function tokenize(text){return normalize(text).match(/\d+(?:[.,]\d+)?|[a-z]+/g)||[];}
  function stageAt(token){
    if(/^(caleado|caleada|caleados|calado|calada|dosado|dosada)$/.test(token))return 'caleado';
    if(/^(clarificado|clarificada|clarificados|decantado|decantada)$/.test(token))return 'clarificado';
    if(/^(xarope|xaropes)$/.test(token))return 'xarope';
    if(/^(torre|resfriamento)$/.test(token))return 'torre';
    return '';
  }
  function metricAt(token){
    return token==='ph'?'ph':token==='brix'?'brix':'';
  }
  function numericWord(token){return Object.prototype.hasOwnProperty.call(units,token);}
  function parseInteger(tokens,index){
    const first=tokens[index];
    if(!first)return null;
    if(/^\d{1,3}$/.test(first))return {value:Number(first),end:index+1,raw:first};
    if(!numericWord(first))return null;
    let sum=0,j=index,seen=0;
    while(j<tokens.length&&j-index<10){
      const t=tokens[j];
      if(t==='e'){
        if(!seen||!numericWord(tokens[j+1]||''))break;
        // "sete e cinco" nao define decimal nem inteiro de forma inequívoca.
        if(sum<10)return null;
        j++;continue;
      }
      if(!numericWord(t))break;
      const val=units[t];
      if(seen && sum<10 && val<10)return null;
      if(seen && val>=100)break;
      if(val>=100&&sum>=100)break;
      sum+=val;j++;seen++;
    }
    return seen?{value:sum,end:j,raw:tokens.slice(index,j).join(' ')}:null;
  }
  function decimalDigits(tokens,index){
    const t=tokens[index];
    if(!t)return null;
    if(/^\d{1,3}$/.test(t))return {digits:t,end:index+1};
    if(!numericWord(t))return null;
    const digits=[];
    let j=index;
    while(j<tokens.length && digits.length<3 && numericWord(tokens[j]) && units[tokens[j]]<=9){
      digits.push(String(units[tokens[j]]));j++;
    }
    if(digits.length)return {digits:digits.join(''),end:j};
    const whole=parseInteger(tokens,index);
    if(whole&&whole.value>=0&&whole.value<100)return {digits:String(whole.value),end:whole.end};
    return null;
  }
  function parseNumber(tokens,index){
    const literal=tokens[index];
    if(!literal)return null;
    if(/^\d{1,3}[.,]\d{1,3}$/.test(literal))
      return {value:Number(literal.replace(',','.')),end:index+1};
    const whole=parseInteger(tokens,index);
    if(!whole||whole.value>1000)return null;
    const marker=tokens[whole.end];
    if(marker==='virgula'||marker==='ponto'){
      const part=decimalDigits(tokens,whole.end+1);
      if(!part)return null;
      return {value:Number(String(whole.value)+'.'+part.digits),end:part.end};
    }
    return {value:whole.value,end:whole.end};
  }
  function parse(text){
    const tokens=tokenize(text),values=new Map(),conflicts=new Set(),warnings=[];
    let stage='',matches=0;
    for(let i=0;i<tokens.length;i++){
      const token=tokens[i],newStage=stageAt(token);
      if(newStage){stage=newStage;continue;}
      const metric=metricAt(token);
      if(!metric)continue;
      matches++;
      let destStage=stage,j=i+1,skips=0;
      while(j<tokens.length && skips<7){
        if(stageAt(tokens[j])){destStage=stageAt(tokens[j]);stage=destStage;j++;skips++;continue;}
        if(filler.has(tokens[j])){j++;skips++;continue;}
        break;
      }
      const number=parseNumber(tokens,j);
      if(!number){
        warnings.push('Não entendi o valor de '+(metric==='ph'?'pH':'Brix')+'. Confira a frase.');
        continue;
      }
      if(!destStage){
        warnings.push('Etapa não identificada para '+(metric==='ph'?'pH':'Brix')+' '+number.value+'.');
        i=number.end-1;continue;
      }
      const field=fields[destStage]&&fields[destStage][metric];
      if(!field){
        warnings.push('Não existe campo de '+(metric==='ph'?'pH':'Brix')+' para '+destStage+'.');
        i=number.end-1;continue;
      }
      const maximum=metric==='ph'?14:100;
      if(!Number.isFinite(number.value)||number.value<0||number.value>maximum){
        warnings.push(labels[field]+': valor '+number.value+' fora da faixa permitida (0 a '+maximum+').');
        i=number.end-1;continue;
      }
      if(values.has(field)&&Math.abs(values.get(field)-number.value)>0.000001){
        conflicts.add(field);
        warnings.push('Valores diferentes encontrados para '+labels[field]+'. Fale novamente ou corrija manualmente.');
      }else if(!values.has(field))values.set(field,number.value);
      i=number.end-1;
    }
    for(const field of conflicts)values.delete(field);
    if(!matches)warnings.push('Diga a etapa (caleado, clarificado, xarope ou torre) e pH ou Brix.');
    const pairs=[...values].map(([field,value])=>({field,label:labels[field],value}));
    return {pairs,warnings,recognized:matches,normalized:normalize(text)};
  }
  return {parse,normalize,tokenize};
});
