import * as SecureStore from 'expo-secure-store';
import type { ChatMessage, Entry } from './types';
const URL_KEY='arcane_api_url';
const TOKEN_KEY='arcane_api_token';
export async function getConfig() { const [url,token]=await Promise.all([SecureStore.getItemAsync(URL_KEY),SecureStore.getItemAsync(TOKEN_KEY)]); return {url:url||'',token:token||''}; }
export async function saveConfig(url:string,token:string) {
  const u=url.trim().replace(/\/+$/,'');
  if(u && !/^https:\/\//.test(u) && !/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(u)) throw new Error('Use um endereço HTTPS para o servidor.');
  await Promise.all([SecureStore.setItemAsync(URL_KEY,u), SecureStore.setItemAsync(TOKEN_KEY,token.trim())]);
}
export async function askAI(question:string,canon:Entry[],history:ChatMessage[]) {
  const {url,token}=await getConfig();
  if(!url||!token) throw new Error('Configure a URL e o token do servidor na aba Assistente.');
  const controller=new AbortController(); const timeout=setTimeout(()=>controller.abort(),30000);
  try {
    const r=await fetch(`${url}/api/chat`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${token}`},body:JSON.stringify({message:question,canon:canon.filter(e=>e.status==='canon').slice(0,60).map(e=>({kind:e.kind,title:e.title,content:e.content.slice(0,1800)})),history:history.slice(-8).map(x=>({role:x.role,content:x.content.slice(0,1500)}))}),signal:controller.signal});
    const data=await r.json().catch(()=>({}));
    if(!r.ok) throw new Error(data.error || `Erro HTTP ${r.status}`);
    if(typeof data.text!=='string'||!data.text.trim()) throw new Error('Resposta vazia do servidor.');
    return data.text as string;
  } catch(e:any) { if(e?.name==='AbortError') throw new Error('A resposta excedeu 30 segundos.'); throw e; }
  finally {clearTimeout(timeout);}
}
