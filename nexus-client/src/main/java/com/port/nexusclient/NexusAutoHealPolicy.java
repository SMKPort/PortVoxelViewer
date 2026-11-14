package com.port.nexusclient;

/**
 * Pure safety policy for owner-authorized, client-side automatic Heal.
 * Does not send packets, use a microphone, or call a remote API.
 */
final class NexusAutoHealPolicy {
    static final long RETRY_COOLDOWN_MS=35_000L;
    static final int MIN_PERCENT=10;
    static final int MAX_PERCENT=75;
    static final int DEFAULT_PERCENT=35;

    private NexusAutoHealPolicy(){}

    static int clampPercent(int value){
        return Math.max(MIN_PERCENT,Math.min(MAX_PERCENT,value));
    }

    static boolean shouldAttempt(boolean permission,boolean clientReady,
        boolean alreadyCasting,float health,float maximum,int thresholdPercent,
        long currentTime,long lastAttemptTime){
        if(!permission||!clientReady||alreadyCasting)return false;
        if(!Float.isFinite(health)||!Float.isFinite(maximum)||
            health<=0||maximum<=0||health>=maximum)return false;
        if(currentTime<0||currentTime-lastAttemptTime<RETRY_COOLDOWN_MS)return false;
        return (double)health/maximum*100<=clampPercent(thresholdPercent);
    }
}
