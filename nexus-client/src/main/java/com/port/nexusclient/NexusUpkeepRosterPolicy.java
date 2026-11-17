package com.port.nexusclient;

import java.util.Collection;

/** Pure registration/activation rules for multiple independently maintained spells. */
final class NexusUpkeepRosterPolicy {
    static final int MAX_SPELLS=8;
    private NexusUpkeepRosterPolicy(){}

    static boolean mayAdd(String id,Collection<String> alreadyAdded,
                          Collection<String> equipped){
        return NexusBlessingPolicy.plausibleSpellId(id)&&alreadyAdded!=null&&
            equipped!=null&&equipped.contains(id)&&
            !alreadyAdded.contains(id)&&alreadyAdded.size()<MAX_SPELLS;
    }

    static boolean mayEnable(boolean equipped,Collection<String> learnedEffects,
                             String learnedRealm,String currentRealm){
        return equipped&&learnedEffects!=null&&!learnedEffects.isEmpty()&&
            learnedRealm!=null&&!learnedRealm.isBlank()&&
            currentRealm!=null&&learnedRealm.equals(currentRealm);
    }

    static boolean mayDispatch(long now,long lastAnyAttempt,long globalGapMs,
                               boolean casting,boolean itemInUse){
        return now>=0&&!casting&&!itemInUse&&
            (lastAnyAttempt<=0||now-lastAnyAttempt>=globalGapMs);
    }
}
