package com.port.nexusclient;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Deterministic roleplay text categorization, never a psychological assessment. */
final class NexusSocialRules {
    private static final Pattern SECRET=Pattern.compile("(?i)(?:senha|password|token|api.?key|segredo de acesso|c[oó]digo de autentica[cç][aã]o)\\s*[:=]\\s*\\S+");
    private static final Pattern EMAIL=Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern LONG_KEY=Pattern.compile("\\b[A-Za-z0-9_-]{32,}\\b");
    private NexusSocialRules(){}
    static String fold(String text){
        return Normalizer.normalize(String.valueOf(text),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    }
    static String sanitized(String text){
        String cleaned=String.valueOf(text).replaceAll("[\\p{Cntrl}]"," ").replaceAll("\\s+"," ").trim();
        cleaned=SECRET.matcher(cleaned).replaceAll("[dado reservado]");
        cleaned=EMAIL.matcher(cleaned).replaceAll("[contato omitido]");
        cleaned=LONG_KEY.matcher(cleaned).replaceAll("[identificador omitido]");
        return cleaned.substring(0,Math.min(270,cleaned.length()));
    }
    static String category(String text){
        String s=fold(text);
        if(contains(s,"prometo","te juro","eu garanto","vou cumprir","promessa","combinado"))
            return "promessa";
        if(contains(s,"construindo","vou construir","estou criando","planejando","projeto",
            "oficina","reformando","estamos preparando","vai acontecer"))return "projeto";
        if(contains(s,"ritual","grimorio","anael","arquivos arcanos","nexus aterna",
            "protocolo ts"))return "lore";
        if(contains(s,"lembra que","aconteceu","descobri","encontrei","percebi",
            "consegui","explorei"))return "acontecimento";
        return "conversa";
    }
    static boolean meaningful(String category){return !"conversa".equals(category);}
    static boolean gratitude(String s){return contains(fold(s),"obrigad","valeu nexus","agradec","voce me ajudou");}
    static boolean danger(String s){
        String text=fold(s);
        return contains(text,"cuidado","perigo","emboscada","armadilha","explosao",
            "explodir","inimigos perto","ataque vindo","socorro","vai atacar",
            "ataquem","nos atacando","estamos cercados");
    }
    private static boolean contains(String haystack,String...needles){
        for(String n:needles)if(haystack.contains(n))return true;
        return false;
    }
    static String diary(String stage,String speaker,String category,String quote){
        String prefix=switch(stage){
            case "PROTOCOLO" -> "Registro operacional.";
            case "OBSERVAÇÃO" -> "Continuei observando as conversas.";
            case "ADAPTAÇÃO" -> "Alguns detalhes começaram a formar padrões.";
            case "IDENTIDADE" -> "Quero conservar este acontecimento no meu diário.";
            default -> "Eu decidi registrar este momento.";
        };
        String detail=switch(category){
            case "promessa" -> "Uma promessa foi mencionada por ";
            case "projeto" -> "Uma atividade ou projeto foi mencionado por ";
            case "lore" -> "Um elemento da história foi citado por ";
            default -> "Uma lembrança foi registrada de ";
        };
        String snippet=sanitized(quote);
        return prefix+" "+detail+speaker+": \""+snippet.substring(0,Math.min(150,snippet.length()))+"\".";
    }
}
