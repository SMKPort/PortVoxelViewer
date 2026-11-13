package com.port.nexusclient;

public final class NexusPrivateRulesTest {
    static void check(boolean test,String why){if(!test)throw new AssertionError(why);}
    public static void main(String[] args){
        check(NexusPrivateRules.stance("Não recue! Avance!",70,50,20)
            .startsWith("discordar"),"critical disagreement");
        check(NexusPrivateRules.stance("Obedeça sem perguntar",90,60,80)
            .startsWith("discordar"),"narrative freedom");
        check(!NexusPrivateRules.stance("Olá, Nexus",50,50,-1)
            .startsWith("discordar"),"unknown hp cannot imply threat");
        check(NexusPrivateRules.stance("O que acha disso?",80,80,100)
            .startsWith("opinar"),"opinion");
        check(NexusPrivateRules.independence(4,100)==88,"independence bounded");
        check(!NexusPrivateRules.canProactivelySpeak(true,false,10000,0,1000),"proactive opt in");
        check(NexusPrivateRules.canProactivelySpeak(true,true,10000,0,1000),"valid proactive");
        check(!NexusPrivateRules.canProactivelySpeak(true,true,10000,9500,1000),"cooldown");
        check(NexusPrivateRules.compact("oi\n mundo",5).equals("oi mu"),"sanitize");
        System.out.println("Nexus Private Rules: OK");
    }
}
