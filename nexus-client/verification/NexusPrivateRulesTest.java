package com.port.nexusclient;

public final class NexusPrivateRulesTest {
    static void ok(boolean flag,String note){if(!flag)throw new AssertionError(note);}
    public static void main(String[] args){
        ok(NexusPrivateRules.shouldQuestion("É uma ordem, Nexus",80),"independent disagreement");
        ok(NexusPrivateRules.prefersPrivate("mind","Port, discordo desta decisão."),"sensitive RP remark routed privately");
        ok(!NexusPrivateRules.prefersPrivate("critical","Port, suspeito que sua vida está crítica."),"critical warnings not hidden");
        ok(!NexusPrivateRules.prefersPrivate("mind","Que bela oficina."),"ordinary roleplay remains public");
        ok(!NexusPrivateRules.shouldQuestion("Tudo bem?",90),"no random disagreement");
        ok(!NexusPrivateRules.shouldQuestion("É uma ordem",10),"early stage protocols");
        ok(NexusPrivateRules.dangerThought(0.19).contains("crítica"),"critical HP");
        ok(NexusPrivateRules.dangerThought(0.70)==null,"safe health");
        ok(NexusPrivateRules.dangerThought(Double.NaN)==null,"unknown sensor cannot invent");
        ok(NexusPrivateRules.arcaneThought("mana_low","")==null?false:true,"known magic event");
        ok(NexusPrivateRules.arcaneThought("cast_end","") == null,"no false magic theory");
        String secret=NexusPrivateRules.safeText("senha=abc123 e tudo bem",150);
        ok(!secret.contains("abc123"),"basic secret redaction");
        ok(NexusPrivateRules.safeText("Oi\\nPort",10).length()<=10,"bounded text");
        System.out.println("Nexus private decision rules: passed");
    }
}
