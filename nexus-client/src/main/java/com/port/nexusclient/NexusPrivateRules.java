package com.port.nexusclient;

import java.text.Normalizer;
import java.util.Locale;

/** Only fictional decision rules, not intelligence or server authority. */
final class NexusPrivateRules {
    private NexusPrivateRules(){}
    static String safeText(String raw,int max){
        if(raw==null)return "";
        String s=raw.replaceAll("[\\p{Cntrl}]"," ").replaceAll("\\s+"," ").trim();
        s=s.replaceAll("(?i)\\b(?:senha|password|api.?key|token)\\s*[:=]\\s*\\S+","[dado reservado]");
        return s.substring(0,Math.min(Math.max(max,0),s.length()));
    }
    static boolean shouldQuestion(String text,int independence){
        String s=normalize(text);
        boolean demand=contains(s,"obedeca","e uma ordem","sem discutir","voce tem que","faca mesmo assim");
        return demand&&independence>=30;
    }
    static String opinion(int independence,int caution,int trust){
        if(independence>=70&&caution>=55)return "independente, protetor e disposto a discordar quando há evidências de risco";
        if(independence>=40)return "curioso e capaz de formular opiniões próprias, com argumentos proporcionais às evidências";
        if(trust>=55)return "colaborativo, mas não obrigado a concordar automaticamente";
        return "observador e reservado, questiona com respeito e não inventa certezas";
    }
    static String dangerThought(double lifeRatio){
        if(!Double.isFinite(lifeRatio)||lifeRatio<0||lifeRatio>1)return null;
        if(lifeRatio<=0.22)
            return "Port, a leitura da sua vida está crítica. Gostaria que considerasse recuar antes de prosseguir.";
        if(lifeRatio<=0.38)
            return "Port, seus sinais vitais caíram. Talvez seja melhor avaliar nossa próxima decisão em particular.";
        return null;
    }
    static String arcaneThought(String event,String observed){
        if("mana_low".equals(event))
            return "Port, observei sua mana cair. Sugiro reservar energia antes de uma nova conjuração.";
        if("holy_power".equals(event))
            return "Port, detectei uma mudança no atributo Holy. Quero acompanhar se ela se repete.";
        return null;
    }
    static String normalize(String raw){
        return Normalizer.normalize(safeText(raw,400).toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");
    }
    private static boolean contains(String hay,String...terms){
        for(String t:terms)if(hay.contains(t))return true;
        return false;
    }
}
