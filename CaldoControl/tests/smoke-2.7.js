const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const src=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/index.html'),'utf8');
const html=src.split('<script>')[0];
const required=['phCalado','phDec','phXarope','phTorre','brixCalado','brixClarificado','brixXarope',
 'aovivo','fechamentos','diagnostico','importacao','auditoria','batchSave','exportExcel','activeOperator','closureCards','liveTrend'];
for(const id of required)assert(html.includes('id="'+id+'"'),'Campo ausente: '+id);
const removed=['temperature','tempInput','heaterId','steamPressure','baume','decanterId','decanterStatus',
 'sludgeLevelPct','sludgeFlow','turbidity','residenceTime','juiceFlow','limeFlow','limeUnit','polymerFlow',
 'decantacao','aquecimento','reportDecanter','gatewayLimeUnit','ratioResult','percentResult','tempRiseResult','procTemp','quickUnits'];
for(const id of removed)assert(!html.includes('id="'+id+'"'),'Campo removido reapareceu: '+id);
for(const marker of ['function appendAudit(', 'function createShiftDrafts(', 'function renderLive(', 'function renderTrends(', 'function xlsxWorkbook(', 'window.CaldoControlTableOCR','ph_xarope'])
 assert(src.includes(marker),'Funcionalidade ausente '+marker);
const fields=src.match(/const FIELDS = (\[[^\n;]+\]);/);
const nums=src.match(/const NUMS = (\[[^\n;]+\]);/);
const metricLine=src.match(/const METRICS = (\[[^\n;]+\]);/);
const sensors=src.match(/const SENSOR_FIELDS=(\[[^\n;]+\]);/);
assert(fields&&nums&&metricLine&&sensors,'Esquemas de pH/Brix ausentes');
const forbidden=/temperature|tempInput|heaterId|steamPressure|baume|decanterId|decanterStatus|sludge|turbidity|residenceTime|juiceFlow|limeFlow|limeUnit|polymerFlow/;
for(const [name,match] of [['FIELDS',fields],['NUMS',nums],['METRICS',metricLine],['SENSOR_FIELDS',sensors]])
 assert(!forbidden.test(match[1]),'Parâmetro removido em '+name);
