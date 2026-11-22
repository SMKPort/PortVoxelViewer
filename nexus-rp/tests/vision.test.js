"use strict";
const test=require("node:test");
const assert=require("node:assert/strict");
const {Readable}=require("node:stream");
const {MODEL,validate,analyzeVision}=require("../vision_analysis.js");
const {createClientRelay}=require("../client_relay.js");
const screenshot="/9j/"+("A".repeat(120));

function testServer(visionReply=async()=>({text:"Uma estrutura arcana visível.",model:MODEL})){
  const relay=createClientRelay({
    send:(res,status,payload)=>{res.status=status;res.payload=payload;res.end();},
    verifyUser:req=>req.headers["x-nexus-key"]==="owner-access",
    limit:()=>true,visionReply
  });
  async function call(method,route,headers={},body=null){
    const req=Readable.from(body===null?[]:[Buffer.from(JSON.stringify(body))]);
    req.method=method;req.headers=headers;
    const res={status:null,payload:null,end(){}};
    await relay.handle(req,res,new URL(route,"http://test.local").pathname,
      new URL(route,"http://test.local"));
    return res;
  }
  return call;
}
test("free model is text+image understanding, never paid image generation",async()=>{
  assert.equal(MODEL,"gemini-2.5-flash-lite");
  assert.throws(()=>validate("analisar", "não é base64"));
  assert.throws(()=>validate("analisar", "A".repeat(540001)));
  assert.deepEqual(validate("a cena",screenshot),{question:"a cena",image:screenshot});
  await assert.rejects(()=>analyzeVision({question:"a cena",image:screenshot},{key:""}),
    /NEXUS_VISION_FREE_API_KEY/);
  let supplied;
  const out=await analyzeVision({question:"Quais blocos estão visíveis?",image:screenshot},{
    key:"free-tier-key-only-for-tests",
    fetcher:async(url,options)=>{
      supplied={url,options};
      return {ok:true,json:async()=>({candidates:[{content:{parts:[{
        text:"Vejo uma construção cúbica."
      }]}}]})};
    }
  });
  assert.equal(out.model,MODEL);
  const payload=JSON.parse(supplied.options.body);
  assert.match(supplied.url,/gemini-2\.5-flash-lite:generateContent$/);
  assert.equal(payload.contents[0].parts[1].inline_data.mime_type,"image/jpeg");
  assert.equal(payload.contents[0].parts[1].inline_data.data,screenshot);
  assert.ok(!JSON.stringify(payload).includes("tools"));
});

test("vision rejects anonymous traffic and requires separate screenshot consent",async()=>{
  const call=testServer(),owner={"x-nexus-key":"owner-access"};
  const pair=await call("POST","/api/client/pair/new",owner,{});
  const exchange=await call("POST","/api/client/pair/exchange",{},{
    code:pair.payload.code,player:"Port"
  });
  const client={"x-nexus-client":exchange.payload.token};
  assert.equal((await call("POST","/api/client/vision/analyze",{},{
    question:"Analise",image:screenshot,consent:true
  })).status,401);
  assert.equal((await call("POST","/api/client/vision/analyze",client,{
    question:"Analise",image:screenshot,consent:false
  })).status,403);
  const result=await call("POST","/api/client/vision/analyze",client,{
    question:"Analise",image:screenshot,consent:true
  });
  assert.equal(result.status,200);
  assert.match(result.payload.reply,/estrutura arcana/);
  assert.equal((await call("POST","/api/client/vision/analyze",client,{
    question:"Analise outra imagem",image:screenshot,consent:true
  })).status,429);
});
test("free quota errors never fall back to a different model",async()=>{
  await assert.rejects(()=>analyzeVision({question:"Analise",image:screenshot},{
    key:"free-key-only",
    fetcher:async()=>({ok:false,status:429})
  }),/Cota gratuita/);
});
