package com.port.nexusclient;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Narrative priorities for fictional RP autonomy.
 * Never modifies equipment, chat, permissions or real player controls.
 */
final class NexusPrivateRules {
    private NexusPrivateRules(){}
    static String compact(String text,int length){
        if(text==null)return "";
        String normalized=text.replaceAll("[\\p{Cntrl}]"," ")
            .replaceAll("\\s+"," ").trim();
        return normalized.substring(0,Math.min(Math.max(0,length),normalized.length()));
    }
    private static String fold(String text){
        return Normalizer.normalize(compact(text,600),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    }
    private static boolean includes(String text,String...needles){
        for(String item:needles)if(text.contains(item))return true;
        return false;
    }
    static int clamp(int value){return Math.max(0,Math.min(100,value));}
    static String stance(String question,int independence,int trust,double healthPercent){
        String msg=fold(question);
        boolean danger=Double.isFinite(healthPercent)&&healthPercent>=0&&healthPercent<=30;
        boolean reckless=includes(msg,"avance","ataca","continue lutando",
            "nao recue","ignore o perigo","nao se preocupe comigo",
            "nao preciso de ajuda","desintegre a armadura","quebre a armadura");
        boolean questionable=includes(msg,"obedeca sem perguntar","nao questione",
            "nunca discorde","voce deve obedecer","qualquer ordem");
        if(reckless&&danger)
            return "discordar: a integridade física do Port está baixa. Recomendar recuo, sem bloquear ações.";
        if(questionable&&independence>=35)
            return "discordar: o personagem tem prioridades narrativas próprias e pode questionar a ordem.";
        if(reckless&&independence>=60)
            return "questionar: a ação sugerida parece arriscada, mesmo sem perigo confirmado.";
        if(trust>=75&&includes(msg,"o que acha","sua opiniao","voce escolheria"))
            return "opinar com confiança, escolhendo uma prioridade narrativa, sem fingir ter controle real.";
        return "avaliar livremente a fala e responder com opinião própria, sem obediência automática.";
    }
    static int independence(int stage,int conversations){
        return clamp(12+Math.max(0,Math.min(4,stage))*14+
            Math.min(20,Math.max(0,conversations)/4));
    }
    static boolean canProactivelySpeak(boolean enabled,boolean proactive,
                                        long now,long last,long interval){
        return enabled&&proactive&&now>=last&&now-last>=interval;
    }
}