assert(src.includes("version:6,createdAt:"),'Backup versão 5 ausente');
assert(src.includes('legacyNumeric')&&src.includes('legacyText'),'Migração de dados legados ausente');
// Todos os IDs estáticos usados pela UI devem existir e ser únicos.
const ids=[...html.matchAll(/\bid="([^"]+)"/g)].map(m=>m[1]);
assert(ids.length===new Set(ids).size,'IDs duplicados');
const missing=[...src.matchAll(/\$\('([^']+)'\)/g)].map(m=>m[1]).filter(id=>!ids.includes(id));
assert(!missing.length,'Referências órfãs: '+[...new Set(missing)].join(','));
const start=src.indexOf('const utf8=new TextEncoder();');
const end=src.indexOf("  $('exportExcel').addEventListener('click'",start);
assert(start>0&&end>start,'Exportador XLSX ausente');
const sandbox={TextEncoder,Uint8Array,Uint32Array,DataView,ArrayBuffer,Blob,URL,console,document:{},window:{}};
vm.runInNewContext(src.slice(start,end)+'\n globalThis.smoke=xlsxWorkbook([{name:"pH e Brix",rows:[["pH xarope","Brix xarope"],[6.7,62.1]]}]);',sandbox,{timeout:1500});
const zip=sandbox.smoke;
assert(zip.length>1000&&zip[0]===0x50&&zip[1]===0x4b,'ZIP XLSX inválido');
fs.writeFileSync('/tmp/CaldoControl-2.7-smoke.xlsx',zip);
console.log('Validação CaldoControl 2.7 concluída; XLSX:',zip.length,'bytes');


// Hidratação manual: dois campos independentes, exportáveis e fora das médias do processo.
for(const id of ['hydrationLime','hydrationPolymer'])
  assert(html.includes('id="'+id+'"'),'Campo de hidratação ausente no formulário: '+id);
const fieldLiteral=fields[1],numericLiteral=nums[1],metricLiteral=metricLine[1],sensorLiteral=sensors[1];
for(const key of ['hydrationLime','hydrationPolymer']){
  assert(fieldLiteral.includes("'"+key+"'"),'Hidratação fora do armazenamento: '+key);
  assert(!numericLiteral.includes("'"+key+"'"),'Hidratação foi marcada incorretamente como numérica: '+key);
  assert(!metricLiteral.includes("'"+key+"'"),'Hidratação entrou indevidamente nas médias: '+key);
  assert(!sensorLiteral.includes("'"+key+"'"),'Hidratação entrou indevidamente na telemetria: '+key);
}
const validStart=src.indexOf('function validForm(r){');
const validEnd=src.indexOf('function renderCalc(){',validStart);
assert(validStart>0&&validEnd>validStart,'Validação do formulário não encontrada');
const ctx={NUMS:['phCalado','phDec','phTorre','phXarope','brixCalado','brixClarificado','brixXarope']};
vm.runInNewContext(src.slice(validStart,validEnd)+'\n globalThis.validate=validForm;',ctx);
const manualOnly={
 dateTime:'2026-11-01T14:00:00',process:'ambos',source:'manual',
 phCalado:null,phDec:null,phTorre:null,phXarope:null,brixCalado:null,brixClarificado:null,brixXarope:null,
 hydrationLime:'Hidratacao conforme registro do operador',hydrationPolymer:'',notes:'',photoData:''
};
assert.equal(ctx.validate(manualOnly),null,'Registro somente de hidratação deveria ser válido');
assert(ctx.validate({...manualOnly,hydrationLime:'',hydrationPolymer:''}),'Registro vazio não pode ser salvo');
assert(ctx.validate({...manualOnly,hydrationLime:'x'.repeat(1201)}),'Campo de 1201 caracteres não pode ser aceito');
for(const marker of ["'hydrationLime','hydrationPolymer','notes'", "'hydrationLime','hydrationPolymer'].map", "version:6,createdAt:"])
  assert(src.includes(marker),'Exportação, auditoria ou backup sem hidratação: '+marker);
console.log('Validação de hidratação manual: OK');


// Voz: frases reais, variantes numéricas, ambiguidades e proteção contra gravação automática.
const voice=require('../app/src/main/assets/voice.js');
function checkPhrase(raw,expected){
  const result=voice.parse(raw),actual=Object.fromEntries(result.pairs.map(x=>[x.field,x.value]));
  assert.deepEqual(actual,expected,'Reconhecimento errado: '+raw+' / avisos: '+result.warnings.join('|'));
}
checkPhrase('Caleado pH sete vírgula dois e Brix quinze',{phCalado:7.2,brixCalado:15});
checkPhrase('Clarificado PH 6,8 e BRIX quatorze vírgula cinco',{phDec:6.8,brixClarificado:14.5});
checkPhrase('Xarope Brix sessenta e dois e pH seis',{brixXarope:62,phXarope:6});
checkPhrase('Torre de resfriamento pH sete vírgula quatro',{phTorre:7.4});
checkPhrase('pH do caleado 7.1 e Brix do caleado 15',{phCalado:7.1,brixCalado:15});
checkPhrase('Dosado pe aga sete ponto dois e brics quinze',{phCalado:7.2,brixCalado:15});
checkPhrase('Caleado pH zero vírgula zero cinco',{phCalado:0.05});
checkPhrase('Caleado pH 7,2 e Brix 15. Clarificado pH 6,8 e Brix 14,5. Xarope Brix 62 e pH 6. Torre pH 7,4',
  {phCalado:7.2,brixCalado:15,phDec:6.8,brixClarificado:14.5,brixXarope:62,phXarope:6,phTorre:7.4});
checkPhrase('pH sete e Brix quinze',{});
checkPhrase('Torre Brix quinze',{});
checkPhrase('Caleado pH quinze',{});
checkPhrase('Xarope Brix cento e cinco',{});
checkPhrase('Caleado pH sete e cinco',{});
checkPhrase('Caleado pH 7 e pH 8',{});
assert(src.includes('window.CaldoVoice={'),'Ponte para Android voz ausente');
assert(src.includes("$('applyVoice').addEventListener"),'Revisão de voz ausente');
for(const id of ['voiceTranscript','startVoice','parseVoice','applyVoice','voicePreview','voiceStatus'])
  assert(html.includes('id="'+id+'"'),'Interface voz faltante: '+id);
for(const tab of ['id="sensores"','id="aovivo"','data-tab="sensores"','data-tab="aovivo"'])
  assert(!html.includes(tab),'Interface de gateway/telemetria não foi removida: '+tab);
for(const codeMarker of ['readGatewayJson','CaldoGatewayNative','new WebSocket('])
  assert(!src.includes(codeMarker),'Conector gateway ainda presente no JS: '+codeMarker);
console.log('Ditado pt-BR, campos, segurança e remoção do gateway: OK');
