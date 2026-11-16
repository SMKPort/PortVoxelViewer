package com.port.nexusclient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection adapter for the Iron's Spells 1.21.1 public spell wheel API.
 * No mod dependency in bytecode; safe to test without Minecraft installed.
 */
final class NexusSpellAccess {
    private NexusSpellAccess(){}

    static List<NexusVoiceGrammar.Spell> listEquipped(Object spellManager,
        Class<?> dataType,Class<?> spellType)throws ReflectiveOperationException{
        Object value=spellManager.getClass().getMethod("getAllSpells").invoke(spellManager);
        if(!(value instanceof List<?> options))
            throw new IllegalStateException("Iron's Spells getAllSpells não é uma lista.");
        Method at=spellManager.getClass().getMethod("getSpellData",int.class);
        Method getSpell=dataType.getMethod("getSpell");
        Method getId=spellType.getMethod("getSpellId");
        Method isLocked=dataType.getMethod("isLocked");
        List<NexusVoiceGrammar.Spell> out=new ArrayList<>();
        for(int index=0;index<Math.min(128,options.size());index++){
            Object data=at.invoke(spellManager,index);
            if(data==null||Boolean.TRUE.equals(isLocked.invoke(data)))continue;
            Object spell=getSpell.invoke(data);
            if(spell==null)continue;
            String id=String.valueOf(getId.invoke(spell));
            if(id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")&&!id.endsWith(":none"))
                out.add(new NexusVoiceGrammar.Spell(id,index));
        }
        return List.copyOf(out);
    }
}
