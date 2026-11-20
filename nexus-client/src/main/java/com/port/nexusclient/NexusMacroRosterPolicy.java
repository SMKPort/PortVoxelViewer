package com.port.nexusclient;

import java.util.Collection;

/** Pure rules for multi-combo macro selection; no Minecraft dependency. */
final class NexusMacroRosterPolicy {
    static final int MAX_MACROS=8;
    private NexusMacroRosterPolicy(){}

    static boolean validSpell(String id){
        return id!=null&&id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    static boolean mayAdd(String spell,Collection<String> registered,
                          Collection<String> equipped){
        return validSpell(spell)&&registered!=null&&equipped!=null&&
            !registered.contains(spell)&&registered.size()<MAX_MACROS&&
            equipped.contains(spell);
    }

    /** Suppress all extra spell requests while another macro recently fired. */
    static boolean mayTrigger(long now,long lastTrigger,long minGapMillis){
        return now>=0&&(lastTrigger<0||now-lastTrigger>=minGapMillis);
    }
}
