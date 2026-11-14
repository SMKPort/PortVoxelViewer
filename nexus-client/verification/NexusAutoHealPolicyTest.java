package com.port.nexusclient;

public final class NexusAutoHealPolicyTest {
    private static void check(boolean condition,String name){
        if(!condition)throw new AssertionError(name);
    }
    public static void main(String[] args){
        long now=1_000_000L,old=now-40_000L;
        check(!NexusAutoHealPolicy.shouldAttempt(false,true,false,5,20,35,now,old),
            "Owner permission is mandatory");
        check(!NexusAutoHealPolicy.shouldAttempt(true,false,false,5,20,35,now,old),
            "World and player must be ready");
        check(!NexusAutoHealPolicy.shouldAttempt(true,true,true,5,20,35,now,old),
            "Never override a spell already being cast");
        check(NexusAutoHealPolicy.shouldAttempt(true,true,false,6,20,35,now,old),
            "30% health triggers a configured 35% threshold");
        check(NexusAutoHealPolicy.shouldAttempt(true,true,false,7,20,35,now,old),
            "Exactly 35% health triggers healing");
        check(!NexusAutoHealPolicy.shouldAttempt(true,true,false,8,20,35,now,old),
            "Above threshold does not trigger healing");
        check(!NexusAutoHealPolicy.shouldAttempt(true,true,false,0,20,35,now,old),
            "Dead player never triggers healing");
        check(!NexusAutoHealPolicy.shouldAttempt(true,true,false,6,0,35,now,old),
            "Zero maximum life fails closed");
        check(!NexusAutoHealPolicy.shouldAttempt(true,true,false,Float.NaN,20,35,now,old),
            "Invalid health fails closed");
        check(!NexusAutoHealPolicy.shouldAttempt(true,true,false,6,20,35,now,now-2_000),
            "No retry spam while on 35-second cooldown");
        check(NexusAutoHealPolicy.shouldAttempt(true,true,false,6,20,35,now,
            now-NexusAutoHealPolicy.RETRY_COOLDOWN_MS),
            "Retry allowed only after the entire cooldown interval");
        check(NexusAutoHealPolicy.clampPercent(-100)==10,"Minimum threshold enforced");
        check(NexusAutoHealPolicy.clampPercent(150)==75,"Maximum threshold enforced");
        check(NexusAutoHealPolicy.clampPercent(35)==35,"Valid threshold retained");
        System.out.println("NexusAutoHealPolicyTest OK");
    }
}
