import * as SQLite from 'expo-sqlite';
import type { ChatMessage, Entry, EntryKind, EntryStatus } from './types';

let handle: Promise<SQLite.SQLiteDatabase> | null = null;
function db() { if (!handle) handle = SQLite.openDatabaseAsync('arcane-codex.db'); return handle; }
const uid = () => `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
const now = () => new Date().toISOString();

const SEED: Array<Pick<Entry, 'kind' | 'title' | 'content' | 'tags' | 'status'>> = [
  {kind:'lore', title:'Anael, deusa deste mundo', content:'Anael é a deusa do mundo em que ocorre a história de Port Morningstar. Port dirige suas orações a Anael. Não presumir os motivos dessa escolha.', tags:'Anael,divindade,cosmologia',status:'canon'},
  {kind:'lore', title:'Port Morningstar, o Portador', content:'Port Morningstar é um anjo de 2,10 m de altura. Possui seis asas, olho arcano esquerdo e braço direito protético de natureza arcana. Sua bengala pode se transformar em um grimório.',tags:'Port,portador,personagem',status:'canon'},
  {kind:'magia',title:'Braço arcano: Holy Spellpower',content:'Ao ativar o braço arcano direito, Port recebe um bônus aleatório de 10% a 60% de Holy Spellpower.',tags:'Port,holy,braço arcano',status:'canon'},
  {kind:'ritual', title:'O Preço da Verdade',content:'Ritual de revelação ligado a um crucifixo roubado. Materiais: Essência Arcana, Holy Ingot, sangue de demônio, sangue de anjo, pena de Port, fragmento vinculado ao crucifixo, cinzas de pergaminho consagrado, cera ritual branca e vermelha, fio de ouro sagrado, espelho de obsidiana, poeira de asa angelical e selo pessoal de Port. Preço: perda permanente de uma vida. Etapas: preparação, sacrifício, revelação de raça, rosto, nome verdadeiro e rastro do objeto. Revela o possuidor atual, não necessariamente o ladrão. Anael pode testemunhar, mas não precisa executar o ritual.',tags:'revelação,sacrifício,crucifixo',status:'canon'},
];

export async function initDB() {
  const d = await db();
  await d.execAsync(`PRAGMA journal_mode = WAL;
    CREATE TABLE IF NOT EXISTS entries (id TEXT PRIMARY KEY,kind TEXT NOT NULL,title TEXT NOT NULL,content TEXT NOT NULL,tags TEXT NOT NULL DEFAULT '',status TEXT NOT NULL CHECK(status IN ('canon','rascunho')),created_at TEXT NOT NULL,updated_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS chat (id TEXT PRIMARY KEY,role TEXT NOT NULL,content TEXT NOT NULL,created_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY,value TEXT NOT NULL);`);
  const seeded = await d.getFirstAsync<{value:string}>('SELECT value FROM meta WHERE key = ?', ['seed_1']);
  if (!seeded) {
    await d.withTransactionAsync(async () => {
      for (const e of SEED) await d.runAsync('INSERT INTO entries (id,kind,title,content,tags,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)',
        [uid(),e.kind,e.title,e.content,e.tags,e.status,now(),now()]);
      await d.runAsync('INSERT INTO meta (key,value) VALUES (?,?)', ['seed_1','true']);
    });
  }
}
export async function listEntries(search = ''): Promise<Entry[]> {
  const d = await db(); const s = `%${search.trim()}%`;
  return d.getAllAsync<Entry>('SELECT * FROM entries WHERE title LIKE ? OR content LIKE ? OR tags LIKE ? ORDER BY updated_at DESC', [s,s,s]);
}
export async function saveEntry(e: { id?: string; kind:EntryKind; title:string; content:string; tags:string; status:EntryStatus }) {
  const d = await db(); const id=e.id || uid();
  const old=await d.getFirstAsync<{created_at:string}>('SELECT created_at FROM entries WHERE id=?',[id]);
  await d.runAsync('INSERT OR REPLACE INTO entries (id,kind,title,content,tags,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)',
    [id,e.kind,e.title.trim(),e.content.trim(),e.tags.trim(),e.status,old?.created_at || now(),now()]); return id;
}
export async function setStatus(id:string,status:EntryStatus) { const d=await db(); await d.runAsync('UPDATE entries SET status=?,updated_at=? WHERE id=?',[status,now(),id]); }
export async function deleteEntry(id:string) { const d=await db(); await d.runAsync('DELETE FROM entries WHERE id=?',[id]); }
export async function listChat(): Promise<ChatMessage[]> { const d=await db(); return d.getAllAsync<ChatMessage>('SELECT * FROM chat ORDER BY created_at ASC'); }
export async function saveChat(role:ChatMessage['role'],content:string) { const d=await db(); await d.runAsync('INSERT INTO chat (id,role,content,created_at) VALUES (?,?,?,?)',[uid(),role,content,now()]); }
export async function clearChat() { const d=await db(); await d.runAsync('DELETE FROM chat'); }
export async function exportBackup() {
  const d=await db(); const entries=await listEntries(); const chat=await listChat();
  return JSON.stringify({format:'port-arcane-codex',version:1,exported_at:now(),entries,chat},null,2);
}
export async function importBackup(raw:string) {
  if (raw.length > 2_000_000) throw new Error('Backup acima de 2 MB.');
  const p=JSON.parse(raw) as {format?:string;version?:number;entries?:unknown[];chat?:unknown[]};
  if (p.format!=='port-arcane-codex'||p.version!==1||!Array.isArray(p.entries)||p.entries.length>5000) throw new Error('Backup inválido ou incompatível.');
  const valid=(v:any):v is Entry => v && typeof v.id==='string' && v.id.length<100 && ['oracao','ritual','magia','lore'].includes(v.kind) && ['canon','rascunho'].includes(v.status) && typeof v.title==='string' && v.title.length<=300 && typeof v.content==='string' && v.content.length<=50000 && typeof v.tags==='string' && typeof v.created_at==='string' && typeof v.updated_at==='string';
  if (!p.entries.every(valid)) throw new Error('Há registros inválidos no backup.');
  const d=await db();
  await d.withTransactionAsync(async () => {
    for(const e of p.entries as Entry[]) await d.runAsync('INSERT OR REPLACE INTO entries (id,kind,title,content,tags,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)',[e.id,e.kind,e.title,e.content,e.tags,e.status,e.created_at,e.updated_at]);
  });
  return p.entries.length;
}
