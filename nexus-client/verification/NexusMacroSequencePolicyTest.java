package com.port.nexusclient;

import java.util.List;

public final class NexusMacroSequencePolicyTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        var equipped=List.of("irons_spellbooks:divine_smite",
            "irons_spellbooks:heal","somake:blessing");
        var current=List.of("irons_spellbooks:divine_smite");
        check(NexusMacroSequencePolicy.validSequence(current,current.getFirst()),
            "A single-spell legacy macro is a valid ordered sequence.");
        check(NexusMacroSequencePolicy.canAppend("irons_spellbooks:heal",
            current,equipped),
            "A second equipped spell may be appended to the SAME macro.");
        check(NexusMacroSequencePolicy.validSequence(List.of(
            "irons_spellbooks:divine_smite","irons_spellbooks:heal",
            "somake:blessing"),current.getFirst()),
            "Three different spells preserve the trigger macro's identity.");
        check(!NexusMacroSequencePolicy.canAppend("somake:not_equipped",
            current,equipped),"Un-equipped spells cannot be appended.");
        check(!NexusMacroSequencePolicy.canAppend("irons_spellbooks:divine_smite",
            current,equipped),"Duplicate spells rejected within one macro.");
        check(!NexusMacroSequencePolicy.validSequence(List.of(
            "irons_spellbooks:divine_smite","irons_spellbooks:divine_smite"),
            current.getFirst()),"Duplicates rejected in saved configuration.");
        check(!NexusMacroSequencePolicy.validSequence(List.of("somake:blessing"),
            "irons_spellbooks:divine_smite"),"First spell must be macro anchor.");
        check(!NexusMacroSequencePolicy.validSequence(List.of(
            "irons_spellbooks:divine_smite","invalid"),current.getFirst()),
            "Reject malformed IDs from user config.");
        check(!NexusMacroSequencePolicy.canAppend("somake:blessing",
            List.of("one:a","two:b","three:c","four:d","five:e","six:f"),
            equipped),"No more than six spells per combo.");
        check(!NexusMacroSequencePolicy.canRemove("irons_spellbooks:divine_smite",
            List.of("irons_spellbooks:divine_smite","irons_spellbooks:heal")),
            "Do not remove macro anchor without deleting entire macro.");
        check(NexusMacroSequencePolicy.canRemove("irons_spellbooks:heal",
            List.of("irons_spellbooks:divine_smite","irons_spellbooks:heal")),
            "Extra spells may be removed independently.");
        check(NexusMacroSequencePolicy.canRequest(10_000,0,-1,6_000,false),
            "May request first spell immediately.");
        check(!NexusMacroSequencePolicy.canRequest(11_000,16_000,
            10_000,6_000,false),"Do not cast the next spell on the same tick.");
        check(!NexusMacroSequencePolicy.canRequest(16_001,16_000,
            10_000,6_000,true),"Do not interrupt a spell currently casting.");
        check(NexusMacroSequencePolicy.canRequest(16_001,16_000,
            10_000,6_000,false),"May request the next spell after the shared interval.");
        check(NexusMacroSequencePolicy.DEFAULT_INTERVAL_SECONDS==6,
            "Macros antigos preservam intervalo padrão de seis segundos.");
        check(NexusMacroSequencePolicy.normalizedInterval(0)==6,
            "Um campo legado ausente deve migrar para seis segundos.");
        check(NexusMacroSequencePolicy.intervalMs(2)==2_000L,
            "Intervalo configurado de dois segundos deve ter efeito real.");
        check(NexusMacroSequencePolicy.intervalMs(3)==3_000L,
            "Intervalo configurado de três segundos deve ter efeito real.");
        check(NexusMacroSequencePolicy.intervalMs(30)==30_000L,
            "Intervalo máximo de trinta segundos deve ser aceito.");
        check(!NexusMacroSequencePolicy.validInterval(1)&&
            !NexusMacroSequencePolicy.validInterval(31),
            "Intervalos fora do limite são rejeitados.");
        check(NexusMacroSequencePolicy.validInterval(2)&&
            NexusMacroSequencePolicy.validInterval(30),
            "Extremos do intervalo são válidos.");
        check(!NexusMacroSequencePolicy.canRequest(12_900,13_000,
            10_000,2_000,false),
            "Três segundos configurados nunca autorizam lançamento antecipado.");
        check(NexusMacroSequencePolicy.canRequest(13_000,13_000,
            10_000,2_000,false),
            "Sequência pode avançar após três segundos mesmo com antigo padrão 6s.");
        check(!NexusMacroSequencePolicy.canRequest(13_000,13_000,
            10_000,6_000,false),
            "Proteção antiga de 6s seria bloqueio e não deve ser usada na fila.");
        check(!NexusMacroSequencePolicy.expired(15_000,10_000),
            "Short queued sequence remains active.");
        check(!NexusMacroSequencePolicy.expired(160_000,10_000),
            "Six spells at the configured 30s gap must fit within sequence lifetime.");
        check(NexusMacroSequencePolicy.expired(221_000,10_000),
            "Stale queued cast must expire after safe maximum lifetime.");
        System.out.println("NexusMacroSequencePolicyTest OK");
    }
}
