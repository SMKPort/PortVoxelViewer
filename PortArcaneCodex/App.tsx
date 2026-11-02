import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, AppState, FlatList, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as SecureStore from 'expo-secure-store';
import * as Clipboard from 'expo-clipboard';
import { StatusBar } from 'expo-status-bar';
import { askAI, getConfig, saveConfig } from './src/ai';
import { clearChat, deleteEntry, exportBackup, importBackup, initDB, listChat, listEntries, saveChat, saveEntry, setStatus } from './src/db';
import { C } from './src/theme';
import { Entry, EntryKind, EntryStatus, KIND_LABEL, Tab, ChatMessage } from './src/types';

const PIN_KEY='arcane_private_pin';
const TABS: {id:Tab;label:string;icon:React.ComponentProps<typeof Ionicons>['name']}[]=[
  {id:'assistente',label:'Assistente',icon:'sparkles-outline'},
  {id:'oracoes',label:'Orações',icon:'sunny-outline'},
  {id:'rituais',label:'Rituais',icon:'flame-outline'},
  {id:'grimorio',label:'Grimório',icon:'book-outline'},
  {id:'lore',label:'Lore',icon:'planet-outline'},
  {id:'biblioteca',label:'Biblioteca',icon:'library-outline'},
];
const KIND_FOR_TAB: Partial<Record<Tab,EntryKind>>={oracoes:'oracao',rituais:'ritual',grimorio:'magia',lore:'lore'};
const empty=(kind:EntryKind):EditorValue=>({kind,title:'',content:'',tags:'',status:'rascunho'});
type EditorValue={id?:string;kind:EntryKind;title:string;content:string;tags:string;status:EntryStatus};

