/**
 * Gemini gratuito para interpretação de comandos Nexus.
 * A API key fica exclusivamente no Render. Não enviar ao APK.
 */
export class GeminiGatewayError extends Error {
  constructor(code, status, message, upstreamStatus = null) {
    super(message);
    this.code = code;
    this.status = status;
    this.upstreamStatus = upstreamStatus;
  }
}

const DEFAULT_MODEL = "gemini-3.5-flash-lite";

function classifyError(status, body) {
  const code = String(body?.error?.status || body?.error?.code || "");
  if (status === 401 || status === 403) {
    return new GeminiGatewayError("GEMINI_AUTH", 424,
      "Chave do Gemini inválida ou não autorizada. Confira GEMINI_API_KEY no Render.", status);
  }
  if (status === 429 || code === "RESOURCE_EXHAUSTED") {
    return new GeminiGatewayError("GEMINI_QUOTA", 429,
      "A cota gratuita do Gemini foi atingida. Consulte os limites no AI Studio.", status);
  }
  if (status === 400 || status === 404 || status === 422) {
    return new GeminiGatewayError("GEMINI_MODEL", 424,
      "O Gemini rejeitou o modelo ou a configuração. Confira GEMINI_MODEL.", status);
  }
  return new GeminiGatewayError("GEMINI_UPSTREAM", 502,
    "A API Gemini está indisponível. Tente novamente.", status);
}

export async function planWithGemini({utterance, key, model = DEFAULT_MODEL, instructions, validatePlan}) {
  if (!/^gemini-[a-z0-9.-]+$/.test(model)) {
    throw new GeminiGatewayError("GEMINI_MODEL", 424, "Modelo Gemini inválido.");
  }
  const endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
    encodeURIComponent(model) + ":generateContent";
  const request = {
    systemInstruction: {parts: [{text: instructions +
      "\nRetorne SOMENTE um objeto JSON válido com reply e steps. Cada step tem obrigatoriamente action, target, value inteiro e text. Ações desconhecidas são UNSUPPORTED. Não execute comandos."}]},
    contents: [{role: "user", parts: [{text: utterance}]}],
    generationConfig: {responseMimeType: "application/json", temperature: 0.1, maxOutputTokens: 1700},
  };
  const response = await fetch(endpoint, {
    method: "POST",
    signal: AbortSignal.timeout(24000),
    headers: {"x-goog-api-key": key, "Content-Type": "application/json"},
    body: JSON.stringify(request)
  });
  if (!response.ok) {
    let info = {};
    try { info = await response.json(); } catch { /* Never expose upstream body. */ }
    throw classifyError(response.status, info);
  }
  const result = await response.json();
  const output = (result?.candidates?.[0]?.content?.parts || [])
    .map(part => typeof part.text === "string" ? part.text : "").join("").trim();
  if (!output) {
    throw new GeminiGatewayError("GEMINI_EMPTY", 502,
      "Gemini não retornou um plano. Tente um comando mais curto.");
  }
  try {
    return validatePlan(JSON.parse(output));
  } catch {
    throw new GeminiGatewayError("GEMINI_PLAN", 502,
      "Gemini retornou um plano inválido. Tente reformular o comando.");
  }
}
