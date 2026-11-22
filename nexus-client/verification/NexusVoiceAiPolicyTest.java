package com.port.nexusclient;

public final class NexusVoiceAiPolicyTest {
    private static void ok(boolean pass,String description){
        if(!pass)throw new AssertionError(description);
    }
    public static void main(String[] args){
        ok("quem e a eva?".equals(NexusVoiceAiPolicy.question("Nexus, pergunte quem é a Eva?")),
            "explicit AI wake phrase");
        ok("como estamos?".equals(NexusVoiceAiPolicy.question("Nexo responda como estamos?")),
            "alternate wake + conversational phrase");
        for(String unsafe:new String[]{"nexus divine smite","Nexus, cancelar",
            "nexus situação atual","nexus ativar macro","nexus","nexus pergunte",
            "qual a mana","nexus pergunte /op","nexus pergunte x"}){
            ok(NexusVoiceAiPolicy.question(unsafe)==null,
                "unqualified spell, narration or short requests never reach AI: "+unsafe);
        }
        ok(!NexusVoiceAiPolicy.allowed(false,true,true,true,true,true),
            "AI off by default");
        ok(!NexusVoiceAiPolicy.allowed(true,false,true,true,true,true),
            "paired client required");
        ok(!NexusVoiceAiPolicy.allowed(true,true,false,true,true,true),
            "owner-authorized chat required");
        ok(!NexusVoiceAiPolicy.allowed(true,true,true,true,false,true),
            "microphone opt in required");
        ok(!NexusVoiceAiPolicy.allowed(true,true,true,true,true,false),
            "do not capture during menus");
        ok(NexusVoiceAiPolicy.allowed(true,true,true,true,true,true),
            "consented in-game request");
        System.out.println("NexusVoiceAiPolicyTest OK");
    }
}
