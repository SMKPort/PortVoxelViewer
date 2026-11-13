"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {synthesizeSpeech}=require("../speech_synthesis.js");
const {createClientRelay}=require("../client_relay.js");
test("Gemini TTS creates mono PCM without writing files or returning API keys",async()=>{
 let request;
 const sample=Buffer.alloc(24000*2);
 const result=await synthesizeSpeech({text:"Port, integridade estável.",voice:"Kore",stage:3},{
  key:"local-test-secret-key",model:"gemini-2.5-flash-preview-tts",
  fetcher:async(url,options)=>{
   request={url,options};
   return {ok:true,json:async()=>({candidates:[{content:{parts:[
    {inlineData:{mimeType:"audio/L16;rate=24000",data:sample.toString("base64")}}
   ]}}]})};
  }
 });
 assert.equal(result.sampleRate,24000);
 assert.equal(result.voice,"Kore");
 assert.equal(result.stage,"IDENTIDADE");
 assert.equal(Buffer.from(result.pcm,"base64").length,sample.length);
 const params=JSON.parse(request.options.body);
 assert.deepEqual(params.generationConfig.responseModalities,["AUDIO"]);
 assert.equal(params.generationConfig.speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName,"Kore");
 assert.equal(request.options.headers["x-goog-api-key"],"local-test-secret-key");
 assert.doesNotMatch(JSON.stringify(result),/local-test-secret-key/);
});
test("TTS invalid settings and missing key are rejected",async()=>{
 await assert.rejects(()=>synthesizeSpeech({text:"Hello",voice:"Kore"},{
  key:"",fetcher:async()=>{throw Error("must not call")}
 }),e=>e.publicStatus===503);
 await assert.rejects(()=>synthesizeSpeech({text:"Hello",voice:"Invalid"},{
  key:"local-test-secret-key"
 }),e=>e.publicStatus===422);
});
test("TTS route is client-authenticated and requires explicit local voice opt-in",async()=>{
 const requests=[];
 const relay=createClientRelay({send:(res,status,payload)=>{res.code=status;res.payload=payload;},
  verifyUser:req=>req.headers["x-nexus-key"]==="panel-key",limit:()=>true,
  synthesize:async data=>{requests.push(data);
    return {pcm:Buffer.alloc(512).toString("base64"),sampleRate:24000,voice:data.voice};}
 });
 async function call(method,path,headers={},data){
  const req=Readable.from(data===undefined?[]:[Buffer.from(JSON.stringify(data))]);
  req.method=method;req.headers=headers;
  const res={code:0,payload:null};
  await relay.handle(req,res,path,new URL(path,"https://local.invalid"));
  return res;
 }
 const panel={"x-nexus-key":"panel-key"};
 const pair=await call("POST","/api/client/pair/new",panel,{});
 const token=(await call("POST","/api/client/pair/exchange",{},{
  code:pair.payload.code,player:"Port"})).payload.token;
 const client={"x-nexus-client":token};
 const data={text:"Olá Port.",voice:"Charon",stage:0};
 assert.equal((await call("POST","/api/client/tts",panel,data)).code,401);
 assert.equal((await call("POST","/api/client/tts",client,data)).code,403);
 await call("POST","/api/client/hearing/state",client,{
  ready:true,listening:false,memory:false,answer:false,allowedPlayers:[],
  speaker:{enabled:true,voice:"auto"}
 });
 const output=await call("POST","/api/client/tts",client,data);
 assert.equal(output.code,200);
 assert.equal(requests.length,1);
 assert.equal((await call("POST","/api/client/tts",client,data)).code,429);
});
