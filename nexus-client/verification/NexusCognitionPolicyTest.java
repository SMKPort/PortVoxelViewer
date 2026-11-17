package com.port.nexusclient;

public final class NexusCognitionPolicyTest {
    private static void check(boolean expression,String why){
        if(!expression)throw new AssertionError(why);
    }
    private static NexusCognitionPolicy.Facts facts(double life,double max,
        double mana,double maxMana,boolean heal,boolean equipped,
        double integrity,boolean monitored){
        return new NexusCognitionPolicy.Facts(life,max,17,mana,maxMana,
            integrity,monitored,false,heal,equipped,"minecraft:overworld");
    }
    public static void main(String[] args){
        check(NexusCognitionPolicy.percent(5,20)==25,"Correct life percent");
        check(NexusCognitionPolicy.percent(-1,100)==-1,"Missing readings remain unknown");
        check(NexusCognitionPolicy.percent(50,0)==-1,"No division by zero");
        check(NexusCognitionPolicy.percent(200,100)==100,"Overheal clamped");
        var critical=NexusCognitionPolicy.assess(
            facts(4,20,2,100,false,false,-1,false),25);
        check(critical.critical()&&"CRÍTICO".equals(critical.risk()),
            "Critical health assessed from direct client state");
        check(critical.notes().stream().anyMatch(s->s.contains("não autorizada")),
            "No claim of automatic casting when heal is not authorized");
        var healing=NexusCognitionPolicy.assess(
            facts(4,20,25,100,true,false,-1,false),25);
        check(healing.notes().stream().anyMatch(s->s.contains("não está equipado")),
            "Warn about missing healing spell after user has authorized auto heal");
        var healthy=NexusCognitionPolicy.assess(
            facts(20,20,80,100,false,false,-1,false),25);
        check(!healthy.critical()&&"ESTÁVEL".equals(healthy.risk()),
            "Healthy state is not marked as dangerous");
        var mana=NexusCognitionPolicy.assess(
            facts(20,20,10,100,false,false,-1,false),25);
        check("MANA BAIXA".equals(mana.risk())&&mana.manaPercent()==10,
            "Low mana is a contextual observation, not a cast trigger");
        var arcaneUnavailable=NexusCognitionPolicy.assess(
            facts(20,20,-1,-1,false,false,-1,false),25);
        check(arcaneUnavailable.manaPercent()==-1,
            "Missing mana is unknown, not falsely reported as 0%");
        var rp=NexusCognitionPolicy.assess(
            facts(20,20,-1,-1,false,false,20,true),25);
        check("ESTRUTURAL".equals(rp.risk())&&
            rp.notes().stream().anyMatch(s->s.contains("RP")),
            "RP integrity is labeled as narrative-only");
        var absent=NexusCognitionPolicy.assess(null,25);
        check(!absent.critical(),"No unsafe assumptions outside game");
        System.out.println("NexusCognitionPolicyTest OK");
    }
}
