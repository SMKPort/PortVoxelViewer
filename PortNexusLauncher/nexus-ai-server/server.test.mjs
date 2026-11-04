import test from "node:test";
import assert from "node:assert/strict";
import { validatePlan, createServer } from "./server.mjs";

test("plan rejects unknown and unsafe actions", () => {
  assert.throws(() => validatePlan({ reply:"ok", steps:[{action:"RUN_SHELL",target:"",value:0,text:""}] }));
  assert.throws(() => validatePlan({ reply:"ok", steps:Array.from({length:7},()=>({action:"OPEN_APP",target:"A",value:0,text:""})) }));
  assert.throws(() => validatePlan({ reply:"ok", steps:[{action:"SET_VOLUME",target:"",value:200,text:""}] }));
});
test("plan accepts approved volume and app steps", () => {
  const p=validatePlan({reply:"Tudo certo",steps:[
    {action:"SET_VOLUME",target:"",value:60,text:""},
    {action:"OPEN_APP",target:"Spotify",value:0,text:""}
  ]});
  assert.equal(p.steps.length,2);
});
test("unconfigured server refuses commands",async()=>{
  const app=createServer({}); await new Promise(resolve=>app.listen(0,"127.0.0.1",resolve));
  try {
    const url="http://127.0.0.1:"+app.address().port;
    const health=await (await fetch(url+"/health")).json();
    assert.equal(health.ai_configured,false);
    const resp=await fetch(url+"/v1/plan",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({utterance:"Abrir Spotify"})});
    assert.equal(resp.status,503);
  } finally {app.close();}
});

test("responds with distinct quota and invalid-key diagnostic codes", async () => {
  const app = createServer({ OPENAI_API_KEY: "unit-test-secret", NEXUS_CLIENT_TOKEN: "unit-test-client" });
  await new Promise(resolve => app.listen(0, "127.0.0.1", resolve));
  const originalFetch = globalThis.fetch;
  try {
    const endpoint = "http://127.0.0.1:" + app.address().port;
    for (const [upstreamStatus, code, expectedHttp, expectedCode] of [
      [429, "insufficient_quota", 402, "OPENAI_QUOTA"],
      [401, "invalid_api_key", 424, "OPENAI_AUTH"]
    ]) {
      globalThis.fetch = (url, options) => String(url).startsWith("https://api.openai.com/")
        ? Promise.resolve(new Response(JSON.stringify({error: {code, type: code}}), {
          status: upstreamStatus, headers: {"Content-Type": "application/json"}
        }))
        : originalFetch(url, options);
      const response = await originalFetch(endpoint + "/v1/plan", {
        method: "POST",
        headers: {"Authorization": "Bearer unit-test-client", "Content-Type":"application/json"},
        body: JSON.stringify({utterance:"Abrir Spotify"})
      });
      assert.equal(response.status, expectedHttp);
      const body = await response.json();
      assert.equal(body.code, expectedCode);
      assert.ok(!JSON.stringify(body).includes("unit-test-secret"));
    }
  } finally {
    globalThis.fetch = originalFetch;
    app.close();
  }
});
