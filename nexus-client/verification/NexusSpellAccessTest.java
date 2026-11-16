package com.port.nexusclient;

import java.util.List;

public final class NexusSpellAccessTest {
    public static final class Spell {
        private final String id;
        public Spell(String id){this.id=id;}
        public String getSpellId(){return id;}
    }
    public static final class Data {
        private final Spell spell;
        private final boolean locked;
        public Data(String id,boolean locked){spell=new Spell(id);this.locked=locked;}
        public Spell getSpell(){return spell;}
        public boolean isLocked(){return locked;}
    }
    public static final class Manager {
        private final List<Data> data=List.of(
            new Data("irons_spellbooks:sunbeam",false),
            new Data("irons_spellbooks:heal",false),
            new Data("irons_spellbooks:divine_smite",false),
            new Data("irons_spellbooks:locked",true),
            new Data("irons_spellbooks:none",false));
        // Exactly the real 1.21.1 API. There is no getSpellCount() here.
        public List<Data> getAllSpells(){return data;}
        public Data getSpellData(int index){return data.get(index);}
    }
    private static void check(boolean value,String reason){
        if(!value)throw new AssertionError(reason);
    }
    public static void main(String[] args)throws Exception{
        var spells=NexusSpellAccess.listEquipped(new Manager(),Data.class,Spell.class);
        check(spells.size()==3,"Three castable slots remain after filtering");
        check(spells.get(0).id().equals("irons_spellbooks:sunbeam")&&spells.get(0).slot()==0,
            "Voice command can find Sunbeam at the authoritative wheel index");
        check(spells.get(1).id().equals("irons_spellbooks:heal")&&spells.get(1).slot()==1,
            "Automatic Heal uses the same fixed spell index");
        check(spells.get(2).id().equals("irons_spellbooks:divine_smite")&&spells.get(2).slot()==2,
            "Divine Smite macro uses the same fixed spell index");
        var phrase=NexusVoiceGrammar.phrases(spells);
        check(NexusVoiceGrammar.recognize("Nexus execute Sunbeam",phrase).slot()==0,
            "End-to-end spell index -> grammar");
        System.out.println("NexusSpellAccessTest OK");
    }
}
