package com.port.nexusclient;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/** Pure rules for an explicitly approved, ordered combo spell sequence. */
final class NexusMacroSequencePolicy {
    static final int MAX_SPELLS=6;
    static final long BETWEEN_CASTS_MS=6_000L;
    static final long MAX_SEQUENCE_MS=120_000L;
    private NexusMacroSequencePolicy(){}

    static boolean validSequence(List<String> spells,String anchorSpell){
        if(spells==null||spells.isEmpty()||spells.size()>MAX_SPELLS||
            anchorSpell==null||!anchorSpell.equals(spells.getFirst()))return false;
        HashSet<String> unique=new HashSet<>();
        for(String id:spells)
            if(!NexusMacroRosterPolicy.validSpell(id)||!unique.add(id))return false;
        return true;
    }
    static boolean canAppend(String spell,Collection<String> current,
                             Collection<String> equipped){
        return NexusMacroRosterPolicy.validSpell(spell)&&current!=null&&
            equipped!=null&&equipped.contains(spell)&&!current.contains(spell)&&
            current.size()<MAX_SPELLS;
    }
    static boolean canRemove(String spell,List<String> current){
        return current!=null&&current.size()>1&&!spell.equals(current.getFirst())&&
            current.contains(spell);
    }
    static boolean canRequest(long now,long notBefore,long lastCastAt,
                              long gap,boolean currentlyCasting){
        return now>=0&&!currentlyCasting&&now>=notBefore&&
            (lastCastAt<0||now-lastCastAt>=gap);
    }
    static boolean expired(long now,long startedAt){
        return startedAt<0||now<startedAt||now-startedAt>MAX_SEQUENCE_MS;
    }
}
