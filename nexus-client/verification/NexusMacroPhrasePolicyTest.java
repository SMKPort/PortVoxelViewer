package com.port.nexusclient;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NexusMacroPhrasePolicyTest {
    private static void check(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    public static void main(String[] args){
        check("iniciar julgamento divino".equals(NexusMacroPhrasePolicy.activation(
            "Nexus, Iniciar Julgamento Divino!")),"Wake word stripped on registration");
        check(NexusMacroPhrasePolicy.validActivation("Iniciar Julgamento Divino"),
            "Allow distinct human phrase");
        check(!NexusMacroPhrasePolicy.validActivation("cancelar"),
            "Cancel word reserved for emergency spell cancel");
        check(!NexusMacroPhrasePolicy.validActivation("Nexus cancelar"),
            "Wrapped cancel phrase reserved");
        check(!NexusMacroPhrasePolicy.validActivation("viajar para casa"),
            "Waystone vocabulary protected");
        check(!NexusMacroPhrasePolicy.validActivation("abc"),
            "Reject tiny accidental activations");
        check(!NexusMacroPhrasePolicy.validActivation("a".repeat(80)),
            "Reject excessive trigger phrases");
        check(NexusMacroPhrasePolicy.validAnnouncement("Iniciando julgamento divino."),
            "Allow start announcement");
        check(NexusMacroPhrasePolicy.validAnnouncement(""),
            "Allow announcements to be silenced");
        check(!NexusMacroPhrasePolicy.validAnnouncement("b".repeat(170)),
            "Cap announcements");
        check("Primeira linha segunda linha".equals(
            NexusMacroPhrasePolicy.announcement("Primeira linha\nsegunda linha")),
            "Sanitize control characters for HUD/chat");
        Map<String,String> macros=new LinkedHashMap<>();
        macros.put("irons_spellbooks:divine_smite","Iniciar Julgamento Divino");
        macros.put("irons_spellbooks:heal","Ativar Cura Suprema");
        Map<String,String> index=NexusMacroPhrasePolicy.index(macros);
        check("irons_spellbooks:divine_smite".equals(
            NexusMacroPhrasePolicy.interpret("Nexus, iniciar julgamento divino",index)),
            "Exact custom phrase activates correct macro");
        check("irons_spellbooks:divine_smite".equals(
            NexusMacroPhrasePolicy.interpret("Nexo iniciar julgamento divino",index)),
            "Accept known wake word variant");
        check("irons_spellbooks:heal".equals(
            NexusMacroPhrasePolicy.interpret("Nexus, Ativar Cura Suprema",index)),
            "Second macro remains independently addressable");
        check(NexusMacroPhrasePolicy.interpret("ativar cura suprema",index)==null,
            "No activation without explicit wake word");
        check(NexusMacroPhrasePolicy.interpret("Nexus cancelar",index)==null,
            "Cancel command cannot accidentally activate macro");
        check(NexusMacroPhrasePolicy.interpret("Nexus iniciar julgamento",index)==null,
            "No fuzzy or partial activation");
        macros.put("mod:duplicate","Iniciar Julgamento Divino");
        Map<String,String> collisions=NexusMacroPhrasePolicy.index(macros);
        check(NexusMacroPhrasePolicy.interpret(
            "Nexus iniciar julgamento divino",collisions)==null,
            "Ambiguous macro phrase fails closed");
        System.out.println("NexusMacroPhrasePolicyTest OK");
    }
}
