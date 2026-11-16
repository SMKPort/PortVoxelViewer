package com.port.nexusclient;

public final class NexusBuffPolicyTest {
    private static void check(boolean ok,String description){
        if(!ok)throw new AssertionError(description);
    }
    public static void main(String[] args){
        long now=500_000;
        check(!NexusBuffPolicy.autoEligible(false,true,true,false,true,false,
            now,-1,120000,0),"Explicit automatic buff permission required");
        check(!NexusBuffPolicy.autoEligible(true,false,true,false,true,false,
            now,-1,120000,0),"Do not use buff outside combat");
        check(!NexusBuffPolicy.autoEligible(true,true,false,false,true,false,
            now,-1,120000,0),"No usage while other actions are active");
        check(!NexusBuffPolicy.autoEligible(true,true,true,true,true,false,
            now,-1,120000,0),"Never spend last consumable automatically");
        check(!NexusBuffPolicy.autoEligible(true,true,true,false,false,false,
            now,-1,120000,0),"Never renew an active buff");
        check(!NexusBuffPolicy.autoEligible(true,true,true,false,true,true,
            now,-1,120000,0),"No action while using another item");
        check(NexusBuffPolicy.autoEligible(true,true,true,false,true,false,
            now,-1,120000,0),"Configured buff may be used at start of combat");
        check(!NexusBuffPolicy.autoEligible(true,true,true,false,true,false,
            now,now-2000,120000,0),"No automatic double-consumption");
        check(!NexusBuffPolicy.autoEligible(true,true,true,false,true,false,
            now,-1,120000,2),"Max two automatic uses per combat");
        check(NexusBuffPolicy.manualEligible(true,false,now,-1),
            "Explicit manual item-use allowed without global auto permission");
        check(!NexusBuffPolicy.manualEligible(true,false,now,now-500),
            "Manual requests are throttled");
        check(NexusBuffPolicy.effectNeedsRefresh(true,false,true),
            "Use a potion only when its positive effect is missing");
        check(!NexusBuffPolicy.effectNeedsRefresh(true,false,false),
            "Avoid wasting consumables while the buff is active");
        check(!NexusBuffPolicy.effectNeedsRefresh(true,true,true),
            "Never auto-consume mixtures with negative effects");
        check(!NexusBuffPolicy.effectNeedsRefresh(false,false,true),
            "No positive effect means not a buff potion");
        System.out.println("NexusBuffPolicyTest OK");
    }
}
