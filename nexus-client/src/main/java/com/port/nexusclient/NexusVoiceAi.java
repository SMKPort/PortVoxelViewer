package com.port.nexusclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Optional Gemini text conversation from VoiceCast. Audio stays in local Vosk.
 * Never grants the AI control over spells, Minecraft commands or macros.
 * Replies are local to the player; public chat is never triggered by this path.
 */
final class NexusVoiceAi {
    private static boolean busy;
    private static long lastAsk;
    private static String lastResult="Nenhuma pergunta enviada.";
    private NexusVoiceAi(){}

    static boolean enabled(){return ClientConfig.allowVoiceAi;}
    static String status(){
        return "VoiceCast Gemini: "+(enabled()?"ON":"OFF")+
            " | "+(busy?"Processando pergunta":"Livre")+
            " | resposta no chat local do jogador. "+lastResult;
    }
    static void setEnabled(boolean enabled){
        if(enabled&&(ClientConfig.token.isBlank()||!ClientConfig.allowChat))
            throw new IllegalStateException(
                "Pareie o Nexus e autorize /nexusclient chat on primeiro.");
        ClientConfig.allowVoiceAi=enabled;
        ClientConfig.save();
        NexusVoiceCasting.refresh();
        if(!enabled)busy=false;
        lastResult=enabled?
            "Diga 'Nexus, pergunte...' para conversar com Gemini. Feitiços continuam locais.":
            "Conversa Gemini desligada.";
    }
    static boolean active(){
        Minecraft mc=Minecraft.getInstance();
        return NexusVoiceAiPolicy.allowed(enabled(),
            !ClientConfig.token.isBlank(),ClientConfig.allowChat,
            mc.player!=null&&mc.getConnection()!=null&&mc.level!=null,
            NexusVoiceCasting.enabled(),mc.screen==null&&mc.isWindowActive());
    }
    static void ask(String utterance){
        String question=NexusVoiceAiPolicy.question(utterance);
        if(question==null||!active()||busy)return;
        long now=System.currentTimeMillis();
        if(now-lastAsk<6500)return;
        lastAsk=now;busy=true;
        lastResult="Pergunta recebida, aguardando Gemini.";
        NexusClientMod.notifyPlayer("VoiceCast IA: consultando Gemini...");
        JsonArray ownerMemories=new JsonArray();
        if(ClientConfig.allowGeminiMemory){
            List<NexusOwnerMemories.Memory> notes=NexusOwnerMemories.relevant(question,6);
            for(var note:notes){
                JsonObject item=new JsonObject();
                item.addProperty("topic",note.topic);
                item.addProperty("text",note.text);
                ownerMemories.add(item);
            }
        }
        ClientLink.askVoicecastGemini(question,ownerMemories);
    }
    static void answered(String text){
        busy=false;
        if(!enabled())return; // Revoked while the request was in flight.
        String answer=String.valueOf(text).replaceAll("[\\p{Cntrl}§]"," ").trim();
        if(answer.length()>380)answer=answer.substring(0,380);
        if(answer.isEmpty()){failed("A IA não retornou texto.");return;}
        lastResult="Gemini respondeu.";
        var player=Minecraft.getInstance().player;
        if(player!=null)player.displayClientMessage(
            Component.literal("§6§l[Nexus Aterna] §r§f"+answer),false);
        if(NexusSpeaker.settingEnabled())NexusSpeaker.onSpeech(answer,0);
    }
    static void failed(String reason){
        busy=false;
        if(!enabled())return;
        lastResult=String.valueOf(reason).replaceAll("[\\p{Cntrl}§]"," ").trim();
        NexusClientMod.notifyPlayer("VoiceCast IA: "+lastResult);
    }
    static void reset(){busy=false;lastAsk=0;}
}
