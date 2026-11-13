package com.port.nexusclient;

import java.util.List;
import java.util.Set;

public final class NexusHearingSelectorsTest {
    static void ok(boolean value,String description){
        if(!value)throw new AssertionError(description);
    }
    public static void main(String[] args){
        List<String> consent=List.of("Alice","Bob");
        ok(NexusHearingSelectors.isSelector("@a"),"@a is a special local selector");
        ok(!NexusHearingSelectors.isSelector("@e"),"other selectors not supported");
        ok(NexusHearingSelectors.isEligible("ALICE",true,consent,List.of("Alice","Charlie")),
            "online approved player is eligible");
        ok(!NexusHearingSelectors.isEligible("Charlie",true,consent,List.of("Alice","Charlie")),
            "online non-consenter never eligible");
        ok(!NexusHearingSelectors.isEligible("Bob",true,consent,List.of("Alice")),
            "offline approved player excluded from collective");
        ok(NexusHearingSelectors.isEligible("Bob",true,consent,List.of("Alice","Bob")),
            "returning approved player included without granting new permission");
        ok(!NexusHearingSelectors.isEligible("Alice",true,consent,List.of()),
            "collective fails closed while online roster is unavailable");
        ok(NexusHearingSelectors.isEligible("Alice",false,consent,Set.of()),
            "direct player authorization works without collective mode");
        ok(!NexusHearingSelectors.isEligible("Charlie",false,consent,List.of("Charlie")),
            "direct mode still rejects unauthorized speaker");
        ok(NexusHearingSelectors.eligibleOnline(consent,List.of("Alice","Charlie")).equals(Set.of("alice")),
            "only the intersection appears in the collective");
        System.out.println("Nexus hearing @a selector tests: OK");
    }
}