function Icon({name,size=21,color=C.gold}:{name:React.ComponentProps<typeof Ionicons>['name'];size?:number;color?:string}) {return <Ionicons name={name} size={size} color={color}/>;}
function Btn({text,onPress,outline=false,disabled=false,icon}:{text:string;onPress:()=>void;outline?:boolean;disabled?:boolean;icon?:React.ComponentProps<typeof Ionicons>['name']}) {
  return <Pressable accessibilityRole="button" disabled={disabled} onPress={onPress} style={[styles.btn,outline?styles.outline:styles.btnSolid,disabled&&{opacity:.45}]}>
    {icon&&<Icon name={icon} color={outline?C.gold:C.bg} size={17}/>}
    <Text style={[styles.btnText,{color:outline?C.gold:C.bg}]}>{text}</Text>
  </Pressable>;
}
function Field({label,value,onChangeText,placeholder='',multiline=false,secure=false}:{label:string;value:string;onChangeText:(v:string)=>void;placeholder?:string;multiline?:boolean;secure?:boolean}) {
  return <View style={{gap:7}}><Text style={styles.label}>{label}</Text><TextInput
    style={[styles.field,multiline&&{height:160,textAlignVertical:'top'}]}
    value={value} onChangeText={onChangeText} placeholder={placeholder} placeholderTextColor="#7D7078"
    multiline={multiline} secureTextEntry={secure} autoCapitalize={secure?'none':'sentences'} autoCorrect={!secure}
    selectionColor={C.gold}/></View>;
}
function Pill({text,active,onPress}:{text:string;active:boolean;onPress:()=>void}) {return <Pressable onPress={onPress} style={[styles.pill,active&&styles.pillActive]}><Text style={[styles.pillText,active&&{color:C.gold}]}>{text}</Text></Pressable>;}
function EntryCard({entry,onEdit,onStatus,onDelete}:{entry:Entry;onEdit:()=>void;onStatus:()=>void;onDelete:()=>void}) {
 return <View style={styles.entry}>
   <View style={styles.rowBetween}>
     <Text style={styles.entryKind}>{KIND_LABEL[entry.kind].toUpperCase()}</Text>
     <View style={[styles.badge,{borderColor:entry.status==='canon'?C.gold:C.border}]}><Text style={{color:entry.status==='canon'?C.gold:C.muted,fontSize:11,fontWeight:'700'}}>{entry.status==='canon'?'CÂNONE':'RASCUNHO'}</Text></View>
   </View>
   <Text style={styles.entryTitle}>{entry.title}</Text>
   <Text style={styles.entryPreview} numberOfLines={3}>{entry.content}</Text>
   {!!entry.tags&&<Text style={styles.entryTags}>#{entry.tags.split(',').map(t=>t.trim()).filter(Boolean).join('  #')}</Text>}
   <View style={[styles.row,{marginTop:12,flexWrap:'wrap'}]}>
    <Pressable onPress={onEdit} style={styles.smallAction}><Icon name="create-outline" size={17}/><Text style={styles.smallActionText}>Editar</Text></Pressable>
    <Pressable onPress={onStatus} style={styles.smallAction}><Icon name={entry.status==='canon'?'document-outline':'checkmark-circle-outline'} size={17}/><Text style={styles.smallActionText}>{entry.status==='canon'?'Rascunho':'Aprovar cânone'}</Text></Pressable>
    <Pressable onPress={onDelete} style={styles.smallAction}><Icon name="trash-outline" size={17} color={C.red}/><Text style={[styles.smallActionText,{color:C.red}]}>Excluir</Text></Pressable>
   </View>
 </View>;
}
function LockScreen({onUnlocked}:{onUnlocked:()=>Promise<void>}) {
 const [exists,setExists]=useState<boolean|null>(null); const [pin,setPin]=useState('');const [repeat,setRepeat]=useState('');const [busy,setBusy]=useState(false);const [error,setError]=useState('');
 useEffect(()=>{SecureStore.getItemAsync(PIN_KEY).then(v=>setExists(!!v)).catch(()=>setError('Não foi possível acessar o armazenamento seguro.'));},[]);
 async function enter(){if(busy)return;setBusy(true);setError('');try{
  const saved=await SecureStore.getItemAsync(PIN_KEY);
  if(!saved){if(!/^\d{6,12}$/.test(pin)){setError('Defina um PIN de 6 a 12 dígitos.');return;}
   if(pin!==repeat){setError('A confirmação do PIN não confere.');return;}
   await SecureStore.setItemAsync(PIN_KEY,pin,{keychainAccessible:SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY});
  } else if(saved!==pin) {setError('PIN incorreto.');setPin('');return;}
  await onUnlocked();
 } catch(e:any){setError(e?.message||'Não foi possível abrir o grimório.');}finally{setBusy(false);}}
 return <SafeAreaView style={styles.screen}><View style={styles.lockBox}>
   <View style={styles.lockIcon}><Icon name="shield-checkmark-outline" size={48}/></View>
   <Text style={styles.brandBig}>PORT ARCANE</Text><Text style={styles.lockSub}>CODEX  •  ACESSO PRIVADO</Text>
   {exists===null?<ActivityIndicator color={C.gold}/>:<>
     <Text style={styles.description}>{exists?'Informe seu PIN para acessar o arquivo.':'Crie um PIN local para proteger a abertura do aplicativo.'}</Text>
     <TextInput style={[styles.field,{width:'100%',textAlign:'center',fontSize:24,letterSpacing:5}]} placeholder="••••••" placeholderTextColor={C.muted} keyboardType="number-pad" secureTextEntry maxLength={12} value={pin} onChangeText={setPin}/>
     {!exists&&<TextInput style={[styles.field,{width:'100%',textAlign:'center'}]} placeholder="Confirmar PIN" placeholderTextColor={C.muted} keyboardType="number-pad" secureTextEntry maxLength={12} value={repeat} onChangeText={setRepeat}/>}
     {!!error&&<Text style={styles.error}>{error}</Text>}
     <Btn text={busy?'Abrindo...':exists?'Desbloquear':'Criar cofre'} onPress={enter} disabled={busy||!pin} icon="lock-open-outline"/>
     <Text style={styles.footnote}>O PIN limita o acesso pela interface. O banco local não possui criptografia própria. Guarde seu PIN.</Text>
   </>}
 </View></SafeAreaView>;
}
function MainApp({lock}:{lock:()=>void}) {
 const [tab,setTab]=useState<Tab>('assistente');const [entries,setEntries]=useState<Entry[]>([]);const [chat,setChat]=useState<ChatMessage[]>([]);const [search,setSearch]=useState('');
 const [editor,setEditor]=useState<EditorValue|null>(null);const [busy,setBusy]=useState(false);const [question,setQuestion]=useState('');const [aiKind,setAiKind]=useState<EntryKind>('ritual');
 const [settings,setSettings]=useState(false);const [url,setUrl]=useState('');const [token,setToken]=useState('');const [importVisible,setImportVisible]=useState(false);const [backupText,setBackupText]=useState('');
 const refresh=useCallback(async()=>{const [items,messages]=await Promise.all([listEntries(),listChat()]);setEntries(items);setChat(messages);},[]);
 useEffect(()=>{refresh().catch(e=>Alert.alert('Erro',e.message));getConfig().then(c=>{setUrl(c.url);setToken(c.token);});},[refresh]);
 const view=useMemo(()=>entries.filter(e=>(!KIND_FOR_TAB[tab]||e.kind===KIND_FOR_TAB[tab])&&(tab!=='biblioteca'||(`${e.title} ${e.tags} ${e.content}`.toLowerCase().includes(search.toLowerCase())))),[entries,search,tab]);
 const canonCount=entries.filter(e=>e.status==='canon').length;
 async function persist(){if(!editor)return;if(!editor.title.trim()||!editor.content.trim())return Alert.alert('Campos necessários','Insira título e conteúdo.');try{await saveEntry(editor);setEditor(null);await refresh();}catch(e:any){Alert.alert('Erro',e.message);}}
 function updateStatus(entry:Entry){const next=entry.status==='canon'?'rascunho':'canon';Alert.alert(next==='canon'?'Aprovar como cânone?':'Rebaixar para rascunho?',next==='canon'?'A IA poderá tratar este registro como verdade do seu universo.':'Este registro deixará de orientar o cânone da IA.',[{text:'Cancelar',style:'cancel'},{text:'Confirmar',onPress:async()=>{await setStatus(entry.id,next);await refresh();}}]);}
 function remove(entry:Entry){Alert.alert('Excluir registro?',`“${entry.title}” será excluído do aparelho.`,[{text:'Cancelar',style:'cancel'},{text:'Excluir',style:'destructive',onPress:async()=>{await deleteEntry(entry.id);await refresh();}}]);}
 async function send(){const q=question.trim();if(!q||busy)return;setBusy(true);setQuestion('');try{
   await saveChat('user',q);await refresh();const text=await askAI(q,entries,chat);await saveChat('assistant',text);await refresh();
 }catch(e:any){Alert.alert('Assistente',e?.message||'Erro de conexão.');setQuestion(q);}finally{setBusy(false);}}
 async function saveServer(){try{await saveConfig(url,token);setSettings(false);Alert.alert('Configuração salva','A conexão será utilizada nas próximas perguntas.');}catch(e:any){Alert.alert('Configuração inválida',e.message);}}
 async function copyBackup(){try{const value=await exportBackup();await Clipboard.setStringAsync(value);Alert.alert('Backup copiado','O JSON foi copiado. Cole em um arquivo e armazene em local privado.');}catch(e:any){Alert.alert('Erro',e.message);}}
 async function restoreBackup(){try{const total=await importBackup(backupText);setImportVisible(false);setBackupText('');await refresh();Alert.alert('Importação concluída',`${total} registros foram importados ou atualizados.`);}catch(e:any){Alert.alert('Backup inválido',e.message);}}
 function draftFromAI(message:string){setEditor({...empty(aiKind),title:`${KIND_LABEL[aiKind]} gerado pela IA`,content:message});}
 const entryHeader=tab==='oracoes'?'ORAÇÕES':tab==='rituais'?'RITUAIS':tab==='grimorio'?'GRIMÓRIO':tab==='lore'?'LORE':'BIBLIOTECA';
 return <SafeAreaView style={styles.screen} edges={['top','left','right','bottom']}><StatusBar style="light"/>
   <View style={styles.top}><View style={styles.row}><View style={styles.logo}><Icon name="flame" size={24} color={C.gold}/></View><View><Text style={styles.brand}>PORT ARCANE CODEX</Text><Text style={styles.brandSub}>O ARQUIVO VIVO DA CRIAÇÃO</Text></View></View><Pressable accessibilityLabel="Bloquear aplicativo" onPress={lock}><Icon name="lock-closed-outline" size={23}/></Pressable></View>
   <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.tabBar} style={{flexGrow:0}}>
     {TABS.map(t=><Pressable key={t.id} onPress={()=>setTab(t.id)} style={[styles.tab,tab===t.id&&styles.tabActive]}><Icon name={t.icon} size={18} color={tab===t.id?C.gold:C.muted}/><Text style={[styles.tabText,tab===t.id&&{color:C.gold}]}>{t.label}</Text></Pressable>)}
   </ScrollView>
   {tab==='assistente'?<KeyboardAvoidingView style={{flex:1}} behavior={Platform.OS==='ios'?'padding':undefined}>
     <View style={[styles.rowBetween,{paddingHorizontal:18,paddingTop:14,paddingBottom:10}]}><View><Text style={styles.heading}>ORÁCULO ARCANO</Text><Text style={styles.subheading}>{canonCount} registros canônicos disponíveis</Text></View><Pressable onPress={()=>setSettings(!settings)} accessibilityLabel="Configurar IA"><Icon name="settings-outline" size={24}/></Pressable></View>
     {settings?<ScrollView contentContainerStyle={{padding:18,gap:14}} keyboardShouldPersistTaps="handled"><Text style={styles.description}>Use a URL HTTPS do seu servidor e um token secreto. A chave OpenAI fica somente no servidor.</Text><Field label="URL do servidor" placeholder="https://seu-servidor.exemplo" value={url} onChangeText={setUrl}/><Field label="Token privado do servidor" secure value={token} onChangeText={setToken}/><Btn text="Salvar configuração" onPress={saveServer} icon="save-outline"/><Btn text="Voltar ao chat" outline onPress={()=>setSettings(false)}/></ScrollView>:<>
       <ScrollView style={{flex:1}} contentContainerStyle={{padding:16,gap:12}} keyboardShouldPersistTaps="handled">
         {chat.length===0&&<View style={styles.welcome}><Icon name="sparkles-outline" size={38}/><Text style={styles.entryTitle}>Seu assistente de criação</Text><Text style={[styles.description,{textAlign:'center'}]}>Crie orações para Anael, magias, rituais e narrativas. A IA lê seu cânone aprovado, mas guarda ideias como rascunhos até você decidir.</Text></View>}
         {chat.map(msg=><View key={msg.id} style={[styles.message,msg.role==='user'?styles.userMessage:styles.aiMessage]}>
           <Text style={styles.messageAuthor}>{msg.role==='user'?'VOCÊ':'CODEX • IA'}</Text><Text style={styles.messageText}>{msg.content}</Text>
           {msg.role==='assistant'&&<Pressable onPress={()=>draftFromAI(msg.content)} style={styles.smallAction}><Icon name="bookmark-outline" size={16}/><Text style={styles.smallActionText}>Salvar como rascunho</Text></Pressable>}
         </View>)}
         {busy&&<View style={[styles.message,styles.aiMessage]}><ActivityIndicator color={C.gold}/><Text style={styles.description}>Consultando o oráculo...</Text></View>}
         <Btn text="Limpar conversa" outline icon="trash-outline" onPress={()=>Alert.alert('Limpar conversa?','Seus registros do grimório não serão alterados.',[{text:'Cancelar',style:'cancel'},{text:'Limpar',style:'destructive',onPress:async()=>{await clearChat();await refresh();}}])}/>
       </ScrollView>
       <View style={styles.composer}><ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{gap:7,paddingBottom:8}}>
         {(['ritual','oracao','magia','lore'] as EntryKind[]).map(k=><Pill key={k} text={KIND_LABEL[k]} active={aiKind===k} onPress={()=>{setAiKind(k);if(!question.trim())setQuestion(`Crie um(a) ${KIND_LABEL[k].toLowerCase()} para o universo de Port Morningstar.`);}}/>)}
       </ScrollView><View style={styles.row}><TextInput value={question} onChangeText={setQuestion} style={[styles.field,{flex:1,maxHeight:110}]} multiline placeholder="O que vamos criar hoje?" placeholderTextColor="#82757D"/><Pressable onPress={send} disabled={busy||!question.trim()} style={[styles.send,{opacity:busy||!question.trim()?.5:1}]}><Icon name="arrow-up" size={22} color={C.bg}/></Pressable></View></View>
     </>}
   </KeyboardAvoidingView>:<View style={{flex:1}}>
     <View style={styles.listHeader}><View><Text style={styles.heading}>{entryHeader}</Text><Text style={styles.subheading}>{view.length} registros • {canonCount} canônicos</Text></View><Pressable onPress={()=>setEditor(empty(KIND_FOR_TAB[tab]||'lore'))} style={styles.addButton}><Icon name="add" size={24} color={C.bg}/></Pressable></View>
     {tab==='biblioteca'&&<View style={{paddingHorizontal:16,paddingBottom:10}}><TextInput value={search} onChangeText={setSearch} style={styles.field} placeholder="Buscar títulos, textos ou tags..." placeholderTextColor={C.muted}/></View>}
     <FlatList data={view} keyExtractor={e=>e.id} keyboardShouldPersistTaps="handled" contentContainerStyle={{padding:16,paddingTop:0,paddingBottom:30,gap:12}} renderItem={({item})=><EntryCard entry={item} onEdit={()=>setEditor({...item})} onStatus={()=>updateStatus(item)} onDelete={()=>remove(item)}/>} ListEmptyComponent={<View style={styles.welcome}><Icon name="library-outline" size={39}/><Text style={styles.description}>Nenhum registro nesta aba. Use o botão + para criar.</Text></View>}/>
     {tab==='biblioteca'&&<View style={{padding:12,flexDirection:'row',gap:10}}><View style={{flex:1}}><Btn text="Copiar backup" outline icon="copy-outline" onPress={copyBackup}/></View><View style={{flex:1}}><Btn text="Importar" outline icon="download-outline" onPress={()=>setImportVisible(true)}/></View></View>}
   </View>}
   <Modal visible={!!editor} animationType="slide" onRequestClose={()=>setEditor(null)}><SafeAreaView style={styles.screen}><View style={[styles.rowBetween,{padding:18,borderBottomWidth:1,borderColor:C.border}]}><Text style={styles.heading}>{editor?.id?'EDITAR REGISTRO':'NOVO REGISTRO'}</Text><Pressable onPress={()=>setEditor(null)}><Icon name="close" size={26}/></Pressable></View>{editor&&<ScrollView contentContainerStyle={{padding:18,gap:18}} keyboardShouldPersistTaps="handled">
      <Text style={styles.label}>CATEGORIA</Text><View style={[styles.row,{flexWrap:'wrap'}]}>{(['oracao','ritual','magia','lore'] as EntryKind[]).map(k=><Pill key={k} text={KIND_LABEL[k]} active={editor.kind===k} onPress={()=>setEditor({...editor,kind:k})}/>)}</View>
      <Field label="TÍTULO" value={editor.title} onChangeText={v=>setEditor({...editor,title:v})} placeholder="Ex.: A Revelação de Anael"/>
      <Field label="TEXTO COMPLETO" value={editor.content} onChangeText={v=>setEditor({...editor,content:v})} placeholder="Recitação, materiais, mecânicas, história..." multiline/>
      <Field label="TAGS (SEPARADAS POR VÍRGULAS)" value={editor.tags} onChangeText={v=>setEditor({...editor,tags:v})} placeholder="Port, revelação, Anael"/>
      <Text style={styles.label}>CLASSIFICAÇÃO</Text><View style={styles.row}><Pill text="Rascunho" active={editor.status==='rascunho'} onPress={()=>setEditor({...editor,status:'rascunho'})}/><Pill text="Cânone aprovado" active={editor.status==='canon'} onPress={()=>setEditor({...editor,status:'canon'})}/></View>
      <Text style={styles.footnote}>A IA utiliza apenas os registros classificados como cânone. Revise novas histórias antes de aprová-las.</Text>
      <Btn text="Salvar no grimório" icon="save-outline" onPress={persist}/><Btn text="Cancelar" outline onPress={()=>setEditor(null)}/>
     </ScrollView>}</SafeAreaView></Modal>
   <Modal visible={importVisible} animationType="slide" onRequestClose={()=>setImportVisible(false)}><SafeAreaView style={styles.screen}><View style={{padding:18,gap:16}}><Text style={styles.heading}>RESTAURAR BACKUP</Text><Text style={styles.description}>Cole o JSON exportado anteriormente. Registros com o mesmo ID serão atualizados, sem apagar os demais.</Text><Field label="JSON DO BACKUP" value={backupText} multiline onChangeText={setBackupText} placeholder="{...}"/><Btn text="Colar da área de transferência" outline icon="clipboard-outline" onPress={async()=>setBackupText(await Clipboard.getStringAsync())}/><Btn text="Importar registros" icon="download-outline" disabled={!backupText.trim()} onPress={restoreBackup}/><Btn text="Cancelar" outline onPress={()=>setImportVisible(false)}/></View></SafeAreaView></Modal>
 </SafeAreaView>;
}
export default function App(){const [unlocked,setUnlocked]=useState(false);useEffect(()=>{const sub=AppState.addEventListener('change',s=>{if(s==='background')setUnlocked(false);});return()=>sub.remove();},[]);
 return <SafeAreaProvider><StatusBar style="light"/>{unlocked?<MainApp lock={()=>setUnlocked(false)}/>:<LockScreen onUnlocked={async()=>{await initDB();setUnlocked(true);}}/>}</SafeAreaProvider>;
}
const styles=StyleSheet.create({
 screen:{flex:1,backgroundColor:C.bg},row:{flexDirection:'row',alignItems:'center',gap:10},rowBetween:{flexDirection:'row',alignItems:'center',justifyContent:'space-between',gap:10},
 top:{paddingHorizontal:17,paddingVertical:12,borderBottomWidth:1,borderBottomColor:C.border,flexDirection:'row',alignItems:'center',justifyContent:'space-between'},
 logo:{width:40,height:40,borderRadius:12,backgroundColor:C.redDark,borderWidth:1,borderColor:C.red,alignItems:'center',justifyContent:'center'},brand:{color:C.gold,fontSize:15,fontWeight:'900',letterSpacing:1},brandSub:{color:C.muted,fontSize:9,letterSpacing:1.7,marginTop:3},
 tabBar:{paddingHorizontal:14,paddingVertical:12,gap:9},tab:{paddingHorizontal:13,paddingVertical:9,flexDirection:'row',gap:7,alignItems:'center',borderRadius:13,backgroundColor:C.surface,borderWidth:1,borderColor:C.border},tabActive:{backgroundColor:C.redDark,borderColor:C.goldMuted},tabText:{fontWeight:'700',fontSize:12,color:C.muted},
 heading:{fontSize:18,fontWeight:'900',color:C.gold,letterSpacing:1},subheading:{color:C.muted,fontSize:12,marginTop:5},description:{color:C.muted,fontSize:14,lineHeight:21},
 field:{borderColor:C.border,borderWidth:1,backgroundColor:C.surface,borderRadius:12,color:C.text,paddingHorizontal:14,paddingVertical:13,fontSize:15,minHeight:50},label:{color:C.goldMuted,fontSize:11,fontWeight:'800',letterSpacing:1.1},
 btn:{minHeight:45,borderRadius:12,paddingHorizontal:16,justifyContent:'center',alignItems:'center',flexDirection:'row',gap:8},btnSolid:{backgroundColor:C.gold},outline:{borderColor:C.goldMuted,borderWidth:1,backgroundColor:C.surface},btnText:{fontSize:13,fontWeight:'800'},
 pill:{paddingHorizontal:13,paddingVertical:9,borderRadius:20,backgroundColor:C.surface,borderWidth:1,borderColor:C.border},pillActive:{backgroundColor:C.redDark,borderColor:C.goldMuted},pillText:{color:C.muted,fontSize:12,fontWeight:'700'},
 listHeader:{paddingHorizontal:18,paddingVertical:16,flexDirection:'row',alignItems:'center',justifyContent:'space-between'},addButton:{backgroundColor:C.gold,width:43,height:43,borderRadius:13,alignItems:'center',justifyContent:'center'},
 entry:{backgroundColor:C.surface,borderWidth:1,borderColor:C.border,borderRadius:16,padding:16},entryKind:{color:C.red,fontSize:11,letterSpacing:1.2,fontWeight:'800'},badge:{borderWidth:1,paddingHorizontal:9,paddingVertical:4,borderRadius:12},entryTitle:{color:C.text,fontWeight:'800',fontSize:17,marginTop:10},entryPreview:{color:C.muted,fontSize:13,lineHeight:20,marginTop:8},entryTags:{color:C.goldMuted,fontSize:11,marginTop:10},smallAction:{flexDirection:'row',gap:5,alignItems:'center',paddingVertical:5,marginRight:7},smallActionText:{color:C.gold,fontSize:12,fontWeight:'700'},
 message:{padding:15,borderRadius:14,gap:9,borderWidth:1},aiMessage:{backgroundColor:C.surface,borderColor:C.border,marginRight:20},userMessage:{backgroundColor:C.redDark,borderColor:C.red,marginLeft:25},messageAuthor:{color:C.gold,fontSize:10,fontWeight:'900',letterSpacing:1},messageText:{color:C.text,fontSize:14,lineHeight:22},
 welcome:{borderWidth:1,borderColor:C.border,borderRadius:18,backgroundColor:C.surface,padding:25,alignItems:'center',gap:14,marginTop:15},composer:{padding:12,borderTopWidth:1,borderColor:C.border,backgroundColor:C.bg},send:{height:49,width:49,alignItems:'center',justifyContent:'center',backgroundColor:C.gold,borderRadius:13},
 lockBox:{flex:1,justifyContent:'center',padding:30,gap:17,alignItems:'center'},lockIcon:{width:92,height:92,borderRadius:46,alignItems:'center',justifyContent:'center',backgroundColor:C.redDark,borderWidth:1,borderColor:C.gold},brandBig:{fontSize:25,color:C.gold,fontWeight:'900',letterSpacing:2},lockSub:{fontSize:12,color:C.goldMuted,letterSpacing:2},error:{color:'#F19393',fontSize:13},footnote:{color:C.muted,fontSize:11,lineHeight:18,textAlign:'center'},
});
