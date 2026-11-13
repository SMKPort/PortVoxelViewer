package com.port.nexusclient;

public final class NexusArcaneRulesTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(NexusArcaneRules.percent(25,100)==25.0,"percent");
        check(NexusArcaneRules.percent(0,0)==-1,"missing max is not invented");
        check(NexusArcaneRules.percent(Double.NaN,100)==-1,"invalid mana");
        check(NexusArcaneRules.percent(300,100)==100,"clamp excessive mana");
        check(NexusArcaneRules.manaLow(21)&&!NexusArcaneRules.manaLow(-1),"low mana only when known");
        check(NexusArcaneRules.manaRecovered(38)&&!NexusArcaneRules.manaRecovered(30),"recovery hysteresis");
        check(NexusArcaneRules.spellLabel("irons_spellbooks:divine_smite").equals("Divine smite"),"spell title");
        check(NexusArcaneRules.spellLabel("../../x").equals("Feitiço não identificado"),"invalid id");
        check(NexusArcaneRules.sameSpell("abc:SPELL","abc:spell"),"spell id");
        check(NexusArcaneRules.eventTitle("cast_end","abc:fireball","fire",1).contains("encerrada"),"no cast success claim");
        System.out.println("Nexus Arcane Rules: tests passed");
    }
}
