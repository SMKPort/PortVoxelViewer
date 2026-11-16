package com.port.nexusclient;

/** Deterministic safeguards for opt-in hotbar buff automation. */
final class NexusBuffPolicy {
    static final int MAX_REGISTERED=8;
    static final int MAX_AUTO_USES_PER_COMBAT=2;
    static final long COMBAT_ACTIVE_MS=12_000L;
    static final long COMBAT_RESET_MS=20_000L;
    static final long MIN_DELAY_MS=1_800L;
    static final long AUTO_COOLDOWN_MS=120_000L;
    static final long MANUAL_COOLDOWN_MS=2_000L;

    private NexusBuffPolicy(){}

    static boolean autoEligible(boolean ownerPermission,boolean inCombat,
        boolean ready,boolean isLastItem,boolean buffNeeded,
        boolean alreadyUsing,long now,long lastUse,long cooldown,int usedThisCombat){
        if(!ownerPermission||!inCombat||!ready||isLastItem||
            !buffNeeded||alreadyUsing||usedThisCombat>=MAX_AUTO_USES_PER_COMBAT)return false;
        if(now<0||lastUse>now)return false;
        return lastUse<0||now-lastUse>=Math.max(AUTO_COOLDOWN_MS,cooldown);
    }

    static boolean manualEligible(boolean ready,boolean alreadyUsing,
        long now,long lastUse){
        return ready&&!alreadyUsing&&now>=0&&
            (lastUse<0||(lastUse<=now&&now-lastUse>=MANUAL_COOLDOWN_MS));
    }

    static boolean effectNeedsRefresh(boolean anyPositive,boolean anyNegative,
        boolean oneMissingOrExpiring){
        return anyPositive&&!anyNegative&&oneMissingOrExpiring;
    }
}
