package com.port.nexusclient;

import java.util.List;

public final class NexusVoiceGrammarTest {
    private static void check(boolean expression,String reason){
        if(!expression)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        List<NexusVoiceGrammar.Spell> spells=List.of(
            new NexusVoiceGrammar.Spell("irons_spellbooks:sunbeam",2),
            new NexusVoiceGrammar.Spell("irons_spellbooks:fire_bolt",7),
            new NexusVoiceGrammar.Spell("irons_spellbooks:healing_circle",9),
            new NexusVoiceGrammar.Spell("irons_spellbooks:divine_smite",11),
            new NexusVoiceGrammar.Spell("moddedspells:arcane_vortex",13));
        var index=NexusVoiceGrammar.phrases(spells);
        var sun=NexusVoiceGrammar.recognize("Nexus, execute Sunbeam!",index);
        check(sun!=null&&!sun.cancel()&&sun.slot()==2,
            "Requested equipped Sunbeam must select its exact QuickCast slot");
        // Dynamic English names are derived from equipped registry IDs.
        var smite=NexusVoiceGrammar.recognize("Nexus, Divine Smite",index);
        check(smite!=null&&smite.spellId().equals("irons_spellbooks:divine_smite")
            &&smite.slot()==11,"Bare wake-word + English spell name uses equipped slot");
        var smiteAccent=NexusVoiceGrammar.recognize("Néxus, divine smite!",index);
        check(smiteAccent!=null&&smiteAccent.slot()==11,"Accent and punctuation");
        var fireDirect=NexusVoiceGrammar.recognize("Nexus, fire bolt",index);
        check(fireDirect!=null&&fireDirect.slot()==7,"Underscore spell IDs automatically map to English spoken names");
        var unknownMod=NexusVoiceGrammar.recognize("Nexus, Arcane Vortex",index);
        check(unknownMod!=null&&unknownMod.slot()==13,
            "New modded spells should not require manually translated aliases");
        var withArticle=NexusVoiceGrammar.recognize("Nexus, execute o Sunbeam!",index);
        check(withArticle!=null&&!withArticle.cancel()&&withArticle.slot()==2,
            "Brazilian Portuguese article in spoken command");
        var portuguese=NexusVoiceGrammar.recognize("Nexus, execute o raio solar!",index);
        check(portuguese!=null&&portuguese.spellId().endsWith(":sunbeam"),
            "Portuguese Sunbeam alias works even if English words are absent from PT model");
        var sunVariant=NexusVoiceGrammar.recognize("Néxus, executa sun beam",index);
        check(sunVariant!=null&&sunVariant.spellId().endsWith(":sunbeam"),
            "Portuguese wake phrase and spaced English spell name");
        var healed=NexusVoiceGrammar.recognize("Nexus lançar Healing Circle",index);
        check(healed!=null&&healed.slot()==9,"Normalize Portuguese accents");
        var fire=NexusVoiceGrammar.recognize("Nexus cast fire bolt",index);
        check(fire!=null&&fire.slot()==7,"Cast alias resolves from equipped spell id");
        check(NexusVoiceGrammar.recognize("Sunbeam",index)==null,
            "Never cast without Nexus wake word and action");
        check(NexusVoiceGrammar.recognize("Nexus, conte sobre Sunbeam",index)==null,
            "Conversation with Nexus is not a cast instruction");
        check(NexusVoiceGrammar.recognize("Nexus, execute banished_soul",index)==null,
            "Unowned spells are not castable");
        check(NexusVoiceGrammar.recognize("Nexus execute sunbeam depois ataque",index)==null,
            "Never fuzzy-match extra instructions");
        var cancel=NexusVoiceGrammar.recognize("Nexus, cancelar!",index);
        check(cancel!=null&&cancel.cancel()&&cancel.slot()==-1,
            "Cancel is distinct from a spell and never has a QuickCast slot");
        check(NexusVoiceGrammar.recognize("Nexus, cancele",index).cancel(),
            "Alternative cancellation");
        check(NexusVoiceGrammar.recognize("cancelar",index)==null,
            "No cancellation without wake word");
        check(NexusVoiceGrammar.vocabulary(index).contains("[unk]"),
            "Closed Vosk grammar includes unknown noise exit");
        var duplicate=NexusVoiceGrammar.phrases(List.of(
            new NexusVoiceGrammar.Spell("one:sunbeam",0),
            new NexusVoiceGrammar.Spell("two:sunbeam",1)));
        check(!duplicate.containsKey("nexus execute sunbeam")&&!duplicate.containsKey("nexus sunbeam"),
            "Ambiguous same-name spells fail closed");

        var repeatedSameSpell=NexusVoiceGrammar.phrases(List.of(
            new NexusVoiceGrammar.Spell("irons_spellbooks:divine_smite",1),
            new NexusVoiceGrammar.Spell("irons_spellbooks:divine_smite",4)));
        check(!repeatedSameSpell.containsKey("nexus divine smite"),
            "Duplicate spell on different slots must be ambiguous");
        check(NexusVoiceGrammar.recognize("Nexus Divine Smite depois cura",index)==null,
            "Unknown suffix cannot trigger spells");
        check(NexusVoiceGrammar.recognize("Divine Smite",index)==null,
            "Wake word remains mandatory");
        System.out.println("NexusVoiceGrammarTest OK");
    }
}
