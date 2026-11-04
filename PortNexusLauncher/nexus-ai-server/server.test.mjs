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

test("Gemini provider runs a safe two-step plan without calling OpenAI", async () => {
  const app = createServer({
    NEXUS_AI_PROVIDER: "gemini", GEMINI_API_KEY: "fake-google-key",
    GEMINI_MODEL: "gemini-3.5-flash-lite",
    NEXUS_CLIENT_TOKEN: "unit-test-client"
  });
  await new Promise(resolve => app.listen(0, "127.0.0.1", resolve));
  const originalFetch = globalThis.fetch;
  let calledGemini = 0;
  try {
    const endpoint = "http://127.0.0.1:" + app.address().port;
    globalThis.fetch = (url, options) => {
      assert.ok(String(url).startsWith("https://generativelanguage.googleapis.com/"));
      assert.equal(options.headers["x-goog-api-key"], "fake-google-key");
      const body = JSON.parse(options.body);
      assert.equal(body.contents[0].parts[0].text, "Abra Spotify e ajuste volume em 50%");
      calledGemini++;
      const output = {
        reply: "Vou abrir Spotify e ajustar volume.",
        steps: [
          {action: "OPEN_APP", target: "Spotify", value: 0, text: ""},
          {action: "SET_VOLUME", target: "", value: 50, text: ""}
        ]
      };
      return Promise.resolve(new Response(JSON.stringify({
        candidates: [{content: {parts: [{text: JSON.stringify(output)}]}}]
      }), {status: 200, headers: {"Content-Type": "application/json"}}));
    };
    const health = await (await originalFetch(endpoint + "/health")).json();
    assert.equal(health.provider, "gemini");
    assert.equal(health.ai_configured, true);
    const result = await originalFetch(endpoint + "/v1/plan", {
      method: "POST",
      headers: {"Authorization": "Bearer unit-test-client", "Content-Type": "application/json"},
      body: JSON.stringify({utterance: "Abra Spotify e ajuste volume em 50%"})
    });
    assert.equal(result.status, 200);
    const plan = await result.json();
    assert.equal(plan.steps.length, 2);
    assert.equal(calledGemini, 1);
    assert.ok(!JSON.stringify(plan).includes("fake-google-key"));
  } finally {
    globalThis.fetch = originalFetch;
    app.close();
  }
});

test("Gemini quota failures are clearly separated from OpenAI billing", async () => {
  const app = createServer({
    NEXUS_AI_PROVIDER: "gemini", GEMINI_API_KEY: "fake-google-key",
    NEXUS_CLIENT_TOKEN: "unit-test-client"
  });
  await new Promise(resolve => app.listen(0, "127.0.0.1", resolve));
  const originalFetch = globalThis.fetch;
  try {
    const endpoint = "http://127.0.0.1:" + app.address().port;
    globalThis.fetch = (url, options) => Promise.resolve(
      new Response(JSON.stringify({error:{status:"RESOURCE_EXHAUSTED", code:429}}), {
        status:429, headers:{"Content-Type":"application/json"}
      })
    );
    const result = await originalFetch(endpoint + "/v1/plan", {
      method: "POST",
      headers: {"Authorization": "Bearer unit-test-client", "Content-Type":"application/json"},
      body:JSON.stringify({utterance: "Nexus, abrir Spotify"})
    });
    assert.equal(result.status, 429);
    const error = await result.json();
    assert.equal(error.code, "GEMINI_QUOTA");
  } finally {
    globalThis.fetch = originalFetch;
    app.close();
  }
});
