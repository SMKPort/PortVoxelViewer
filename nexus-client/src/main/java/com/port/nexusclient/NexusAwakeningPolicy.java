package com.port.nexusclient;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Deterministic interpretation of actually observed local telemetry. */
final class NexusAwakeningPolicy {
    enum Risk { UNKNOWN, STABLE, CAUTION, CRITICAL }
    record Reading(double health,double maxHealth,double manaPercent,boolean manaKnown) {}
    private NexusAwakeningPolicy(){}

    static int healthPercent(Reading reading){
        if(reading==null||!Double.isFinite(reading.health())||
           !Double.isFinite(reading.maxHealth())||reading.maxHealth()<=0)return -1;
        return (int)Math.round(Math.max(0,Math.min(100,
            100*reading.health()/reading.maxHealth())));
    }
    static Risk risk(Reading reading,int criticalThreshold){
        int hp=healthPercent(reading);
        if(hp<0)return Risk.UNKNOWN;
        if(hp<=criticalThreshold)return Risk.CRITICAL;
        if(hp<=Math.min(75,criticalThreshold+25))return Risk.CAUTION;
        return Risk.STABLE;
    }
    static boolean holdOffense(boolean enabled,Reading reading,int threshold){
        return enabled&&healthPercent(reading)>=0&&
            healthPercent(reading)<=threshold;
    }
    static boolean releaseOffense(boolean enabled,Reading reading,int threshold){
        return !enabled||(healthPercent(reading)>threshold+5);
    }
    static String manaDescription(Reading reading){
        if(reading==null||!reading.manaKnown()||
           !Double.isFinite(reading.manaPercent())||
           reading.manaPercent()<0)return "Mana: sem leitura disponível.";
        int amount=(int)Math.round(Math.max(0,Math.min(100,reading.manaPercent())));
        return "Mana: "+amount+"% (leitura do Iron's Spells).";
    }
    static String healthDescription(Reading reading){
        int hp=healthPercent(reading);
        return hp<0?"Vida: leitura indisponível.":"Vida: "+hp+"% da vida máxima.";
    }
    static String suggestion(Risk risk,boolean healAuthorized){
        return switch(risk){
            case CRITICAL -> healAuthorized?
                "Risco crítico. Recomendo recuar. Cura automática já autorizada.":
                "Risco crítico. Recomendo recuar e avaliar uma cura.";
            case CAUTION -> "Atenção à integridade vital. Evite exposição desnecessária.";
            case STABLE -> "Integridade vital estável no instante da leitura.";
            case UNKNOWN -> "Sem dados suficientes para avaliar sua integridade vital.";
        };
    }
    static String voiceCommand(String utterance){
        if(utterance==null)return null;
        String norm=Normalizer.normalize(utterance,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim().replaceAll("\\s+"," ");
        for(String wake:List.of("nexus","nexo","nexos")){
            if(norm.equals(wake+" situacao atual")||
               norm.equals(wake+" estado atual")||
               norm.equals(wake+" avaliar situacao"))return "report";
            if(norm.equals(wake+" recordar memorias")||
               norm.equals(wake+" minhas memorias"))return "memories";
        }
        return null;
    }
    static List<String> voiceVocabulary(){
        return List.of(
            "nexus situacao atual","nexo situacao atual","nexos situacao atual",
            "nexus estado atual","nexo estado atual","nexos estado atual",
            "nexus avaliar situacao","nexo avaliar situacao","nexos avaliar situacao",
            "nexus recordar memorias","nexo recordar memorias","nexos recordar memorias",
            "nexus minhas memorias","nexo minhas memorias","nexos minhas memorias");
    }
}
