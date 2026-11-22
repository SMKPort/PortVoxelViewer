"use strict";

/**
 * Separate free-quota-only Gemini image understanding endpoint.
 * REQUIRE a Gemini API key from a project without billing:
 * NEXUS_VISION_FREE_API_KEY. We do not fall back to the main Gemini key.
 * This source cannot inspect the account's billing settings.
 */
const MODEL="gemini-2.5-flash-lite";
function validate(question,image){
  if(typeof question!=="string"||question.trim().length<3||question.length>180)
    throw Object.assign(Error("Pergunta inválida para a Visão Arcana."),{publicStatus:422});
  if(typeof image!=="string"||image.length<80||image.length>540000||
    !/^[A-Za-z0-9+/]+={0,2}$/.test(image))
    throw Object.assign(Error("JPEG inválido ou acima do limite de tamanho."),{publicStatus:422});
  return {question:question.trim().replace(/[\u0000-\u001f\u007f]/g," "),image};
}
async function analyzeVision({question,image},deps={}){
  const payload=validate(question,image);
  const key=deps.key??process.env.NEXUS_VISION_FREE_API_KEY;
  if(!key||key.length<10)throw Object.assign(
    Error("Configure NEXUS_VISION_FREE_API_KEY de um projeto Gemini sem faturamento. Visão Arcana não realizará chamadas pagas."),
    {publicStatus:503});
  const fetcher=deps.fetcher||fetch;
  let response;
  try{
    response=await fetcher(
      "https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent",{
      method:"POST",
      headers:{"Content-Type":"application/json","x-goog-api-key":key},
      body:JSON.stringify({
        systemInstruction:{parts:[{text:[
          "Você é a Visão Arcana do Nexus Aterna, em um roleplay do Minecraft.",
          "Interprete somente o conteúdo VISÍVEL da captura enviada.",
          "Descreva objetos, estruturas e mecanismos que se pode observar.",
          "Se não reconhecer algo, dê hipóteses identificadas como incertas.",
          "Não invente blocos, mods, texto, jogadores ou detalhes não visíveis.",
          "Não execute comandos nem ofereça instruções para burlar servidores.",
          "Responda em português brasileiro em até 600 caracteres."
        ].join(" ")}]},
        contents:[{role:"user",parts:[
          {text:payload.question},
          {inline_data:{mime_type:"image/jpeg",data:payload.image}}
        ]}],
        generationConfig:{temperature:0.3,maxOutputTokens:300}
      }),
      signal:AbortSignal.timeout(25000)
    });
  }catch(e){
    throw Object.assign(Error("Gemini gratuito não respondeu à imagem."),{publicStatus:504});
  }
  if(!response.ok){
    const status=response.status===429?429:response.status===401||response.status===403?503:502;
    throw Object.assign(Error(response.status===429?
      "Cota gratuita do Gemini esgotada. Nenhum modelo pago será usado.":
      response.status===401||response.status===403?
      "Chave de Visão Arcana sem acesso gratuito. Verifique um projeto sem faturamento.":
      "Não foi possível analisar esta imagem no modelo gratuito."),{publicStatus:status});
  }
  let result;
  try{result=await response.json();}catch(e){
    throw Object.assign(Error("Resposta de visão inválida."),{publicStatus:502});
  }
  const text=(result?.candidates?.[0]?.content?.parts||[])
    .map(x=>typeof x.text==="string"?x.text:"").join(" ")
    .replace(/[\u0000-\u001f\u007f]/g," ").trim().slice(0,700);
  if(!text)throw Object.assign(Error("O Gemini não identificou conteúdo suficiente."),{publicStatus:502});
  return {text,model:MODEL};
}
module.exports={MODEL,validate,analyzeVision};
