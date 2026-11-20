package com.port.nexusclient;

import java.util.List;

public final class NexusVoiceCandidatePolicyTest {
    private static void check(boolean value,String reason) {
        if(!value)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        var en=new NexusVoiceCandidatePolicy.Candidate("en","nexus divine smite",
            "spell:irons_spellbooks:divine_smite:3",.91f);
        var pt=new NexusVoiceCandidatePolicy.Candidate("pt","nexus divine smite",
            "spell:irons_spellbooks:divine_smite:3",.77f);
        var chosen=NexusVoiceCandidatePolicy.choose(List.of(pt,en),.58f);
        check(chosen.accepted()&&"en".equals(chosen.winner().language()),
            "English recognition may win without a translated alias");
        check(NexusVoiceCandidatePolicy.choose(List.of(pt,en),.58f).winner().intent()
            .equals("spell:irons_spellbooks:divine_smite:3"),
            "Same intent from two languages must dispatch only once");
        var falsePositive=new NexusVoiceCandidatePolicy.Candidate("pt",
            "nexus sunbeam","spell:irons_spellbooks:sunbeam:2",.87f);
        check(!NexusVoiceCandidatePolicy.choose(List.of(en,falsePositive),.58f).accepted(),
            "Conflicting valid spells from two recognizers must fail closed");
        var low=new NexusVoiceCandidatePolicy.Candidate("pt","nexus divine smite",
            "spell:irons_spellbooks:divine_smite:3",.22f);
        check(NexusVoiceCandidatePolicy.choose(List.of(low,en),.58f).winner()==en,
            "Low confidence candidates do not veto a high confidence result");
        check(!NexusVoiceCandidatePolicy.choose(List.of(low),.58f).accepted(),
            "Low confidence words never trigger a spell");
        check(!NexusVoiceCandidatePolicy.choose(List.of(),.58f).accepted(),
            "Empty recognition never acts");
        var unknown=new NexusVoiceCandidatePolicy.Candidate("en",
            "nexus unknown","",.99f);
        check(!NexusVoiceCandidatePolicy.choose(List.of(unknown),.58f).accepted(),
            "Unknown intents never trigger despite high ASR confidence");
        System.out.println("NexusVoiceCandidatePolicyTest OK");
    }
}
