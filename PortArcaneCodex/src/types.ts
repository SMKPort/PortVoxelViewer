export const ENTRY_KINDS = ['oracao', 'ritual', 'magia', 'lore'] as const;
export type EntryKind = typeof ENTRY_KINDS[number];
export type EntryStatus = 'canon' | 'rascunho';
export type Tab = 'assistente' | 'oracoes' | 'rituais' | 'grimorio' | 'lore' | 'biblioteca';
export type Entry = {
  id: string;
  kind: EntryKind;
  title: string;
  content: string;
  tags: string;
  status: EntryStatus;
  created_at: string;
  updated_at: string;
};
export type ChatMessage = { id: string; role: 'user' | 'assistant'; content: string; created_at: string };
export const KIND_LABEL: Record<EntryKind, string> = {
  oracao: 'Oração', ritual: 'Ritual', magia: 'Magia', lore: 'Lore',
};
