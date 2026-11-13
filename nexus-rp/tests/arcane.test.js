"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),fs=require("node:fs");
const {Readable}=require("node:stream");
const {createClientRelay}=require("../client_relay.js");
function harness(){
 const relay=createClientRelay({
  send:(res,status,payload)=>{res.status=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="OWNER",
  limit:()=>true
 });
 async function call(method,path,headers={},data){
  const req=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
  req.headers=headers;req.method=method;const res={status:0,payload:null};
  const parsed=new URL(path,"https://test.invalid");
  await relay.handle(req,res,parsed.pathname,parsed);
  return res;
 }
 return call;
}
test("Arcane routes require owner for settings and paired client for readings",async()=>{
 const call=harness();
 const owner={"x-nexus-key":"OWNER"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{
  code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 assert.equal((await call("GET","/api/client/arcane")).status,401);
 assert.equal((await call("POST","/api/client/arcane/settings",client,
  {enabled:true,reactions:false})).status,401);
 assert.equal((await call("POST","/api/client/arcane/state",owner,{
  enabled:true,reactions:false,present:false,available:false,events:[]})).status,401);
 assert.equal((await call("POST","/api/client/arcane/settings",owner,{
  enabled:false,reactions:true})).status,422);
 const config=await call("POST","/api/client/arcane/settings",owner,{
  enabled:true,reactions:true});
 assert.equal(config.status,200);
 const job=(await call("GET","/api/client/poll",client)).payload.jobs[0];
 assert.equal(job.type,"arcane_settings");
 assert.deepEqual(job.payload,{enabled:true,reactions:true,scanNearby:false});
});
test("Arcane readings use synced client values, distinguish RP arm, and sanitize events",async()=>{
 const call=harness();
 const owner={"x-nexus-key":"OWNER"};
 const code=(await call("POST","/api/client/pair/new",owner,{})).payload.code;
 const token=(await call("POST","/api/client/pair/exchange",{},{code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const state={enabled:true,reactions:true,present:true,available:true,
  mana:43,maxMana:110,manaPercent:39.09,casting:true,
  castProgress:0.55,spellId:"irons_spellbooks:healing_circle",
  spellName:"Círculo de cura",school:"irons_spellbooks:holy",spellLevel:3,
  spellPower:1.25,holyPower:1.40,armAvailable:true,armIntegrity:72,armEnergy:88,
  totalCasts:17,totalNearby:2,scanNearby:true,
  nearby:[{player:"FriendA",spellId:"irons_spellbooks:fireball",
    spellName:"Fireball",school:"irons_spellbooks:fire",level:2,distance:9}],
  lastReadAt:1794566000000,lastError:"",
  events:[{at:1794566000000,type:"cast_start",level:3,
    spellId:"irons_spellbooks:healing_circle",school:"irons_spellbooks:holy",
    message:"Iniciada <script>magia</script>"}]};
 assert.equal((await call("POST","/api/client/arcane/state",client,state)).status,200);
 const view=await call("GET","/api/client/arcane",owner);
 assert.equal(view.status,200);assert.equal(view.payload.state.holyPower,1.4);
 assert.equal(view.payload.state.mana,43);
 assert.equal(view.payload.state.armIntegrity,72);
 assert.equal(view.payload.state.events.length,1);
 assert.equal(view.payload.state.nearby[0].player,"FriendA");
 assert.equal(view.payload.state.nearby[0].distance,9);
 assert.equal(view.payload.state.events[0].message,"Iniciada <script>magia</script>");
 assert.equal(JSON.stringify(view.payload).includes("event:unknown"),false);
 assert.ok(view.payload.state.syncedAt);
 const altered=await call("POST","/api/client/arcane/state",client,{
  ...state,events:[{at:1,type:"unknown",message:"bad"}]});
 assert.equal(altered.status,422);
 const disabled=await call("POST","/api/client/arcane/state",client,{
  ...state,enabled:false,present:false,available:false,armAvailable:false,events:[]});
 assert.equal(disabled.status,200);
 const off=await call("GET","/api/client/arcane",owner);
 assert.equal(off.payload.state.mana,null);
 assert.equal(off.payload.state.armIntegrity,null);
 assert.equal(off.payload.state.enabled,false);
});
test("Arcane UI labels distinguish real mana from narrative plates",()=>{
 const ui=fs.readFileSync(require("node:path").join(__dirname,"../public/arcane.js"),"utf8");
 for(const fragment of ["Núcleo Arcano","mana (atributo do jogo)","DIAGNÓSTICO RP",
   "holyPower","state().events","/api/client/arcane","/arcane/settings"]){
  assert.ok(ui.includes(fragment)||fragment==="state().events"&&ui.includes("state()?.events"),
    "Missing UI fragment "+fragment);
 }
 assert.ok(!ui.includes("sendCommand("),"UI must not send server commands");
});
