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
            new NexusVoiceGrammar.Spell("irons_spellbooks:healing_circle",9));
        var index=NexusVoiceGrammar.phrases(spells);
        var sun=NexusVoiceGrammar.recognize("Nexus, execute Sunbeam!",index);
        check(sun!=null&&!sun.cancel()&&sun.slot()==2,
            "Requested equipped Sunbeam must select its exact QuickCast slot");
        var withArticle=NexusVoiceGrammar.recognize("Nexus, execute o Sunbeam!",index);
        check(withArticle!=null&&!withArticle.cancel()&&withArticle.slot()==2,
            "Brazilian Portuguese article in spoken command");
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
        check(!duplicate.containsKey("nexus execute sunbeam"),
            "Ambiguous same-name spells fail closed");
        System.out.println("NexusVoiceGrammarTest OK");
    }
}
