package com.port.nexusclient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure, testable logic for safe OpenAL microphone choices. No audio capture here. */
final class NexusMicrophoneChoices {
    static final String SYSTEM_DEFAULT="";

    private NexusMicrophoneChoices(){}

    static String normalize(String raw){
        if(raw==null||raw.isBlank())return SYSTEM_DEFAULT;
        String result=raw.replaceAll("[\\p{Cntrl}]", " ").trim();
        return result.length()>200?result.substring(0,200):result;
    }

    static List<String> unique(List<String> raw){
        Set<String> seen=new LinkedHashSet<>();
        if(raw!=null)for(String name:raw){
            String cleaned=normalize(name);
            if(!cleaned.isBlank())seen.add(cleaned);
        }
        return List.copyOf(seen);
    }

    static boolean available(String selected,List<String> available){
        String chosen=normalize(selected);
        return chosen.isBlank()||unique(available).contains(chosen);
    }

    static List<String> withDefault(List<String> available){
        List<String> options=new ArrayList<>();
        options.add(SYSTEM_DEFAULT);
        options.addAll(unique(available));
        return List.copyOf(options);
    }
}
