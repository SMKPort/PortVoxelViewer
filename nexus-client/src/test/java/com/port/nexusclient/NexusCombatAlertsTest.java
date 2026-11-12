package com.port.nexusclient;

/** Testes sem dependência de Minecraft nem JUnit: java -ea NexusCombatAlertsTest. */
public final class NexusCombatAlertsTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static NexusCombatAlerts.Decision sample(
        NexusCombatAlerts alerts,String type,float amount,float health,
        double before,double after,int stage,long time){
        return alerts.observe(type,amount,health,20,before,after,stage,true,time);
    }
    private static void highDamageAndCooldown(){
        var alerts=new NexusCombatAlerts();
        var first=sample(alerts,"damage",6,14,100,70,0,1000);
        check(first!=null&&first.kind.equals("heavy"),"Expected high-damage warning");
        check(first.message.contains("Port"),"Warning must address Port");
        alerts.delivered(first,1000);
        check(sample(alerts,"damage",7,7,70,35,3,5000)==null,
            "Global cooldown must suppress repeated spam");
        var next=sample(alerts,"damage",6,6,35,20,4,48000);
        check(next!=null&&next.kind.equals("structure"),"Critical plates take priority");
        check(next.message.contains("Port"),"High-stage warning must address Port");
        alerts.delivered(next,48000);
        check(sample(alerts,"damage",6,6,20,15,4,95000)!=null,
            "Other threats should remain available without repeating critical threshold");
    }
    private static void sustainedAttackAndRisk(){
        var alerts=new NexusCombatAlerts();
        check(sample(alerts,"damage",1,18,100,95,2,1000)==null,"One hit should not warn");
        check(sample(alerts,"damage",1,17,95,90,2,5000)==null,"Two hits should not warn");
        var third=sample(alerts,"damage",1,16,90,85,2,9000);
        check(third!=null&&third.kind.equals("streak"),"Three hits should warn");
        alerts.delivered(third,9000);
        alerts.reset();
        var risk=sample(alerts,"damage",2,7,50,40,4,1000);
        check(risk!=null&&risk.kind.equals("risk"),"Combined low HP and integrity should warn");
        check(sample(alerts,"death",0,0,40,0,4,3000)==null,
            "Death must reset hit tracking");
    }
    private static void recoveryAndReset(){
        var alerts=new NexusCombatAlerts();
        check(sample(alerts,"heal",1,12,50,52,1,1000)==null,
            "Small heal should not spam");
        var second=sample(alerts,"heal",2,14,52,56,1,5000);
        check(second!=null&&second.kind.equals("recovery"),"Accumulated repair should warn");
        alerts.delivered(second,5000);
        check(sample(alerts,"heal",2,16,56,62,1,6000)==null,"Repair cooldown must apply");
        alerts.reset();
        check(sample(alerts,"heal",1,17,62,64,1,9000)==null,
            "Session reset clears recovery accumulator");
        check(alerts.observe("heal",4,20,20,64,80,4,false,12000)==null,
            "No structural-recovery warning without configured monitor");
    }
    public static void main(String[] args){
        highDamageAndCooldown();
        sustainedAttackAndRisk();
        recoveryAndReset();
        System.out.println("Nexus Combat Alerts: 3 test groups passed");
    }
}
