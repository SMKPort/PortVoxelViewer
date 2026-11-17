package com.port.nexusclient;

import java.util.List;

public final class NexusUpkeepRosterPolicyTest {
    private static void check(boolean b,String description){
        if(!b)throw new AssertionError(description);
    }
    public static void main(String[] args){
        var equipped=List.of("somake:blessing","somake:seraphic_guard",
            "modded:holy_skin");
        check(NexusUpkeepRosterPolicy.mayAdd("somake:blessing",List.of(),equipped),
            "First equipped spell may be registered.");
        check(NexusUpkeepRosterPolicy.mayAdd("somake:seraphic_guard",
            List.of("somake:blessing"),equipped),
            "Second spell can be added without replacing Blessing.");
        check(!NexusUpkeepRosterPolicy.mayAdd("somake:blessing",
            List.of("somake:blessing"),equipped),
            "Duplicate spell registration rejected.");
        check(!NexusUpkeepRosterPolicy.mayAdd("other:spell",
            List.of(),equipped),"Spells not equipped are not allowed.");
        check(!NexusUpkeepRosterPolicy.mayAdd("invalid",
            List.of(),equipped),"Malformed spell ids fail closed.");
        check(!NexusUpkeepRosterPolicy.mayAdd("somake:blessing",
            List.of("a:a","b:b","c:c","d:d","e:e","f:f","g:g","h:h"),equipped),
            "Maximum eight tracked spells.");

        check(NexusUpkeepRosterPolicy.mayEnable(true,List.of("somake:blessing"),
            "server1","server1"),"Learned same-server spell can be enabled.");
        check(!NexusUpkeepRosterPolicy.mayEnable(false,List.of("somake:blessing"),
            "server1","server1"),"Must remain equipped.");
        check(!NexusUpkeepRosterPolicy.mayEnable(true,List.of(),
            "server1","server1"),"Never activate a spell without a learned effect.");
        check(!NexusUpkeepRosterPolicy.mayEnable(true,List.of("somake:blessing"),
            "server1","server2"),"Learned buffs cannot bleed between servers.");

        check(NexusUpkeepRosterPolicy.mayDispatch(100_000,0,4_000,false,false),
            "Can cast first owner-approved spell.");
        check(!NexusUpkeepRosterPolicy.mayDispatch(101_000,100_000,4_000,false,false),
            "Cannot cast second spell immediately after first.");
        check(NexusUpkeepRosterPolicy.mayDispatch(104_001,100_000,4_000,false,false),
            "Second spell may cast after the shared interval.");
        check(!NexusUpkeepRosterPolicy.mayDispatch(120_000,100_000,4_000,true,false),
            "Active user spellcast blocks upkeep.");
        check(!NexusUpkeepRosterPolicy.mayDispatch(120_000,100_000,4_000,false,true),
            "Item use blocks upkeep.");
        System.out.println("NexusUpkeepRosterPolicyTest OK");
    }
}
