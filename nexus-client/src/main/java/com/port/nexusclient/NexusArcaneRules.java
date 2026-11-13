package com.port.nexusclient;

import java.util.Locale;

/** Pure rules; no reflective Iron's Spells calls or game state mutation. */
final class NexusArcaneRules {
    private NexusArcaneRules(){}
    static double bounded(double value,double min,double max,double fallback){
        return Double.isFinite(value)?Math.min(max,Math.max(min,value)):fallback;
    }
    static double percent(double mana,double maxMana){
        if(!Double.isFinite(mana)||!Double.isFinite(maxMana)||maxMana<=0)return -1;
        return bounded(mana*100.0/maxMana,0,100,-1);
    }
    static boolean manaLow(double percentage){return percentage>=0&&percentage<=22.0;}
    static boolean manaRecovered(double percentage){return percentage>=36.0;}
    static String spellLabel(String registryId){
        String id=String.valueOf(registryId);
        int sep=id.indexOf(':');
        String path=sep>=0?id.substring(sep+1):id;
        if(path.isBlank()||path.length()>90||!path.matches("[a-z0-9_/.-]+"))
            return "Feitiço não identificado";
        String title=path.substring(path.lastIndexOf('/')+1).replace('_',' ').replace('-',' ');
        if(title.isEmpty())return "Feitiço não identificado";
        return Character.toUpperCase(title.charAt(0))+title.substring(1);
    }
    static boolean sameSpell(String left,String right){
        return String.valueOf(left).equalsIgnoreCase(String.valueOf(right));
    }
    static String eventTitle(String type,String spell,String school,int level){
        String name=spellLabel(spell);
        return switch(type){
            case "cast_start" -> "Conjuração iniciada: "+name+" (nível "+Math.max(0,level)+")";
            case "cast_end" -> "Canalização encerrada: "+name;
            case "mana_low" -> "Reserva de mana baixa";
            case "mana_recovered" -> "Reserva de mana estabilizada";
            case "holy_power" -> "Potência da escola Holy alterada";
            default -> "Leitura arcana";
        };
    }
    static String reply(String type,String spell,double percentage){
        if("mana_low".equals(type))
            return "Port, sua reserva de mana caiu para aproximadamente "+
                Math.round(Math.max(0,percentage))+"%. Sugiro preservar energia arcana.";
        if("holy_power".equals(type))
            return "Port, os atributos de poder da escola Holy sofreram uma alteração detectável.";
        if("cast_start".equals(type))
            return "Conjuração identificada: "+spellLabel(spell)+". Monitorando o fluxo arcano.";
        return "";
    }
}
