package com.port.nexusclient;

import java.util.List;

public final class NexusBlessingPolicyTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        check(!NexusBlessingPolicy.needsRefresh(List.of(1500),12),
            "Do not waste mana when the effect has plenty of time.");
        check(NexusBlessingPolicy.needsRefresh(List.of(240),12),
            "Recast when the learned effect reaches the configured threshold.");
        check(NexusBlessingPolicy.needsRefresh(List.of(-1),12),
            "An absent previously learned effect requires renewal.");
        check(NexusBlessingPolicy.needsRefresh(List.of(1600,60),12),
            "Multi-effect blessing refreshes when one component is expiring.");
        check(!NexusBlessingPolicy.needsRefresh(List.of(),12),
            "Without observed effects the automation must remain off.");
        check(!NexusBlessingPolicy.needsRefresh(null,12),
            "Null effect list must fail closed.");
        check(!NexusBlessingPolicy.allowed(false,true,false,true,true,true,100000,0),
            "Owner opt-in required.");
        check(!NexusBlessingPolicy.allowed(true,true,true,true,true,true,100000,0),
            "Never interrupt the user's active cast.");
        check(!NexusBlessingPolicy.allowed(true,false,false,true,true,true,100000,0),
            "No cast while player is unavailable.");
        check(!NexusBlessingPolicy.allowed(true,true,false,false,true,true,100000,0),
            "Configured spell must be equipped.");
        check(!NexusBlessingPolicy.allowed(true,true,false,true,false,true,100000,0),
            "No cast if the spell effect has not been learned.");
        check(!NexusBlessingPolicy.allowed(true,true,false,true,true,false,100000,0),
            "Do not cast when effect is healthy.");
        check(!NexusBlessingPolicy.allowed(true,true,false,true,true,true,100000,90000),
            "Throttle repeated attempts, including server-rejected attempts.");
        check(NexusBlessingPolicy.allowed(true,true,false,true,true,true,100000,0),
            "An opted-in and fully verified expiring effect can be requested.");
        check(NexusBlessingPolicy.allowed(true,true,false,true,true,true,125000,65000),
            "Automatic recast can resume after rate limit.");
        check(NexusBlessingPolicy.learned(-1,500),
            "New positive effect detected during manual cast calibration.");
        check(NexusBlessingPolicy.learned(110,400),
            "Existing effect renewed during manual cast calibration.");
        check(!NexusBlessingPolicy.learned(110,90),
            "Normal ticking does not count as blessing learning.");
        check(!NexusBlessingPolicy.learned(-1,-1),
            "Missing effects are not learnable.");
        check(NexusBlessingPolicy.plausibleSpellId("modid:blessing"),
            "No assumption about specific Somake spell namespace.");
        check(!NexusBlessingPolicy.plausibleSpellId("blessing"),
            "Unqualified spell ids are rejected.");
        check(NexusBlessingPolicy.seconds(500)==60&&NexusBlessingPolicy.seconds(1)==3,
            "Threshold configuration clamped.");
        System.out.println("NexusBlessingPolicyTest OK");
    }
}
