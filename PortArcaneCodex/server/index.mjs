import 'dotenv/config';
import express from 'express';
import rateLimit from 'express-rate-limit';
import OpenAI from 'openai';
import { timingSafeEqual } from 'node:crypto';
import { instructions, makeInput, validPayload } from './prompt.mjs';

const {OPENAI_API_KEY,APP_ACCESS_TOKEN,OPENAI_MODEL='gpt-5-mini',PORT=8787}=process.env;
if (!OPENAI_API_KEY || !APP_ACCESS_TOKEN || Buffer.byteLength(APP_ACCESS_TOKEN)<32) {
  console.error('Configure OPENAI_API_KEY e APP_ACCESS_TOKEN com pelo menos 32 caracteres.');
  process.exit(1);
}
const openai=new OpenAI({apiKey:OPENAI_API_KEY});
const app=express();
app.disable('x-powered-by');
app.use(express.json({limit:'180kb'}));
app.use('/api',rateLimit({windowMs:15*60*1000,limit:40,standardHeaders:'draft-7',legacyHeaders:false}));
app.get('/health',(_req,res)=>res.json({ok:true,name:'arcane-codex-api'}));
app.post('/api/chat',async(req,res)=>{
  const bearer=req.header('authorization')||'';
  const given=Buffer.from(bearer.startsWith('Bearer ')?bearer.slice(7):'');
  const expected=Buffer.from(APP_ACCESS_TOKEN);
  if(given.length!==expected.length || !timingSafeEqual(given,expected)) return res.status(401).json({error:'Acesso negado.'});
  if(!validPayload(req.body)) return res.status(400).json({error:'Pedido inválido ou grande demais.'});
  try {
    const response=await openai.responses.create({model:OPENAI_MODEL,instructions,input:makeInput(req.body.message,req.body.canon,req.body.history),store:false,max_output_tokens:1600});
    res.json({text:response.output_text||'Não foi possível gerar uma resposta.'});
  } catch(error) {
    console.error('Falha OpenAI:',error?.status||error?.name||'unknown');
    res.status(502).json({error:'Falha temporária ao consultar a IA. Verifique chave, créditos ou conectividade.'});
  }
});
app.listen(Number(PORT),'0.0.0.0',()=>console.log(`Arcane Codex API ouvindo na porta ${PORT}`));
