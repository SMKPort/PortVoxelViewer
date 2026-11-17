package com.port.nexusclient;

import java.util.Collection;

/** Pure safety decisions for owner-approved ongoing spell effects. */
final class NexusBlessingPolicy {
    static final int DEFAULT_SECONDS=12;
    static final int MIN_SECONDS=3,MAX_SECONDS=60;
    static final long CHECK_INTERVAL_MS=1_000L;
    static final long RETRY_DELAY_MS=60_000L;
    static final long LEARN_WINDOW_MS=30_000L;

    private NexusBlessingPolicy(){}

    static int seconds(int value) {
        return Math.max(MIN_SECONDS,Math.min(MAX_SECONDS,value));
    }

    /** Negative duration means a previously learned effect is now absent. */
    static boolean needsRefresh(Collection<Integer> remainingTicks,int thresholdSeconds) {
        if(remainingTicks==null||remainingTicks.isEmpty())return false;
        int limit=seconds(thresholdSeconds)*20;
        for(int duration:remainingTicks)
            if(duration<0||duration<=limit)return true;
        return false;
    }

    static boolean allowed(boolean enabled,boolean ownerPresent,boolean isCasting,
                           boolean equipped,boolean effectsLearned,boolean expiring,
                           long now,long lastAttempt) {
        if(!enabled||!ownerPresent||isCasting||!equipped||!effectsLearned||!expiring)
            return false;
        if(now<0)return false;
        return lastAttempt<=0||now-lastAttempt>=RETRY_DELAY_MS;
    }

    /** An explicit learning session must observe an effect appear or renew. */
    static boolean learned(int beforeTicks,int afterTicks) {
        if(afterTicks<=0)return false;
        if(beforeTicks<0)return true;
        return afterTicks-beforeTicks>=40;
    }

    static boolean plausibleSpellId(String source) {
        return source!=null&&source.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }
}
