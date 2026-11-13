"use strict";
/**
 * Optional local-only Nexus TTS. The PCM is returned to the paired Minecraft
 * client, never injected into Plasmo Voice or broadcast to other players.
 */
const VOICES=new Set(["Charon","Orus","Puck","Kore","Algenib","Fenrir"]);
const STAGES=["PROTOCOLO","OBSERVAÇÃO","ADAPTAÇÃO","IDENTIDADE","INDIVIDUAÇÃO"];
function style(stage){
  return [
    "artificial, mechanical cadence, crisp and neutral",
    "precise, composed and observant",
    "natural pace, gently curious and analytical",
    "self-assured, serious but with subtle warmth",
    "calm, deep, protective and deliberate"
  ][Math.max(0,Math.min(4,stage))];
}
async function synthesizeSpeech({text,voice="Charon",stage=0},deps={}){
  const key=String(deps.key??process.env.NEXUS_GEMINI_API_KEY??process.env.GEMINI_API_KEY??"").trim();
  if(key.length<15)throw Object.assign(Error("Configure NEXUS_GEMINI_API_KEY para ativar a voz do Nexus."),{publicStatus:503});
  const model=String(deps.model||process.env.NEXUS_GEMINI_TTS_MODEL||"gemini-2.5-flash-preview-tts").trim();
  if(!/^[a-z0-9][a-z0-9.-]{1,65}$/.test(model))
    throw Object.assign(Error("Modelo de síntese inválido."),{publicStatus:503});
  if(typeof text!=="string"||text.trim().length<1||text.length>320)
    throw Object.assign(Error("Mensagem de voz deve ter até 320 caracteres."),{publicStatus:422});
  if(!VOICES.has(voice)||!Number.isInteger(stage)||stage<0||stage>4)
    throw Object.assign(Error("Configuração vocal inválida."),{publicStatus:422});
  const clean=text.replace(/[\u0000-\u001f\u007f]/g," ").replace(/\s+/g," ").trim();
  const request={
    contents:[{parts:[{text:"Say exactly this Portuguese Brazilian text, without adding words. "+
      "Narration style: "+style(stage)+". Text: "+clean}]}],
    generationConfig:{
      responseModalities:["AUDIO"],
      speechConfig:{languageCode:"pt-BR",voiceConfig:{prebuiltVoiceConfig:{voiceName:voice}}}
    }
  };
  let response;
  try{
    response=await (deps.fetcher||fetch)(
      "https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent",{
      method:"POST",headers:{"Content-Type":"application/json","x-goog-api-key":key},
      body:JSON.stringify(request),signal:AbortSignal.timeout(35000)
    });
  }catch(e){throw Object.assign(Error("Serviço de voz indisponível."),{publicStatus:502});}
  if(!response.ok)throw Object.assign(Error(response.status===429?
    "Limite do serviço de voz atingido.":"Falha no serviço de voz (HTTP "+response.status+")."),
    {publicStatus:response.status===429?429:502});
  let body;
  try{body=await response.json();}
  catch(e){throw Object.assign(Error("O serviço de voz devolveu uma resposta inválida."),{publicStatus:502});}
  const part=(body.candidates?.[0]?.content?.parts||[]).find(x=>x.inlineData?.data);
  if(!part||typeof part.inlineData.data!=="string")
    throw Object.assign(Error("Nenhuma fala foi gerada pelo Gemini."),{publicStatus:502});
  const pcm=Buffer.from(part.inlineData.data,"base64");
  const mime=String(part.inlineData.mimeType||"").toLowerCase();
  if(!/^audio\//.test(mime)||pcm.length<100||pcm.length>2_400_000||pcm.length%2)
    throw Object.assign(Error("Formato de áudio retornado não suportado."),{publicStatus:502});
  // Supported model returns headerless 16-bit 24 kHz mono PCM. No audio retention.
  if(pcm.subarray(0,4).toString("ascii")==="RIFF")
    throw Object.assign(Error("O provedor retornou WAV; o cliente espera PCM sem cabeçalho."),{publicStatus:502});
  if(mime.includes("rate=")&&!mime.includes("rate=24000"))
    throw Object.assign(Error("Taxa de áudio inesperada."),{publicStatus:502});
  return {pcm:pcm.toString("base64"),sampleRate:24000,channels:1,sampleBits:16,voice,stage:STAGES[stage]};
}
module.exports={synthesizeSpeech,VOICES};
