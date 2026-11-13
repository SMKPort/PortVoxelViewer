package com.port.nexusclient;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Treats @a as a Nexus-local selector over already-consenting players.
 * It NEVER grants consent or scans unapproved players.
 */
final class NexusHearingSelectors {
    private NexusHearingSelectors(){}

    static boolean isEligible(String speaker,boolean collective,
                              Collection<String> consented,Collection<String> online){
        if(speaker==null||consented==null||speaker.isBlank())return false;
        boolean approved=false;
        for(String nick:consented)
            if(nick!=null&&nick.equalsIgnoreCase(speaker)){approved=true;break;}
        if(!approved)return false;
        if(!collective)return true;
        if(online==null)return false;
        for(String nick:online)
            if(nick!=null&&nick.equalsIgnoreCase(speaker))return true;
        return false;
    }

    static Set<String> eligibleOnline(Collection<String> consented,Collection<String> online){
        Set<String> result=new HashSet<>();
        if(online==null)return Set.of();
        for(String name:online)
            if(isEligible(name,true,consented,online))
                result.add(name.toLowerCase(Locale.ROOT));
        return Set.copyOf(result);
    }

    static boolean isSelector(String text){return "@a".equals(text);}
}
