package com.port.nexusclient;

public final class NexusAwakeningPolicyTest {
    private static void ok(boolean value,String reason){
        if(!value)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        var critical=new NexusAwakeningPolicy.Reading(5,20,-1,false);
        var safe=new NexusAwakeningPolicy.Reading(19,20,80,true);
        var unknown=new NexusAwakeningPolicy.Reading(Double.NaN,20,-1,false);
        ok(NexusAwakeningPolicy.healthPercent(critical)==25,"critical HP");
        ok(NexusAwakeningPolicy.risk(critical,35)==NexusAwakeningPolicy.Risk.CRITICAL,
            "critical classification");
        ok(NexusAwakeningPolicy.risk(safe,35)==NexusAwakeningPolicy.Risk.STABLE,
            "stable classification");
        ok(NexusAwakeningPolicy.risk(unknown,35)==NexusAwakeningPolicy.Risk.UNKNOWN,
            "unavailable health is UNKNOWN, not stable");
        ok(!NexusAwakeningPolicy.holdOffense(false,critical,35),
            "default disabled safety never interrupts existing macros");
        ok(NexusAwakeningPolicy.holdOffense(true,critical,35),
            "opt-in protection suppresses offense at critical health");
        ok(!NexusAwakeningPolicy.holdOffense(true,unknown,35),
            "unknown telemetry cannot trigger protection");
        ok(!NexusAwakeningPolicy.releaseOffense(true,
            new NexusAwakeningPolicy.Reading(39,100,-1,false),35),
            "5 percent hysteresis prevents flicker");
        ok(NexusAwakeningPolicy.releaseOffense(true,
            new NexusAwakeningPolicy.Reading(42,100,-1,false),35),
            "protection releases above threshold plus hysteresis");
        ok(NexusAwakeningPolicy.manaDescription(unknown).contains("sem leitura"),
            "unknown mana must not be invented");
        ok(NexusAwakeningPolicy.manaDescription(safe).contains("80%"),
            "known mana summary");
        ok("report".equals(NexusAwakeningPolicy.voiceCommand(
            "Nexus, situação atual!")),"PT wake-word report");
        ok("report".equals(NexusAwakeningPolicy.voiceCommand(
            "Nexo avaliar situação")),"alternate wake-word report");
        ok("memories".equals(NexusAwakeningPolicy.voiceCommand(
            "Nexus, recordar memórias")),"explicit narrated recall");
        ok(NexusAwakeningPolicy.voiceCommand("situação atual")==null,
            "wake word mandatory");
        ok(NexusAwakeningPolicy.voiceCommand("nexus iniciar julgamento divino")==null,
            "existing combat macro phrase protected");
        ok(NexusAwakeningPolicy.voiceCommand("nexus cancelar")==null,
            "existing cancel command protected");
        System.out.println("NexusAwakeningPolicyTest OK");
    }
}
