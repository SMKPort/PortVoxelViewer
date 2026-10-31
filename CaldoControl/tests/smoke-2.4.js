const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const src=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/index.html'),'utf8');
for(const id of ['aovivo','fechamentos','diagnostico','importacao','auditoria','batchSave','exportExcel','activeOperator','closureCards','liveTrend','phXarope'])
  assert(src.includes('id="'+id+'"'),'Missing UI field '+id);
for(const marker of ['function appendAudit(', 'function createShiftDrafts(', 'function renderLive(', 'function renderTrends(', 'function xlsxWorkbook(', 'window.CaldoControlTableOCR','ph_xarope'])
  assert(src.includes(marker),'Missing feature '+marker);
assert(src.includes("const NUMS = ['phCalado','phDec','phTorre','phXarope'"), 'phXarope must be numeric');
// Exercise the *actual* workbook generator from the embedded app, not a duplicated implementation.
const start=src.indexOf('const utf8=new TextEncoder();');
const end=src.indexOf("  $('exportExcel').addEventListener('click'",start);
assert(start>0&&end>start,'Embedded XLSX writer not found');
const code=src.slice(start,end);
const sandbox={TextEncoder,Uint8Array,Uint32Array,DataView,ArrayBuffer,Blob,URL,console,document:{},window:{}};
vm.runInNewContext(code+'\n globalThis.smoke=xlsxWorkbook([{name:"Teste",rows:[["pH xarope","°Brix"],[6.7,61.2],["Última amostra",null]]}]);',sandbox,{timeout:1500});
const zip=sandbox.smoke;
assert(zip.length>1000&&zip[0]===0x50&&zip[1]===0x4b,'ZIP header missing');
fs.writeFileSync('/tmp/CaldoControl-2.4-smoke.xlsx',zip);
console.log('Smoke XLSX valid layout:',zip.length,'bytes');
