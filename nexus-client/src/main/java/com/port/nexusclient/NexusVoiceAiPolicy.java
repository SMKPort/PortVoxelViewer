package com.port.nexusclient;

import java.text.Normalizer;
import java.util.Locale;

/** Strict voice-only request gate: no AI may infer a spell or Minecraft command. */
final class NexusVoiceAiPolicy {
    private NexusVoiceAiPolicy(){}
    static String question(String spoken){
        if(spoken==null||spoken.length()>650)return null;
        String normalized=Normalizer.normalize(spoken,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[\\p{Cntrl}]"," ").replaceAll("\\s+"," ").trim();
        for(String wake:new String[]{"nexus","nexo","nexos"}){
            for(String verb:new String[]{"pergunte","pergunta","responda","converse","conversar"}){
                String prefix=wake+" "+verb+" ";
                if(!normalized.startsWith(prefix))continue;
                String q=normalized.substring(prefix.length()).trim();
                if(q.length()<3||q.length()>400||q.startsWith("/")||
                   q.contains("\n")||q.contains("\r"))return null;
                return q;
            }
        }
        return null;
    }
    static boolean allowed(boolean enabled,boolean paired,boolean chatAuthorized,
                           boolean inWorld,boolean voiceActive,boolean screenClosed){
        return enabled&&paired&&chatAuthorized&&inWorld&&voiceActive&&screenClosed;
    }
}
