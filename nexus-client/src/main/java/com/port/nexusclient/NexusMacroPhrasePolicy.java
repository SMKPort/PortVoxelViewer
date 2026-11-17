package com.port.nexusclient;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Exact, wake-word-gated phrases for opt-in combat macros.
 * The recognizer does not use generative language or fuzzy intent matching.
 */
final class NexusMacroPhrasePolicy {
    static final int MAX_ACTIVATION=55,MAX_ANNOUNCEMENT=160;
    private static final List<String> WAKE=List.of("nexus","nexo","nexos");
    private static final Set<String> RESERVED=Set.of(
        "cancelar","cancele","cancel","execute","executa","lance","cast",
        "ativar","ativar macro","macro","stop","parar");
    private NexusMacroPhrasePolicy(){}

    static String normalize(String text){
        if(text==null)return "";
        return Normalizer.normalize(text,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim().replaceAll("\\s+"," ");
    }
    static String activation(String raw){
        if(raw==null)return "";
        String result=normalize(raw);
        for(String wake:WAKE)
            if(result.startsWith(wake+" ")){
                result=result.substring(wake.length()+1);
                break;
            }
        return result;
    }
    static boolean validActivation(String raw){
        String text=activation(raw);
        if(text.length()<4||text.length()>MAX_ACTIVATION||
            RESERVED.contains(text)||text.startsWith("cancelar ")||
            text.startsWith("cancele ")||text.startsWith("cancel ")||
            text.startsWith("viajar ")||text.startsWith("teletransportar "))
            return false;
        for(String wake:WAKE)
            if(text.equals(wake)||text.startsWith(wake+" "))return false;
        return true;
    }
    static String announcement(String raw){
        if(raw==null)return "";
        return raw.replaceAll("[\\p{Cntrl}§]"," ").trim();
    }
    static boolean validAnnouncement(String raw){
        return raw!=null&&announcement(raw).length()<=MAX_ANNOUNCEMENT;
    }
    static Map<String,String> index(Map<String,String> macroToActivation) {
        Map<String,String> phrases=new LinkedHashMap<>();
        if(macroToActivation==null)return phrases;
        for(var entry:macroToActivation.entrySet()){
            if(entry.getKey()==null||!validActivation(entry.getValue()))continue;
            String phrase=activation(entry.getValue());
            for(String wake:WAKE){
                String command=wake+" "+phrase;
                String prior=phrases.putIfAbsent(command,entry.getKey());
                if(prior!=null&&!prior.equals(entry.getKey()))
                    phrases.put(command,""); // ambiguous commands cannot launch
            }
        }
        phrases.entrySet().removeIf(e->e.getValue().isBlank());
        return Map.copyOf(phrases);
    }
    static String interpret(String recognized,Map<String,String> phrases){
        return phrases==null?null:phrases.get(normalize(recognized));
    }
}
