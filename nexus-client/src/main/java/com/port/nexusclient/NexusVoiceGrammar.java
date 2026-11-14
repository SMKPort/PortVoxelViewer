package com.port.nexusclient;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Offline, exact wake-word grammar: no generative model controls game actions. */
final class NexusVoiceGrammar {
    record Spell(String id,int slot){}
    record Command(boolean cancel,String spellId,int slot,String phrase){}
    private NexusVoiceGrammar(){}

    static String normalize(String source){
        if(source==null)return "";
        return Normalizer.normalize(source,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim().replaceAll("\\s+"," ");
    }

    static List<String> names(String spellId){
        if(spellId==null||!spellId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            return List.of();
        String id=spellId.substring(spellId.indexOf(':')+1).replaceAll("[^a-z0-9_]+"," ");
        String spaced=normalize(id.replace('_',' '));
        String combined=normalize(id.replace("_",""));
        LinkedHashSet<String> result=new LinkedHashSet<>();
        if(!spaced.isBlank())result.add(spaced);
        if(!combined.isBlank())result.add(combined);
        // Common English spell names spoken with a Brazilian Portuguese accent.
        if(spellId.endsWith(":sunbeam")){
            result.add("sun beam");result.add("san bim");
        }
        return List.copyOf(result);
    }

    static Map<String,Spell> phrases(List<Spell> equipped){
        Map<String,Spell> out=new LinkedHashMap<>();
        Set<String> ambiguous=new LinkedHashSet<>();
        if(equipped==null)return out;
        for(Spell spell:equipped){
            if(spell==null||spell.slot<0||spell.slot>255)continue;
            for(String name:names(spell.id)){
                for(String wake:List.of("nexus","nexo","nexos")){
                    for(String instruction:List.of("execute ","execute o ","execute a ",
                        "executa ","executa o ","cast ","lancar ","lance ")){
                        String phrase=normalize(wake+" "+instruction+name);
                        Spell previous=out.putIfAbsent(phrase,spell);
                        if(previous!=null&&!previous.id.equals(spell.id))
                            ambiguous.add(phrase);
                    }
                }
            }
        }
        for(String phrase:ambiguous)out.remove(phrase);
        return Map.copyOf(out);
    }

    static List<String> vocabulary(Map<String,Spell> index){
        List<String> list=new ArrayList<>(index.keySet());
        for(String wake:List.of("nexus","nexo","nexos")){
            list.add(wake+" cancelar");
            list.add(wake+" cancele");
            list.add(wake+" cancel");
        }
        list.add("[unk]");
        return list;
    }

    static Command recognize(String spoken,Map<String,Spell> index){
        String phrase=normalize(spoken);
        if(phrase.matches("(nexus|nexo|nexos) (cancelar|cancele|cancel)"))
            return new Command(true,"",-1,phrase);
        Spell spell=index.get(phrase);
        return spell==null?null:new Command(false,spell.id,spell.slot,phrase);
    }
}
